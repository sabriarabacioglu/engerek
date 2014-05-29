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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextStatsType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensFocusContextType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensProjectionContextType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author semancik
 *
 */
public class LensContext<F extends ObjectType> implements ModelContext<F> {

    private static final long serialVersionUID = -778283437426659540L;
    private static final String DOT_CLASS = LensContext.class.getName() + ".";

    private ModelState state = ModelState.INITIAL;
	
	/**
     * Channel that is the source of primary change (GUI, live sync, import, ...)
     */
    private String channel;
    
	private LensFocusContext<F> focusContext;
	private Collection<LensProjectionContext> projectionContexts = new ArrayList<LensProjectionContext>();

	private Class<F> focusClass;
	
	private boolean lazyAuditRequest = false;
	private boolean requestAudited = false;
	private boolean executionAudited = false;
	private LensContextStatsType stats = new LensContextStatsType();
	
	private List<LensObjectDeltaOperation<?>> rottenExecutedDeltas = new ArrayList<LensObjectDeltaOperation<?>>();

	transient private ObjectTemplateType focusTemplate;
	transient private ProjectionPolicyType accountSynchronizationSettings;
	transient private ValuePolicyType globalPasswordPolicy;
	
	transient private DeltaSetTriple<EvaluatedAssignment> evaluatedAssignmentTriple;
	
	/**
	 * Just a cached copy. Keep it in context so we do not need to reload it all the time.
	 */
	transient private PrismObject<SystemConfigurationType> systemConfiguration;
	
	/**
     * True if we want to reconcile all accounts in this context.
     */
    private boolean doReconciliationForAllProjections = false;
    
    /**
	 * Current wave of computation and execution.
	 */
	int projectionWave = 0;

    /**
	 * Current wave of execution.
	 */
	int executionWave = 0;

	transient private boolean isFresh = false;
	transient private boolean isRequestAuthorized = false;
	
	/**
     * Cache of resource instances. It is used to reduce the number of read (getObject) calls for ResourceType objects.
     */
    transient private Map<String, ResourceType> resourceCache;
	
	transient private PrismContext prismContext;

    transient private ProvisioningService provisioningService;
	
	private ModelExecuteOptions options;
	
	/**
	 * Used mostly in unit tests.
	 */
	transient private LensDebugListener debugListener;
	
	public LensContext(Class<F> focusClass, PrismContext prismContext, ProvisioningService provisioningService) {
		Validate.notNull(prismContext, "No prismContext");
		
        this.prismContext = prismContext;
        this.provisioningService = provisioningService;
        this.focusClass = focusClass;
    }
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	protected PrismContext getNotNullPrismContext() {
		if (prismContext == null) {
			throw new IllegalStateException("Null prism context in "+this+"; the context was not adopted (most likely)");
		}
		return prismContext;
	}

    public ProvisioningService getProvisioningService() {
        return provisioningService;
    }

    @Override
	public ModelState getState() {
		return state;
	}

	public void setState(ModelState state) {
		this.state = state;
	}

	@Override
	public LensFocusContext<F> getFocusContext() {
		return focusContext;
	}
	
	public void setFocusContext(LensFocusContext<F> focusContext) {
		this.focusContext = focusContext;
	}
	
	public LensFocusContext<F> createFocusContext() {
		return createFocusContext(null);
	}
	
	public LensFocusContext<F> createFocusContext(Class<F> explicitFocusClass) {
		if (explicitFocusClass != null) {
			this.focusClass = explicitFocusClass;
		}
		focusContext = new LensFocusContext<F>(focusClass, this);
		return focusContext;
	}
	
	public LensFocusContext<F> getOrCreateFocusContext() {
		return getOrCreateFocusContext(null);
	}
	
	public LensFocusContext<F> getOrCreateFocusContext(Class<F> explicitFocusClass) {
		if (focusContext == null) {
			createFocusContext(explicitFocusClass);
		}
		return focusContext;
	}

	@Override
	public Collection<LensProjectionContext> getProjectionContexts() {
		return projectionContexts;
	}
	
	public Iterator<LensProjectionContext> getProjectionContextsIterator() {
		return projectionContexts.iterator();
	}
	
	public void addProjectionContext(LensProjectionContext projectionContext) {
		projectionContexts.add(projectionContext);
	}
	
	public LensProjectionContext findProjectionContextByOid(String oid) {
		for (LensProjectionContext projCtx: getProjectionContexts()) {
			if (oid.equals(projCtx.getOid())) {
				return projCtx;
			}
		}
		return null;
	}
		
