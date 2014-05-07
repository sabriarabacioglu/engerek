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

package com.evolveum.midpoint.model.scripting.actions;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.ContextFactory;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.scripting.Data;
import com.evolveum.midpoint.model.scripting.ExecutionContext;
import com.evolveum.midpoint.model.scripting.ScriptExecutionException;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class RecomputeExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeExecutor.class);

    private static final String NAME = "recompute";

    @Autowired
    private ContextFactory contextFactory;

    @Autowired
    private Clockwork clockwork;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        for (Item item : input.getData()) {
            if (item instanceof PrismObject && UserType.class.isAssignableFrom(((PrismObject) item).getCompileTimeClass())) {
                PrismObject<UserType> userPrismObject = (PrismObject) item;
                try {
                    LensContext<UserType> syncContext = contextFactory.createRecomputeContext(userPrismObject, context.getTask(), result);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Recomputing user {}: context:\n{}", userPrismObject, syncContext.debugDump());
                    }
                    clockwork.run(syncContext, context.getTask(), result);
                    LOGGER.trace("Recomputing of user {}: {}", userPrismObject, result.getStatus());
                } catch (ObjectNotFoundException|ConfigurationException|SecurityViolationException|PolicyViolationException|ExpressionEvaluationException|ObjectAlreadyExistsException|CommunicationException|SchemaException e) {
                    throw new ScriptExecutionException("Couldn't recompute user " + userPrismObject + ": " + e.getMessage(), e);
                }
                context.println("Recomputed " + item.toString());
            } else {
                throw new ScriptExecutionException("Item could not be recomputed, because it is not a PrismObject: " + item.toString());
            }
        }
        return Data.createEmpty();
    }
}
