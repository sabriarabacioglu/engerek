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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author semancik
 *
 */
public class ConstructionPack implements DebugDumpable {
	
	private Collection<PrismPropertyValue<Construction>> constructions = new ArrayList<PrismPropertyValue<Construction>>();
	private boolean forceRecon;
	private boolean hasValidAssignment = false;
	
	public boolean isForceRecon() {
		return forceRecon;
	}
	
	public void setForceRecon(boolean forceRecon) {
		this.forceRecon = forceRecon;
	}
	
	public Collection<PrismPropertyValue<Construction>> getConstructions() {
		return constructions;
	}

	public void add(PrismPropertyValue<Construction> construction) {
		constructions.add(construction);
	}

	public boolean hasValidAssignment() {
		return hasValidAssignment;
	}

	public void setHasValidAssignment(boolean hasValidAssignment) {
		this.hasValidAssignment = hasValidAssignment;
	}

	@Override
	public String toString() {
		return "ConstructionPack(" + SchemaDebugUtil.prettyPrint(constructions) + (forceRecon ? ", forceRecon" : "") + ")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "ConstructionPack", indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "forceRecon", forceRecon, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "hasValidAssignment", hasValidAssignment, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Constructions", constructions, indent + 1);
		return sb.toString();
	}
	
	

}
