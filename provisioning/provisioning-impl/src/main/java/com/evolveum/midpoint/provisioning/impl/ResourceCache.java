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
package com.evolveum.midpoint.provisioning.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

/**
 * Class for caching ResourceType instances with a parsed schemas.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ResourceCache {

	private Map<String,PrismObject<ResourceType>> cache;
    @Autowired(required = true)
	private PrismContext prismContext;

    ResourceCache() {
        cache = new HashMap<String, PrismObject<ResourceType>>();
    }
	
	public synchronized void put(PrismObject<ResourceType> resource) throws SchemaException {
		String oid = resource.getOid();
		if (oid == null) {
			throw new SchemaException("Attempt to cache "+resource+" without an OID");
		}
		
		String version = resource.getVersion();
		if (version == null) {
			throw new SchemaException("Attempt to cache "+resource+" without version");
		}
		
		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			cache.put(oid, resource.clone());
		} else {
			if (compareVersion(resource.getVersion(), cachedResource.getVersion())) {
				// We already have equivalent resource, nothing to do
				return;
			} else {
				cache.put(oid, resource.clone());
			}
		}
	}
	
	private boolean compareVersion(String version1, String version2) {
		if (version1 == null && version2 == null) {
			return true;
		}
		if (version1 == null || version2 == null) {
			return false;
		}
		return version1.equals(version2);
	}

	public synchronized PrismObject<ResourceType> get(PrismObject<ResourceType> resource) throws SchemaException {
		return get(resource.getOid(), resource.getVersion());
	}
	
	public synchronized PrismObject<ResourceType> get(String oid, String version) throws SchemaException {
		if (oid == null) {
			return null;
		}
		
		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			return null;
		}

		if (!compareVersion(version, cachedResource.getVersion())) {
			return null;
		}
		
		return cachedResource.clone();
	}
	
	/**
	 * Returns currently cached version. FOR DIAGNOSTICS ONLY. 
	 */
	public String getVersion(String oid) {
		if (oid == null) {
			return null;
		}
		PrismObject<ResourceType> cachedResource = cache.get(oid);
		if (cachedResource == null) {
			return null;
		}
		return cachedResource.getVersion();
	}

	public void remove(String oid) {
		cache.remove(oid);
	}

}
