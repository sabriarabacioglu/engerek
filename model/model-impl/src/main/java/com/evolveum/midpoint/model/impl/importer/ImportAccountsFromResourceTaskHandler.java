/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.model.impl.importer;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.SynchronizeAccountResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Task handler for "Import from resource" task.
 * <p/>
 * The import task will search for all the accounts on a specific resource. It will
 * pretend that all the accounts were just created and notify other components (mode)
 * using the ResourceObjectChangeListener interface. This will efficiently result in
 * importing all the accounts. Depending on the sync policy, appropriate user objects
 * may be created, accounts may be linked to existing users, etc.
 * <p/>
 * The handler will execute the import in background. It is using Task Manager
 * for that purpose, so the Task Manager instance needs to be injected. Most of the "import"
 * action is actually done in the callbacks from provisioning searchObjectsIterative() operation.
 * <p/>
 * The import task may be executed on a different node (as usual for async tasks).
 *
 * @author Radovan Semancik
 * @see TaskHandler
 * @see ResourceObjectChangeListener
 */
@Component
public class ImportAccountsFromResourceTaskHandler extends AbstractSearchIterativeTaskHandler<ShadowType, SynchronizeAccountResultHandler> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/import/handler-3";

    // WARNING! This task handler is efficiently singleton!
 	// It is a spring bean and it is supposed to handle all search task instances
 	// Therefore it must not have task-specific fields. It can only contain fields specific to
 	// all tasks of a specified type
    
    @Autowired(required = true)
    private TaskManager taskManager;
    
    @Autowired(required = true)
    private ProvisioningService provisioningService;

    @Autowired(required = true)
    private ChangeNotificationDispatcher changeNotificationDispatcher;
    
    private PrismPropertyDefinition<QName> objectclassPropertyDefinition;

    private static final Trace LOGGER = TraceManager.getTrace(ImportAccountsFromResourceTaskHandler.class);

    public ImportAccountsFromResourceTaskHandler() {
        super(ShadowType.class, "Import from resource", OperationConstants.IMPORT_ACCOUNTS_FROM_RESOURCE);
        objectclassPropertyDefinition = new PrismPropertyDefinition<QName>(ModelConstants.OBJECTCLASS_PROPERTY_NAME, 
        		DOMUtil.XSD_QNAME, prismContext);
        setLogFinishInfo(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching import from resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ImportAccountsFromResourceTaskHandler.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);
        // TODO

        // Set handler URI so we will be called back
        task.setHandlerUri(HANDLER_URI);

        // Readable task name
        PolyStringType polyString = new PolyStringType("Import from resource " + resource.getName());
        task.setName(polyString);
        

        // Set reference to the resource
        task.setObjectRef(ObjectTypeUtil.createObjectRef(resource));

        try {
        	PrismProperty<?> objectclassProp = objectclassPropertyDefinition.instantiate();
        	objectclassProp.setRealValue(objectclass);
        	task.setExtensionProperty(objectclassProp);
        	task.savePendingModifications(result);		// just to be sure (if the task was already persistent)
//          task.modify(modifications, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Task object not found, expecting it to exist (task {})", task, e);
            result.recordFatalError("Task object not found", e);
            throw new IllegalStateException("Task object not found, expecting it to exist", e);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.error("Task object wasn't updated (task {})", task, e);
            result.recordFatalError("Task object wasn't updated", e);
            throw new IllegalStateException("Task object wasn't updated", e);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            result.recordFatalError("Error dealing with schema", e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
        result.computeStatus("Import launch failed");

        LOGGER.trace("Import from resource {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(resource), task);
    }

	@Override
	protected SynchronizeAccountResultHandler createHandler(TaskRunResult runResult, Task task,
			OperationResult opResult) {
		
		ResourceType resource = resolveObjectRef(ResourceType.class, runResult, task, opResult);
		if (resource == null) {
			return null;
		}
		
        return createHandler(resource, runResult, task, opResult);
	}
	
	private SynchronizeAccountResultHandler createHandler(ResourceType resource, TaskRunResult runResult, Task task,
			OperationResult opResult) {
		
		RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            LOGGER.error("Import: Schema error during processing account definition: {}",e.getMessage());
            opResult.recordFatalError("Schema error during processing account definition: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }

        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Refined schema:\n{}", refinedSchema.debugDump());
        }
        
        RefinedObjectClassDefinition rObjectClass = Utils.determineObjectClass(refinedSchema, task);        
        if (rObjectClass == null) {
            LOGGER.error("Import: No objectclass specified and no default can be determined.");
            opResult.recordFatalError("No objectclass specified and no default can be determined");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }
        
        LOGGER.info("Start executing import from resource {}, importing object class {}", resource, rObjectClass.getTypeName());
		
		SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource, rObjectClass, "import", 
        		task, changeNotificationDispatcher);
        handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        handler.setForceAdd(true);
        handler.setStopOnError(false);
        handler.setContextDesc("from "+resource);
        handler.setLogObjectProgress(true);
        
        return handler;
	}
	
	@Override
	protected boolean initializeRun(SynchronizeAccountResultHandler handler,
			TaskRunResult runResult, Task task, OperationResult opResult) {
		return super.initializeRun(handler, runResult, task, opResult);
	}

	@Override
	protected ObjectQuery createQuery(SynchronizeAccountResultHandler handler, TaskRunResult runResult, Task task, OperationResult opResult) {
        try {
			return ObjectQueryUtil.createResourceAndAccountQuery(handler.getResource().getOid(), 
					handler.getRefinedObjectClass().getTypeName(), prismContext);
		} catch (SchemaException e) {
			LOGGER.error("Import: Schema error during creating search query: {}",e.getMessage());
            opResult.recordFatalError("Schema error during creating search query: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		}
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.IMPORTING_ACCOUNTS;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
    
    /**
     * Imports a single shadow. Synchronously. The task is NOT switched to background by default.
     */
    public boolean importSingleShadow(String shadowOid, Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
    	
    	PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, shadowOid, null, task, parentResult);
    	PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, ShadowUtil.getResourceOid(shadow), null, task, parentResult);
    	
    	// Create a result handler just for one object. Invoke the handle() method manually.
    	TaskRunResult runResult = new TaskRunResult();
		SynchronizeAccountResultHandler resultHandler = createHandler(resource.asObjectable(), runResult, task, parentResult);
		if (resultHandler == null) {
			return false;
		}
		// This is required for proper error reporting
		resultHandler.setStopOnError(true);
		
		boolean cont = initializeRun(resultHandler, runResult, task, parentResult);
		if (!cont) {
			return false;
		}
		
		cont = resultHandler.handle(shadow, parentResult);
		if (!cont) {
			return false;
		}
		
		finish(resultHandler, runResult, task, parentResult);
		
		return true;
    }
}
