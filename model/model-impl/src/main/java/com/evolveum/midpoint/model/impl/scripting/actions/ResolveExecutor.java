/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptExecutionException;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

/**
 * Resolves a reference, e.g. a linkRef into a set of accounts.
 *
 * @author mederly
 */
@Component
public class ResolveExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ResolveExecutor.class);

    private static final String NAME = "resolve";
    private static final String PARAM_NO_FETCH = "noFetch";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

        boolean noFetch = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_NO_FETCH, input, context, false, NAME, result);

        Data output = Data.createEmpty();

        for (Item item : input.getData()) {
            if (item instanceof PrismReference) {
                PrismReference prismReference = (PrismReference) item;
                for (PrismReferenceValue prismReferenceValue : prismReference.getValues()) {
                    String oid = prismReferenceValue.getOid();
                    QName targetTypeQName = prismReferenceValue.getTargetType();
                    if (targetTypeQName == null) {
                        throw new ScriptExecutionException("Couldn't resolve reference, because target type is unknown: " + prismReferenceValue);
                    }
                    Class<? extends ObjectType> typeClass = (Class) prismContext.getSchemaRegistry().determineCompileTimeClass(targetTypeQName);
                    if (typeClass == null) {
                        throw new ScriptExecutionException("Couldn't resolve reference, because target type class is unknown for target type " + targetTypeQName);
                    }
                    PrismObject<? extends ObjectType> prismObject = operationsHelper.getObject(typeClass, oid, noFetch, context, result);
                    output.addItem(prismObject);
                }
            } else {
                throw new ScriptExecutionException("Item could not be resolved, because it is not a PrismReference: " + item.toString());
            }
        }
        return output;
    }

    private ObjectDelta createDelta(ObjectType objectType, Data deltaData) throws ScriptExecutionException {
        if (deltaData.getData().size() != 1) {
            throw new ScriptExecutionException("Expected exactly one delta to apply, found "  + deltaData.getData().size() + " instead.");
        }
        ObjectDeltaType deltaType = ((PrismProperty<ObjectDeltaType>) deltaData.getData().get(0)).getRealValue();
        if (deltaType.getChangeType() == null) {
            deltaType.setChangeType(ChangeTypeType.MODIFY);
        }
        if (deltaType.getOid() == null && deltaType.getChangeType() != ChangeTypeType.ADD) {
            deltaType.setOid(objectType.getOid());
        }
        if (deltaType.getObjectType() == null) {
            if (objectType.asPrismObject().getDefinition() == null) {
                throw new ScriptExecutionException("No definition for prism object " + objectType);
            }
            deltaType.setObjectType(objectType.asPrismObject().getDefinition().getTypeName());
        }
        try {
            return DeltaConvertor.createObjectDelta(deltaType, prismContext);
        } catch (SchemaException e) {
            throw new ScriptExecutionException("Couldn't process delta due to schema exception", e);
        }
    }
}
