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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;

/**
 * @author semancik
 *
 */
public class LiteralExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {
	
	private PrismContext prismContext;

	public LiteralExpressionEvaluatorFactory(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
	}

	@Override
	public QName getElementName() {
		return new ObjectFactory().createValue(null).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public <V extends PrismValue> ExpressionEvaluator<V> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, ItemDefinition outputDefinition, 
			String contextDescription, OperationResult result) throws SchemaException {
		
		Item<V> output = StaticExpressionUtil.parseValueElements(evaluatorElements, outputDefinition, contextDescription, prismContext);
		
		PrismValueDeltaSetTriple<V> deltaSetTriple = ItemDelta.toDeltaSetTriple(output, null);
		
		return new LiteralExpressionEvaluator<V>(deltaSetTriple);
	}
	
}
