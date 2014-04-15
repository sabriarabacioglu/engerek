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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SearchObjectExpressionEvaluatorType;

/**
 * @author Radovan Semancik
 */
public class AssignmentTargetSearchExpressionEvaluator 
			extends AbstractSearchExpressionEvaluator<PrismContainerValue<AssignmentType>> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentTargetSearchExpressionEvaluator.class);
	
	public AssignmentTargetSearchExpressionEvaluator(SearchObjectExpressionEvaluatorType expressionEvaluatorType, 
			ItemDefinition outputDefinition, Protector protector, ObjectResolver objectResolver, 
			ModelService modelService, PrismContext prismContext) {
		super(expressionEvaluatorType, outputDefinition, protector, objectResolver, modelService, prismContext);
	}
	
	protected PrismContainerValue<AssignmentType> createPrismValue(String oid, QName targetTypeQName, ExpressionEvaluationContext params) {
		AssignmentType assignmentType = new AssignmentType();
		PrismContainerValue<AssignmentType> assignmentCVal = assignmentType.asPrismContainerValue();
		
		ObjectReferenceType targetRef = new ObjectReferenceType();
		targetRef.setOid(oid);
		targetRef.setType(targetTypeQName);
		assignmentType.setTargetRef(targetRef);
		
		try {
			getPrismContext().adopt(assignmentCVal, FocusType.COMPLEX_TYPE, new ItemPath(FocusType.F_ASSIGNMENT));
			if (InternalsConfig.consistencyChecks) {
				assignmentCVal.assertDefinitions("assignmentCVal in assignment expression in "+params.getContextDescription());
			}
		} catch (SchemaException e) {
			// Should not happen
			throw new SystemException(e);
		}
		
		return assignmentCVal;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "assignmentExpression";
	}

}
