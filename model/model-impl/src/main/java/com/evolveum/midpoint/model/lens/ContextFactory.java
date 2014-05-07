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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@Component
public class ContextFactory {
	
	@Autowired(required = true)
	PrismContext prismContext;

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	Protector protector;
	
	public <F extends ObjectType> LensContext<F> createContext(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ObjectDelta<F> focusDelta = null;
		Collection<ObjectDelta<ShadowType>> projectionDeltas = new ArrayList<ObjectDelta<ShadowType>>(deltas.size());
		ObjectDelta<? extends ObjectType> confDelta = null;
		Class<F> focusClass = null;
		// Sort deltas to focus and projection deltas, check if the classes are correct;
		for (ObjectDelta<? extends ObjectType> delta: deltas) {
			Class<? extends ObjectType> typeClass = delta.getObjectTypeClass();
			Validate.notNull(typeClass, "Object type class is null in "+delta);
			if (isFocalClass(typeClass)) {
				if (confDelta != null) {
					throw new IllegalArgumentException("Mixed configuration and focus deltas in one executeChanges invocation");
				}
				
				focusClass = (Class<F>) typeClass;
				if (!delta.isAdd() && delta.getOid() == null) {
					throw new IllegalArgumentException("Delta "+delta+" does not have an OID");
				}
				if (InternalsConfig.consistencyChecks) {
					// Focus delta has to be complete now with all the definition already in place
					delta.checkConsistence(false, true, true);
				}
                if (focusDelta != null) {
                    throw new IllegalStateException("More than one focus delta used in model operation");
                }
				focusDelta = (ObjectDelta<F>) delta;
			} else if (isProjectionClass(typeClass)) {
				if (confDelta != null) {
					throw new IllegalArgumentException("Mixed configuration and projection deltas in one executeChanges invocation");
				}
				
				projectionDeltas.add((ObjectDelta<ShadowType>) delta);
			} else {
				if (confDelta != null) {
					throw new IllegalArgumentException("More than one configuration delta in a single executeChanges invovation");
				}
				confDelta = delta;
			}
		}
		
		if (confDelta != null) {
			focusClass = (Class<F>) confDelta.getObjectTypeClass();
		}
		
		if (focusClass == null) {
			focusClass = determineFocusClass();
		}
		LensContext<F> context = new LensContext<F>(focusClass, prismContext, provisioningService);
		context.setChannel(task.getChannel());
		context.setOptions(options);
		context.setDoReconciliationForAllProjections(ModelExecuteOptions.isReconcile(options));
		
		if (confDelta != null) {
			LensFocusContext<F> focusContext = context.createFocusContext();
			focusContext.setPrimaryDelta((ObjectDelta<F>) confDelta);
			
		} else {
		
			if (focusDelta != null) {
				LensFocusContext<F> focusContext = context.createFocusContext();
				focusContext.setPrimaryDelta(focusDelta);
			}
			
			for (ObjectDelta<ShadowType> projectionDelta: projectionDeltas) {
				LensProjectionContext projectionContext = context.createProjectionContext();
				projectionContext.setPrimaryDelta(projectionDelta);
				
				// We are little bit more liberal regarding projection deltas. 
				// If the deltas represent shadows we tolerate missing attribute definitions.
				// We try to add the definitions by calling provisioning
				provisioningService.applyDefinition(projectionDelta, result);
						
				if (projectionDelta instanceof ShadowDiscriminatorObjectDelta) {
					ShadowDiscriminatorObjectDelta<ShadowType> shadowDelta = (ShadowDiscriminatorObjectDelta<ShadowType>)projectionDelta;
					projectionContext.setResourceShadowDiscriminator(shadowDelta.getDiscriminator());
				} else {
					if (!projectionDelta.isAdd() && projectionDelta.getOid() == null) {
						throw new IllegalArgumentException("Delta "+projectionDelta+" does not have an OID");
					}
				}
			}
			
		}

		// This forces context reload before the next projection
		context.rot();
		
		if (InternalsConfig.consistencyChecks) context.checkConsistence();
		
		return context;
	}
	
	
	public <F extends ObjectType, O extends ObjectType> LensContext<F> createRecomputeContext(
    		PrismObject<O> object, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		Class<O> typeClass = object.getCompileTimeClass();
		LensContext<F> context;
		if (isFocalClass(typeClass)) {
			context = createRecomputeFocusContext((Class<F>)typeClass, (PrismObject<F>) object, task, result);
		} else if (ShadowType.class.isAssignableFrom(typeClass)) {
			context =  createRecomputeProjectionContext((PrismObject<ShadowType>) object, task, result);
		} else {
			throw new IllegalArgumentException("Cannot create recompute context for "+object);
		}
		context.setLazyAuditRequest(true);
		return context;
	}
	
	public <F extends ObjectType> LensContext<F> createRecomputeFocusContext(
    		Class<F> focusType, PrismObject<F> focus, Task task, OperationResult result) {
    	LensContext<F> syncContext = new LensContext<F>(focusType,
				prismContext, provisioningService);
		LensFocusContext<F> focusContext = syncContext.createFocusContext();
		focusContext.setLoadedObject(focus);
		focusContext.setOid(focus.getOid());
		syncContext.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECOMPUTE));
		syncContext.setDoReconciliationForAllProjections(true);
		return syncContext;
    }
	
	public <F extends ObjectType> LensContext<F> createRecomputeProjectionContext(
    		PrismObject<ShadowType> shadow, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		provisioningService.applyDefinition(shadow, result);
    	LensContext<F> syncContext = new LensContext<F>(null,
				prismContext, provisioningService);
    	LensProjectionContext projectionContext = syncContext.createProjectionContext();
    	projectionContext.setLoadedObject(shadow);
    	projectionContext.setOid(shadow.getOid());
    	projectionContext.setDoReconciliation(true);
		syncContext.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECOMPUTE));
		return syncContext;
    }
	
	 /**
     * Creates empty lens context for synchronization purposes, filling in only the very basic metadata (such as channel).
     */
	public <F extends ObjectType> LensContext<F> createSyncContext(Class<F> focusClass, ResourceObjectShadowChangeDescription change) {
		
		LensContext<F> context = new LensContext<F>(focusClass, prismContext, provisioningService);
    	context.setChannel(change.getSourceChannel());
    	return context;
	}
	
	public static <F extends ObjectType> Class<F> determineFocusClass() {
		// TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		return (Class<F>) UserType.class;
	}

	private static <T extends ObjectType> Class<T> checkProjectionClass(Class<T> oldProjectionClass, Class<T> newProjectionClass) {
		if (oldProjectionClass == null) {
			return newProjectionClass;
		} else {
			if (oldProjectionClass != oldProjectionClass) {
				throw new IllegalArgumentException("Mixed projection classes in the deltas, got both "+oldProjectionClass+" and "+oldProjectionClass);
			}
			return oldProjectionClass;
		}
	}

	public static <T extends ObjectType> boolean isFocalClass(Class<T> aClass) {
		return FocusType.class.isAssignableFrom(aClass);
	}
	
	public boolean isProjectionClass(Class<? extends ObjectType> aClass) {
		return ShadowType.class.isAssignableFrom(aClass);
	}

}
