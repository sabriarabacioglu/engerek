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

package com.evolveum.midpoint.model.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConditionalSearchFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;

@Component
public class CorrelationConfirmationEvaluator {

	private static transient Trace LOGGER = TraceManager.getTrace(CorrelationConfirmationEvaluator.class);

	@Autowired(required = true)
	private RepositoryService repositoryService;

	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;
	
	public <F extends FocusType> List<PrismObject<F>> findFocusesByCorrelationRule(Class<F> focusType, ShadowType currentShadow,
			List<ConditionalSearchFilterType> conditionalFilters, ResourceType resourceType, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

		if (conditionalFilters == null || conditionalFilters.isEmpty()) {
			LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
					+ "returning empty list of users.", resourceType);
			return null;
		}

		List<PrismObject<F>> users = null;
		if (conditionalFilters.size() == 1){
			if (satisfyCondition(currentShadow, conditionalFilters.get(0), resourceType, "Condition expression", task, result)){
				LOGGER.trace("Condition {} in correlation expression evaluated to true", conditionalFilters.get(0).getCondition());
				users = findUsersByCorrelationRule(focusType, currentShadow, conditionalFilters.get(0), resourceType, task, result);
			}
			
		} else {

			for (ConditionalSearchFilterType conditionalFilter : conditionalFilters) {
				//TODO: better description
				if (satisfyCondition(currentShadow, conditionalFilter, resourceType, "Condition expression", task, result)) {
					LOGGER.trace("Condition {} in correlation expression evaluated to true", conditionalFilter.getCondition());
					List<PrismObject<F>> foundUsers = findUsersByCorrelationRule(focusType,
							currentShadow, conditionalFilter, resourceType, task, result);
					if (foundUsers == null && users == null) {
						continue;
					}
					if (foundUsers != null && foundUsers.isEmpty() && users == null) {
						users = new ArrayList<PrismObject<F>>();
					}

					if (users == null && foundUsers != null) {
						users = foundUsers;
					}
					if (users != null && !users.isEmpty() && foundUsers != null && !foundUsers.isEmpty()) {
						for (PrismObject<F> foundUser : foundUsers) {
							if (!contains(users, foundUser)) {
								users.add(foundUser);
							}
						}
					}
				}
			}
		}
		
		if (users != null) {
			LOGGER.debug(
					"SYNCHRONIZATION: CORRELATION: expression for {} returned {} users: {}",
					new Object[] { currentShadow, users.size(),
							PrettyPrinter.prettyPrint(users, 3) });
		}
		return users;
	}
	
	private boolean satisfyCondition(ShadowType currentShadow, ConditionalSearchFilterType conditionalFilter,
			ResourceType resourceType, String shortDesc, Task task,
			OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException {
		
		if (conditionalFilter.getCondition() == null){
			return true;
		}
		
		ExpressionType condition = conditionalFilter.getCondition();
		ExpressionVariables variables = Utils.getDefaultExpressionVariables(null,currentShadow, resourceType);
		ItemDefinition outputDefinition = new PrismPropertyDefinition(
				ExpressionConstants.OUTPUT_ELMENT_NAME, DOMUtil.XSD_BOOLEAN,
				prismContext);
		PrismPropertyValue<Boolean> satisfy = ExpressionUtil.evaluateExpression(variables,
				outputDefinition, condition, expressionFactory, shortDesc, task, parentResult);
		if (satisfy.getValue() == null) {
			return false;
		}

		return satisfy.getValue();
	}

	private <F extends FocusType> boolean contains(List<PrismObject<F>> users, PrismObject<F> foundUser){
		for (PrismObject<F> user : users){
			if (user.getOid().equals(foundUser.getOid())){
				return true;
			}
		}
		return false;
	}
	
	
		
		private <F extends FocusType> List<PrismObject<F>> findUsersByCorrelationRule(Class<F> focusType,
				ShadowType currentShadow, ConditionalSearchFilterType conditionalFilter, ResourceType resourceType, Task task, OperationResult result)
						throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
			if (!conditionalFilter.containsFilterClause()) {
				LOGGER.warn("Correlation rule for resource '{}' doesn't contain filter clause, "
						+ "returning empty list of users.", resourceType);
				return null;
			}
			
			ObjectQuery q = null;
			try {
				q = QueryJaxbConvertor.createObjectQuery(focusType, conditionalFilter, prismContext);
				q = updateFilterWithAccountValues(currentShadow, resourceType, q, "Correlation expression", task, result);
				if (q == null) {
					// Null is OK here, it means that the value in the filter
					// evaluated
					// to null and the processing should be skipped
					return null;
				}
				
				
			} catch (SchemaException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
						SchemaDebugUtil.prettyPrint(conditionalFilter));
				throw new SchemaException("Couldn't convert query.", ex);
			} catch (ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
						SchemaDebugUtil.prettyPrint(conditionalFilter));
				throw new ObjectNotFoundException("Couldn't convert query.", ex);
			} catch (ExpressionEvaluationException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
						SchemaDebugUtil.prettyPrint(conditionalFilter));
				throw new ExpressionEvaluationException("Couldn't convert query.", ex);
			}
			
			List<PrismObject<F>> users = null;
			try {
				// query = new QueryType();
				// query.setFilter(filter);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for results in filter\n{}",
							new Object[] {q.debugDump() });
				}
				PagingType paging = new PagingType();
				// ObjectQuery q = QueryConvertor.createObjectQuery(UserType.class,
				// query, prismContext);
				users = repositoryService.searchObjects(focusType, q, null, result);

				if (users == null) {
					users = new ArrayList<PrismObject<F>>();
				}
			} catch (RuntimeException ex) {
				LoggingUtils.logException(LOGGER,
						"Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.debugDump());
				throw new SystemException(
						"Couldn't search users in repository, based on filter (See logs).", ex);
			}
			
			return users;
		}


