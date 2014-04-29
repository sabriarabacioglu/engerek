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
package com.evolveum.midpoint.security.api;

import java.util.List;

import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;

import org.springframework.security.core.GrantedAuthority;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSpecificationType;

/**
 * @author semancik
 *
 */
public class Authorization implements GrantedAuthority, DebugDumpable {
	
	AuthorizationType authorizationType;

	public Authorization(AuthorizationType authorizationType) {
		super();
		this.authorizationType = authorizationType;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.GrantedAuthority#getAuthority()
	 */
	@Override
	public String getAuthority() {
		// this is complex authority. Just return null
		return null;
	}

	public String getDescription() {
		return authorizationType.getDescription();
	}

	public AuthorizationDecisionType getDecision() {
		 AuthorizationDecisionType decision = authorizationType.getDecision();
		 if (decision == null) {
			 return AuthorizationDecisionType.ALLOW;
		 }
		 return decision;
	}

	public List<String> getAction() {
		return authorizationType.getAction();
	}

	public AuthorizationPhaseType getPhase() {
		return authorizationType.getPhase();
	}

	public List<ObjectSpecificationType> getObject() {
		return authorizationType.getObject();
	}

	public List<ItemPathType> getItem() {
		return authorizationType.getItem();
	}

	public List<ObjectSpecificationType> getTarget() {
		return authorizationType.getTarget();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		// TODO Auto-generated method stub
		return debugDump(0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "Authorization", indent);
		if (authorizationType == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			authorizationType.asPrismContainerValue().debugDump(indent+1);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Authorization(" + authorizationType == null ? "null" : authorizationType.getAction() + ")";
	}

}
