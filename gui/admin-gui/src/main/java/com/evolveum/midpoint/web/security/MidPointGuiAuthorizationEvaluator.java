/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MidPointGuiAuthorizationEvaluator implements SecurityEnforcer {

	private SecurityEnforcer securityEnforcer;
	
    public MidPointGuiAuthorizationEvaluator(SecurityEnforcer securityEnforcer) {
		super();
		this.securityEnforcer = securityEnforcer;
	}

	public UserProfileService getUserProfileService() {
		return securityEnforcer.getUserProfileService();
	}

	public void setUserProfileService(UserProfileService userProfileService) {
		securityEnforcer.setUserProfileService(userProfileService);
	}

	public void setupPreAuthenticatedSecurityContext(PrismObject<UserType> user) {
		securityEnforcer.setupPreAuthenticatedSecurityContext(user);
	}

	public MidPointPrincipal getPrincipal() throws SecurityViolationException {
		return securityEnforcer.getPrincipal();
	}

	public <O extends ObjectType, T extends ObjectType> boolean isAuthorized(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver) throws SchemaException {
		return securityEnforcer.isAuthorized(operationUrl, phase, object, delta, target, ownerResolver);
	}

	public boolean supports(ConfigAttribute attribute) {
		return securityEnforcer.supports(attribute);
	}

	@Override
	public <O extends ObjectType, T extends ObjectType> void authorize(String operationUrl, AuthorizationPhaseType phase,
			PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target, OwnerResolver ownerResolver, OperationResult result)
			throws SecurityViolationException, SchemaException {
		securityEnforcer.authorize(operationUrl, phase, object, delta, target, ownerResolver, result);
	}

	public boolean supports(Class<?> clazz) {
		return securityEnforcer.supports(clazz);
	}

	@Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {

        if (!(object instanceof FilterInvocation)) {
            return;
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        Collection<ConfigAttribute> guiConfigAttr = new ArrayList<>();

        for (PageUrlMapping urlMapping : PageUrlMapping.values()) {
            addSecurityConfig(filterInvocation, guiConfigAttr, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, String[]> actions = DescriptorLoader.getActions();
        for (Map.Entry<String, String[]> entry : actions.entrySet()) {
            addSecurityConfig(filterInvocation, guiConfigAttr, entry.getKey(), entry.getValue());
        }

        if (configAttributes == null && guiConfigAttr.isEmpty()) {
            return;
        }

        securityEnforcer.decide(authentication, object, guiConfigAttr.isEmpty() ? configAttributes : guiConfigAttr);
    }

    private void addSecurityConfig(FilterInvocation filterInvocation, Collection<ConfigAttribute> guiConfigAttr,
                      String url, String[] actions) {

        AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
        if (!matcher.matches(filterInvocation.getRequest()) || actions == null) {
            return;
        }

        for (String action : actions) {
            if (StringUtils.isBlank(action)) {
                continue;
            }

            //all users has permission to access these resources
            if (action.equals(AuthorizationConstants.AUTZ_UI_PERMIT_ALL_URL)) {
                return;
            }

            guiConfigAttr.add(new SecurityConfig(action));
        }
    }

    @Override
	public <O extends ObjectType> ObjectSecurityConstraints compileSecurityContraints(PrismObject<O> object, OwnerResolver ownerResolver)
			throws SchemaException {
		return securityEnforcer.compileSecurityContraints(object, ownerResolver);
	}

    @Override
	public <O extends ObjectType> ObjectFilter preProcessObjectFilter(String operationUrl, AuthorizationPhaseType phase,
			Class<O> objectType, ObjectFilter origFilter) throws SchemaException {
		return securityEnforcer.preProcessObjectFilter(operationUrl, phase, objectType, origFilter);
	}
    
    
}
