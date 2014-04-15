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

package com.evolveum.midpoint.model.lens.projector;

import com.evolveum.prism.xml.ns._public.types_2.ProtectedStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.policy.PasswordPolicyUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;


@Component
public class PasswordPolicyProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(FocusPolicyProcessor.class);
	
	@Autowired(required = true)
	Protector protector;

	void processPasswordPolicy(ValuePolicyType passwordPolicy, PrismProperty password, OperationResult result)
			throws PolicyViolationException, SchemaException {

		if (passwordPolicy == null) {
			LOGGER.trace("Skipping processing password policies. Password policy not specified.");
			return;
		}

        String passwordValue = determinePasswordValue(password);

        boolean isValid = PasswordPolicyUtils.validatePassword(passwordValue, passwordPolicy, result);

		if (!isValid) {
			result.computeStatus();
			throw new PolicyViolationException("Provided password does not satisfy password policies. " + result.getMessage());
		}
	}
	
	<F extends FocusType> void processPasswordPolicy(LensFocusContext<F> focusContext, 
			LensContext<F> context, OperationResult result)
			throws PolicyViolationException, SchemaException {
		
		if (!UserType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			LOGGER.trace("Skipping processing password policies because focus is not user");
			return;
		}
		
//		PrismProperty<PasswordType> password = getPassword(focusContext);
		ObjectDelta userDelta = focusContext.getDelta();

		if (userDelta == null) {
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
			return;
		}

		PrismProperty<PasswordType> password = null;
		PrismObject<F> user = null;
		if (ChangeType.ADD == userDelta.getChangeType()) {
			user = focusContext.getDelta().getObjectToAdd();
			if (user != null) {
				password = user.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
			}
		} else if (ChangeType.MODIFY == userDelta.getChangeType()) {
			PropertyDelta<PasswordType> passwordValueDelta = null;
			if (userDelta != null) {
				passwordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
				if (passwordValueDelta == null) {
					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
					return ;
				}
				if (userDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null) {
					if (passwordValueDelta.isAdd()) {
						password = passwordValueDelta.getPropertyNew();
					} else if (passwordValueDelta.isDelete()) {
						password = null;
					} else {
						password = passwordValueDelta.getPropertyNew();
					}
				} else {
					password = passwordValueDelta.getPropertyNew();
				}
			}
		}
		
		ValuePolicyType passwordPolicy = context.getGlobalPasswordPolicy();
		
		processPasswordPolicy(passwordPolicy, password, result);

	}
	
	<F extends ObjectType> void processPasswordPolicy(LensProjectionContext projectionContext, 
			LensContext<F> context, OperationResult result) throws SchemaException, PolicyViolationException{
		
ObjectDelta accountDelta = projectionContext.getDelta();
		
		if (accountDelta == null){
			LOGGER.trace("Skipping processing password policies. User delta not specified.");
			return;
		}
		
		if (ChangeType.DELETE == accountDelta.getChangeType()){
			return;
		}
		
		PrismObject<ShadowType> accountShadow = null;
		PrismProperty<PasswordType> password = null;
		if (ChangeType.ADD == accountDelta.getChangeType()){
			accountShadow = accountDelta.getObjectToAdd();
			if (accountShadow != null){
				password = accountShadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
				
			}
		}
		if (ChangeType.MODIFY == accountDelta.getChangeType() || password == null) {
			PropertyDelta<PasswordType> passwordValueDelta = null;
			if (accountDelta != null) {
				passwordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
				// Modification sanity check
				if (accountDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
				}
				if (passwordValueDelta == null) {
					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
					return;
				}
				password = passwordValueDelta.getPropertyNew();
			}
		}

//		PrismProperty<PasswordType> password = getPassword(projectionContext);
		
		ValuePolicyType passwordPolicy = projectionContext.getEffectivePasswordPolicy();
		
		processPasswordPolicy(passwordPolicy, password, result);
	}

//	private PrismProperty<PasswordType> getPassword(LensProjectionContext<AccountShadowType> projectionContext) throws SchemaException{
//		ObjectDelta accountDelta = projectionContext.getDelta();
//		
//		if (accountDelta == null){
//			LOGGER.trace("Skipping processing password policies. User delta not specified.");
//			return null;
//		}
//		
//		if (ChangeType.DELETE == accountDelta.getChangeType()){
//			return null;
//		}
//		
//		PrismObject<AccountShadowType> accountShadow = null;
//		PrismProperty<PasswordType> password = null;
//		if (ChangeType.ADD == accountDelta.getChangeType()){
//			accountShadow = accountDelta.getObjectToAdd();
//			if (accountShadow != null){
//				password = accountShadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
//				
//			}
//		}
//		if (ChangeType.MODIFY == accountDelta.getChangeType() || password == null) {
//			PropertyDelta<PasswordType> passwordValueDelta = null;
//			if (accountDelta != null) {
//				passwordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
//				// Modification sanity check
//				if (accountDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
//						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
//					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
//				}
//				if (passwordValueDelta == null) {
//					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
//					return null;
//				}
//				password = passwordValueDelta.getPropertyNew();
//			}
//		}
//
//		return password;
//	}
	
//	private PrismProperty<PasswordType> getPassword(LensFocusContext<UserType> focusContext)
//			throws SchemaException {
//		
//
//		ObjectDelta userDelta = focusContext.getDelta();
//
//		if (userDelta == null) {
//			LOGGER.trace("Skipping processing password policies. User delta not specified.");
//			return null;
//		}
//
//		PrismProperty<PasswordType> password = null;
//		PrismObject<UserType> user = null;
//		if (ChangeType.ADD == userDelta.getChangeType()) {
//			user = focusContext.getDelta().getObjectToAdd();
//			if (user != null) {
//				password = user.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
//			}
//		} else if (ChangeType.MODIFY == userDelta.getChangeType()) {
//			PropertyDelta<PasswordType> passwordValueDelta = null;
//			if (userDelta != null) {
//				passwordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
//				// Modification sanity check
//				if (userDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
//						&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
//					throw new SchemaException("User password value cannot be added or deleted, it can only be replaced");
//				}
//				if (passwordValueDelta == null) {
//					LOGGER.trace("Skipping processing password policies. User delta does not contain password change.");
//					return null;
//				}
//				password = passwordValueDelta.getPropertyNew();
//			}
//		}
//		return password;
//	}


    // On missing password this returns empty string (""). It is then up to password policy whether it allows empty passwords or not.
	private String determinePasswordValue(PrismProperty<PasswordType> password) {
		if (password == null || password.getValue(ProtectedStringType.class) == null) {
			return "";
		}

		ProtectedStringType passValue = password.getValue(ProtectedStringType.class).getValue();

		if (passValue == null) {
			return "";
		}

		String passwordStr = passValue.getClearValue();

		if (passwordStr == null && passValue.getEncryptedDataType () != null) {
			// TODO: is this appropriate handling???
			try {
				passwordStr = protector.decryptString(passValue);
			} catch (EncryptionException ex) {
				throw new SystemException("Failed to process password for user: " , ex);
			}
		}

		return passwordStr != null ? passwordStr : "";
	}


}