	public LensProjectionContext findProjectionContext(ResourceShadowDiscriminator rat) {
		Validate.notNull(rat);
		for (LensProjectionContext projCtx: getProjectionContexts()) {
			if (projCtx.compareResourceShadowDiscriminator(rat, true)) {
				return projCtx;
			}
		}
		return null;
	}

	public LensProjectionContext findOrCreateProjectionContext(ResourceShadowDiscriminator rat) {
		LensProjectionContext projectionContext = findProjectionContext(rat);
		if (projectionContext == null) {
			projectionContext = createProjectionContext(rat);
		}
		return projectionContext;
	}
	
	public ObjectTemplateType getFocusTemplate() {
		return focusTemplate;
	}
	
	public LensProjectionContext findProjectionContext(ResourceShadowDiscriminator rat, String oid) {
		LensProjectionContext projectionContext = findProjectionContext(rat);
		
		if (projectionContext == null || projectionContext.getOid() == null || !oid.equals(projectionContext.getOid())) {
			return null;
		}
		 
		return projectionContext;
	}

	public PrismObject<SystemConfigurationType> getSystemConfiguration() {
		return systemConfiguration;
	}

	public void setSystemConfiguration(
			PrismObject<SystemConfigurationType> systemConfiguration) {
		this.systemConfiguration = systemConfiguration;
	}

	public void setFocusTemplate(ObjectTemplateType focusTemplate) {
		this.focusTemplate = focusTemplate;
	}

	public ProjectionPolicyType getAccountSynchronizationSettings() {
		return accountSynchronizationSettings;
	}

	public void setAccountSynchronizationSettings(
			ProjectionPolicyType accountSynchronizationSettings) {
		this.accountSynchronizationSettings = accountSynchronizationSettings;
	}
	
	public ValuePolicyType getGlobalPasswordPolicy() {
		return globalPasswordPolicy;
	}
	
	public void setGlobalPasswordPolicy(ValuePolicyType globalPasswordPolicy) {
		this.globalPasswordPolicy = globalPasswordPolicy;
	}
	
	public int getProjectionWave() {
		return projectionWave;
	}

	public void setProjectionWave(int wave) {
		this.projectionWave = wave;
	}
	
	public void incrementProjectionWave() {
		projectionWave++;
	}
	
	public void resetProjectionWave() {
		projectionWave = executionWave;
	}
	
	public int getExecutionWave() {
		return executionWave;
	}

	public void setExecutionWave(int executionWave) {
		this.executionWave = executionWave;
	}

	public void incrementExecutionWave() {
		executionWave++;
	}

	public int getMaxWave() {
		int maxWave = 0;
		for (LensProjectionContext projContext: projectionContexts) {
			if (projContext.getWave() > maxWave) {
				maxWave = projContext.getWave();
			}
		}
		return maxWave;
	}
	
	public boolean isFresh() {
		return isFresh;
	}

	public void setFresh(boolean isFresh) {
		this.isFresh = isFresh;
	}
	
	public boolean isRequestAuthorized() {
		return isRequestAuthorized;
	}

	public void setRequestAuthorized(boolean isRequestAuthorized) {
		this.isRequestAuthorized = isRequestAuthorized;
	}

	/**
	 * Makes the context and all sub-context non-fresh.
	 */
	public void rot() {
		setFresh(false);
		if (focusContext != null) {
			focusContext.setFresh(false);
		}
		for (LensProjectionContext projectionContext: projectionContexts) {
			projectionContext.setFresh(false);
			projectionContext.setFullShadow(false);
		}
	}
	
	/**
	 * Make the context as clean as new. Except for the executed deltas and other "traces" of
	 * what was already done and cannot be undone. Also the configuration items that were loaded may remain.
	 * This is used to restart the context computation but keep the trace of what was already done.
	 */
	public void reset() {
		state = ModelState.INITIAL;
		evaluatedAssignmentTriple = null;
		projectionWave = 0;
		executionWave = 0;
		isFresh = false;
		if (focusContext != null) {
			focusContext.reset();
		}
		if (projectionContexts != null) {
			for (LensProjectionContext projectionContext: projectionContexts) {
				projectionContext.reset();
			}
		}
	}

	public String getChannel() {
        return channel;
    }

    public void setChannel(String channelUri) {
        this.channel = channelUri;
    }
    
    public void setChannel(QName channelQName) {
        this.channel = QNameUtil.qNameToUri(channelQName);
    }

