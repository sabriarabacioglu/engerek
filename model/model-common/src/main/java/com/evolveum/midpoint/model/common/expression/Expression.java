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
package com.evolveum.midpoint.model.common.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class Expression<V extends PrismValue> {
	
	ExpressionType expressionType;
	ItemDefinition outputDefinition;
	PrismContext prismContext;
	ObjectResolver objectResolver;
	List<ExpressionEvaluator<V>> evaluators = new ArrayList<ExpressionEvaluator<V>>(1);
	
	private static final Trace LOGGER = TraceManager.getTrace(Expression.class);

	public Expression(ExpressionType expressionType, ItemDefinition outputDefinition, ObjectResolver objectResolver, PrismContext prismContext) {
		Validate.notNull(outputDefinition, "null outputDefinition");
		Validate.notNull(objectResolver, "null objectResolver");
		Validate.notNull(prismContext, "null prismContext");
		this.expressionType = expressionType;
		this.outputDefinition = outputDefinition;
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
	}
	
	public void parse(ExpressionFactory factory, String contextDescription, OperationResult result) 
			throws SchemaException, ObjectNotFoundException {
		if (expressionType == null) {
			evaluators.add(createDefaultEvaluator(factory, contextDescription, result));
			return;
		}
		if (expressionType.getExpressionEvaluator() == null /* && expressionType.getSequence() == null */) {
			throw new SchemaException("No evaluator was specified in "+contextDescription);
		}
		if (expressionType.getExpressionEvaluator() != null) {
			ExpressionEvaluator evaluator = createEvaluator(expressionType.getExpressionEvaluator(), factory, 
					contextDescription, result);
			evaluators.add(evaluator);
		}
		if (evaluators.isEmpty()) {
			evaluators.add(createDefaultEvaluator(factory, contextDescription, result));
		}
	}

	private ExpressionEvaluator<V> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, ExpressionFactory factory,
			String contextDescription, OperationResult result) 
			throws SchemaException, ObjectNotFoundException {
		if (evaluatorElements.isEmpty()) {
			throw new SchemaException("Empty evaluator list in "+contextDescription);
		}
		JAXBElement<?> fistEvaluatorElement = evaluatorElements.iterator().next();
		ExpressionEvaluatorFactory evaluatorFactory = factory.getEvaluatorFactory(fistEvaluatorElement.getName());
		if (evaluatorFactory == null) {
			throw new SchemaException("Unknown expression evaluator element "+fistEvaluatorElement.getName()+" in "+contextDescription);
		}
		return evaluatorFactory.createEvaluator(evaluatorElements, outputDefinition, contextDescription, result);
	}

	private ExpressionEvaluator<V> createDefaultEvaluator(ExpressionFactory factory, String contextDescription, 
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		ExpressionEvaluatorFactory evaluatorFactory = factory.getDefaultEvaluatorFactory();
		if (evaluatorFactory == null) {
			throw new SystemException("Internal error: No default expression evaluator factory");
		}
		return evaluatorFactory.createEvaluator(null, outputDefinition, contextDescription, result);
	}
	
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
		
		ExpressionVariables processedVariables = null;
		
		try {
		
			processedVariables = processInnerVariables(context.getVariables(), context.getContextDescription(),
					context.getResult());
			
			ExpressionEvaluationContext processedParameters = context.shallowClone();
			processedParameters.setVariables(processedVariables);
			
			for (ExpressionEvaluator<?> evaluator: evaluators) {
				PrismValueDeltaSetTriple<V> outputTriple = (PrismValueDeltaSetTriple<V>) evaluator.evaluate(processedParameters);
				if (outputTriple != null) {
					traceSuccess(context, processedVariables, outputTriple);
					return outputTriple;
				}
			}
			traceSuccess(context, processedVariables, null);
			return null;
		} catch (SchemaException ex) {
			traceFailure(context, processedVariables, ex);
			throw ex;
		} catch (ExpressionEvaluationException ex) {
			traceFailure(context, processedVariables, ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			traceFailure(context, processedVariables, ex);
			throw ex;
		} catch (RuntimeException ex) {
			traceFailure(context, processedVariables, ex);
			throw ex;
		}
	}
	
	private void traceSuccess(ExpressionEvaluationContext context, ExpressionVariables processedVariables, PrismValueDeltaSetTriple<V> outputTriple) {
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Expression trace:\n");
		appendTraceHeader(sb, context, processedVariables);
		sb.append("\nResult: ");
		if (outputTriple == null) {
			sb.append("null");
		} else {
			sb.append(outputTriple.toHumanReadableString());
		}
		appendTraceFooter(sb);
		LOGGER.trace(sb.toString());
	}
	
	private void traceFailure(ExpressionEvaluationContext context, ExpressionVariables processedVariables, Exception e) {
		LOGGER.error("Error evaluating expression in {}: {}", new Object[]{context.getContextDescription(), e.getMessage(), e});
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Expression failure:\n");
		appendTraceHeader(sb, context, processedVariables);
		sb.append("\nERROR: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
		appendTraceFooter(sb);
		LOGGER.trace(sb.toString());
	}

	private void appendTraceHeader(StringBuilder sb, ExpressionEvaluationContext context, ExpressionVariables processedVariables) {
		sb.append("---[ EXPRESSION in ");
		sb.append(context.getContextDescription());
		sb.append("]---------------------------");
		sb.append("\nSources:");
		Collection<Source<? extends PrismValue>> sources = context.getSources();
		if (sources == null) {
			sb.append(" null");
		} else {
			for (Source<? extends PrismValue> source: sources) {
				sb.append("\n");
				sb.append(source.debugDump(1));
			}
		}
		sb.append("\nVariables:");
		if (processedVariables == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			sb.append(processedVariables.debugDump(1));
		}
		sb.append("\nOutput definition: ").append(MiscUtil.toString(outputDefinition));
		sb.append("\nEvaluators: ");
		sb.append(shortDebugDump());
	}
	
	private void appendTraceFooter(StringBuilder sb) {
		sb.append("\n------------------------------------------------------");
	}

	private ExpressionVariables processInnerVariables(ExpressionVariables variables, String contextDescription,
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (expressionType == null || expressionType.getVariable() == null || expressionType.getVariable().isEmpty()) {
			// shortcut
			return variables;
		}
		ExpressionVariables newVariables = new ExpressionVariables();
		for(Entry<QName,Object> entry: variables.entrySet()) {
			newVariables.addVariableDefinition(entry.getKey(), entry.getValue());
		}
		for (ExpressionVariableDefinitionType variableDefType: expressionType.getVariable()) {
			QName varName = variableDefType.getName();
			if (varName == null) {
				throw new SchemaException("No variable name in expression in "+contextDescription);
			}
			if (variableDefType.getObjectRef() != null) {
				ObjectType varObject = objectResolver.resolve(variableDefType.getObjectRef(), ObjectType.class, null, "variable "+varName+" in "+contextDescription, result);
				newVariables.addVariableDefinition(varName, varObject);
			} else if (variableDefType.getValue() != null) {
				// Only string is supported now
				Object valueObject = variableDefType.getValue();
				if (valueObject instanceof String) {
					newVariables.addVariableDefinition(varName, valueObject);
				} else if (valueObject instanceof Element) {
					newVariables.addVariableDefinition(varName, ((Element)valueObject).getTextContent());
				} else if (valueObject instanceof RawType) {
					newVariables.addVariableDefinition(varName, ((RawType) valueObject).getParsedValue(null, varName));
				} else {
					throw new SchemaException("Unexpected type "+valueObject.getClass()+" in variable definition "+varName+" in "+contextDescription);
				}
			} else if (variableDefType.getPath() != null) {
				ItemPath itemPath = variableDefType.getPath().getItemPath();
				Object resolvedValue = ExpressionUtil.resolvePath(itemPath, variables, null, objectResolver, contextDescription, result);
				newVariables.addVariableDefinition(varName, resolvedValue);
			} else {
				throw new SchemaException("No value for variable "+varName+" in "+contextDescription);
			}
		}
		return newVariables;
	}

	@Override
	public String toString() {
		return "Expression(expressionType=" + expressionType + ", outputDefinition=" + outputDefinition
				+ ": " + shortDebugDump() + ")";
	}

	public String shortDebugDump() {
		if (evaluators == null) {
			return "null evaluators";
		}
		if (evaluators.isEmpty()) {
			return "[]";
		}
		if (evaluators.size() == 1) {
			return evaluators.iterator().next().shortDebugDump();
		}
		StringBuilder sb = new StringBuilder("[");
		for (ExpressionEvaluator<V> evaluator: evaluators) {
			sb.append(evaluator.shortDebugDump());
			sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
	
}
