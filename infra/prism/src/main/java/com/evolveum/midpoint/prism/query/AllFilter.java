/*
 * Copyright (c) 2014 Evolveum
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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Filter designed to explicitly match everything. It is used in some special cases, e.g.
 * a security component explicitly indicating that all objects should be returned. 
 * 
 * @author Radovan Semancik
 */
public class AllFilter extends ObjectFilter {

	public AllFilter() {
		super();
	}

	public static AllFilter createAll() {
		return new AllFilter();
	}
	
	@Override
	public AllFilter clone() {
		return new AllFilter();
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ALL");
		return sb.toString();

	}
	
	@Override
	public String toString() {
		return "ALL";
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		return true;
		
	}
}