	public boolean isDoReconciliationForAllProjections() {
		return doReconciliationForAllProjections;
	}

	public void setDoReconciliationForAllProjections(boolean doReconciliationForAllProjections) {
		this.doReconciliationForAllProjections = doReconciliationForAllProjections;
	}
	
	public DeltaSetTriple<EvaluatedAssignment> getEvaluatedAssignmentTriple() {
		return evaluatedAssignmentTriple;
	}

	public void setEvaluatedAssignmentTriple(DeltaSetTriple<EvaluatedAssignment> evaluatedAssignmentTriple) {
		this.evaluatedAssignmentTriple = evaluatedAssignmentTriple;
	}
	
	public ModelExecuteOptions getOptions() {
		return options;
	}
	
	public void setOptions(ModelExecuteOptions options) {
		this.options = options;
	}

	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	/**
	 * If set to true then the request will be audited right before execution.
	 * If no execution takes place then no request will be audited.
	 */
	public boolean isLazyAuditRequest() {
		return lazyAuditRequest;
	}

	public void setLazyAuditRequest(boolean lazyAuditRequest) {
		this.lazyAuditRequest = lazyAuditRequest;
	}

	public boolean isRequestAudited() {
		return requestAudited;
	}

	public void setRequestAudited(boolean requestAudited) {
		this.requestAudited = requestAudited;
	}
	
	public boolean isExecutionAudited() {
		return executionAudited;
	}

	public void setExecutionAudited(boolean executionAudited) {
		this.executionAudited = executionAudited;
	}

	public LensContextStatsType getStats() {
		return stats;
	}

	public void setStats(LensContextStatsType stats) {
		this.stats = stats;
	}

