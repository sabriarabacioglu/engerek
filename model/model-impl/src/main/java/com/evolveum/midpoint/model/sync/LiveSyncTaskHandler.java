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
package com.evolveum.midpoint.model.sync;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.ModelConstants;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.List;

/**
 * The task hander for a live synchronization.
 * 
 *  This handler takes care of executing live synchronization "runs". It means that the handler "run" method will
 *  be called every few seconds. The responsibility is to scan for changes that happened since the last run.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class LiveSyncTaskHandler implements TaskHandler {
	
	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/live-sync/handler-3";

    @Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	private ProvisioningService provisioningService;
	
	@Autowired(required = true)
    private PrismContext prismContext;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(LiveSyncTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("LiveSyncTaskHandler.run starting");
		
		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(OperationConstants.LIVE_SYNC);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		
		 String resourceOid = task.getObjectOid();
	        if (resourceOid == null) {
	            LOGGER.error("Live Sync: No resource OID specified in the task");
	            opResult.recordFatalError("No resource OID specified in the task");
	            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
	            return runResult;
	        }
	        
	        ResourceType resource = null;
	        try {

	            resource = provisioningService.getObject(ResourceType.class, resourceOid, null, task, opResult).asObjectable();

	        } catch (ObjectNotFoundException ex) {
	            LOGGER.error("Live Sync: Resource {} not found: {}", new Object[]{resourceOid, ex.getMessage(), ex});
	            // This is bad. The resource does not exist. Permanent problem.
	            opResult.recordFatalError("Resource not found " + resourceOid, ex);
	            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
	            return runResult;
	        } catch (SchemaException ex) {
	            LOGGER.error("Live Sync: Error dealing with schema: {}", ex.getMessage(), ex);
	            // Not sure about this. But most likely it is a misconfigured resource or connector
	            // It may be worth to retry. Error is fatal, but may not be permanent.
	            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
	            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
	            return runResult;
	        } catch (RuntimeException ex) {
	            LOGGER.error("Live Sync: Internal Error: {}", ex.getMessage(), ex);
	            // Can be anything ... but we can't recover from that.
	            // It is most likely a programming error. Does not make much sense to retry.
	            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
	            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
	            return runResult;
	        } catch (CommunicationException ex) {
	        	LOGGER.error("Live Sync: Error getting resource {}: {}", new Object[]{resourceOid, ex.getMessage(), ex});
	            opResult.recordFatalError("Error getting resource " + resourceOid+": "+ex.getMessage(), ex);
	            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
	            return runResult;
			} catch (ConfigurationException ex) {
				LOGGER.error("Live Sync: Error getting resource {}: {}", new Object[]{resourceOid, ex.getMessage(), ex});
	            opResult.recordFatalError("Error getting resource " + resourceOid+": "+ex.getMessage(), ex);
	            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
	            return runResult;
			} catch (SecurityViolationException ex) {
				LOGGER.error("Live Sync: Error getting resource {}: {}", new Object[]{resourceOid, ex.getMessage(), ex});
	            opResult.recordFatalError("Error getting resource " + resourceOid+": "+ex.getMessage(), ex);
	            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
	            return runResult;
			}

		
//		ResourceType resource = null;
//        try {
//
//            resource = task.getObject(ResourceType.class, opResult).asObjectable();
//
//        } catch (ObjectNotFoundException ex) {
//            String resourceOid = null;
//            if (task.getObjectRef() != null) {
//                resourceOid = task.getObjectRef().getOid();
//            }
//            LOGGER.error("Import: Resource not found: {}", resourceOid, ex);
//            // This is bad. The resource does not exist. Permanent problem.
//            opResult.recordFatalError("Resource not found " + resourceOid, ex);
//            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
//            return runResult;
//        } catch (SchemaException ex) {
//            LOGGER.error("Import: Error dealing with schema: {}", ex.getMessage(), ex);
//            // Not sure about this. But most likely it is a misconfigured resource or connector
//            // It may be worth to retry. Error is fatal, but may not be permanent.
//            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
//            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
//            return runResult;
//        } catch (RuntimeException ex) {
//            LOGGER.error("Import: Internal Error: {}", ex.getMessage(), ex);
//            // Can be anything ... but we can't recover from that.
//            // It is most likely a programming error. Does not make much sense to retry.
//            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
//            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
//            return runResult;
//        }

        if (resource == null) {
            LOGGER.error("Live Sync: No resource specified");
            opResult.recordFatalError("No resource specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
//        String resourceOid = resource.getOid();
        
		RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            LOGGER.error("Live Sync: Schema error during processing account definition: {}",e.getMessage());
            opResult.recordFatalError("Schema error during processing account definition: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
        
        if (refinedSchema == null){
        	opResult.recordFatalError("No refined schema defined. Probably some configuration problem.");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            LOGGER.error("Live Sync: No refined schema defined. Probably some configuration problem.");
//        	throw new SchemaException();re
            return runResult;
        }
        
        RefinedObjectClassDefinition rObjectClass = Utils.determineObjectClass(refinedSchema, task);        
        if (rObjectClass == null) {
            LOGGER.error("Live Sync: No objectclass specified and no default can be determined.");
            opResult.recordFatalError("No objectclass specified and no default can be determined");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        int changesProcessed;
		
		try {
			
			// MAIN PART
			// Calling synchronize(..) in provisioning.
			// This will detect the changes and notify model about them.
			// It will use extension of task to store synchronization state

            Utils.clearRequestee(task);
			changesProcessed = provisioningService.synchronize(resourceOid, rObjectClass.getTypeName(), task, opResult);
            progress += changesProcessed;
			
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Live Sync: A required object does not exist, OID: {}", ex.getOid());
            LOGGER.error("Exception stack trace", ex);
			// This is bad. The resource or task or something like that does not exist. Permanent problem.
			opResult.recordFatalError("A required object does not exist, OID: " + ex.getOid(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (CommunicationException ex) {
			LOGGER.error("Live Sync: Communication error:",ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Live Sync: Error dealing with schema:",ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (RuntimeException ex) {
			LOGGER.error("Live Sync: Internal Error:", ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense to retry.
			opResult.recordFatalError("Internal Error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (ConfigurationException ex) {
			LOGGER.error("Live Sync: Configuration error:",ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Configuration error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Recompute: Security violation: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Security violation: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		
		opResult.computeStatus("Live sync run has failed");

        opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS).recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + changesProcessed);

        // This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {})", resourceOid);
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null;	// not to reset progress information
	}

	@Override
	public void refreshStatus(Task task) {
		// Do nothing. Everything is fresh already.		
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.LIVE_SYNCHRONIZATION;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
