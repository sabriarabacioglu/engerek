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

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Provides an interface between the model and the workflow engine:
 * catches hook calls and delegates them to change processors.
 *
 * @author mederly
 */
@Component
public class WfHook implements ChangeHook {

    private static final Trace LOGGER = TraceManager.getTrace(WfHook.class);

    public static final String WORKFLOW_HOOK_URI = "http://midpoint.evolveum.com/model/workflow-hook-1";        // todo

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private HookRegistry hookRegistry;

    private static final String DOT_CLASS = WfHook.class.getName() + ".";
    private static final String OPERATION_INVOKE = DOT_CLASS + "invoke";

    @PostConstruct
    public void init() {
        if (wfConfiguration.isEnabled()) {
            hookRegistry.registerChangeHook(WfHook.WORKFLOW_HOOK_URI, this);
            LOGGER.info("Workflow change hook was registered.");
        } else {
            LOGGER.info("Workflow change hook is not registered, because workflows are disabled.");
        }
    }

    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult parentResult) {

        Validate.notNull(context);
        Validate.notNull(task);
        Validate.notNull(parentResult);

        OperationResult result = parentResult.createSubresult(OPERATION_INVOKE);
        result.addParam("taskFromModel", task.toString());
        result.addContext("model state", context.getState());

        logOperationInformation(context);

        HookOperationMode retval = processModelInvocation(context, task, result);
        result.recordSuccessIfUnknown();
        return retval;
    }

    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
        // do nothing
    }

    private void logOperationInformation(ModelContext context) {

        if (LOGGER.isTraceEnabled()) {

            LensContext lensContext = (LensContext) context;

            LOGGER.trace("=====================================================================");
            LOGGER.trace("WfHook invoked in state " + context.getState() + " (wave " + lensContext.getProjectionWave() + ", max " + lensContext.getMaxWave() + "):");

            ObjectDelta pdelta = context.getFocusContext() != null ? context.getFocusContext().getPrimaryDelta() : null;
            ObjectDelta sdelta = context.getFocusContext() != null ? context.getFocusContext().getSecondaryDelta() : null;

            LOGGER.trace("Primary delta: " + (pdelta == null ? "(null)" : pdelta.debugDump()));
            LOGGER.trace("Secondary delta: " + (sdelta == null ? "(null)" : sdelta.debugDump()));
            LOGGER.trace("Projection contexts: " + context.getProjectionContexts().size());

            for (Object o : context.getProjectionContexts()) {
                ModelProjectionContext mpc = (ModelProjectionContext) o;
                ObjectDelta ppdelta = mpc.getPrimaryDelta();
                ObjectDelta psdelta = mpc.getSecondaryDelta();
                LOGGER.trace(" - Primary delta: " + (ppdelta == null ? "(null)" : ppdelta.debugDump()));
                LOGGER.trace(" - Secondary delta: " + (psdelta == null ? "(null)" : psdelta.debugDump()));
                LOGGER.trace(" - Sync delta:" + (mpc.getSyncDelta() == null ? "(null)" : mpc.getSyncDelta().debugDump()));
            }
        }
    }

    HookOperationMode processModelInvocation(ModelContext context, Task taskFromModel, OperationResult result) {

        for (ChangeProcessor changeProcessor : wfConfiguration.getChangeProcessors()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Trying change processor: " + changeProcessor.getClass().getName());
            }
            if (!changeProcessor.isEnabled()) {
                LOGGER.trace("It is disabled, continuing with next one.");
                continue;
            }
            try {
                HookOperationMode hookOperationMode = changeProcessor.processModelInvocation(context, taskFromModel, result);
                if (hookOperationMode != null) {
                    return hookOperationMode;
                }
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Schema exception while running change processor {}", e, changeProcessor.getClass().getName());   // todo message
                result.recordFatalError("Schema exception while running change processor " + changeProcessor.getClass(), e);
                return HookOperationMode.ERROR;
            } catch (RuntimeException e) {
                LoggingUtils.logException(LOGGER, "Runtime exception while running change processor {}", e, changeProcessor.getClass().getName());   // todo message
                result.recordFatalError("Runtime exception while running change processor " + changeProcessor.getClass(), e);
                return HookOperationMode.ERROR;
            }
        }

        LOGGER.trace("No change processor caught this request, returning the FOREGROUND flag.");
        result.recordSuccess();
        return HookOperationMode.FOREGROUND;
    }


}
