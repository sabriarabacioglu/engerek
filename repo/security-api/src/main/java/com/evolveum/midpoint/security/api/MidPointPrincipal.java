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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class MidPointPrincipal implements UserDetails,  DebugDumpable {
	
	private static final long serialVersionUID = 8299738301872077768L;
    private UserType user;
    private Collection<Authorization> authorizations = new ArrayList<Authorization>();
    private ActivationStatusType effectiveActivationStatus;

    public MidPointPrincipal(UserType user) {
        Validate.notNull(user, "User must not be null.");
        this.user = user;
    }

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getAuthorities()
	 */
	@Override
	public Collection<Authorization> getAuthorities() {
		return authorizations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getPassword()
	 */
	@Override
	public String getPassword() {
		// We won't return password
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getUsername()
	 */
	@Override
	public String getUsername() {
		return getUser().getName().getOrig();
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonExpired()
	 */
	@Override
	public boolean isAccountNonExpired() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonLocked()
	 */
	@Override
	public boolean isAccountNonLocked() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isCredentialsNonExpired()
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
        if (effectiveActivationStatus == null) {
            effectiveActivationStatus = createEffectiveActivationStatus();
        }
		return effectiveActivationStatus == ActivationStatusType.ENABLED;
	}

    private ActivationStatusType createEffectiveActivationStatus() {
        //todo improve

        CredentialsType credentials = user.getCredentials();
        if (credentials == null || credentials.getPassword() == null){
            return ActivationStatusType.DISABLED;
        }

        if (user.getActivation() == null) {
            return ActivationStatusType.DISABLED;
        }

        ActivationType activation = user.getActivation();
        
        if (activation.getAdministrativeStatus() != null) {
            return activation.getAdministrativeStatus();
        }
        
        long time = System.currentTimeMillis();
        if (activation.getValidFrom() != null) {
            long from = MiscUtil.asDate(activation.getValidFrom()).getTime();
            if (time < from) {
                return ActivationStatusType.DISABLED;
            }
        }
        if (activation.getValidTo() != null) {
            long to = MiscUtil.asDate(activation.getValidTo()).getTime();
            if (to < time) {
                return ActivationStatusType.DISABLED;
            }
        }
        
        return ActivationStatusType.ENABLED;
    }

	public UserType getUser() {
        return user;
    }

    public PolyStringType getName() {
        return getUser().getName();
    }

    public String getFamilyName() {
        PolyStringType string = getUser().getFamilyName();
        return string != null ? string.getOrig() : null;
    }

    public String getFullName() {
        PolyStringType string = getUser().getFullName();
        return string != null ? string.getOrig() : null;
    }

    public String getGivenName() {
        PolyStringType string = getUser().getGivenName();
        return string != null ? string.getOrig() : null;
    }

    public String getOid() {
        return getUser().getOid();
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "MidPointPrincipal", indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "User", user.asPrismObject(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Authorizations", authorizations, indent + 1);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "MidPointPrincipal(" + user + ", autz=" + authorizations + ")";
	}

}
