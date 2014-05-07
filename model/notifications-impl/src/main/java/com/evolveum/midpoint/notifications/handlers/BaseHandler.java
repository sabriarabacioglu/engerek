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

package com.evolveum.midpoint.notifications.handlers;

import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.notifications.NotificationManagerImpl;
import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.NotificationsUtil;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author mederly
 */
@Component
public abstract class BaseHandler implements EventHandler {

    private static final Trace LOGGER = TraceManager.getTrace(BaseHandler.class);

    @Autowired
    protected NotificationManagerImpl notificationManager;

    @Autowired
    protected NotificationsUtil notificationsUtil;

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ExpressionFactory expressionFactory;

    protected void register(Class<? extends EventHandlerType> clazz) {
        notificationManager.registerEventHandler(clazz, this);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType) {
        logStart(LOGGER, event, eventHandlerType, null);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    eventHandlerType.getClass() + " (name: " + eventHandlerType.getName() +
                    (additionalData != null ? (", parameters: " + additionalData) :
                                             (", configuration: " + eventHandlerType)) +
                    ")");
        }
    }

    public static void staticLogStart(Trace LOGGER, Event event, String description, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    description +
                    (additionalData != null ? (", parameters: " + additionalData) : ""));
        }
    }

    public void logEnd(Trace LOGGER, Event event, EventHandlerType eventHandlerType, boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event " + event + " result = " + result);
        }
    }

    // expressions

    // shortDesc = what is to be evaluated e.g. "event filter expression"
    protected boolean evaluateBooleanExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables, 
    		String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateBooleanExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException e) {
            failReason = e;
        } catch (SchemaException e) {
            failReason = e;
        } catch (ExpressionEvaluationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    protected boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, String shortDesc, 
    		Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition resultDef = new PrismPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = expression.evaluate(params);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Filter expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    protected List<String> evaluateExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables, 
    		String shortDesc, Task task, OperationResult result) {

        Throwable failReason;
        try {
            return evaluateExpression(expressionType, expressionVariables, shortDesc, task, result);
        } catch (ObjectNotFoundException e) {
            failReason = e;
        } catch (SchemaException e) {
            failReason = e;
        } catch (ExpressionEvaluationException e) {
            failReason = e;
        }

        LoggingUtils.logException(LOGGER, "Couldn't evaluate {} {}", failReason, shortDesc, expressionType);
        result.recordFatalError("Couldn't evaluate " + shortDesc, failReason);
        throw new SystemException(failReason);
    }

    private List<String> evaluateExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, 
    		String shortDesc, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition resultDef = new PrismPropertyDefinition(resultName, DOMUtil.XSD_STRING, prismContext);

        Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(expressionType, resultDef, shortDesc, result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, shortDesc, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = expression.evaluate(params);

        List<String> retval = new ArrayList<String>();
        for (PrismPropertyValue<String> item : exprResult.getZeroSet()) {
            retval.add(item.getValue());
        }
        return retval;
    }

    protected ExpressionVariables getDefaultVariables(Event event, OperationResult result) {

    	ExpressionVariables expressionVariables = new ExpressionVariables();
        Map<QName, Object> variables = new HashMap<QName, Object>();
		event.createExpressionVariables(variables, result);
		expressionVariables.addVariableDefinitions(variables);
        return expressionVariables;
    }



}
