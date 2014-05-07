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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * @author semancik
 *
 */
public class ExpressionFactory {
	
	private Map<QName,ExpressionEvaluatorFactory> evaluatorFactories = new HashMap<QName, ExpressionEvaluatorFactory>();
	private ExpressionEvaluatorFactory defaultEvaluatorFactory;
	private Map<ExpressionIdentifier, Expression<?>> cache = new HashMap<ExpressionIdentifier, Expression<?>>();
	private PrismContext prismContext;
	private ObjectResolver objectResolver;
	
	public ExpressionFactory(ObjectResolver objectResolver, PrismContext prismContext) {
		super();
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
	}
	
	/**
	 * Factory method created especially to be used from the Spring context.
	 */
	public ExpressionFactory(ObjectResolver objectResolver, PrismContext prismContext, 
			Collection<ExpressionEvaluatorFactory> evaluatorFactories) {
		super();
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
		for (ExpressionEvaluatorFactory evaluatorFactory: evaluatorFactories) {
			addEvaluatorFactory(evaluatorFactory);
		}
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public <V extends PrismValue> Expression<V> makeExpression(ExpressionType expressionType, 
			ItemDefinition outputDefinition, String shortDesc, OperationResult result) 
					throws SchemaException, ObjectNotFoundException {
		ExpressionIdentifier eid = new ExpressionIdentifier(expressionType, outputDefinition);
		Expression<V> expression = (Expression<V>) cache.get(eid);
		if (expression == null) {
			expression = createExpression(expressionType, outputDefinition, shortDesc, result);
			cache.put(eid, expression);
		}
		return expression;
	}

	private <V extends PrismValue> Expression<V> createExpression(ExpressionType expressionType, 
			ItemDefinition outputDefinition, String shortDesc, OperationResult result) 
					throws SchemaException, ObjectNotFoundException {
		Expression<V> expression = new Expression<V>(expressionType, outputDefinition, objectResolver, prismContext);
		expression.parse(this, shortDesc, result);
		return expression;
	}
	
	public <V extends PrismValue> ExpressionEvaluatorFactory getEvaluatorFactory(QName elementName) {
		return evaluatorFactories.get(elementName);
	}
	
	public void addEvaluatorFactory(ExpressionEvaluatorFactory factory) {
		evaluatorFactories.put(factory.getElementName(), factory);
	}
	
	public ExpressionEvaluatorFactory getDefaultEvaluatorFactory() {
		return defaultEvaluatorFactory;
	}
	
	public void setDefaultEvaluatorFactory(ExpressionEvaluatorFactory defaultEvaluatorFactory) {
		this.defaultEvaluatorFactory = defaultEvaluatorFactory;
	}

	class ExpressionIdentifier {
		private ExpressionType expressionType;
		private ItemDefinition outputDefinition;
		
		ExpressionIdentifier(ExpressionType expressionType, ItemDefinition outputDefinition) {
			super();
			this.expressionType = expressionType;
			this.outputDefinition = outputDefinition;
		}

		public ExpressionType getExpressionType() {
			return expressionType;
		}

		public void setExpressionType(ExpressionType expressionType) {
			this.expressionType = expressionType;
		}

		public ItemDefinition getOutputDefinition() {
			return outputDefinition;
		}

		public void setOutputDefinition(ItemDefinition outputDefinition) {
			this.outputDefinition = outputDefinition;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((expressionType == null) ? 0 : expressionType.hashCode());
			result = prime * result + ((outputDefinition == null) ? 0 : outputDefinition.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ExpressionIdentifier other = (ExpressionIdentifier) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (expressionType == null) {
				if (other.expressionType != null)
					return false;
			} else if (!expressionType.equals(other.expressionType))
				return false;
			if (outputDefinition == null) {
				if (other.outputDefinition != null)
					return false;
			} else if (!outputDefinition.equals(other.outputDefinition))
				return false;
			return true;
		}

		private ExpressionFactory getOuterType() {
			return ExpressionFactory.this;
		}
	}

}
