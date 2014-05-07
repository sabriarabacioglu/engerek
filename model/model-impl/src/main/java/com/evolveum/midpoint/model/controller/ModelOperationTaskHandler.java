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

package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Handles a "ModelOperation task" - executes a given model operation in a context
 * of the task (i.e., in most cases, asynchronously).
 *
 * The context of the model operation (i.e., model context) is stored in task extension property
 * called "modelContext". When this handler is executed, the context is retrieved, unwrapped from
 * its XML representation, and the model operation is (re)started.
 *
 * @author mederly
 */

@Component
public class ModelOperationTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ModelOperationTaskHandler.class);

    private static final String DOT_CLASS = ModelOperationTaskHandler.class.getName() + ".";

    public static final String MODEL_OPERATION_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/model/operation/handler-3";

    @Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private ProvisioningService provisioningService;

    @Autowired(required = true)
    private Clockwork clockwork;

	@Override
	public TaskRunResult run(Task task) {

		OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
		TaskRunResult runResult = new TaskRunResult();

        PrismProperty<Boolean> skipProperty = task.getExtensionProperty(SchemaConstants.SKIP_MODEL_CONTEXT_PROCESSING_PROPERTY);

        if (skipProperty != null && Boolean.TRUE.equals(skipProperty.getRealValue())) {

            LOGGER.trace("Found " + skipProperty + ", skipping the model operation execution.");
            if (result.isUnknown()) {
                result.computeStatus();
            }
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);

        } else {

            PrismContainer<LensContextType> contextTypeContainer = (PrismContainer) task.getExtensionItem(SchemaConstants.MODEL_CONTEXT_NAME);
            if (contextTypeContainer == null) {
                throw new SystemException("There's no model context container in task " + task + " (" + SchemaConstants.MODEL_CONTEXT_NAME + ")");
            }

            LensContext context = null;
            try {
                context = LensContext.fromLensContextType(contextTypeContainer.getValue().asContainerable(), prismContext, provisioningService, result);
            } catch (SchemaException e) {
                throw new SystemException("Cannot recover model context from task " + task + " due to schema exception", e);
            } catch (ObjectNotFoundException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);
            } catch (CommunicationException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);     // todo wait and retry
            } catch (ConfigurationException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Context to be executed = " + context.debugDump());
            }

            try {
                clockwork.run(context, task, result);

                task.setExtensionContainer(context.toPrismContainer());
                task.savePendingModifications(result);

                if (result.isUnknown()) {
                    result.computeStatus();
                }
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
            } catch (Exception e) { // too many various exceptions; will be fixed with java7 :)
                String message = "An exception occurred within model operation, in task " + task;
                LoggingUtils.logException(LOGGER, message, e);
                result.recordPartialError(message, e);
                // TODO: here we do not know whether the error is temporary or permanent (in the future we could discriminate on the basis of particular exception caught)
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
            }
        }

        task.getResult().recomputeStatus();
		runResult.setOperationResult(task.getResult());
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		return null; // null - as *not* to record progress
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

	@PostConstruct
	private void initialize() {
        if (LOGGER.isTraceEnabled()) {
		    LOGGER.trace("Registering with taskManager as a handler for " + MODEL_OPERATION_TASK_URI);
        }
		taskManager.registerHandler(MODEL_OPERATION_TASK_URI, this);
	}
}
