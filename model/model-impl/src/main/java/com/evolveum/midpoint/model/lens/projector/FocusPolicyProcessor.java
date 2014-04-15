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
package com.evolveum.midpoint.model.lens.projector;

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.xpath.FoundIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Processor to handle user template and possible also other user "policy"
 * elements.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class FocusPolicyProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(FocusPolicyProcessor.class);

	private PrismContainerDefinition<ActivationType> activationDefinition;
	
	@Autowired(required = true)
	private MappingFactory mappingFactory;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private PasswordPolicyProcessor passwordPolicyProcessor;
	
	@Autowired(required = true)
	private ModelObjectResolver modelObjectResolver;
	
	@Autowired(required = true)
	private ActivationComputer activationComputer;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
    private MappingEvaluationHelper mappingHelper;

	<O extends ObjectType, F extends FocusType> void processUserPolicy(LensContext<O> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException {

		LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for FocusType objects.
    		return;
    	}
    	
    	LensContext<F> fContext = (LensContext<F>) context;
    	LensFocusContext<F> fFocusContext = fContext.getFocusContext();
    	
    	ObjectDelta<F> focusDelta = fFocusContext.getDelta();
    	if (focusDelta != null && focusDelta.isDelete()) {
    		return;
    	}
    	
		passwordPolicyProcessor.processPasswordPolicy(fFocusContext, fContext, result);
		
		processActivation(fContext, now, result);

		applyUserTemplate(fContext, now, task, result);

	}

	private <F extends FocusType> void processActivation(LensContext<F> context, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		
		if (focusContext.isDelete()) {
			return;
		}
		
		TimeIntervalStatusType validityStatusNew = null;
		TimeIntervalStatusType validityStatusOld = null;
		XMLGregorianCalendar validityChangeTimestamp = null;
		
		PrismObject<F> focusNew = focusContext.getObjectNew();
		F focusNewType = focusNew.asObjectable();
		
		ActivationType activationNew = focusNewType.getActivation();
		ActivationType activationOld = null;
		
		if (activationNew != null) {
			validityStatusNew = activationComputer.getValidityStatus(activationNew, now);
			validityChangeTimestamp = activationNew.getValidityChangeTimestamp();
		}
		
		PrismObject<F> focusOld = focusContext.getObjectOld();
		if (focusOld != null) {
			F focusOldType = focusOld.asObjectable();
			activationOld = focusOldType.getActivation();
			if (activationOld != null) {
				validityStatusOld = activationComputer.getValidityStatus(activationOld, validityChangeTimestamp);
			}
		}
		
		if (validityStatusOld == validityStatusNew) {
			// No change, (almost) no work
			if (validityStatusNew != null && activationNew.getValidityStatus() == null) {
				// There was no validity change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordValidityDelta(focusContext, validityStatusNew, now);
			} else {
				LOGGER.trace("Skipping validity processing because there was no change ({} -> {})", validityStatusOld, validityStatusNew);
			}
		} else {
			LOGGER.trace("Validity change {} -> {}", validityStatusOld, validityStatusNew);
			recordValidityDelta(focusContext, validityStatusNew, now);
		}
		
		ActivationStatusType effectiveStatusNew = activationComputer.getEffectiveStatus(activationNew, validityStatusNew);
		ActivationStatusType effectiveStatusOld = activationComputer.getEffectiveStatus(activationOld, validityStatusOld);
		
		if (effectiveStatusOld == effectiveStatusNew) {
			// No change, (almost) no work
			if (effectiveStatusNew != null && (activationNew == null || activationNew.getEffectiveStatus() == null)) {
				// There was no effective status change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
			} else {
				if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().hasItemDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
					LOGGER.trace("Forcing effective status delta even though there was no change ({} -> {}) because there is explicit administrativeStatus delta", effectiveStatusOld, effectiveStatusNew);
					// We need this to force the change down to the projections later in the activation processor
					// some of the mappings will use effectiveStatus as a source, therefore there has to be a delta for the mapping to work correctly
					recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
				} else {
					LOGGER.trace("Skipping effective status processing because there was no change ({} -> {})", effectiveStatusOld, effectiveStatusNew);
				}
			}
		} else {
			LOGGER.trace("Effective status change {} -> {}", effectiveStatusOld, effectiveStatusNew);
			recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
		}
	}
	
	private <F extends ObjectType> void recordValidityDelta(LensFocusContext<F> focusContext, TimeIntervalStatusType validityStatusNew,
			XMLGregorianCalendar now) throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		PrismPropertyDefinition<TimeIntervalStatusType> validityStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_STATUS);
		PropertyDelta<TimeIntervalStatusType> validityStatusDelta 
				= validityStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS));
		if (validityStatusNew == null) {
			validityStatusDelta.setValueToReplace();
		} else {
			validityStatusDelta.setValueToReplace(new PrismPropertyValue<TimeIntervalStatusType>(validityStatusNew, OriginType.USER_POLICY, null));
		}
		focusContext.swallowToProjectionWaveSecondaryDelta(validityStatusDelta);
		
		PrismPropertyDefinition<XMLGregorianCalendar> validityChangeTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
		PropertyDelta<XMLGregorianCalendar> validityChangeTimestampDelta 
				= validityChangeTimestampDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP));
		validityChangeTimestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(now, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(validityChangeTimestampDelta);
	}
	
	private <F extends ObjectType> void recordEffectiveStatusDelta(LensFocusContext<F> focusContext, 
			ActivationStatusType effectiveStatusNew, XMLGregorianCalendar now)
			throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		PrismPropertyDefinition<ActivationStatusType> effectiveStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_EFFECTIVE_STATUS);
		PropertyDelta<ActivationStatusType> effectiveStatusDelta 
				= effectiveStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		effectiveStatusDelta.setValueToReplace(new PrismPropertyValue<ActivationStatusType>(effectiveStatusNew, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(effectiveStatusDelta);
		
		PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(effectiveStatusNew, now, activationDefinition, OriginType.USER_POLICY);
		focusContext.swallowToProjectionWaveSecondaryDelta(timestampDelta);
	}
	
	private <F extends FocusType> void applyUserTemplate(LensContext<F> context, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, ObjectAlreadyExistsException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		ObjectTemplateType userTemplate = context.getFocusTemplate();

		if (userTemplate == null) {
			// No applicable template
			LOGGER.trace("Skipping processing of user template: no user template");
			return;
		}
		
		int maxIterations = LensUtil.determineMaxIterations(userTemplate.getIteration());
		int iteration = focusContext.getIteration();
		String iterationToken = focusContext.getIterationToken();
		boolean wasResetIterationCounter = false;
		
		// This is fixed now. TODO: make it configurable
		boolean resetOnRename = true;

		ObjectDelta<F> userSecondaryDelta = focusContext.getProjectionWaveSecondaryDelta();
		ObjectDelta<F> userPrimaryDelta = focusContext.getProjectionWavePrimaryDelta();
		ObjectDeltaObject<F> userOdo = focusContext.getObjectDeltaObject();
		PrismObjectDefinition<F> focusDefinition = getFocusDefinition(focusContext.getObjectTypeClass());
		Collection<ItemDelta<? extends PrismValue>> itemDeltas = null;
		XMLGregorianCalendar nextRecomputeTime = null;
		
		PrismObject<F> focusCurrent = focusContext.getObjectCurrent();
		if (focusCurrent != null && iterationToken == null) {
			Integer focusIteration = focusCurrent.asObjectable().getIteration();
			if (focusIteration != null) {
				iteration = focusIteration;
			}
			iterationToken = focusCurrent.asObjectable().getIterationToken();
		}
	
		while (true) {
		
			ExpressionVariables variables = Utils.getDefaultExpressionVariables(focusContext.getObjectNew(), null, null, null);
			if (iterationToken == null) {
				iterationToken = LensUtil.formatIterationToken(context, focusContext, 
					userTemplate.getIteration(), iteration, expressionFactory, variables, task, result);
			}
			
			LOGGER.trace("Applying {} to {}, iteration {} ({})", 
					new Object[]{userTemplate, focusContext.getObjectNew(), iteration, iterationToken});
			
			String conflictMessage;
			if (!LensUtil.evaluateIterationCondition(context, focusContext, 
					userTemplate.getIteration(), iteration, iterationToken, true, expressionFactory, variables, task, result)) {
				
				conflictMessage = "pre-iteration condition was false";
				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the pre-iteration condition was false",
						new Object[]{iteration, iterationToken, focusContext.getHumanReadableName()});
			} else {
			
				Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap 
					= new HashMap<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>>();
				
				nextRecomputeTime = collectTripleFromTemplate(context, userTemplate, userOdo, outputTripleMap,
						iteration, iterationToken,
						now, userTemplate.toString(), task, result);
				
				DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>> nameTriple = outputTripleMap.get(new ItemPath(FocusType.F_NAME));
				if (resetOnRename && !wasResetIterationCounter && nameTriple != null && 
						focusContext.getIterationToken() == null && (nameTriple.hasPlusSet() || nameTriple.hasMinusSet())) {
					// Make sure this happens only the very first time during the first recompute.
					// Otherwise it will always change the token (especially if the token expression has a random part)
					// hence the focusContext.getIterationToken() == null
		        	wasResetIterationCounter = true;
		        	iteration = 0;
		    		iterationToken = null;
		    		LOGGER.trace("Resetting iteration counter and token because rename was detected");
		    		continue;
		        }
				
				itemDeltas = new ArrayList<>();
				for (Entry<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> entry: outputTripleMap.entrySet()) {
					ItemPath itemPath = entry.getKey();
					DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>> outputTriple = entry.getValue();
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Computed triple for {}:\n{}", itemPath, outputTriple.debugDump());
					}
					ItemDelta<? extends PrismValue> apropriItemDelta = null;
//					boolean addUnchangedValues = focusContext.isAdd();
					// We need to add unchanged values otherwise the unconditional mappings will not be applies
					boolean addUnchangedValues = true;
					ItemDelta<? extends PrismValue> itemDelta = LensUtil.consolidateTripleToDelta(itemPath, (DeltaSetTriple)outputTriple,
							focusDefinition.findItemDefinition(itemPath), apropriItemDelta, userOdo.getNewObject(), null, 
							addUnchangedValues, true, false, "object template "+userTemplate, true);
					
					itemDelta.simplify();
					itemDelta.validate("object template "+userTemplate);
					itemDeltas.add(itemDelta);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Computed delta:\n{}", itemDelta.debugDump());
					}
				}
				
				// construct objectNew as the preview how the change will look like
				// We do NOT want to this in the context because there is a change that this won't be
				// unique and we will need to drop all the deltas and start again
				PrismObject<F> previewObjectNew;
				PrismObject<F> previewBase = focusContext.getObjectNew();
				if (itemDeltas.isEmpty()) {
					// No change
		        	previewObjectNew = previewBase;
				} else {
			    	ObjectDelta<F> previewDelta = ObjectDelta.createEmptyModifyDelta(focusContext.getObjectTypeClass(), focusContext.getOid(), prismContext);
			        for (ItemDelta<? extends PrismValue> itemDelta: itemDeltas) {
			        	previewDelta.addModification(itemDelta.clone());
			        }
			        if (previewBase == null) {
			        	previewBase = focusDefinition.instantiate();
			        }
			        LOGGER.trace("previewDelta={}, previewBase={}", previewDelta, previewBase);
		        	previewObjectNew = previewDelta.computeChangedObject(previewBase);
		        }
				LOGGER.trace("previewObjectNew={}, itemDeltas={}", previewObjectNew, itemDeltas);
	
				if (previewObjectNew == null) {
					// this must be delete
				} else {
			        // Explicitly check for name. The checker would check for this also. But checking it here
					// will produce better error message
					PolyStringType objectName = previewObjectNew.asObjectable().getName();
					if (objectName == null || objectName.getOrig().isEmpty()) {
						throw new SchemaException("No name in new object "+objectName+" as produced by template "+userTemplate+
								" in iteration "+iteration+", we cannot process an object without a name");
					}
				}
				
				// Check if iteration constraints are OK
				FocusConstraintsChecker<F> checker = new FocusConstraintsChecker<>();
				checker.setPrismContext(prismContext);
		        checker.setContext(context);
		        checker.setRepositoryService(cacheRepositoryService);
		        checker.check(previewObjectNew, result);
		        if (checker.isSatisfiesConstraints()) {
		        	LOGGER.trace("Current focus satisfies uniqueness constraints. Iteration {}, token '{}'", iteration, iterationToken);
		        	
		        	if (LensUtil.evaluateIterationCondition(context, focusContext, 
		        			userTemplate.getIteration(), iteration, iterationToken, false, expressionFactory, variables, 
		        			task, result)) {
	    				// stop the iterations
	    				break;
	    			} else {
	    				conflictMessage = "post-iteration condition was false";
	    				LOGGER.debug("Skipping iteration {}, token '{}' for {} because the post-iteration condition was false",
	    						new Object[]{iteration, iterationToken, focusContext.getHumanReadableName()});
	    			}
		        } 
		        LOGGER.trace("Current focus does not satisfy constraints. Conflicting object: {}; iteration={}, maxIterations={}",
		        		new Object[]{checker.getConflictingObject(), iteration, maxIterations});
		        conflictMessage = checker.getMessages();
		        
				if (!wasResetIterationCounter) {
		        	wasResetIterationCounter = true;
			        if (iteration != 0) {
			        	iterationToken = null;
			        	iteration = 0;
			    		LOGGER.trace("Resetting iteration counter and token after conflict");
			    		continue;
			        }
		        }
			}
				        
	        // Next iteration
			iteration++;
	        iterationToken = null;
	        if (iteration > maxIterations) {
	        	StringBuilder sb = new StringBuilder();
	        	if (iteration == 1) {
	        		sb.append("Error processing ");
	        	} else {
	        		sb.append("Too many iterations ("+iteration+") for ");
	        	}
	        	sb.append(focusContext.getHumanReadableName());
	        	if (iteration == 1) {
	        		sb.append(": constraint violation: ");
	        	} else {
	        		sb.append(": cannot determine values that satisfy constraints: ");
	        	}
	        	if (conflictMessage != null) {
	        		sb.append(conflictMessage);
	        	}
	        	throw new ObjectAlreadyExistsException(sb.toString());
	        }
		}
			
		// Apply iteration deltas
		for (ItemDelta<? extends PrismValue> itemDelta: itemDeltas) {
			
			if (itemDelta != null && !itemDelta.isEmpty()) {
				if (userPrimaryDelta == null || !userPrimaryDelta.containsModification(itemDelta)) {
					if (userSecondaryDelta == null) {
						userSecondaryDelta = new ObjectDelta<F>(focusContext.getObjectTypeClass(), ChangeType.MODIFY, prismContext);
						if (focusContext.getObjectNew() != null && focusContext.getObjectNew().getOid() != null){
							userSecondaryDelta.setOid(focusContext.getObjectNew().getOid());
						}
						focusContext.setProjectionWaveSecondaryDelta(userSecondaryDelta);
					}
					userSecondaryDelta.mergeModification(itemDelta);
				}
			}
		}
		
		// We have to remember the token and iteration in the context.
		// The context can be recomputed several times. But we always want
		// to use the same iterationToken if possible. If there is a random
		// part in the iterationToken expression that we need to avoid recomputing
		// the token otherwise the value can change all the time (even for the same inputs).
		// Storing the token in the secondary delta is not enough because secondary deltas can be dropped
		// if the context is re-projected.
		focusContext.setIteration(iteration);
		focusContext.setIterationToken(iterationToken);
		addIterationTokenDeltas(focusContext, iteration, iterationToken);
		
		if (nextRecomputeTime != null) {
			
			boolean alreadyHasTrigger = false;
			PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
			if (objectCurrent != null) {
				for (TriggerType trigger: objectCurrent.asObjectable().getTrigger()) {
					if (RecomputeTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri()) &&
							nextRecomputeTime.equals(trigger.getTimestamp())) {
								alreadyHasTrigger = true;
								break;
					}
				}
			}
			
			if (!alreadyHasTrigger) {
				PrismObjectDefinition<F> objectDefinition = focusContext.getObjectDefinition();
				PrismContainerDefinition<TriggerType> triggerContDef = objectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
				ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
				PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
				triggerDelta.addValueToAdd(triggerCVal);
				TriggerType triggerType = triggerCVal.asContainerable();
				triggerType.setTimestamp(nextRecomputeTime);
				triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);
				
				focusContext.swallowToProjectionWaveSecondaryDelta(triggerDelta);
			}
		}

	}

	private <F extends FocusType> XMLGregorianCalendar collectTripleFromTemplate(LensContext<F> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<F> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			int iteration, String iterationToken,
			XMLGregorianCalendar now, String contextDesc, Task task, OperationResult result)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		// Process includes
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> includeObject = includeRef.asReferenceValue().getObject();
			if (includeObject == null) {
				ObjectTemplateType includeObjectType = modelObjectResolver.resolve(includeRef, ObjectTemplateType.class, 
						null, "include reference in "+objectTemplateType + " in " + contextDesc, result);
				includeObject = includeObjectType.asPrismObject();
				// Store resolved object for future use (e.g. next waves).
				includeRef.asReferenceValue().setObject(includeObject);
			}
			LOGGER.trace("Including template {}", includeObject);
			ObjectTemplateType includeObjectType = includeObject.asObjectable();
			XMLGregorianCalendar includeNextRecomputeTime = collectTripleFromTemplate(context, includeObjectType, userOdo, 
					outputTripleMap, iteration, iterationToken, 
					now, "include "+includeObject+" in "+objectTemplateType + " in " + contextDesc, task, result);
			if (includeNextRecomputeTime != null) {
				if (nextRecomputeTime == null || nextRecomputeTime.compare(includeNextRecomputeTime) == DatatypeConstants.GREATER) {
					nextRecomputeTime = includeNextRecomputeTime;
				}
			}
		}
		
		// Process own mappings
		Collection<MappingType> mappings = objectTemplateType.getMapping();
		XMLGregorianCalendar templateNextRecomputeTime = collectTripleFromMappings(mappings, context, objectTemplateType, userOdo, 
				outputTripleMap, iteration, iterationToken, now, contextDesc, task, result);
		if (templateNextRecomputeTime != null) {
			if (nextRecomputeTime == null || nextRecomputeTime.compare(templateNextRecomputeTime) == DatatypeConstants.GREATER) {
				nextRecomputeTime = templateNextRecomputeTime;
			}
		}
		
		return nextRecomputeTime;
	}
	
	
	private <V extends PrismValue, F extends FocusType> XMLGregorianCalendar collectTripleFromMappings(Collection<MappingType> mappings, LensContext<F> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<F> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			int iteration, String iterationToken,
			XMLGregorianCalendar now, String contextDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		for (MappingType mappingType : mappings) {
			Mapping<V> mapping = createMapping(context, mappingType, objectTemplateType, userOdo, 
					iteration, iterationToken, now, contextDesc, result);
			if (mapping == null) {
				continue;
			}
			
			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(result);
			
			if (timeConstraintValid != null && !timeConstraintValid) {
				// Delayed mapping. Just schedule recompute time
				XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
				LOGGER.trace("Evaluation of mapping {} delayed to {}", mapping, mappingNextRecomputeTime);
				if (mappingNextRecomputeTime != null) {
					if (nextRecomputeTime == null || nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER) {
						nextRecomputeTime = mappingNextRecomputeTime;
					}
				}
				continue;
			}
			
			LensUtil.evaluateMapping(mapping, context, task, result);
			
			ItemPath itemPath = mapping.getOutputPath();
			DeltaSetTriple<ItemValueWithOrigin<V>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
			if (outputTriple == null) {
				continue;
			}
			DeltaSetTriple<ItemValueWithOrigin<V>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V>>) outputTripleMap.get(itemPath);
			if (mapTriple == null) {
				outputTripleMap.put(itemPath, outputTriple);
			} else {
				mapTriple.merge(outputTriple);
			}
		}
		
		return nextRecomputeTime;
	}
	
	private <V extends PrismValue, F extends FocusType> Mapping<V> createMapping(final LensContext<F> context, final MappingType mappingType, ObjectTemplateType userTemplate, 
			ObjectDeltaObject<F> userOdo, int iteration, String iterationToken, XMLGregorianCalendar now, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<V> mapping = mappingFactory.createMapping(mappingType,
				"object template mapping in " + contextDesc
				+ " while processing user " + userOdo.getAnyObject());
		
		if (!mapping.isApplicableToChannel(context.getChannel())) {
			return null;
		}
		
		mapping.setSourceContext(userOdo);
		mapping.setTargetContext(getFocusDefinition(context.getFocusClass()));
		mapping.setRootNode(userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION, iteration);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken);
		mapping.setOriginType(OriginType.USER_POLICY);
		mapping.setOriginObject(userTemplate);
		mapping.setNow(now);

		ItemDefinition outputDefinition = mapping.getOutputDefinition();
		ItemPath itemPath = mapping.getOutputPath();
		
		Item<V> existingUserItem = (Item<V>) userOdo.getNewObject().findItem(itemPath);
		if (existingUserItem != null && !existingUserItem.isEmpty() 
				&& mapping.getStrength() == MappingStrengthType.WEAK) {
			// This valueConstruction only applies if the property does not have a value yet.
			// ... but it does
			return null;
		}

		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private ItemPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				if (outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					ValuePolicyType passwordPolicy = context.getGlobalPasswordPolicy();
					if (passwordPolicy == null) {
						return null;
					}
					return passwordPolicy.getStringPolicy();
				}
				if (mappingType.getExpression() != null){
					List<JAXBElement<?>> evaluators = mappingType.getExpression().getExpressionEvaluator();
					if (evaluators != null){
						for (JAXBElement jaxbEvaluator : evaluators){
							Object object = jaxbEvaluator.getValue();
							if (object != null && object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null){
								ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
								try{
								ValuePolicyType valuePolicyType = mappingFactory.getObjectResolver().resolve(ref, ValuePolicyType.class, 
										null, "resolving value policy for generate attribute "+ outputDefinition.getName()+" value", new OperationResult("Resolving value policy"));
								if (valuePolicyType != null){
									return valuePolicyType.getStringPolicy();
								}
								} catch (Exception ex){
									throw new SystemException(ex.getMessage(), ex);
								}
							}
						}
						
					}
				}
				return null;
				
			}
		};
		mapping.setStringPolicyResolver(stringPolicyResolver);

		return mapping;
	}

	private <V extends PrismValue> boolean hasValue(Item<V> existingUserItem, V newValue) {
		if (existingUserItem == null) {
			return false;
		}
		return existingUserItem.contains(newValue, true);
	}


	private <F extends ObjectType> PrismObjectDefinition<F> getFocusDefinition(Class<F> focusClass) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
	}
	
	private PrismContainerDefinition<ActivationType> getActivationDefinition() {
		if (activationDefinition == null) {
			ComplexTypeDefinition focusDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinition(FocusType.COMPLEX_TYPE);
			activationDefinition = focusDefinition.findContainerDefinition(FocusType.F_ACTIVATION);
		}
		return activationDefinition;
	}
	
	/**
	 * Adds deltas for iteration and iterationToken to the focus if needed.
	 */
	private <F extends FocusType> void addIterationTokenDeltas(LensFocusContext<F> focusContext, int iteration, String iterationToken) throws SchemaException {
		PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
		if (objectCurrent != null) {
			Integer iterationOld = objectCurrent.asObjectable().getIteration();
			String iterationTokenOld = objectCurrent.asObjectable().getIterationToken();
			if (iterationOld != null && iterationOld == iteration &&
					iterationTokenOld != null && iterationTokenOld.equals(iterationToken)) {
				// Already stored
				return;
			}
		}
		PrismObjectDefinition<F> objDef = focusContext.getObjectDefinition();
		
		PrismPropertyValue<Integer> iterationVal = new PrismPropertyValue<Integer>(iteration);
		iterationVal.setOriginType(OriginType.USER_POLICY);
		PropertyDelta<Integer> iterationDelta = PropertyDelta.createReplaceDelta(objDef, 
				FocusType.F_ITERATION, iterationVal);
		focusContext.swallowToSecondaryDelta(iterationDelta);
		
		PrismPropertyValue<String> iterationTokenVal = new PrismPropertyValue<String>(iterationToken);
		iterationTokenVal.setOriginType(OriginType.USER_POLICY);
		PropertyDelta<String> iterationTokenDelta = PropertyDelta.createReplaceDelta(objDef, 
				FocusType.F_ITERATION_TOKEN, iterationTokenVal);
		focusContext.swallowToSecondaryDelta(iterationTokenDelta);
		
	}

}
