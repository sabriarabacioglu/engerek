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

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;
import ch.qos.logback.core.pattern.parser.ScanException;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SynchronizationSituationUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.RawType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

	private static final String OPERATION_EXECUTE_DELTA = ChangeExecutor.class.getName() + ".executeDelta";
	private static final String OPERATION_EXECUTE = ChangeExecutor.class.getName() + ".execute";
	private static final String OPERATION_EXECUTE_FOCUS = OPERATION_EXECUTE + ".focus";
	private static final String OPERATION_EXECUTE_PROJECTION = OPERATION_EXECUTE + ".projection";
	private static final String OPERATION_LINK_ACCOUNT = ChangeExecutor.class.getName() + ".linkShadow";
	private static final String OPERATION_UNLINK_ACCOUNT = ChangeExecutor.class.getName() + ".unlinkShadow";
	private static final String OPERATION_UPDATE_SITUATION_ACCOUNT = ChangeExecutor.class.getName() + ".updateSituationInShadow";

    @Autowired(required = true)
    private transient TaskManager taskManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired(required = true)
    private ProvisioningService provisioning;
    
    @Autowired(required = true)
    private PrismContext prismContext;
    
    @Autowired(required = true)
	private ExpressionFactory expressionFactory;
    
    @Autowired(required = true)
    private SecurityEnforcer securityEnforcer;

    // for inserting workflow-related metadata to changed object
    @Autowired(required = false)
    private WorkflowManager workflowManager;
    
    private PrismObjectDefinition<UserType> userDefinition = null;
    private PrismObjectDefinition<ShadowType> shadowDefinition = null;
    
    @PostConstruct
    private void locateDefinitions() {
    	userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    	shadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
    }

    public <O extends ObjectType> void executeChanges(LensContext<O> syncContext, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	
    	OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE);
    	
    	// FOCUS
    	
    	LensFocusContext<O> focusContext = syncContext.getFocusContext();
    	if (focusContext != null) {
	        ObjectDelta<O> userDelta = focusContext.getWaveDelta(syncContext.getExecutionWave());
	        if (userDelta != null) {
	
	        	OperationResult subResult = result.createSubresult(OPERATION_EXECUTE_FOCUS+"."+focusContext.getObjectTypeClass().getSimpleName());
	        	try {
	        		
		            executeDelta(userDelta, focusContext, syncContext, null, null, task, subResult);
		
	                subResult.computeStatus();
	                
	        	} catch (SchemaException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ObjectNotFoundException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ObjectAlreadyExistsException e) {
	    			subResult.computeStatus();
	    			if (!subResult.isSuccess()) {
	    				subResult.recordFatalError(e);
	    			}
	    			result.computeStatusComposite();
	    			throw e;
	    		} catch (CommunicationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ConfigurationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (SecurityViolationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ExpressionEvaluationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (RuntimeException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		}  
	        } else {
	            LOGGER.trace("Skipping focus change execute, because user delta is null");
	        }
    	}

    	// PROJECTIONS
    	
        for (LensProjectionContext accCtx : syncContext.getProjectionContexts()) {
        	if (accCtx.getWave() != syncContext.getExecutionWave()) {
        		continue;
			}
        	OperationResult subResult = result.createSubresult(OPERATION_EXECUTE_PROJECTION+"."+accCtx.getObjectTypeClass().getSimpleName());
        	subResult.addContext("discriminator", accCtx.getResourceShadowDiscriminator());
			if (accCtx.getResource() != null) {
				subResult.addParam("resource", accCtx.getResource().getName());
			}
			try {
				
				executeReconciliationScript(accCtx, syncContext, BeforeAfterType.BEFORE, task, subResult);
				
				ObjectDelta<ShadowType> accDelta = accCtx.getExecutableDelta();
				if (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
					if (syncContext.getFocusContext().getDelta() != null
							&& syncContext.getFocusContext().getDelta().isDelete()
							&& syncContext.getOptions() != null
							&& ModelExecuteOptions.isForce(syncContext.getOptions())) {
						if (accDelta == null) {
							accDelta = ObjectDelta.createDeleteDelta(accCtx.getObjectTypeClass(),
									accCtx.getOid(), prismContext);
						}
					}
					if (accDelta != null && accDelta.isDelete()) {

						executeDelta(accDelta, accCtx, syncContext, null, accCtx.getResource(), task, subResult);

					}
				} else {
					
					if (accDelta == null || accDelta.isEmpty()) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("No change for account "
									+ accCtx.getResourceShadowDiscriminator());
						}
						if (focusContext != null) {
							updateLinks(focusContext.getObjectNew(), focusContext, accCtx, task,
                                    subResult);
						}
						
						// Make sure post-reconcile delta is always executed, even if there is no change
						executeReconciliationScript(accCtx, syncContext, BeforeAfterType.AFTER, task, subResult);
						
						subResult.computeStatus();
						subResult.recordNotApplicableIfUnknown();
						continue;
						
					} else if (accDelta.isDelete() && accCtx.getResourceShadowDiscriminator() != null && accCtx.getResourceShadowDiscriminator().getOrder() > 0) {
						// HACK ... for higher-order context check if this was already deleted
						LensProjectionContext lowerOrderContext = LensUtil.findLowerOrderContext(syncContext, accCtx);
						if (lowerOrderContext != null && lowerOrderContext.isDelete()) {
							// We assume that this was already executed
							subResult.setStatus(OperationResultStatus.NOT_APPLICABLE);
							continue;
						}
					}

					executeDelta(accDelta, accCtx, syncContext, null, accCtx.getResource(), task, subResult);

				}

				if (focusContext != null) {
					updateLinks(focusContext.getObjectNew(), focusContext, accCtx, task, subResult);
				}
				
				executeReconciliationScript(accCtx, syncContext, BeforeAfterType.AFTER, task, subResult);
				
				subResult.computeStatus();
				subResult.recordNotApplicableIfUnknown();
				
			} catch (SchemaException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ObjectNotFoundException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ObjectAlreadyExistsException e) {
				// in his case we do not need to set account context as
				// broken, instead we need to restart projector for this
				// context to recompute new account or find out if the
				// account was already linked..
				// and also do not set fatal error to the operation result, this is a special case
				// if it is fatal, it will be set later
				// but we need to set some result
				subResult.recordHandledError(e);
				continue;
			} catch (CommunicationException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ConfigurationException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (SecurityViolationException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ExpressionEvaluationException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (RuntimeException e) {
				recordProjectionExecutionException(e, accCtx, subResult, SynchronizationPolicyDecision.BROKEN);
				continue;
			}
		}
        
        // Result computation here needs to be slightly different
        result.computeStatusComposite();

    }

	private <P extends ObjectType> void recordProjectionExecutionException(Exception e, LensProjectionContext accCtx,
			OperationResult subResult, SynchronizationPolicyDecision decision) {
		subResult.recordFatalError(e);
		LOGGER.error("Error executing changes for {}: {}", new Object[]{accCtx.toHumanReadableString(), e.getMessage(), e});
		if (decision != null) {
			accCtx.setSynchronizationPolicyDecision(decision);
		}
	}

	private void recordFatalError(OperationResult subResult, OperationResult result, String message, Throwable e) {
		if (message == null) {
			message = e.getMessage();
		}
		subResult.recordFatalError(e);
		if (result != null) {
			result.computeStatusComposite();
		}
	}

	/**
     * Make sure that the account is linked (or unlinked) as needed.
     */
    private <F extends ObjectType> void updateLinks(PrismObject<F> prismObject,
                                                    LensFocusContext<F> focusContext, LensProjectionContext projCtx,
                                                    Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
    	if (prismObject == null) {
    		return;
    	}
        F objectTypeNew = prismObject.asObjectable();
        if (!(objectTypeNew instanceof FocusType)) {
        	return;
        }
        FocusType focusTypeNew = (FocusType) objectTypeNew;

        if (projCtx.getResourceShadowDiscriminator() != null && projCtx.getResourceShadowDiscriminator().getOrder() > 0) {
        	// Don't mess with links for higher-order contexts. The link should be dealt with
        	// during processing of zero-order context.
        	return;
        }
        
        String projOid = projCtx.getOid();
        if (projOid == null) {
        	if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
        		// This seems to be OK. In quite a strange way, but still OK.
        		return;
        	}
        	LOGGER.trace("Shadow has null OID, this should not happen, context:\n{}", projCtx.debugDump());
            throw new IllegalStateException("Shadow has null OID, this should not happen");
        }

        if (projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK
        		|| projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
        		|| projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN
        		|| projCtx.isDelete()) {
            // Link should NOT exist
        	
        	PrismReference linkRef = focusTypeNew.asPrismObject().findReference(FocusType.F_LINK_REF);
        	if (linkRef != null) {
        		for (PrismReferenceValue linkRefVal: linkRef.getValues()) {
        			if (linkRefVal.getOid().equals(projOid)) {
                        // Linked, need to unlink
                        unlinkShadow(focusTypeNew.getOid(), linkRefVal, focusContext, task, result);
                    }
        		}
        		
        	}
            
    		if (projCtx.isDelete() || projCtx.isThombstone()) {
    			LOGGER.trace("Resource object {} deleted, updating also situation in shadow.", projOid);
    			// HACK HACK?
    			try {
    				updateSituationInShadow(task, SynchronizationSituationType.DELETED, focusContext, projCtx, result);
    			} catch (ObjectNotFoundException e) {
    				// HACK HACK?
    				LOGGER.trace("Resource object {} is gone, cannot update situation in shadow (this is probably harmless).", projOid);
    				result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
    			}
    		} else {
    			// This should NOT be UNLINKED. We just do not know the situation here. Reflect that in the shadow.
				LOGGER.trace("Resource object {} unlinked from the user, updating also situation in shadow.", projOid);
				updateSituationInShadow(task, null, focusContext, projCtx, result);
    		}
            // Not linked, that's OK

        } else {
            // Link should exist
        	
            for (ObjectReferenceType linkRef : focusTypeNew.getLinkRef()) {
                if (projOid.equals(linkRef.getOid())) {
                    // Already linked, nothing to do, only be sure, the situation is set with the good value
                	LOGGER.trace("Updating situation in already linked shadow.");
                	updateSituationInShadow(task, SynchronizationSituationType.LINKED, focusContext, projCtx, result);
                	return;
                }
            }
            // Not linked, need to link
            linkShadow(focusTypeNew.getOid(), projOid, focusContext, task, result);
            //be sure, that the situation is set correctly
            LOGGER.trace("Updating situation after shadow was linked.");
            updateSituationInShadow(task, SynchronizationSituationType.LINKED, focusContext, projCtx, result);
        }
    }

    private <F extends ObjectType> void linkShadow(String userOid, String accountOid, LensElementContext<F> focusContext, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {

        Class<F> typeClass = focusContext.getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(typeClass)) {
            return;
        }

        LOGGER.trace("Linking shadow " + accountOid + " to focus " + userOid);
        OperationResult result = parentResult.createSubresult(OPERATION_LINK_ACCOUNT);
        PrismReferenceValue linkRef = new PrismReferenceValue();
        linkRef.setOid(accountOid);
        linkRef.setTargetType(ShadowType.COMPLEX_TYPE);
        Collection<? extends ItemDelta> linkRefDeltas = ReferenceDelta.createModificationAddCollection(
        		FocusType.F_LINK_REF, getUserDefinition(), linkRef);

        try {
            cacheRepositoryService.modifyObject(typeClass, userOid, linkRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        } finally {
        	result.computeStatus();
        	ObjectDelta<F> userDelta = ObjectDelta.createModifyDelta(userOid, linkRefDeltas, typeClass, prismContext);
        	LensObjectDeltaOperation<F> userDeltaOp = new LensObjectDeltaOperation<F>(userDelta);
            userDeltaOp.setExecutionResult(result);
    		focusContext.addToExecutedDeltas(userDeltaOp);
        }

    }

	private PrismObjectDefinition<UserType> getUserDefinition() {
		return userDefinition;
	}

	private <F extends ObjectType> void unlinkShadow(String userOid, PrismReferenceValue accountRef, LensElementContext<F> focusContext,
                                                     Task task, OperationResult parentResult) throws
            ObjectNotFoundException, SchemaException {

        Class<F> typeClass = focusContext.getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(typeClass)) {
            return;
        }

        LOGGER.trace("Deleting linkRef " + accountRef + " from focus " + userOid);
        OperationResult result = parentResult.createSubresult(OPERATION_UNLINK_ACCOUNT);
        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationDeleteCollection(
        		FocusType.F_LINK_REF, getUserDefinition(), accountRef.clone());
        
        try {
            cacheRepositoryService.modifyObject(typeClass, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
        	result.recordFatalError(ex);
            throw new SystemException(ex);
        } finally {
        	result.computeStatus();
        	ObjectDelta<F> userDelta = ObjectDelta.createModifyDelta(userOid, accountRefDeltas, typeClass, prismContext);
        	LensObjectDeltaOperation<F> userDeltaOp = new LensObjectDeltaOperation<F>(userDelta);
            userDeltaOp.setExecutionResult(result);
    		focusContext.addToExecutedDeltas(userDeltaOp);
        }
 
    }
	
    private <F extends ObjectType> void updateSituationInShadow(Task task,
                                                                SynchronizationSituationType situation, LensFocusContext<F> focusContext, LensProjectionContext projectionCtx,
                                                                OperationResult parentResult) throws ObjectNotFoundException, SchemaException{

    	String projectionOid = projectionCtx.getOid();
    	
    	OperationResult result = new OperationResult(OPERATION_UPDATE_SITUATION_ACCOUNT);
    	result.addParam("situation", situation);
    	result.addParam("accountRef", projectionOid);
		
    	PrismObject<ShadowType> account = null;
    	try {
    		account = provisioning.getObject(ShadowType.class, projectionOid, 
    				SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
    	} catch (Exception ex){
    		LOGGER.trace("Problem with getting account, skipping modifying situation in account.");
			return;
    	}
    	List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil.createSynchronizationSituationAndDescriptionDelta(account,
    			situation, task.getChannel(), projectionCtx.hasFullShadow());

		try {
            Utils.setRequestee(task, focusContext);
			String changedOid = provisioning.modifyObject(ShadowType.class, projectionOid,
					syncSituationDeltas, null, ProvisioningOperationOptions.createCompletePostponed(false),
					task, result);
//			modifyProvisioningObject(AccountShadowType.class, accountRef, syncSituationDeltas, ProvisioningOperationOptions.createCompletePostponed(false), task, result);
			projectionCtx.setSynchronizationSituationResolved(situation);
			LOGGER.trace("Situation in projection {} was updated to {}.", projectionCtx, situation);
		} catch (ObjectNotFoundException ex) {
			// if the object not found exception is thrown, it's ok..probably
			// the account was deleted by previous execution of changes..just
			// log in the trace the message for the user.. 
			LOGGER.trace("Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
			return;
		} catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        } finally {
            Utils.clearRequestee(task);
        }
		// if everything is OK, add result of the situation modification to the
		// parent result
		result.recordSuccess();
		parentResult.addSubresult(result);
		
	}
    
	private <T extends ObjectType, F extends ObjectType>
    	void executeDelta(ObjectDelta<T> objectDelta, LensElementContext<T> objectContext, LensContext<F> context,
    			ModelExecuteOptions options, ResourceType resource, Task task, OperationResult parentResult) 
    			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
    			ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		
        if (objectDelta == null) {
            throw new IllegalArgumentException("Null change");
        }
        
        if (alreadyExecuted(objectDelta, objectContext)) {
        	LOGGER.debug("Skipping execution of delta because it was already executed: {}", objectContext);
        	return;
        }
        
        if (consistencyChecks) objectDelta.checkConsistence();
        
        // Other types than focus types may not be definition-complete (e.g. accounts and resources are completed in provisioning)
        if (FocusType.class.isAssignableFrom(objectDelta.getObjectTypeClass())) {
        	objectDelta.assertDefinitions();
        }
        
    	if (LOGGER.isTraceEnabled()) {
    		logDeltaExecution(objectDelta, context, resource, null, task);
    	}

    	OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE_DELTA);
    		
    	try {
    		
	        if (objectDelta.getChangeType() == ChangeType.ADD) {
	            executeAddition(objectDelta, context, objectContext, options, resource, task, result);
	        } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
	        	executeModification(objectDelta, context, objectContext, options, resource, task, result);
	        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
	            executeDeletion(objectDelta, context, objectContext, options, resource, task, result);
	        }
	        
	        // To make sure that the OID is set (e.g. after ADD operation)
	        LensUtil.setContextOid(context, objectContext, objectDelta.getOid());

    	} finally {
    		
    		result.computeStatus();
    		if (objectContext != null) {
    			if (!objectDelta.hasCompleteDefinition()){
    				throw new SchemaException("object delta does not have complete definition");
    			}
	    		LensObjectDeltaOperation<T> objectDeltaOp = new LensObjectDeltaOperation<T>(objectDelta.clone());
		        objectDeltaOp.setExecutionResult(result);
		        objectContext.addToExecutedDeltas(objectDeltaOp);
    		}
        
	        if (LOGGER.isDebugEnabled()) {
	        	if (LOGGER.isTraceEnabled()) {
	        		LOGGER.trace("EXECUTION result {}", result.getLastSubresult());
	        	} else {
	        		// Execution of deltas was not logged yet
	        		logDeltaExecution(objectDelta, context, resource, result.getLastSubresult(), task);
	        	}
	    	}
    	}
    }
	
	private <T extends ObjectType, F extends FocusType> boolean alreadyExecuted(
			ObjectDelta<T> objectDelta, LensElementContext<T> objectContext) {
		if (objectContext == null) {
			return false;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Checking for already executed delta:\n{}\nIn deltas:\n{}",
					objectDelta.debugDump(), DebugUtil.debugDump(objectContext.getExecutedDeltas()));
		}
		return ObjectDeltaOperation.containsDelta(objectContext.getExecutedDeltas(), objectDelta);
	}
	
	private ProvisioningOperationOptions copyFromModelOptions(ModelExecuteOptions options) {
		ProvisioningOperationOptions provisioningOptions = new ProvisioningOperationOptions();
		if (options == null){
			return provisioningOptions;
		}
		
		provisioningOptions.setForce(options.getForce());
		provisioningOptions.setOverwrite(options.getOverwrite());
		return provisioningOptions;
	}

	private <T extends ObjectType, F extends ObjectType>
				void logDeltaExecution(ObjectDelta<T> objectDelta, LensContext<F> context, 
						ResourceType resource, OperationResult result, Task task) {
		StringBuilder sb = new StringBuilder();
		sb.append("---[ ");
		if (result == null) {
			sb.append("Going to EXECUTE");
		} else {
			sb.append("EXECUTED");
		}
		sb.append(" delta of ").append(objectDelta.getObjectTypeClass().getSimpleName());
		sb.append(" ]---------------------\n");
		DebugUtil.debugDumpLabel(sb, "Channel", 0);
		sb.append(" ").append(getChannel(context, task)).append("\n");
		if (context != null) {
			DebugUtil.debugDumpLabel(sb, "Wave", 0);
			sb.append(" ").append(context.getExecutionWave()).append("\n");
		}
		if (resource != null) {
			sb.append("Resource: ").append(resource.toString()).append("\n");
		}
		sb.append(objectDelta.debugDump());
		sb.append("\n");
		if (result != null) {
			DebugUtil.debugDumpLabel(sb, "Result", 0);
			sb.append(" ").append(result.getStatus()).append(": ").append(result.getMessage());
		}
		sb.append("\n--------------------------------------------------");
		
		LOGGER.debug("\n{}", sb);
	}

    private <T extends ObjectType, F extends ObjectType> void executeAddition(ObjectDelta<T> change, 
    		LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
            ResourceType resource, Task task, OperationResult result)
    				throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, 
    				ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        PrismObject<T> objectToAdd = change.getObjectToAdd();

        if (change.getModifications() != null) {
            for (ItemDelta delta : change.getModifications()) {
                delta.applyTo(objectToAdd);
            }
            change.getModifications().clear();
        }
        
        securityEnforcer.authorize(ModelService.AUTZ_ADD_URL, objectToAdd, null, null, result);

        T objectTypeToAdd = objectToAdd.asObjectable();

    	applyMetadata(context, task, objectTypeToAdd, result);
    	
        String oid;
        if (objectTypeToAdd instanceof TaskType) {
            oid = addTask((TaskType) objectTypeToAdd, result);
        } else if (objectTypeToAdd instanceof NodeType) {
            throw new UnsupportedOperationException("NodeType cannot be added using model interface");
        } else if (ObjectTypes.isManagedByProvisioning(objectTypeToAdd)) {
        	if (options == null && context != null) {
        		options = context.getOptions();
        	}
        	ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(options);
        	
        	// TODO: this is probably wrong. We should not have special case for a channel!
        	if (context != null && context.getChannel() != null && context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON))){
        		provisioningOptions.setCompletePostponed(false);
    		}
            
        	oid = addProvisioningObject(objectToAdd, context, objectContext, provisioningOptions, resource, task, result);
            if (oid == null) {
            	throw new SystemException("Provisioning addObject returned null OID while adding " + objectToAdd);
            }
            result.addReturn("createdAccountOid", oid);
        } else {
        	RepoAddOptions addOpt = new RepoAddOptions();
        	if (ModelExecuteOptions.isOverwrite(options)){
        		addOpt.setOverwrite(true);
        	}
        	if (ModelExecuteOptions.isNoCrypt(options)){
        		addOpt.setAllowUnencryptedValues(true);
        	}
            oid = cacheRepositoryService.addObject(objectToAdd, addOpt, result);
            if (oid == null) {
            	throw new SystemException("Repository addObject returned null OID while adding " + objectToAdd);
            }
        }
        change.setOid(oid);
    }

    
    private <T extends ObjectType, F extends ObjectType> void executeDeletion(ObjectDelta<T> change, 
    		LensContext<F> context, LensElementContext<T> objectContext, ModelExecuteOptions options,
            ResourceType resource, Task task, OperationResult result) throws
            ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        String oid = change.getOid();
        Class<T> objectTypeClass = change.getObjectTypeClass();
        
        PrismObject<T> objectOld = objectContext.getObjectOld();
        securityEnforcer.authorize(ModelService.AUTZ_DELETE_URL, objectOld, null, null, result);

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteTask(oid, result);
        } else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteNode(oid, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
        	if (options == null) {
        		options = context.getOptions();
        	}
        	ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(options);
        	if (context != null && context.getChannel() != null && context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON))){
        		provisioningOptions.setCompletePostponed(false);
    		}
        	try {
        		deleteProvisioningObject(objectTypeClass, oid, context, objectContext, provisioningOptions, resource, task, result);
        	} catch (ObjectNotFoundException e) {
        		// HACK. We wanted to delete something that is not there. So in fact this is OK. Almost.
        		LOGGER.trace("Attempt to delete object {} that is already gone", oid);
        		result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        	}
        } else {
            cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
        }
    }

    private <T extends ObjectType, F extends ObjectType> void executeModification(ObjectDelta<T> change,
            LensContext<F> context, LensElementContext<T> objectContext,
    		ModelExecuteOptions options, ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (change.isEmpty()) {
            // Nothing to do
            return;
        }
        Class<T> objectTypeClass = change.getObjectTypeClass();
        
        PrismObject<T> objectNew = objectContext.getObjectNew();
        securityEnforcer.authorize(ModelService.AUTZ_MODIFY_URL, objectNew, change, null, result);
        	
    	applyMetadata(change, objectContext, objectTypeClass, task, context, result);
        
        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.modifyTask(change.getOid(), change.getModifications(), result);
        } else if (NodeType.class.isAssignableFrom(objectTypeClass)) {
            throw new UnsupportedOperationException("NodeType is not modifiable using model interface");
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
        	if (options == null && context != null) {
        		options = context.getOptions();
        	}
        	ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(options);
        	if (context != null && context.getChannel() != null && context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON))){
        		provisioningOptions.setCompletePostponed(false);
    		}
            String oid = modifyProvisioningObject(objectTypeClass, change.getOid(), change.getModifications(), context, objectContext,
                    provisioningOptions, resource, task, result);
            if (!oid.equals(change.getOid())){
                change.setOid(oid);
            }
        } else {
            cacheRepositoryService.modifyObject(objectTypeClass, change.getOid(), change.getModifications(), result);
        }
    }
    
	private <T extends ObjectType, F extends ObjectType> void applyMetadata(LensContext<F> context, Task task, T objectTypeToAdd, OperationResult result) throws SchemaException {
		MetadataType metaData = new MetadataType();
		String channel = getChannel(context, task);
		metaData.setCreateChannel(channel);
		metaData.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
		if (task.getOwner() != null) {
			metaData.setCreatorRef(ObjectTypeUtil.createObjectRef(task.getOwner()));
		}
        if (workflowManager != null) {
            metaData.getCreateApproverRef().addAll(workflowManager.getApprovedBy(task, result));
        }

		objectTypeToAdd.setMetadata(metaData);
	}
    
    private <F extends ObjectType> String getChannel(LensContext<F> context, Task task){
    	if (context != null && context.getChannel() != null){
    		return context.getChannel();
    	} else if (task.getChannel() != null){
    		return task.getChannel();
    	}
    	return null;
    }
    
    private <T extends ObjectType, F extends ObjectType> void applyMetadata(ObjectDelta<T> change, LensElementContext<T> objectContext, 
    		Class objectTypeClass, Task task, LensContext<F> context, OperationResult result) throws SchemaException {
        String channel = getChannel(context, task);

    	PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectTypeClass);

        if (channel != null) {
            PropertyDelta delta = PropertyDelta.createModificationReplaceProperty((new ItemPath(ObjectType.F_METADATA, MetadataType.F_MODIFY_CHANNEL)), def, channel);
            ((Collection) change.getModifications()).add(delta);
        }
        PropertyDelta delta = PropertyDelta.createModificationReplaceProperty((new ItemPath(ObjectType.F_METADATA, MetadataType.F_MODIFY_TIMESTAMP)), def, XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
        ((Collection) change.getModifications()).add(delta);
        if (task.getOwner() != null) {
            ReferenceDelta refDelta = ReferenceDelta.createModificationReplace((new ItemPath(ObjectType.F_METADATA,
                    MetadataType.F_MODIFIER_REF)), def, task.getOwner().getOid());
            ((Collection) change.getModifications()).add(refDelta);
        }

        List<PrismReferenceValue> approverReferenceValues = new ArrayList<PrismReferenceValue>();

        if (workflowManager != null) {
            for (ObjectReferenceType approverRef : workflowManager.getApprovedBy(task, result)) {
                approverReferenceValues.add(new PrismReferenceValue(approverRef.getOid()));
            }
        }

        if (!approverReferenceValues.isEmpty()) {
            ReferenceDelta refDelta = ReferenceDelta.createModificationReplace((new ItemPath(ObjectType.F_METADATA,
                        MetadataType.F_MODIFY_APPROVER_REF)), def, approverReferenceValues);
            ((Collection) change.getModifications()).add(refDelta);
        } else {

            // a bit of hack - we want to replace all existing values with empty set of values;
            // however, it is not possible to do this using REPLACE, so we have to explicitly remove all existing values

            if (objectContext != null && objectContext.getObjectOld() != null) {
                // a null value of objectOld means that we execute MODIFY delta that is a part of primary ADD operation (in a wave greater than 0)
                // i.e. there are NO modifyApprovers set (theoretically they could be set in previous waves, but because in these waves the data
                // are taken from the same source as in this step - so there are none modify approvers).

                if (objectContext.getObjectOld().asObjectable().getMetadata() != null) {
                    List<ObjectReferenceType> existingModifyApproverRefs = objectContext.getObjectOld().asObjectable().getMetadata().getModifyApproverRef();
                    LOGGER.trace("Original values of MODIFY_APPROVER_REF: {}", existingModifyApproverRefs);

                    if (!existingModifyApproverRefs.isEmpty()) {
                        List<PrismReferenceValue> valuesToDelete = new ArrayList<PrismReferenceValue>();
                        for (ObjectReferenceType approverRef : objectContext.getObjectOld().asObjectable().getMetadata().getModifyApproverRef()) {
                            valuesToDelete.add(new PrismReferenceValue(approverRef.getOid()));
                        }
                        ReferenceDelta refDelta = ReferenceDelta.createModificationDelete((new ItemPath(ObjectType.F_METADATA,
                                MetadataType.F_MODIFY_APPROVER_REF)), def, valuesToDelete);
                        ((Collection) change.getModifications()).add(refDelta);
                    }
                }
            }
        }

    }

    private String addTask(TaskType task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException {
        try {
            return taskManager.addTask(task.asPrismObject(), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private <F extends ObjectType, T extends ObjectType> String addProvisioningObject(PrismObject<T> object,
    		LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options, ResourceType resource,
            Task task, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        if (object.canRepresent(ShadowType.class)) {
            ShadowType shadow = (ShadowType) object.asObjectable();
            String resourceOid = ShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
        }

        OperationProvisioningScriptsType scripts = prepareScripts(object, context, objectContext, ProvisioningOperationTypeType.ADD,
                resource, task, result);
        Utils.setRequestee(task, context);
        String oid = provisioning.addObject(object, scripts, options, task, result);
        Utils.clearRequestee(task);
        return oid;
    }

    private <F extends ObjectType, T extends ObjectType> void deleteProvisioningObject(Class<T> objectTypeClass,
            String oid, LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options,
            ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	
		OperationProvisioningScriptsType scripts = null;
		try {
			PrismObject<T> shadowToModify = provisioning.getObject(objectTypeClass, oid,
					SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
			scripts = prepareScripts(shadowToModify, context, objectContext, ProvisioningOperationTypeType.DELETE, resource,
					task, result);
		} catch (ObjectNotFoundException ex) {
			// this is almost OK, mute the error and try to delete account (it
			// will fail if something is wrong)
			result.muteLastSubresultError();
		}
        Utils.setRequestee(task, context);
		provisioning.deleteObject(objectTypeClass, oid, options, scripts, task, result);
        Utils.clearRequestee(task);
    }

    private <F extends ObjectType, T extends ObjectType> String modifyProvisioningObject(Class<T> objectTypeClass, String oid,
            Collection<? extends ItemDelta> modifications, LensContext<F> context, LensElementContext<T> objectContext, ProvisioningOperationOptions options,
            ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

    	PrismObject<T> shadowToModify = provisioning.getObject(objectTypeClass, oid,
    			SelectorOptions.createCollection(GetOperationOptions.createRaw()), task, result);
    	OperationProvisioningScriptsType scripts = prepareScripts(shadowToModify, context, objectContext,
                ProvisioningOperationTypeType.MODIFY, resource, task, result);
        Utils.setRequestee(task, context);
        String changedOid = provisioning.modifyObject(objectTypeClass, oid, modifications, scripts, options, task, result);
        Utils.clearRequestee(task);
        return changedOid;
    }

    private <F extends ObjectType, T extends ObjectType> OperationProvisioningScriptsType prepareScripts(
    		PrismObject<T> changedObject, LensContext<F> context, LensElementContext<T> objectContext,
    		ProvisioningOperationTypeType operation, ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	
    	if (!changedObject.canRepresent(ShadowType.class)) {
    		return null;
    	}
    	
    	if (resource == null){
    		LOGGER.warn("Resource does not exist. Skipping processing scripts.");
    		return null;
    	}
    	OperationProvisioningScriptsType resourceScripts = resource.getScripts();
    	PrismObject<? extends ShadowType> resourceObject = (PrismObject<? extends ShadowType>) changedObject;
        
        PrismObject<F> user = null;
		if (context.getFocusContext() != null){
			if (context.getFocusContext().getObjectNew() != null){
			user = context.getFocusContext().getObjectNew();
			} else if (context.getFocusContext().getObjectOld() != null){
				user = context.getFocusContext().getObjectOld();
			}	
		}

        ResourceShadowDiscriminator discr = ((LensProjectionContext) objectContext).getResourceShadowDiscriminator();

        ExpressionVariables variables = Utils.getDefaultExpressionVariables(user, resourceObject, discr, resource.asPrismObject());
        return evaluateScript(resourceScripts, discr, operation, null, variables, task, result);
      
    }
	
	private OperationProvisioningScriptsType evaluateScript(OperationProvisioningScriptsType resourceScripts,
            ResourceShadowDiscriminator discr, ProvisioningOperationTypeType operation, BeforeAfterType order, 
            ExpressionVariables variables, Task task, OperationResult result) 
            		throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
		  OperationProvisioningScriptsType outScripts = new OperationProvisioningScriptsType();
	        if (resourceScripts != null) {
	        	OperationProvisioningScriptsType scripts = resourceScripts.clone();
	        	for (OperationProvisioningScriptType script: scripts.getScript()) {
                    if (discr != null) {
                        if (script.getKind() != null && !script.getKind().isEmpty() && !script.getKind().contains(discr.getKind())) {
                            continue;
                        }
                        if (script.getIntent() != null && !script.getIntent().isEmpty()
                                && !script.getIntent().contains(discr.getIntent()) && discr.getIntent() != null) {
                            continue;
                        }
                    }
	        		if (script.getOperation().contains(operation)) {
	        			if (order == null || order == script.getOrder()) {
		        			for (ProvisioningScriptArgumentType argument : script.getArgument()){
		        				evaluateScriptArgument(argument, variables, task, result);
		        			}
		        			outScripts.getScript().add(script);
	        			}
	        		}
	        	}
	        }

	        return outScripts;
	}
    
    private void evaluateScriptArgument(ProvisioningScriptArgumentType argument, ExpressionVariables variables, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
    	
    	QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");
    	
    	PrismPropertyDefinition scriptArgumentDefinition = new PrismPropertyDefinition(FAKE_SCRIPT_ARGUMENT_NAME,
				DOMUtil.XSD_STRING, prismContext);
    	
    	String shortDesc = "Provisioning script argument expression";
    	Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(argument, scriptArgumentDefinition, shortDesc, result);
    	
    	
    	ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(params);
		
		Collection<PrismPropertyValue<String>> nonNegativeValues = null;
		if (outputTriple != null) {
			nonNegativeValues = outputTriple.getNonNegativeValues();
		}
			
		//replace dynamic script with static value..
		argument.getExpressionEvaluator().clear();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			// We need to create at least one evaluator. Otherwise the expression code will complain
//			Element value = DOMUtil.createElement(SchemaConstants.C_VALUE);
//			DOMUtil.setNill(value);
			JAXBElement<RawType> el = new JAXBElement(SchemaConstants.C_VALUE, RawType.class, new RawType());
			argument.getExpressionEvaluator().add(el);
			
		} else {
			for (PrismPropertyValue<String> val : nonNegativeValues){
//				Element value = DOMUtil.createElement(SchemaConstants.C_VALUE);
//				value.setTextContent(val.getValue());
				PrimitiveXNode<String> prim = new PrimitiveXNode<>();
				prim.setValue(val.getValue());
				prim.setTypeQName(DOMUtil.XSD_STRING);
				JAXBElement<RawType> el = new JAXBElement(SchemaConstants.C_VALUE, RawType.class, new RawType(prim));
				argument.getExpressionEvaluator().add(el);
			}
		}
	}
    
    private <T extends ObjectType, F extends ObjectType>
	void executeReconciliationScript(LensProjectionContext projContext, LensContext<F> context,
			BeforeAfterType order, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
    	
    	if (!projContext.isDoReconciliation()) {
    		return;
    	}
    	
    	ResourceType resource = projContext.getResource();
    	if (resource == null){
    		LOGGER.warn("Resource does not exist. Skipping processing reconciliation scripts.");
    		return;
    	}
    	
    	OperationProvisioningScriptsType resourceScripts = resource.getScripts();
    	if (resourceScripts == null) {
    		return;
    	}
        
        PrismObject<F> user = null;
        PrismObject<ShadowType> shadow = null;
        
		if (context.getFocusContext() != null){
			if (context.getFocusContext().getObjectNew() != null){
				user = context.getFocusContext().getObjectNew();
				} else if (context.getFocusContext().getObjectOld() != null){
					user = context.getFocusContext().getObjectOld();
				}	
//			if (order == ProvisioningScriptOrderType.BEFORE) {
//				user = context.getFocusContext().getObjectOld();
//			} else if (order == ProvisioningScriptOrderType.AFTER) {
//				user = context.getFocusContext().getObjectNew();
//			} else {
//				throw new IllegalArgumentException("Unknown order "+order);
//			}	
		}
		
		if (order == BeforeAfterType.BEFORE) {
			shadow = (PrismObject<ShadowType>) projContext.getObjectOld();
		} else if (order == BeforeAfterType.AFTER) {
			shadow = (PrismObject<ShadowType>) projContext.getObjectNew();
		} else {
			throw new IllegalArgumentException("Unknown order "+order);
		}
        
		ExpressionVariables variables = Utils.getDefaultExpressionVariables(user, shadow,
                projContext.getResourceShadowDiscriminator(), resource.asPrismObject());
        OperationProvisioningScriptsType evaluatedScript = evaluateScript(resourceScripts,
                projContext.getResourceShadowDiscriminator(),
        		ProvisioningOperationTypeType.RECONCILE, order, variables, task, parentResult);

        for (OperationProvisioningScriptType script: evaluatedScript.getScript()) {
            Utils.setRequestee(task, context);
        	provisioning.executeScript(resource.getOid(), script, task, parentResult);
            Utils.clearRequestee(task);
        }
    }

}
