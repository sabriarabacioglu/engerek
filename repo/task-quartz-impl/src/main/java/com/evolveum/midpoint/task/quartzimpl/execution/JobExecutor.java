/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.Validate;
import org.quartz.*;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@DisallowConcurrentExecution
public class JobExecutor implements InterruptableJob {

	private static TaskManagerQuartzImpl taskManagerImpl;
    private static final String DOT_CLASS = JobExecutor.class.getName() + ".";

    /*
     * Ugly hack - this class is instantiated not by Spring but explicity by Quartz.
     */
	public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl tmqi) {
		taskManagerImpl = tmqi;
	}
	
	private static final transient Trace LOGGER = TraceManager.getTrace(JobExecutor.class);

    private static final long WATCHFUL_SLEEP_INCREMENT = 500;
	
	/*
	 * JobExecutor is instantiated at each execution of the task, so we can store
	 * the task here.
	 * 
	 * http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03
	 * "Each (and every) time the scheduler executes the job, it creates a new instance of 
	 * the class before calling its execute(..) method."
	 */
	private volatile TaskQuartzImpl task;
	private volatile Thread executingThread;				// used for interruptions
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		OperationResult executionResult = createOperationResult("execute");

        if (taskManagerImpl == null) {
            LOGGER.error("TaskManager not correctly set for JobExecutor, exiting the execution routine.");
            return;
        }

		// get the task instance
		String oid = context.getJobDetail().getKey().getName();
        try {
			task = (TaskQuartzImpl) taskManagerImpl.getTask(oid, executionResult);
		} catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Task with OID {} no longer exists, removing Quartz job and exiting the execution routine.", e, oid);
            taskManagerImpl.getExecutionManager().removeTaskFromQuartz(oid, executionResult);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Task with OID {} cannot be retrieved because of schema exception. Please correct the problem or resynchronize midPoint repository with Quartz job store using 'xxxxxxx' function. Now exiting the execution routine.", e, oid);
            return;
        } catch (Exception e) {
			LoggingUtils.logException(LOGGER, "Task with OID {} could not be retrieved, exiting the execution routine.", e, oid);
			return;
		}

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            LOGGER.warn("Task is not in RUNNABLE state (its state is {}), exiting its execution and removing its Quartz trigger. Task = {}", task.getExecutionStatus(), task);
            try {
                context.getScheduler().unscheduleJob(context.getTrigger().getKey());
            } catch (SchedulerException e) {
                LoggingUtils.logException(LOGGER, "Cannot unschedule job for a non-RUNNABLE task {}", e, task);
            }
            return;
        }

        // if task manager is stopping or stopped, stop this task immediately
        // this can occur in rare situations, see https://jira.evolveum.com/browse/MID-1167
        if (!taskManagerImpl.isRunning()) {
            LOGGER.warn("Task was started while task manager is not running: exiting and rescheduling (if needed)");
            processTaskStop(executionResult);
            return;
        }

        boolean isRecovering = false;

        // if this is a restart, check whether the task is resilient
        if (context.isRecovering()) {

            // reset task node (there's potentially the old information from crashed node)
            try {
                if (task.getNode() != null) {
                    task.setNodeImmediate(null, executionResult);
                }
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Cannot reset executing-at-node information for recovering task {}", e, task);
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Cannot reset executing-at-node information for recovering task {}", e, task);
            }

            if (!processTaskRecovery(executionResult)) {
                return;
            }
            isRecovering = true;
        }
		
        executingThread = Thread.currentThread();

        if (LOGGER.isTraceEnabled()) {
		    LOGGER.trace("execute called; task = {}, thread = {}, isRecovering = {}", new Object[] {task, executingThread, isRecovering});
        }

        TaskHandler handler = null;
		try {

            taskManagerImpl.registerRunningTask(task);
        
			handler = taskManagerImpl.getHandler(task.getHandlerUri());
            logThreadRunStart(handler);
            taskManagerImpl.notifyTaskThreadStart(task, isRecovering);
		
			if (handler==null) {
				LOGGER.error("No handler for URI '{}', task {} - closing it.", task.getHandlerUri(), task);
                executionResult.recordFatalError("No handler for URI '" + task.getHandlerUri() + "', closing the task.");
                closeFlawedTask(task, executionResult);
                return;
				//throw new JobExecutionException("No handler for URI '" + task.getHandlerUri() + "'");
                // actually there is no point in throwing JEE; the only thing that is done with it is
                // that it is logged by Quartz
			}
			
			// Setup Spring Security context
			PrismObject<UserType> taskOwner = task.getOwner();
			taskManagerImpl.getSecurityEnforcer().setupPreAuthenticatedSecurityContext(taskOwner);
		
			if (task.isCycle()) {
				executeRecurrentTask(handler);
			} else if (task.isSingle()) {
				executeSingleTask(handler, executionResult);
			} else {
				LOGGER.error("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
                executionResult.recordFatalError("Tasks must be either recurrent or single-run. This one is neither. Closing it.");
                closeFlawedTask(task, executionResult);
			}
		
		} finally {
            taskManagerImpl.unregisterRunningTask(task);
            executingThread = null;

            if (!task.canRun()) {
                processTaskStop(executionResult);
            }

			logThreadRunFinish(handler);
            taskManagerImpl.notifyTaskThreadFinish(task);
		}

	}

    // returns true if the execution of the task should continue
    private boolean processTaskRecovery(OperationResult executionResult) {
        if (task.getThreadStopAction() == ThreadStopActionType.CLOSE) {
            LOGGER.info("Closing recovered non-resilient task {}", task);
            closeTask(task, executionResult);
            return false;
        } else if (task.getThreadStopAction() == ThreadStopActionType.SUSPEND) {
            LOGGER.info("Suspending recovered non-resilient task {}", task);
            taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);        // we must NOT wait here, as we would wait infinitely -- we do not have to stop the task neither, because we are that task :)
            return false;
        } else if (task.getThreadStopAction() == null || task.getThreadStopAction() == ThreadStopActionType.RESTART) {
            LOGGER.info("Recovering resilient task {}", task);
            return true;
        } else if (task.getThreadStopAction() == ThreadStopActionType.RESCHEDULE) {
            if (task.getRecurrenceStatus() == TaskRecurrence.RECURRING && task.isLooselyBound()) {
                LOGGER.info("Recovering resilient task with RESCHEDULE thread stop action - exiting the execution, the task will be rescheduled; task = {}", task);
                return false;
            } else {
                LOGGER.info("Recovering resilient task {}", task);
                return true;
            }
        } else {
            throw new SystemException("Unknown value of ThreadStopAction: " + task.getThreadStopAction() + " for task " + task);
        }
    }

    // called when task is externally stopped (can occur on node shutdown, node scheduler stop, node threads deactivation, or task suspension)
    // we have to act (i.e. reschedule resilient tasks or close/suspend non-resilient tasks) in all cases, except task suspension
    // we recognize it by looking at task status: RUNNABLE means that the task is stopped as part of node shutdown
    private void processTaskStop(OperationResult executionResult) {

        try {
            task.refresh(executionResult);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "ThreadStopAction cannot be applied, because the task no longer exists: " + task, e);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "ThreadStopAction cannot be applied, because of schema exception. Task = " + task, e);
            return;
        }

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            LOGGER.trace("processTaskStop: task execution status is not RUNNABLE (it is " + task.getExecutionStatus() + "), so ThreadStopAction does not apply; task = " + task);
            return;
        }

        if (task.getThreadStopAction() == ThreadStopActionType.CLOSE) {
            LOGGER.info("Closing non-resilient task on node shutdown; task = {}", task);
            closeTask(task, executionResult);
        } else if (task.getThreadStopAction() == ThreadStopActionType.SUSPEND) {
            LOGGER.info("Suspending non-resilient task on node shutdown; task = {}", task);
            taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);            // we must NOT wait here, as we would wait infinitely -- we do not have to stop the task neither, because we are that task
        } else if (task.getThreadStopAction() == null || task.getThreadStopAction() == ThreadStopActionType.RESTART) {
            LOGGER.info("Node going down: Rescheduling resilient task to run immediately; task = {}", task);
            taskManagerImpl.scheduleRunnableTaskNow(task, executionResult);
        } else if (task.getThreadStopAction() == ThreadStopActionType.RESCHEDULE) {
            if (task.getRecurrenceStatus() == TaskRecurrence.RECURRING && task.isLooselyBound()) {
                ; // nothing to do, task will be automatically started by Quartz on next trigger fire time
            } else {
                taskManagerImpl.scheduleRunnableTaskNow(task, executionResult);     // for tightly-bound tasks we do not know next schedule time, so we run them immediately
            }
        } else {
            throw new SystemException("Unknown value of ThreadStopAction: " + task.getThreadStopAction() + " for task " + task);
        }
    }

    private void closeFlawedTask(TaskQuartzImpl task, OperationResult result) {
        LOGGER.info("Closing flawed task {}", task);
        try {
            task.setResultImmediate(result, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        }
        closeTask(task, result);
    }

    private void closeTask(TaskQuartzImpl task, OperationResult result) {
        try {
            taskManagerImpl.closeTask(task, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot close task {}, because it does not exist in repository.", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Cannot close task {} due to schema exception", e, task);
        } catch (SystemException e) {
            LoggingUtils.logException(LOGGER, "Cannot close task {} due to system exception", e, task);
        }
    }

	private void executeSingleTask(TaskHandler handler, OperationResult executionResult) throws JobExecutionException {

        Validate.notNull(handler, "Task handler is null");

		try {
			
			RepositoryCache.enter();
			TaskRunResult runResult = null;

			recordCycleRunStart(executionResult, handler);
            runResult = executeHandler(handler);        // exceptions thrown by handler are handled in this method

            // we should record finish-related information before dealing with (potential) task closure/restart
            // so we place this method call before the following block
            recordCycleRunFinish(runResult, handler, executionResult);

            // should be after recordCycleRunFinish, e.g. not to overwrite task result
            task.refresh(executionResult);

            // let us treat various exit situations here...

            if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
                // first, if a task was interrupted, we do not want to change its status
                LOGGER.trace("Task was interrupted, exiting the execution routine. Task = {}", task);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
                // in case of temporary error, we want to suspend the task and exit
                LOGGER.info("Task encountered temporary error, suspending it. Task = {}", task);
                taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
                // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
                // this is implemented by pushHandler and by Quartz
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
                // PERMANENT ERROR means we do not continue executing other handlers, we just close this task
                taskManagerImpl.closeTask(task, executionResult);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED || runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
                // FINISHED/FINISHED_HANDLER means we continue with other handlers, if there are any
                task.finishHandler(executionResult);			// this also closes the task, if there are no remaining handlers
                // if there are remaining handlers, task will be re-executed by Quartz
            } else {
                throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
            }

		} catch (Throwable t) {
			LoggingUtils.logException(LOGGER, "An exception occurred during processing of task {}", t, task);
			//throw new JobExecutionException("An exception occurred during processing of task " + task, t);
		} finally {
			RepositoryCache.exit();
		}
	}
	
	private void executeRecurrentTask(TaskHandler handler) throws JobExecutionException {

		try {

mainCycle:

			while (task.canRun()) {

                // executionResult should be initialized here (inside the loop), because for long-running tightly-bound
                // recurring tasks it would otherwise bloat indefinitely
                OperationResult executionResult = createOperationResult("executeTaskRun");

                if (!task.stillCanStart()) {
                    LOGGER.trace("CycleRunner loop: task latest start time ({}) has elapsed, exiting the execution cycle. Task = {}", task.getSchedule().getLatestStartTime(), task);
                    break;
                }
				
				LOGGER.trace("CycleRunner loop: start");

				RepositoryCache.enter();

                TaskRunResult runResult;
                try {
				    recordCycleRunStart(executionResult, handler);
				    runResult = executeHandler(handler);
				    boolean canContinue = recordCycleRunFinish(runResult, handler, executionResult);
                    if (!canContinue) { // in case of task disappeared
                        break;
                    }
                } finally {
    				RepositoryCache.exit();
                }

                // let us treat various exit situations here...

                if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
                    // first, if a task was interrupted, we do not want to change its status
                    LOGGER.trace("Task was interrupted, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
                    LOGGER.trace("Task encountered temporary error, continuing with the execution cycle. Task = {}", task);
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
                    // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
                    // this is implemented by pushHandler and by Quartz
                    LOGGER.trace("Task returned RESTART_REQUESTED state, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
                    LOGGER.info("Task encountered permanent error, suspending the task. Task = {}", task);
                    taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED) {
                    LOGGER.trace("Task handler finished, continuing with the execution cycle. Task = {}", task);
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
                    LOGGER.trace("Task handler finished with FINISHED_HANDLER, calling task.finishHandler() and exiting the execution cycle. Task = {}", task);
                    task.finishHandler(executionResult);			// this also closes the task, if there are no remaining handlers
                                                                    // if there are remaining handlers, task will be re-executed by Quartz
                    break;
                } else {
                    throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
                }

                // if the task is loosely-bound, exit the loop here
				if (task.isLooselyBound()) {
					LOGGER.trace("CycleRunner loop: task is loosely bound, exiting the execution cycle");
					break;
				}

                // or, was the task suspended (closed, ...) remotely?
                LOGGER.trace("CycleRunner loop: refreshing task after one iteration, task = {}", task);
                try {
                    task.refresh(executionResult);
                } catch (ObjectNotFoundException ex) {
                    LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
                    return;			// The task object in repo is gone. Therefore this task should not run any more.
                }

                if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                    LOGGER.info("Task not in the RUNNABLE state, exiting the execution routing. State = {}, Task = {}", task.getExecutionStatus(), task);
                    break;
                }

                // Determine how long we need to sleep and hit the bed

				Integer interval = task.getSchedule() != null ? task.getSchedule().getInterval() : null;
				if (interval == null) {
					LOGGER.error("Tightly bound task " + task + " has no scheduling interval specified.");
					break;
				}

                long lastRunStartTime = task.getLastRunStartTimestamp() == null ? 0 : task.getLastRunStartTimestamp();
				long sleepFor = lastRunStartTime + (interval.intValue() * 1000) - System.currentTimeMillis();
				if (sleepFor < 0)
					sleepFor = 0;
				
				LOGGER.trace("CycleRunner loop: sleep ({})", sleepFor);

                for (long time = 0; time < sleepFor + WATCHFUL_SLEEP_INCREMENT; time += WATCHFUL_SLEEP_INCREMENT) {
                    try {
                        Thread.sleep(WATCHFUL_SLEEP_INCREMENT);
                    } catch (InterruptedException e) {
                        // safely ignored
                    }
                    if (!task.canRun()) {
                        LOGGER.trace("CycleRunner loop: sleep interrupted, task.canRun == false");
                        break mainCycle;
                    }
                }

                LOGGER.trace("CycleRunner loop: refreshing task after sleep, task = {}", task);
				try {
					task.refresh(executionResult);
				} catch (ObjectNotFoundException ex) {
					LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
					return;			// The task object in repo is gone. Therefore this task should not run any more. Therefore commit seppuku
				}

                if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                    LOGGER.info("Task not in the RUNNABLE state, exiting the execution routine. State = {}, Task = {}", task.getExecutionStatus(), task);
                    break;
                }

                LOGGER.trace("CycleRunner loop: end");
			}

		} catch (Throwable t) {
			// This is supposed to run in a thread, so this kind of heavy artillery is needed. If throwable won't be
			// caught here, nobody will catch it and it won't even get logged.
			if (task.canRun()) {
				LOGGER.error("CycleRunner got unexpected exception: {}: {}; task = {}",new Object[] { t.getClass().getName(),t.getMessage(),task,t});
			} else {
				LOGGER.info("CycleRunner got unexpected exception while shutting down: {}: {}; task = {}",new Object[] { t.getClass().getName(),t.getMessage(), task});
				LOGGER.trace("CycleRunner got unexpected exception while shutting down: {}: {}; task = {}",new Object[] { t.getClass().getName(),t.getMessage(),task,t});
			}
			//throw new JobExecutionException("An exception occurred during processing of task " + task, t);
		}
	}
	
    private TaskRunResult executeHandler(TaskHandler handler) {

    	TaskRunResult runResult;
    	try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Executing handler " + handler.getClass().getName());
            }
            if (task.getResult() == null) {
                task.setResult(new OperationResult("run"));
            }
    		runResult = handler.run(task);
    		if (runResult == null) {				// Obviously an error in task handler
                LOGGER.error("Unable to record run finish: task returned null result");
				return createFailureTaskRunResult("Unable to record run finish: task returned null result", null);
			} else {
    		    return runResult;
            }
    	} catch (Throwable t) {
			LOGGER.error("Task handler threw unexpected exception: {}: {}; task = {}", new Object[] { t.getClass().getName(), t.getMessage(), task, t});
            return createFailureTaskRunResult("Task handler threw unexpected exception: " + t.getMessage(), t);
    	}
	}

    private TaskRunResult createFailureTaskRunResult(String message, Throwable t) {
        TaskRunResult runResult = new TaskRunResult();
        OperationResult opResult;
        if (task.getResult() != null) {
            opResult = task.getResult();
        } else {
            opResult = createOperationResult(DOT_CLASS + "executeHandler");
        }
        if (t != null) {
            opResult.recordFatalError(message, t);
        } else {
            opResult.recordFatalError(message);
        }
        runResult.setOperationResult(opResult);
        runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
        runResult.setProgress(task.getProgress());
        return runResult;
    }


    private OperationResult createOperationResult(String methodName) {
		return new OperationResult(DOT_CLASS + methodName);
	}
	
	private void logThreadRunStart(TaskHandler handler) {
		LOGGER.trace("Task thread run STARTING  " + task + ", handler = " + handler);
	}
	
	private void logThreadRunFinish(TaskHandler handler) {
		LOGGER.trace("Task thread run FINISHED " + task + ", handler = " + handler);
	}
    
	private void recordCycleRunStart(OperationResult result, TaskHandler handler) {
		LOGGER.trace("Task cycle run STARTING " + task + ", handler = " + handler);
        taskManagerImpl.notifyTaskStart(task);
        try {
            task.setLastRunStartTimestamp(System.currentTimeMillis());
            if (task.getCategory() == null) {
                task.setCategory(task.getCategoryFromHandler());
            }
            task.setNode(taskManagerImpl.getNodeId());
            task.savePendingModifications(result);

        } catch (Exception e) {	// TODO: implement correctly after clarification
			LoggingUtils.logException(LOGGER, "Cannot record run start for task {}", e, task);
		}
	}

	/*
	 * Returns a flag whether to continue (false if the task has disappeared)
	 */
	private boolean recordCycleRunFinish(TaskRunResult runResult, TaskHandler handler, OperationResult result) {
		LOGGER.trace("Task cycle run FINISHED " + task + ", handler = " + handler);
        taskManagerImpl.notifyTaskFinish(task, runResult);
		try {
            task.setProgress(runResult.getProgress());
            task.setLastRunFinishTimestamp(System.currentTimeMillis());
            if (runResult.getOperationResult() != null) {
                task.setResult(runResult.getOperationResult());
            }
            task.setNode(null);
            task.savePendingModifications(result);

			return true;
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Cannot record run finish for task {}", ex, task);
			return false;
        } catch (com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException ex) {
            LoggingUtils.logException(LOGGER, "Cannot record run finish for task {}", ex, task);
            return true;
		} catch (SchemaException ex) {
			LOGGER.error("Unable to record run finish and close the task: {}", ex.getMessage(), ex);
			return true;
		}
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		LOGGER.trace("Trying to shut down the task " + task + ", executing in thread " + executingThread);
        if (task != null) {
		    task.signalShutdown();
		    if (taskManagerImpl.getConfiguration().getUseThreadInterrupt() == UseThreadInterrupt.ALWAYS) {
                sendThreadInterrupt();
            }
        }
	}

    public void sendThreadInterrupt() {
        if (executingThread != null) {			// in case this method would be (mistakenly?) called after the execution is over
            LOGGER.trace("Calling Thread.interrupt on thread {}.", executingThread);
            executingThread.interrupt();
            LOGGER.trace("Thread.interrupt was called on thread {}.", executingThread);
        }
    }
}
