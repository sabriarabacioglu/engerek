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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class AssignExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AssignExecutor.class);

    private static final String NAME = "assign";
    private static final String PARAM_RESOURCE = "resource";
    private static final String PARAM_ROLE = "role";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

        JAXBElement<?> resourceExpression = expressionHelper.getArgument(expression.getParameter(), PARAM_RESOURCE, false, false, NAME);
        JAXBElement<?> roleExpression = expressionHelper.getArgument(expression.getParameter(), PARAM_ROLE, false, false, NAME);

        Collection<ObjectReferenceType> resources;
        if (resourceExpression != null) {
            Data data = scriptingExpressionEvaluator.evaluateExpression(resourceExpression, input, context, result);
            resources = data.getDataAsReferences(ResourceType.COMPLEX_TYPE);
        } else {
            resources = null;
        }

        Collection<ObjectReferenceType> roles;
        if (roleExpression != null) {
            Data data = scriptingExpressionEvaluator.evaluateExpression(roleExpression, input, context, result);
            roles = data.getDataAsReferences(RoleType.COMPLEX_TYPE);
        } else {
            roles = null;
        }

        if (resources == null && roles == null) {
            throw new ScriptExecutionException("Nothing to assign: neither resource nor role specified");
        }

        for (Item item : input.getData()) {
            if (item instanceof PrismObject && ((PrismObject) item).asObjectable() instanceof FocusType) {
                PrismObject<? extends ObjectType> prismObject = (PrismObject) item;
                ObjectType objectType = prismObject.asObjectable();
                operationsHelper.applyDelta(createDelta(objectType, resources, roles), context, result);
                context.println("Modified " + item.toString());
            } else {
                throw new ScriptExecutionException("Item could not be modified, because it is not a PrismObject of FocusType: " + item.toString());
            }
        }
        return Data.createEmpty();
    }

    private ObjectDelta createDelta(ObjectType objectType, Collection<ObjectReferenceType> resources, Collection<ObjectReferenceType> roles) throws ScriptExecutionException {

        List<AssignmentType> assignments = new ArrayList<>();

        if (roles != null) {
            for (ObjectReferenceType roleRef : roles) {
                AssignmentType assignmentType = new AssignmentType();
                assignmentType.setTargetRef(roleRef);
                assignments.add(assignmentType);
            }
        }

        if (resources != null) {
            for (ObjectReferenceType resourceRef : resources) {
                AssignmentType assignmentType = new AssignmentType();
                ConstructionType constructionType = new ConstructionType();
                constructionType.setResourceRef(resourceRef);
                assignmentType.setConstruction(constructionType);
                assignments.add(assignmentType);
            }
        }

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(objectType.getClass(), objectType.getOid(), prismContext);
        try {
            delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, assignments.toArray(new AssignmentType[0]));
        } catch (SchemaException e) {
            throw new ScriptExecutionException("Couldn't prepare modification to add resource/role assignments", e);
        }
        return delta;
    }
}