private <F extends FocusType> boolean matchUserCorrelationRule(Class<F> focusType, PrismObject<ShadowType> currentShadow, 
		PrismObject<F> userType, ResourceType resourceType, ConditionalSearchFilterType conditionalFilter, Task task, OperationResult result){
	if (conditionalFilter == null) {
		LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
				+ "returning empty list of users.", resourceType);
		return false;
	}

	if (!conditionalFilter.containsFilterClause()) {
		LOGGER.warn("Correlation rule for resource '{}' doesn't contain a filter, "
				+ "returning empty list of users.", resourceType);
		return false;
	}

	ObjectQuery q = null;
	try {
		q = QueryJaxbConvertor.createObjectQuery(focusType, conditionalFilter, prismContext);
		q = updateFilterWithAccountValues(currentShadow.asObjectable(), resourceType, q, "Correlation expression", task, result);
		LOGGER.debug("Start matching user {} with correlation eqpression {}", userType, q.debugDump());
		if (q == null) {
			// Null is OK here, it means that the value in the filter
			// evaluated
			// to null and the processing should be skipped
			return false;
		}
	} catch (Exception ex) {
		LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
				SchemaDebugUtil.prettyPrint(conditionalFilter));
		throw new SystemException("Couldn't convert query.", ex);
	}
//	List<PrismObject<UserType>> users = null;
//	try {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for {} results in filter\n{}",
					new Object[] { currentShadow, q});
		}

//		PagingType paging = new PagingType();
		return ObjectQuery.match(userType, q.getFilter(), matchingRuleRegistry);

//		if (users == null) {
//			users = new ArrayList<PrismObject<UserType>>();
//		}
//	} catch (Exception ex) {
//		LoggingUtils.logException(LOGGER,
//				"Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.dump());
//		throw new SynchronizationException(
//				"Couldn't search users in repository, based on filter (See logs).", ex);
//	}

}
	public <F extends FocusType> boolean matchUserCorrelationRule(Class<F> focusType, PrismObject<ShadowType> currentShadow, 
			PrismObject<F> userType, ObjectSynchronizationType synchronization, ResourceType resourceType, Task task, OperationResult result){

		if (synchronization == null){
			LOGGER.warn(
					"Resource does not support synchornization. Skipping evaluation correlation/confirmation for user {} and account {}",
					userType, currentShadow);
			return false;
		}
		
		List<ConditionalSearchFilterType> conditionalFilters = synchronization.getCorrelation();
		
		for (ConditionalSearchFilterType conditionalFilter : conditionalFilters){
			
			if (true && matchUserCorrelationRule(focusType, currentShadow, userType, resourceType, conditionalFilter, task, result)){
				LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} match user: {}", new Object[] {
						currentShadow, userType });
				return true;
			}
		}
		
		
		LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} does not match user: {}", new Object[] {
				currentShadow, userType });
		return false;
	}

		public <F extends FocusType> List<PrismObject<F>> findUserByConfirmationRule(Class<F> focusType, List<PrismObject<F>> users,
			ShadowType currentShadow, ResourceType resource, ExpressionType expression, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException
			 {

		List<PrismObject<F>> list = new ArrayList<PrismObject<F>>();
		for (PrismObject<F> user : users) {
			try {
				F userType = user.asObjectable();
				boolean confirmedUser = evaluateConfirmationExpression(focusType, userType,
						currentShadow, resource, expression, task, result);
				if (user != null && confirmedUser) {
					list.add(user);
				}
			} catch (RuntimeException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getElementName());
				throw new SystemException("Couldn't confirm user " + user.getElementName(), ex);
			} catch (ExpressionEvaluationException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getElementName());
				throw new ExpressionEvaluationException("Couldn't confirm user " + user.getElementName(), ex);
			} catch (ObjectNotFoundException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getElementName());
				throw new ObjectNotFoundException("Couldn't confirm user " + user.getElementName(), ex);
			} catch (SchemaException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't confirm user {}", ex, user.getElementName());
				throw new SchemaException("Couldn't confirm user " + user.getElementName(), ex);
			}
		}

		LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: expression for {} matched {} users.", new Object[] {
				currentShadow, list.size() });
		return list;
	}

	private ObjectQuery updateFilterWithAccountValues(ShadowType currentShadow, ResourceType resource,
			ObjectQuery origQuery, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		if (origQuery.getFilter() == null) {
			LOGGER.trace("No filter provivided, skipping updating filter");
			return origQuery;
		}
		
		return evaluateQueryExpressions(origQuery, currentShadow, resource, shortDesc, task, result);
	}

	private ObjectQuery evaluateQueryExpressions(ObjectQuery query, ShadowType currentShadow, ResourceType resource, String shortDesc, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ExpressionVariables variables = Utils.getDefaultExpressionVariables(null, currentShadow, resource);
		return ExpressionUtil.evaluateQueryExpressions(query, variables, expressionFactory, prismContext, shortDesc, task, result);
	}
	
	public <F extends FocusType> boolean evaluateConfirmationExpression(Class<F> focusType, F user, ShadowType shadow, ResourceType resource,
			ExpressionType expressionType, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		ExpressionVariables variables = Utils.getDefaultExpressionVariables(user, shadow, resource);
		String shortDesc = "confirmation expression for "+resource.asPrismObject();
		
		PrismPropertyDefinition outputDefinition = new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, 
				outputDefinition, shortDesc, result);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(params);
		Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        PrismPropertyValue<Boolean> resultpval = nonNegativeValues.iterator().next();
        if (resultpval == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        Boolean resultVal = resultpval.getValue();
        if (resultVal == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
		return resultVal;
	}

}
