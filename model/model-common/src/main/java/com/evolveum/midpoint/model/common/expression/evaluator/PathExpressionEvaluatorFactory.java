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

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;

/**
 * @author semancik
 *
 */
public class PathExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {
	
	private PrismContext prismContext;
	private ObjectResolver objectResolver;
	private Protector protector;

	public PathExpressionEvaluatorFactory(PrismContext prismContext, ObjectResolver objectResolver,
			Protector protector) {
		super();
		this.prismContext = prismContext;
		this.objectResolver = objectResolver;
		this.protector = protector;
	}

	@Override
	public QName getElementName() {
		return new ObjectFactory().createPath(null).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.ItemDefinition, com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public <V extends PrismValue> ExpressionEvaluator<V> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
			ItemDefinition outputDefinition, String contextDescription, OperationResult result) throws SchemaException {
		
		if (evaluatorElements.size() > 1) {
			throw new SchemaException("More than one evaluator specified in "+contextDescription);
		}
		JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();
		
		Object evaluatorElementObject = evaluatorElement.getValue();
		 if (!(evaluatorElementObject instanceof ItemPathType)) {
	            throw new IllegalArgumentException("Path expression cannot handle elements of type " 
	            		+ evaluatorElementObject.getClass().getName()+" in "+contextDescription);
	        }
//        if (!(evaluatorElementObject instanceof Element)) {
//            throw new IllegalArgumentException("Path expression cannot handle elements of type " 
//            		+ evaluatorElementObject.getClass().getName()+" in "+contextDescription);
//        }
        
//        XPathHolder xpath = new XPathHolder((Element)evaluatorElementObject);
        ItemPath path = ((ItemPathType)evaluatorElementObject).getItemPath();
        
        return new PathExpressionEvaluator(path, objectResolver, outputDefinition, protector, prismContext);
        
	}

}
