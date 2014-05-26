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

package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;

public class OrFilter extends NaryLogicalFilter {

	public OrFilter(List<ObjectFilter> condition) {
		super(condition);
	}

	
	public static OrFilter createOr(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
		for (ObjectFilter condition : conditions){
			filters.add(condition);
		}
		
		return new OrFilter(filters);
	}
	
	public static OrFilter createOr(List<ObjectFilter> conditions){	
		return new OrFilter(conditions);
	}
	
	@Override
	public OrFilter clone() {
		return new OrFilter(getClonedConditions());
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("OR:");
		for (ObjectFilter filter : getConditions()){
			sb.append("\n");
			sb.append(filter.debugDump(indent + 1));
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("OR");
		sb.append("(");
		for (int i = 0; i < getConditions().size(); i++){
			sb.append(getConditions().get(i));
			if (i != getConditions().size() -1){
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}


	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		for (ObjectFilter filter : getConditions()){
			if (filter.match(object, matchingRuleRegistry)){
				return true;
			}
		}
		return false;
	}
}