	/**
     * Returns all changes, user and all accounts. Both primary and secondary changes are returned, but
     * these are not merged.
     * TODO: maybe it would be better to merge them.
     */
    public Collection<ObjectDelta<? extends ObjectType>> getAllChanges() throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<ObjectDelta<? extends ObjectType>>();
        if (focusContext != null) {
	        addChangeIfNotNull(allChanges, focusContext.getPrimaryDelta());
	        addChangeIfNotNull(allChanges, focusContext.getSecondaryDelta());
        }
        for (LensProjectionContext projCtx: getProjectionContexts()) {
            addChangeIfNotNull(allChanges, projCtx.getPrimaryDelta());
            addChangeIfNotNull(allChanges, projCtx.getSecondaryDelta());
        }
        return allChanges;
    }
    
    public Collection<ObjectDelta<? extends ObjectType>> getPrimaryChanges() throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<ObjectDelta<? extends ObjectType>>();
        if (focusContext != null) {
	        addChangeIfNotNull(allChanges, focusContext.getPrimaryDelta());
        }
        for (LensProjectionContext projCtx: getProjectionContexts()) {
            addChangeIfNotNull(allChanges, projCtx.getPrimaryDelta());
        }
        return allChanges;
    }

	private <T extends ObjectType> void addChangeIfNotNull(Collection<ObjectDelta<? extends ObjectType>> changes,
            ObjectDelta<T> change) {
        if (change != null) {
            changes.add(change);
        }
    }

    public void replacePrimaryFocusDelta(ObjectDelta<F> newDelta) {
        focusContext.setPrimaryDelta(newDelta);
        // todo any other changes have to be done?
    }

    public void replacePrimaryFocusDeltas(List<ObjectDelta<F>> deltas) throws SchemaException {
        replacePrimaryFocusDelta(null);
        if (deltas != null) {
            for (ObjectDelta<F> delta : deltas) {
                focusContext.addPrimaryDelta(delta);
            }
        }
        // todo any other changes have to be done?
    }


    /**
     * Returns all executed deltas, user and all accounts.
     */
    public Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas() throws SchemaException {
    	return getExecutedDeltas(null);
    }

	/**
     * Returns all executed deltas, user and all accounts.
     */
    public Collection<ObjectDeltaOperation<? extends ObjectType>> getUnauditedExecutedDeltas() throws SchemaException {
    	return getExecutedDeltas(false);
    }

    /**
     * Returns all executed deltas, user and all accounts.
     */
    Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas(Boolean audited) throws SchemaException {
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>();
        if (focusContext != null) {
	        executedDeltas.addAll(focusContext.getExecutedDeltas(audited));
        }
        for (LensProjectionContext projCtx: getProjectionContexts()) {
        	executedDeltas.addAll(projCtx.getExecutedDeltas(audited));
        }
        if (audited == null) {
        	executedDeltas.addAll(getRottenExecutedDeltas());
        }
        return executedDeltas;
    }
    
    public void markExecutedDeltasAudited()  {
        if (focusContext != null) {
        	focusContext.markExecutedDeltasAudited();
        }
        for (LensProjectionContext projCtx: getProjectionContexts()) {
        	projCtx.markExecutedDeltasAudited();
        }
    }
 
	public List<LensObjectDeltaOperation<?>> getRottenExecutedDeltas() {
		return rottenExecutedDeltas;
	}
    
	public void recompute() throws SchemaException {
		recomputeFocus();
		recomputeProjections();
	}

    // mainly computes new state based on old state and delta(s)
	public void recomputeFocus() throws SchemaException {
		if (focusContext != null) {
			focusContext.recompute();
		}
	}
	
	public void recomputeProjections() throws SchemaException {
		for (LensProjectionContext projCtx: getProjectionContexts()) {
			projCtx.recompute();
		}
	}

	public void checkConsistence() {
		if (focusContext != null) {
			focusContext.checkConsistence();
		}
		for (LensProjectionContext projectionContext: projectionContexts) {
			projectionContext.checkConsistence(this.toString(), isFresh, ModelExecuteOptions.isForce(options));
		}
	}

	public void checkEncrypted() {
		if (focusContext != null && !focusContext.isDelete()) {
			focusContext.checkEncrypted();
		}
		for (LensProjectionContext projectionContext: projectionContexts) {
			if (!projectionContext.isDelete()) {
				projectionContext.checkEncrypted();
			}
		}
	}
	
	public LensProjectionContext createProjectionContext() {
		return createProjectionContext(null);
	}
	
	public LensProjectionContext createProjectionContext(ResourceShadowDiscriminator rat) {
		LensProjectionContext projCtx = new LensProjectionContext(this, rat);
		addProjectionContext(projCtx);
		return projCtx;
	}
	
	private Map<String, ResourceType> getResourceCache() {
		if (resourceCache == null) {
			resourceCache = new HashMap<String, ResourceType>();
		}
		return resourceCache;
	}

	/**
     * Returns a resource for specified account type.
     * This is supposed to be efficient, taking the resource from the cache. It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public ResourceType getResource(ResourceShadowDiscriminator rat) {
        return getResource(rat.getResourceOid());
    }
    
    /**
     * Returns a resource for specified account type.
     * This is supposed to be efficient, taking the resource from the cache. It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public ResourceType getResource(String resourceOid) {
        return getResourceCache().get(resourceOid);
    }
	
	/**
     * Puts resources in the cache for later use. The resources should be fetched from provisioning
     * and have pre-parsed schemas. So the next time just reuse them without the other overhead.
     */
    public void rememberResources(Collection<ResourceType> resources) {
        for (ResourceType resourceType : resources) {
            rememberResource(resourceType);
        }
    }

    /**
     * Puts resource in the cache for later use. The resource should be fetched from provisioning
     * and have pre-parsed schemas. So the next time just reuse it without the other overhead.
     */
    public void rememberResource(ResourceType resourceType) {
    	getResourceCache().put(resourceType.getOid(), resourceType);
    }
    
	/**
	 * Cleans up the contexts by removing secondary deltas and other working state. The context after cleanup
	 * should be the same as originally requested.
	 * However, the current wave number is retained. Otherwise it ends up in endless loop. 
	 */
	public void cleanup() throws SchemaException {
		if (focusContext != null) {
			focusContext.cleanup();
		}
		for (LensProjectionContext projectionContext: projectionContexts) {
			projectionContext.cleanup();
		}
		recompute();
	}
    
    public void adopt(PrismContext prismContext) throws SchemaException {
    	this.prismContext = prismContext;
    	
    	if (focusContext != null) {
    		focusContext.adopt(prismContext);
    	}
    	for (LensProjectionContext projectionContext: projectionContexts) {
    		projectionContext.adopt(prismContext);
    	}
    }
    
    public void normalize() {
    	if (focusContext != null) {
    		focusContext.normalize();
    	}
    	if (projectionContexts != null) {
    		for (LensProjectionContext projectionContext: projectionContexts) {
    			projectionContext.normalize();
    		}
    	}
    }
    
    public LensContext<F> clone() {
    	LensContext<F> clone = new LensContext<F>(focusClass, prismContext, provisioningService);
    	copyValues(clone);
    	return clone;
    }
    
    protected void copyValues(LensContext<F> clone) {
    	clone.state = this.state;
    	clone.channel = this.channel;
    	clone.doReconciliationForAllProjections = this.doReconciliationForAllProjections;
    	clone.focusClass = this.focusClass;
    	clone.isFresh = this.isFresh;
    	clone.prismContext = this.prismContext;
    	clone.resourceCache = cloneResourceCache();
    	// User template is de-facto immutable, OK to just pass reference here.
    	clone.focusTemplate = this.focusTemplate;
    	clone.projectionWave = this.projectionWave;
        if (options != null) {
            clone.options = this.options.clone();
        }
    	
    	if (this.focusContext != null) {
    		clone.focusContext = this.focusContext.clone(this);
    	}
    	
    	for (LensProjectionContext thisProjectionContext: this.projectionContexts) {
    		clone.projectionContexts.add(thisProjectionContext.clone(this));
    	}
    }

	private Map<String, ResourceType> cloneResourceCache() {
		if (resourceCache == null) {
			return null;
		}
		Map<String, ResourceType> clonedMap = new HashMap<String, ResourceType>();
		for (Entry<String, ResourceType> entry: resourceCache.entrySet()) {
			clonedMap.put(entry.getKey(), entry.getValue());
		}
		return clonedMap;
	}
	
	public void distributeResource() {
		for (LensProjectionContext projCtx: getProjectionContexts()) {
			projCtx.distributeResource();
		}
	}

    @Override
    public Class<F> getFocusClass() {
        return focusClass;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }
    
    public String dump(boolean showTriples) {
        return debugDump(0, showTriples);
    }

    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("LensContext: state=").append(state);
        sb.append(", Wave(e=").append(executionWave);
        sb.append(",p=").append(projectionWave);
        sb.append(",max=").append(getMaxWave());
        sb.append("), ");
        if (focusContext != null) {
        	sb.append("focus, ");
        }
        sb.append(projectionContexts.size());
        sb.append(" projections, ");
        try {
			Collection<ObjectDelta<? extends ObjectType>> allChanges = getAllChanges();
			sb.append(allChanges.size());
		} catch (SchemaException e) {
			sb.append("[ERROR]");
		}
        sb.append(" changes, ");
        sb.append("fresh=").append(isFresh);
        sb.append("\n");

        DebugUtil.debugDumpLabel(sb, "Channel", indent + 1);
        sb.append(" ").append(channel).append("\n");
        DebugUtil.debugDumpLabel(sb, "Options", indent + 1);
        sb.append(" ").append(options).append("\n");
        DebugUtil.debugDumpLabel(sb, "Settings", indent + 1);
        sb.append(" ");
        if (accountSynchronizationSettings != null) {
            sb.append("assignments=");
            sb.append(accountSynchronizationSettings.getAssignmentPolicyEnforcement());
        } else {
            sb.append("null");
        }
        sb.append("\n");

        DebugUtil.debugDumpWithLabel(sb, "FOCUS", focusContext, indent + 1);

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("PROJECTIONS:");
        if (projectionContexts.isEmpty()) {
            sb.append(" none");
        } else {
        	sb.append(" (").append(projectionContexts.size()).append(")");
            for (LensProjectionContext projCtx : projectionContexts) {
            	sb.append(":\n");
            	sb.append(projCtx.debugDump(indent + 2, showTriples));
            }
        }

        return sb.toString();
    }

    public PrismContainer<LensContextType> toPrismContainer() throws SchemaException {

        PrismContainer<LensContextType> lensContextTypeContainer = PrismContainer.newInstance(getPrismContext(), LensContextType.COMPLEX_TYPE);
        LensContextType lensContextType = lensContextTypeContainer.createNewValue().asContainerable();

        lensContextType.setState(state != null ? state.toModelStateType() : null);
        lensContextType.setChannel(channel);

        if (focusContext != null) {
            PrismContainer<LensFocusContextType> lensFocusContextTypeContainer = lensContextTypeContainer.findOrCreateContainer(LensContextType.F_FOCUS_CONTEXT);
            focusContext.addToPrismContainer(lensFocusContextTypeContainer);
        }

        PrismContainer<LensProjectionContextType> lensProjectionContextTypeContainer = lensContextTypeContainer.findOrCreateContainer(LensContextType.F_PROJECTION_CONTEXT);
        for (LensProjectionContext lensProjectionContext : projectionContexts) {
            lensProjectionContext.addToPrismContainer(lensProjectionContextTypeContainer);
        }
        lensContextType.setFocusClass(focusClass != null ? focusClass.getName() : null);
        lensContextType.setDoReconciliationForAllProjections(doReconciliationForAllProjections);
        lensContextType.setProjectionWave(projectionWave);
        lensContextType.setExecutionWave(executionWave);
        lensContextType.setOptions(options != null ? options.toModelExecutionOptionsType() : null);
        lensContextType.setLazyAuditRequest(lazyAuditRequest);
        lensContextType.setRequestAudited(requestAudited);
        lensContextType.setExecutionAudited(executionAudited);
        lensContextType.setStats(stats);
        
        for (LensObjectDeltaOperation executedDelta : rottenExecutedDeltas) {
        	lensContextType.getRottenExecutedDeltas().add(executedDelta.toLensObjectDeltaOperationType());
        }

        return lensContextTypeContainer;
    }


    public static LensContext fromLensContextType(LensContextType lensContextType, PrismContext prismContext, ProvisioningService provisioningService, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "fromLensContextType");

        String focusClassString = lensContextType.getFocusClass();

        if (StringUtils.isEmpty(focusClassString)) {
            throw new SystemException("Focus class is undefined in LensContextType");
        }

        LensContext lensContext;
        try {
            lensContext = new LensContext(Class.forName(focusClassString), prismContext, provisioningService);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensContext because focus or projection class couldn't be found", e);
        }

        lensContext.setState(ModelState.fromModelStateType(lensContextType.getState()));
        lensContext.setChannel(lensContextType.getChannel());
        lensContext.setFocusContext(LensFocusContext.fromLensFocusContextType(lensContextType.getFocusContext(), lensContext, result));
        for (LensProjectionContextType lensProjectionContextType : lensContextType.getProjectionContext()) {
            lensContext.addProjectionContext(LensProjectionContext.fromLensProjectionContextType(lensProjectionContextType, lensContext, result));
        }
        lensContext.setDoReconciliationForAllProjections(lensContextType.isDoReconciliationForAllProjections() != null ?
            lensContextType.isDoReconciliationForAllProjections() : false);
        lensContext.setProjectionWave(lensContextType.getProjectionWave() != null ?
                lensContextType.getProjectionWave() : 0);
        lensContext.setExecutionWave(lensContextType.getExecutionWave() != null ?
            lensContextType.getExecutionWave() : 0);
        lensContext.setOptions(ModelExecuteOptions.fromModelExecutionOptionsType(lensContextType.getOptions()));
        if (lensContextType.isLazyAuditRequest() != null) {
        	lensContext.setLazyAuditRequest(lensContextType.isLazyAuditRequest());
        }
        if (lensContextType.isRequestAudited() != null) {
        	lensContext.setRequestAudited(lensContextType.isRequestAudited());
        }
        if (lensContextType.isExecutionAudited() != null) {
        	lensContext.setExecutionAudited(lensContextType.isExecutionAudited());
        }
        lensContext.setStats(lensContextType.getStats());

        for (LensObjectDeltaOperationType eDeltaOperationType : lensContextType.getRottenExecutedDeltas()) {
            LensObjectDeltaOperation objectDeltaOperation = LensObjectDeltaOperation.fromLensObjectDeltaOperationType(eDeltaOperationType, lensContext.getPrismContext());
            if (objectDeltaOperation.getObjectDelta() != null) {
            	lensContext.fixProvisioningTypeInDelta(objectDeltaOperation.getObjectDelta(), result);
            }
            lensContext.rottenExecutedDeltas.add(objectDeltaOperation);
        }
        
        if (result.isUnknown()) {
            result.computeStatus();
        }
        return lensContext;
    }
    
    protected void fixProvisioningTypeInDelta(ObjectDelta delta, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        if (delta != null && delta.getObjectTypeClass() != null && (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass()) || ResourceType.class.isAssignableFrom(delta.getObjectTypeClass()))) {
            getProvisioningService().applyDefinition(delta, result);
        }
    }

	@Override
	public String toString() {
		return "LensContext(s=" + state + ", W(e=" + executionWave + ",p=" + projectionWave + "): "+focusContext+", "+projectionContexts+")";
	}
	
	
	public ValuePolicyType getEffectivePasswordPolicy(){
		if (getFocusContext().getOrgPasswordPolicy() != null){
			return getFocusContext().getOrgPasswordPolicy();
		}
		return globalPasswordPolicy;
	}

}
