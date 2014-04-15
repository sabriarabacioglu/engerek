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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author Radovan Semancik
 */
public class PathExpressionEvaluator<V extends PrismValue> implements ExpressionEvaluator<V> {
	
	private ItemPath path;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private ItemDefinition outputDefinition;
	private Protector protector;
	
    public PathExpressionEvaluator(ItemPath path, ObjectResolver objectResolver, 
    		ItemDefinition outputDefinition, Protector protector, PrismContext prismContext) {
    	this.path = path;
		this.objectResolver = objectResolver;
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
		this.protector = protector;
	}

    /* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext params) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {

		ItemDeltaItem<?> resolveContext = null;
		
		if (params.getSources() != null && params.getSources().size() == 1) {
			Source<?> source = params.getSources().iterator().next();
			if (path.isEmpty()) {
				PrismValueDeltaSetTriple<V> outputTriple = (PrismValueDeltaSetTriple<V>) source.toDeltaSetTriple();
				return outputTriple.clone();
			}
			resolveContext = source;
		}
		        
        Map<QName, Object> variablesAndSources = ExpressionUtil.compileVariablesAndSources(params);

        ItemPath resolvePath = path;
        ItemPathSegment first = path.first();
        if (first instanceof NameItemPathSegment && ((NameItemPathSegment)first).isVariable()) {
			QName variableName = ((NameItemPathSegment)first).getName();
			Object variableValue = null;
        	if (variablesAndSources.containsKey(variableName)) {
        		variableValue = variablesAndSources.get(variableName);
        	} else if (QNameUtil.matchAny(variableName, variablesAndSources.keySet())){
				QName fullVariableName = QNameUtil.resolveNs(variableName, variablesAndSources.keySet());
				variableValue = variablesAndSources.get(fullVariableName);
			} else {
				throw new ExpressionEvaluationException("No variable with name "+variableName+" in "+params.getContextDescription());
			}
        	
        	if (variableValue == null) {
    			return null;
    		}
    		if (variableValue instanceof Item || variableValue instanceof ItemDeltaItem<?>) {
        		resolveContext = ExpressionUtil.toItemDeltaItem(variableValue, objectResolver, 
        				"path expression in "+params.getContextDescription(), params.getResult());
    		} else if (variableValue instanceof PrismPropertyValue<?>){
    			PrismValueDeltaSetTriple<V> outputTriple = new PrismValueDeltaSetTriple<>();
    			outputTriple.addToZeroSet((V) variableValue);
    			return ExpressionUtil.toOutputTriple(outputTriple, outputDefinition, null, protector, prismContext);
    		} else {
    			throw new ExpressionEvaluationException("Unexpected variable value "+variableValue+" ("+variableValue.getClass()+")");
    		}
    		
        	resolvePath = path.rest();
        }
        
        if (resolveContext == null) {
        	return null;
        }
        
       while (!resolvePath.isEmpty()) {
    	    if (resolveContext.isContainer()) {
        		resolveContext = resolveContext.findIdi(resolvePath.head());
        		resolvePath = resolvePath.tail();
        		if (resolveContext == null) {
        			throw new ExpressionEvaluationException("Cannot find item using path "+path+" in "+params.getContextDescription());
        		}
        	} else if (resolveContext.isStructuredProperty()) {
        		// The output path does not really matter. The delta will be converted to triple anyway
                // But the path cannot be null, oherwise the code will die
        		resolveContext = resolveContext.resolveStructuredProperty(resolvePath, (PrismPropertyDefinition) outputDefinition,
                        new ItemPath());
        		break;
        	} else if (resolveContext.isNull()){
        		break;
        	} else {
        		throw new ExpressionEvaluationException("Cannot resolve path "+resolvePath+" on "+resolveContext+" in "+params.getContextDescription());
        	}
        }
                
        PrismValueDeltaSetTriple<V> outputTriple = ItemDelta.toDeltaSetTriple((Item<V>)resolveContext.getItemOld(), 
        		(ItemDelta<V>)resolveContext.getDelta());
        
        if (outputTriple == null) {
        	return null;
        }
        
        return ExpressionUtil.toOutputTriple(outputTriple, outputDefinition, null, protector, prismContext);
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "path: "+path;
	}


}
