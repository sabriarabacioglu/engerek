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

package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler.FailedOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class SchemaExceptionHandler extends ErrorHandler{

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	
	@Override
	public <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, boolean compensate, 
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		
		ObjectDelta delta = null;
		switch (op) {
		case ADD:
			delta = ObjectDelta.createAddDelta(shadow.asPrismObject());
			break;
		case DELETE:
			delta = ObjectDelta.createDeleteDelta(shadow.getClass(), shadow.getOid(), prismContext);
			break;
		case MODIFY:
			Collection<? extends ItemDelta> modifications = null;
			if (shadow.getObjectChange() != null) {
				ObjectDeltaType deltaType = shadow.getObjectChange();

				modifications = DeltaConvertor.toModifications(deltaType.getItemDelta(), shadow.asPrismObject()
						.getDefinition());
			}
			delta = ObjectDelta.createModifyDelta(shadow.getOid(), modifications, shadow.getClass(), prismContext);
			break;
		}

		if (op != FailedOperation.GET) {
//			Task task = taskManager.createTaskInstance();
			ResourceOperationDescription operationDescription = createOperationDescription(shadow,
					shadow.getResource(), delta, task, parentResult);
			changeNotificationDispatcher.notifyFailure(operationDescription, task, parentResult);
		}

		if (shadow.getOid() == null){
			parentResult.recordFatalError("Schema violation during processing shadow: "+ ObjectTypeUtil.toShortString(shadow)+": "+ex.getMessage(), ex);
			throw new SchemaException("Schema violation during processing shadow: "+ ObjectTypeUtil.toShortString(shadow)+": "+ex.getMessage(), ex);
		}
		
		Collection<ItemDelta> modification = createAttemptModification(shadow, null);
		
		try {
			cacheRepositoryService.modifyObject(shadow.asPrismObject().getCompileTimeClass(), shadow.getOid(),
					modification, parentResult);
		} catch (Exception e) {
			//this should not happen. But if it happens, we should return original exception
//			throw new SchemaException("Schema violation during processing shadow: "
//					+ ObjectTypeUtil.toShortString(shadow) + ": " + ex.getMessage(), ex);
		}
		
		parentResult.recordFatalError("Schema violation during processing shadow: "+ ObjectTypeUtil.toShortString(shadow)+": "+ex.getMessage(), ex);
				throw new SchemaException("Schema violation during processing shadow: "+ ObjectTypeUtil.toShortString(shadow)+": "+ex.getMessage(), ex);
	}	

}
