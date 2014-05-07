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

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ModelEvent extends BaseEvent {

    private static final Trace LOGGER = TraceManager.getTrace(ModelEvent.class);

    // we can expect that modelContext != null and focus context != null as well
    private ModelContext modelContext;

    public ModelEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        super(lightweightIdentifierGenerator);
    }

    public ModelContext getModelContext() {
        return modelContext;
    }

    public ModelElementContext getFocusContext() {
        return modelContext.getFocusContext();
    }

    public void setModelContext(ModelContext modelContext) {
        this.modelContext = modelContext;
    }

    public List<? extends ObjectDeltaOperation> getFocusExecutedDeltas() {
        return getFocusContext().getExecutedDeltas();
    }

    public List<ObjectDeltaOperation> getAllExecutedDeltas() {
        List<ObjectDeltaOperation> retval = new ArrayList<ObjectDeltaOperation>();
        retval.addAll(getFocusContext().getExecutedDeltas());
        for (Object o : modelContext.getProjectionContexts()) {
            ModelProjectionContext modelProjectionContext = (ModelProjectionContext) o;
            retval.addAll(modelProjectionContext.getExecutedDeltas());
        }
        return retval;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        boolean allSuccess = true, anySuccess = false, allFailure = true, anyFailure = false, anyInProgress = false;
        for (ObjectDeltaOperation objectDeltaOperation : getAllExecutedDeltas()) {
            if (objectDeltaOperation.getExecutionResult() != null) {
                switch (objectDeltaOperation.getExecutionResult().getStatus()) {
                    case SUCCESS: anySuccess = true; allFailure = false; break;
                    case FATAL_ERROR: allSuccess = false; anyFailure = true; break;
                    case WARNING: anySuccess = true; allFailure = false; break;
                    case HANDLED_ERROR: anySuccess = true; allFailure = false; break;
                    case IN_PROGRESS: allSuccess = false; allFailure = false; anyInProgress = true; break;
                    case NOT_APPLICABLE: break;
                    case PARTIAL_ERROR: allSuccess = false; anyFailure = true; break;
                    case UNKNOWN: allSuccess = false; allFailure = false; break;
                    default: LOGGER.warn("Unknown execution result: " + objectDeltaOperation.getExecutionResult().getStatus());
                }
            } else {
                allSuccess = false; allFailure = false; anyInProgress = true;
            }
        }

        switch (eventStatusType) {
            case ALSO_SUCCESS: return anySuccess;
            case SUCCESS: return allSuccess;
            case FAILURE: return anyFailure;
            case ONLY_FAILURE: return allFailure;
            case IN_PROGRESS: return anyInProgress;
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatusType);
        }
    }

    // a bit of hack but ...
    public ChangeType getChangeType() {
        if (isOperationType(EventOperationType.ADD)) {
            return ChangeType.ADD;
        } else if (isOperationType(EventOperationType.DELETE)) {
            return ChangeType.DELETE;
        } else {
            return ChangeType.MODIFY;
        }
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {

        // we consider an operation to be 'add' when there is 'add' delta among deltas
        // in a similar way with 'delete'
        //
        // alternatively, we could summarize deltas and then decide based on the type of summarized delta (would be a bit inefficient)

        for (Object o : getFocusExecutedDeltas()) {
            ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
            if (objectDeltaOperation.getObjectDelta().isAdd()) {
                return eventOperationType == EventOperationType.ADD;
            } else if (objectDeltaOperation.getObjectDelta().isDelete()) {
                return eventOperationType == EventOperationType.DELETE;
            }
        }
        return eventOperationType == EventOperationType.MODIFY;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.MODEL_EVENT;
    }

    public List<ObjectDelta<UserType>> getUserDeltas() {
        List<ObjectDelta<UserType>> retval = new ArrayList<ObjectDelta<UserType>>();
        Class c = modelContext.getFocusClass();
        if (c != null && UserType.class.isAssignableFrom(c)) {
            for (Object o : getFocusExecutedDeltas()) {
                ObjectDeltaOperation objectDeltaOperation = (ObjectDeltaOperation) o;
                retval.add(objectDeltaOperation.getObjectDelta());
            }
        }
        return retval;
    }

    public ObjectDelta<UserType> getSummarizedUserDeltas() throws SchemaException {
        return ObjectDelta.summarize(getUserDeltas());
    }
}
