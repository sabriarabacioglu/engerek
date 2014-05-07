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
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyStrictnessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Projector recomputes the context. It takes the context with a few basic data as input. It uses all the policies 
 * and mappings to derive all the other data. E.g. a context with only a user (primary) delta. It applies user template,
 * outbound mappings and the inbound mappings and then inbound and outbound mappings of other accounts and so on until
 * all the data are computed. The output is the original context with all the computed delta.
 * 
 * Primary deltas are in the input, secondary deltas are computed in projector. Projector "projects" primary deltas to
 * the secondary deltas of user and accounts.
 * 
 * Projector does NOT execute the deltas. It only recomputes the context. It may read a lot of objects (user, accounts, policies).
 * But it does not change any of them.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class Projector {
	
	@Autowired(required = true)
	private ContextLoader contextLoader;
	
	@Autowired(required = true)
    private FocusProcessor focusProcessor;

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;
    
    @Autowired(required = true)
    private ProjectionValuesProcessor projectionValuesProcessor;

    @Autowired(required = true)
    private ReconciliationProcessor reconciliationProcessor;

    @Autowired(required = true)
    private CredentialsProcessor credentialsProcessor;

    @Autowired(required = true)
    private ActivationProcessor activationProcessor;
    
    @Autowired(required = true)
    private Clock clock;
	
	private static final Trace LOGGER = TraceManager.getTrace(Projector.class);
	
	public <F extends ObjectType> void project(LensContext<F> context, String activityDescription,
            Task task, OperationResult parentResult)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
			ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		if (context.getDebugListener() != null) {
			context.getDebugListener().beforeProjection(context);
		}
		
		// Read the time at the beginning so all processors have the same notion of "now"
		// this provides nicer unified timestamp that can be used in equality checks in tests and also for
		// troubleshooting
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		LensUtil.traceContext(LOGGER, activityDescription, "projector start", false, context, false);
		
		if (consistencyChecks) context.checkConsistence();
		
		context.normalize();
		context.resetProjectionWave();
		
		OperationResult result = parentResult.createSubresult(Projector.class.getName() + ".project");
		result.addContext("executionWave", context.getExecutionWave());
		
		// Projector is using a set of "processors" to do parts of its work. The processors will be called in sequence
		// in the following code.
		
		try {
		
			contextLoader.load(context, activityDescription, result);
			// Set the "fresh" mark now so following consistency check will be stricter
			context.setFresh(true);
			if (consistencyChecks) context.checkConsistence();
			
			sortAccountsToWaves(context);
	        // Let's do one extra wave with no accounts in it. This time we expect to get the results of the execution to the user
	        // via inbound, e.g. identifiers generated by the resource, DNs and similar things. Hence the +2 instead of +1
	        int maxWaves = computeMaxWaves(context);
	        // Note that the number of waves may be recomputed as new accounts appear in the context (usually after assignment step).
	                
	        // Start the waves ....
	        LOGGER.trace("WAVE: Starting the waves. There will be {} waves (or so we think now)", maxWaves);
	        context.setProjectionWave(0);
	        while (context.getProjectionWave() < maxWaves) {
	    
	        	LOGGER.trace("WAVE {} (maxWaves={}, executionWave={})", new Object[]{
	        			context.getProjectionWave(), maxWaves, context.getExecutionWave()});
	        	
	        	// Process the focus-related aspects of the context. That means inbound, focus activation,
	        	// object template and assignments.
		        focusProcessor.processFocus(context, activityDescription, now, task, result);
		        context.recomputeFocus();
		        LensUtil.traceContext(LOGGER, activityDescription,"focus processing", false, context, false);
		        if (consistencyChecks) context.checkConsistence();
		
		        sortAccountsToWaves(context);
		        maxWaves = computeMaxWaves(context);
		        LOGGER.trace("Continuing wave {}, maxWaves={}", context.getProjectionWave(), maxWaves);
		        LensUtil.traceContext(LOGGER, activityDescription,"assignments", false, context, true);
		        if (consistencyChecks) context.checkConsistence();
		        LensUtil.checkContextSanity(context, "focus processing", result);
		
		        // Focus-related processing is over. Now we will process projections in a loop.
		        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {

		        	if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN ||
		        			projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
						continue;
		        	}
		        	String projectionDesc;
		        	if (projectionContext.getResource() != null) {
		        		projectionDesc = projectionContext.getResource() + "("+projectionContext.getResourceShadowDiscriminator().getIntent()+")";		        		
		        	} else {
		        		ResourceShadowDiscriminator discr = projectionContext.getResourceShadowDiscriminator();
		        		if (discr != null) {
		        			projectionDesc = projectionContext.getResourceShadowDiscriminator().toString();
		        		} else {
		        			projectionDesc = "(UNKNOWN)";
		        		}
		        	}
		        	
		        	if (projectionContext.getWave() != context.getProjectionWave()) {
		        		// Let's skip accounts that do not belong into this wave.
		        		continue;
		        	}
		        	
		        	LOGGER.trace("WAVE {} PROJECTION {}", context.getProjectionWave(), projectionDesc);

		        	// Some projections may not be loaded at this point, e.g. high-order dependency projections
		        	contextLoader.makeSureProjectionIsLoaded(context, projectionContext, result);
		        	
		        	if (consistencyChecks) context.checkConsistence();
		        	
		        	activationProcessor.processActivation(context, projectionContext, now, task, result);
		        	
		        	projectionContext.recompute();
		        	
		        	LensUtil.traceContext(LOGGER, activityDescription, "projection activation of "+projectionDesc, false, context, false);
		        	
		        	if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN ||
		        			projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
						continue;
		        	}
		        	
		        	if (!checkDependencies(context, projectionContext)) {
		        		continue;
		        	}
		        	
		        	// TODO: decide if we need to continue
		        	
		        	// This is a "composite" processor. it contains several more processor invocations inside
		        	projectionValuesProcessor.process(context, projectionContext, activityDescription, task, result);
		        	
		        	if (consistencyChecks) context.checkConsistence();
		        	
		        	projectionContext.recompute();
		        	//SynchronizerUtil.traceContext("values", context, false);
		        	if (consistencyChecks) context.checkConsistence();
		        	
		        	credentialsProcessor.processCredentials(context, projectionContext, task, result);
		        	
		        	//SynchronizerUtil.traceContext("credentials", context, false);
		        	if (consistencyChecks) context.checkConsistence();
			        
		        	projectionContext.recompute();
		        	LensUtil.traceContext(LOGGER, activityDescription, "projection values and credentials of "+projectionDesc, false, context, true);
			        if (consistencyChecks) context.checkConsistence();
			
			        reconciliationProcessor.processReconciliation(context, projectionContext, result);
			        projectionContext.recompute();
			        LensUtil.traceContext(LOGGER, activityDescription, "projection reconciliation of "+projectionDesc, false, context, false);
			        if (consistencyChecks) context.checkConsistence();
			        
			        
		        }
		        
		        // if there exists some conflicting projection contexts, add them to the context so they will be recomputed in the next wave..
		        addConflictingContexts(context);
		        
		        if (consistencyChecks) context.checkConsistence();
		        
		        context.incrementProjectionWave();
	        }
	        
	        LOGGER.trace("WAVE: Stopping the waves. There was {} waves", context.getProjectionWave());
	        
	        // We can do this only when computation of all the waves is finished. Before that we do not know
	        // activation of every account and therefore cannot decide what is OK and what is not
	        checkDependenciesFinal(context);
	        
	        if (consistencyChecks) context.checkConsistence();
	        
	        recordSuccess(now, result);
	        
		} catch (SchemaException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (PolicyViolationException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (ExpressionEvaluationException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (ObjectNotFoundException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (CommunicationException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (ConfigurationException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (SecurityViolationException e) {
			recordFatalError(e, now, result);
			throw e;
		} catch (RuntimeException e) {
			recordFatalError(e, now, result);
			// This should not normally happen unless there is something really bad or there is a bug.
			// Make sure that it is logged.
			LOGGER.error("Runtime error in projector: {}", e.getMessage(), e);
			throw e;
		} finally {
			if (context.getDebugListener() != null) {
				context.getDebugListener().afterProjection(context);
			}
		}
		
	}

	private <F extends ObjectType> void addConflictingContexts(LensContext<F> context) {
		List<LensProjectionContext> conflictingContexts = projectionValuesProcessor.getConflictingContexts();
		if (conflictingContexts != null || !conflictingContexts.isEmpty()){
			for (LensProjectionContext conflictingContext : conflictingContexts){
				context.addProjectionContext(conflictingContext);
			}
		
			projectionValuesProcessor.getConflictingContexts().clear();
		}
		
	}

	private void recordFatalError(Exception e, XMLGregorianCalendar projectoStartTimestampCal, OperationResult result) {
		result.recordFatalError(e);
		result.cleanupResult(e);
		if (LOGGER.isDebugEnabled()) {
			long projectoStartTimestamp = XmlTypeConverter.toMillis(projectoStartTimestampCal);
	    	long projectorEndTimestamp = clock.currentTimeMillis();
			LOGGER.debug("Projector failed: {}, etime: {} ms", e.getMessage(), (projectorEndTimestamp - projectoStartTimestamp));
		}
	}
	
	private void recordSuccess(XMLGregorianCalendar projectoStartTimestampCal, OperationResult result) {
        result.recordSuccess();
        result.cleanupResult();
        if (LOGGER.isDebugEnabled()) {
        	long projectoStartTimestamp = XmlTypeConverter.toMillis(projectoStartTimestampCal);
        	long projectorEndTimestamp = clock.currentTimeMillis();
        	LOGGER.trace("Projector successful, etime: {} ms", (projectorEndTimestamp - projectoStartTimestamp));
        }
	}

	public <F extends ObjectType> void resetWaves(LensContext<F> context) throws PolicyViolationException {
	}
	
	public <F extends ObjectType> void sortAccountsToWaves(LensContext<F> context) throws PolicyViolationException {
		// Create a snapshot of the projection collection at the beginning of computation.
		// The collection may be changed during computation (projections may be added). We do not want to process
		// these added projections. They are already processed inside the computation.
		// This also avoids ConcurrentModificationException
		LensProjectionContext[] projectionArray = context.getProjectionContexts().toArray(new LensProjectionContext[0]);
		
		// Reset incomplete flag for those contexts that are not yet computed
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			if (projectionContext.getWave() < 0) {
				projectionContext.setWaveIncomplete(true);
			}
		}
		
		for (LensProjectionContext projectionContext: projectionArray) {
			determineAccountWave(context, projectionContext, null, null);
			projectionContext.setWaveIncomplete(false);
		}
		
		if (LOGGER.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				sb.append("\n");
				sb.append(projectionContext.getResourceShadowDiscriminator());
				sb.append(": ");
				sb.append(projectionContext.getWave());
			}
			LOGGER.trace("Projections sorted to waves (projection wave {}, execution wave {}):{}", 
					new Object[]{context.getProjectionWave(), context.getExecutionWave(), sb.toString()});
		}
	}
	
	public <F extends ObjectType> int computeMaxWaves(LensContext<F> context) {
		return context.getMaxWave() + 2;
	}
	
	// TODO: check for circular dependencies
	private <F extends ObjectType> LensProjectionContext determineAccountWave(LensContext<F> context, 
			LensProjectionContext accountContext, ResourceObjectTypeDependencyType inDependency, List<ResourceObjectTypeDependencyType> depPath) throws PolicyViolationException {
		if (!accountContext.isWaveIncomplete()) {
			// This was already processed
			return accountContext;
		}
		if (accountContext.isDelete()) {
			// Ignore dependencies if we are being removed
			accountContext.setWave(0);
			return accountContext;
		}
		if (depPath == null) {
			depPath = new ArrayList<ResourceObjectTypeDependencyType>();
		}
		int determinedWave = 0;
		int determinedOrder = 0;
		for (ResourceObjectTypeDependencyType outDependency: accountContext.getDependencies()) {
			if (inDependency != null && isHigerOrder(outDependency, inDependency)) {
				// There is incomming dependency. Deal only with dependencies of this order and lower
				// otherwise we can end up in endless loop even for legal dependencies.
				continue;
			}
			checkForCircular(depPath, outDependency);
			depPath.add(outDependency);
			ResourceShadowDiscriminator refRat = new ResourceShadowDiscriminator(outDependency, accountContext.getKind());
			LensProjectionContext dependencyAccountContext = findDependencyContext(context, accountContext, outDependency);
			if (dependencyAccountContext == null) {
				ResourceObjectTypeDependencyStrictnessType outDependencyStrictness = ResourceTypeUtil.getDependencyStrictness(outDependency);
				if (outDependencyStrictness == ResourceObjectTypeDependencyStrictnessType.STRICT) {
					throw new PolicyViolationException("Unsatisfied strict dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+": Account not provisioned");
				} else if (outDependencyStrictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
					// independent object not in the context, just ignore it
					LOGGER.debug("Unsatisfied lax dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+"; depencency skipped");
				} else if (outDependencyStrictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
					// independent object not in the context, just ignore it
					LOGGER.debug("Unsatisfied relaxed dependency of account "+accountContext.getResourceShadowDiscriminator()+
						" dependent on "+refRat+"; depencency skipped");
				} else {
					throw new IllegalArgumentException("Unknown dependency strictness "+outDependency.getStrictness()+" in "+refRat);
				}
			} else {
				dependencyAccountContext = determineAccountWave(context, dependencyAccountContext, outDependency, depPath);
				if (dependencyAccountContext.getWave() + 1 > determinedWave) {
					determinedWave = dependencyAccountContext.getWave() + 1;
					if (outDependency.getOrder() == null) {
						determinedOrder = 0;
					} else {
						determinedOrder = outDependency.getOrder();
					}
				}
			}
			depPath.remove(outDependency);
		}
		LensProjectionContext resultAccountContext = accountContext; 
		if (accountContext.getWave() >=0 && accountContext.getWave() != determinedWave) {
			// Wave for this context was set during the run of this method (it was not set when we
			// started, we checked at the beginning). Therefore this context must have been visited again.
			// therefore there is a circular dependency. Therefore we need to create another context to split it.
			resultAccountContext = createAnotherContext(context, accountContext, determinedOrder);
		}
//		LOGGER.trace("Wave for {}: {}", resultAccountContext.getResourceAccountType(), wave);
		resultAccountContext.setWave(determinedWave);
		return resultAccountContext;
	}
	
	private void checkForCircular(List<ResourceObjectTypeDependencyType> depPath,
			ResourceObjectTypeDependencyType outDependency) throws PolicyViolationException {
		for (ResourceObjectTypeDependencyType pathElement: depPath) {
			if (pathElement.equals(outDependency)) {
				StringBuilder sb = new StringBuilder();
				Iterator<ResourceObjectTypeDependencyType> iterator = depPath.iterator();
				while (iterator.hasNext()) {
					ResourceObjectTypeDependencyType el = iterator.next();
					sb.append(el.getResourceRef().getOid());
					if (iterator.hasNext()) {
						sb.append("->");
					}
				}
				throw new PolicyViolationException("Circular dependency, path: "+sb.toString());
			}
		}
	}

	private boolean isHigerOrder(ResourceObjectTypeDependencyType a,
			ResourceObjectTypeDependencyType b) {
		Integer ao = a.getOrder();
		Integer bo = b.getOrder();
		if (ao == null) {
			ao = 0;
		}
		if (bo == null) {
			bo = 0;
		}
		return ao > bo;
	}

	/**
	 * Find context that has the closest order to the dependency.
	 */
	private <F extends ObjectType> LensProjectionContext findDependencyContext(
			LensContext<F> context, LensProjectionContext projContext, ResourceObjectTypeDependencyType dependency){
		ResourceShadowDiscriminator refRat = new ResourceShadowDiscriminator(dependency, projContext.getKind());
		LensProjectionContext selected = null;
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			if (!projectionContext.compareResourceShadowDiscriminator(refRat, false)) {
				continue;
			}
			int ctxOrder = projectionContext.getResourceShadowDiscriminator().getOrder();
			if (ctxOrder > refRat.getOrder()) {
				continue;
			}
			if (selected == null) {
				selected = projectionContext;
			} else {
				if (ctxOrder > selected.getResourceShadowDiscriminator().getOrder()) {
					selected = projectionContext;
				}
			}
		}
		return selected;
	}
	
	private <F extends ObjectType> LensProjectionContext createAnotherContext(LensContext<F> context, 
			LensProjectionContext origProjectionContext, int order) throws PolicyViolationException {
		ResourceShadowDiscriminator origDiscr = origProjectionContext.getResourceShadowDiscriminator();
		ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(origDiscr.getResourceOid(), origDiscr.getKind(), origDiscr.getIntent(), origDiscr.isThombstone());
		discr.setOrder(order);
		LensProjectionContext otherCtx = context.createProjectionContext(discr);
		otherCtx.setResource(origProjectionContext.getResource());
		// Force recon for the new context. This is a primitive way how to avoid phantom changes.
		otherCtx.setDoReconciliation(true);
		return otherCtx;
	}
	
	/**
	 * Check that the dependencies are still satisfied. Also check for high-ordes vs low-order operation consistency
	 * and stuff like that. 
	 */
	private <F extends ObjectType> boolean checkDependencies(LensContext<F> context, 
    		LensProjectionContext accountContext) throws PolicyViolationException {
		if (accountContext.isDelete()) {
			// It is OK if we depend on something that is not there if we are being removed ... for now
			return true;
		}
		
		if (accountContext.getOid() == null || accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD) {
			// Check for lower-order contexts
			LensProjectionContext lowerOrderContext = null;
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				if (accountContext == projectionContext) {
					continue;
				}
				if (projectionContext.compareResourceShadowDiscriminator(accountContext.getResourceShadowDiscriminator(), false) &&
						projectionContext.getResourceShadowDiscriminator().getOrder() < accountContext.getResourceShadowDiscriminator().getOrder()) {
					if (projectionContext.getOid() != null) {
						lowerOrderContext = projectionContext;
						break;
					}
				}
			}
			if (lowerOrderContext != null) {
				if (lowerOrderContext.getOid() != null) {
					if (accountContext.getOid() == null) {
						accountContext.setOid(lowerOrderContext.getOid());
					}
					if (accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD) {
						// This context cannot be ADD. There is a lower-order context with an OID
						// it means that the lower-order projection exists, we cannot add it twice
						accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
					}
				}
				if (lowerOrderContext.isDelete()) {
					accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.DELETE);
				}
			}
		}		
		
		for (ResourceObjectTypeDependencyType dependency: accountContext.getDependencies()) {
			ResourceShadowDiscriminator refRat = new ResourceShadowDiscriminator(dependency, accountContext.getKind());
			LOGGER.trace("LOOKING FOR {}", refRat);
			LensProjectionContext dependencyAccountContext = context.findProjectionContext(refRat);
			ResourceObjectTypeDependencyStrictnessType strictness = ResourceTypeUtil.getDependencyStrictness(dependency);
			if (dependencyAccountContext == null) {
				if (strictness == ResourceObjectTypeDependencyStrictnessType.STRICT) {
					// This should not happen, it is checked before projection
					throw new PolicyViolationException("Unsatisfied strict dependency of "
							+ accountContext.getResourceShadowDiscriminator().toHumanReadableString() +
							" dependent on " + refRat.toHumanReadableString() + ": No context in dependency check");
				} else if (strictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
					// independent object not in the context, just ignore it
					LOGGER.trace("Unsatisfied lax dependency of account " + 
							accountContext.getResourceShadowDiscriminator().toHumanReadableString() +
							" dependent on " + refRat.toHumanReadableString() + "; depencency skipped");
				} else if (strictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
					// independent object not in the context, just ignore it
					LOGGER.trace("Unsatisfied relaxed dependency of account "
							+ accountContext.getResourceShadowDiscriminator().toHumanReadableString() +
							" dependent on " + refRat.toHumanReadableString() + "; depencency skipped");
				} else {
					throw new IllegalArgumentException("Unknown dependency strictness "+dependency.getStrictness()+" in "+refRat);
				}
			} else {
				// We have the context of the object that we depend on. We need to check if it was provisioned.
				if (strictness == ResourceObjectTypeDependencyStrictnessType.STRICT
						|| strictness == ResourceObjectTypeDependencyStrictnessType.RELAXED) {
					if (wasProvisioned(dependencyAccountContext, context.getExecutionWave())) {
						// everything OK
					} else {
						// We do not want to throw exception here. That will stop entire projection.
						// Let's just mark the projection as broken and skip it.
						LOGGER.warn("Unsatisfied dependency of account "+accountContext.getResourceShadowDiscriminator()+
								" dependent on "+refRat+": Account not provisioned in dependency check (execution wave "+context.getExecutionWave()+", account wave "+accountContext.getWave() + ", depenedency account wave "+dependencyAccountContext.getWave()+")");
						accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
						return false;
					}
				} else if (strictness == ResourceObjectTypeDependencyStrictnessType.LAX) {
					// we don't care what happened, just go on
					return true;
				} else {
					throw new IllegalArgumentException("Unknown dependency strictness "+dependency.getStrictness()+" in "+refRat);
				}
			}
		}
		return true;
	}
	
	/**
	 * Finally checks for all the dependencies. Some dependencies cannot be checked during wave computations as
	 * we might not have all activation decisions yet. 
	 */
	private <F extends ObjectType> void checkDependenciesFinal(LensContext<F> context) throws PolicyViolationException {
		
		for (LensProjectionContext accountContext: context.getProjectionContexts()) {
			checkDependencies(context, accountContext);
		}
		
		for (LensProjectionContext accountContext: context.getProjectionContexts()) {
			if (accountContext.isDelete() 
					|| accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
				// It is OK if we depend on something that is not there if we are being removed
				// but we still need to check if others depends on me
				for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
					if (projectionContext.isDelete()
							|| projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK
							|| projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN 
							|| projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
						// If someone who is being deleted depends on us then it does not really matter
						continue;
					}
					for (ResourceObjectTypeDependencyType dependency: projectionContext.getDependencies()) {
						if (dependency.getResourceRef().getOid().equals(accountContext.getResource().getOid())) {
							// Someone depends on us
							if (ResourceTypeUtil.getDependencyStrictness(dependency) == ResourceObjectTypeDependencyStrictnessType.STRICT) {
								throw new PolicyViolationException("Cannot remove "+accountContext.getHumanReadableName()
										+" because "+projectionContext.getHumanReadableName()+" depends on it");
							}
						}
					}
				}
				
			}
		}
	}
	
	private <F extends ObjectType> boolean wasProvisioned(LensProjectionContext accountContext, int executionWave) {
		int accountWave = accountContext.getWave();
		if (accountWave >= executionWave) {
			// This had no chance to be provisioned yet, so we assume it will be provisioned
			return true;
		}
		if (accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN 
				|| accountContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
			return false;
		}
		PrismObject<ShadowType> objectCurrent = accountContext.getObjectCurrent();
		if (objectCurrent != null && objectCurrent.asObjectable().getFailedOperationType() != null) {
			// There is unfinished operation in the shadow. We cannot continue.
			return false;
		}
		if (accountContext.isExists()) {
			return true;
		}
		if (accountContext.isAdd()) {
			List<LensObjectDeltaOperation<ShadowType>> executedDeltas = accountContext.getExecutedDeltas();
			if (executedDeltas == null || executedDeltas.isEmpty()) {
				return false;
			}
			for (LensObjectDeltaOperation<ShadowType> executedDelta: executedDeltas) {
				OperationResult executionResult = executedDelta.getExecutionResult();
				if (executionResult == null || !executionResult.isSuccess()) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	

}
