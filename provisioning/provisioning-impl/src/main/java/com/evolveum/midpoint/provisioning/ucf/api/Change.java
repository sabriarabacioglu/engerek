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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;
import java.util.Set;

/**
 * @author Radovan Semancik
 *
 */
public final class Change<T extends ShadowType> {
	
    private Collection<ResourceAttribute<?>> identifiers;
    private ObjectClassComplexTypeDefinition objectClassDefinition;
    private ObjectDelta<T> objectDelta;
    private PrismProperty<?> token;
    // TODO: maybe call this repoShadow?
    private PrismObject<T> oldShadow;
    private PrismObject<T> currentShadow;

    public Change(Collection<ResourceAttribute<?>> identifiers, ObjectDelta<T> change, PrismProperty<?> token) {
        this.identifiers = identifiers;
        this.objectDelta = change;
        this.currentShadow = null;
        this.token = token;
    }

    public Change(Collection<ResourceAttribute<?>> identifiers, PrismObject<T> currentShadow, PrismProperty<?> token) {
        this.identifiers = identifiers;
        this.objectDelta = null;
        this.currentShadow = currentShadow;
        this.token = token;
    }
    
    public Change(Collection<ResourceAttribute<?>> identifiers, PrismObject<T> currentShadow, PrismObject<T> oldStadow, ObjectDelta<T> objectDetla){
    	this.identifiers = identifiers;
    	this.currentShadow = currentShadow;
    	this.oldShadow = oldStadow;
    	this.objectDelta = objectDetla;
    }

    public Change(ObjectDelta<T> change, PrismProperty<?> token) {
        this.objectDelta = change;
        this.token = token;
    }

    public ObjectDelta<? extends ShadowType> getObjectDelta() {
        return objectDelta;
    }

    public void setObjectDelta(ObjectDelta<T> change) {
        this.objectDelta = change;
    }

    public Collection<ResourceAttribute<?>> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Collection<ResourceAttribute<?>> identifiers) {
        this.identifiers = identifiers;
    }
    
	public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
		return objectClassDefinition;
	}

	public void setObjectClassDefinition(ObjectClassComplexTypeDefinition objectClassDefinition) {
		this.objectClassDefinition = objectClassDefinition;
	}

	public PrismProperty<?> getToken() {
		return token;
	}

	public void setToken(PrismProperty<?> token) {
		this.token = token;
	}

	public PrismObject<T> getOldShadow() {
		return oldShadow;
	}

	public void setOldShadow(PrismObject<T> oldShadow) {
		this.oldShadow = oldShadow;
	}

	public PrismObject<T> getCurrentShadow() {
		return currentShadow;
	}

	public void setCurrentShadow(PrismObject<T> currentShadow) {
		this.currentShadow = currentShadow;
	}

	@Override
	public String toString() {
		return "Change(identifiers=" + identifiers + ", objectDelta=" + objectDelta + ", token=" + token
				+ ", oldShadow=" + oldShadow + ", currentShadow=" + currentShadow + ")";
	}
	
}