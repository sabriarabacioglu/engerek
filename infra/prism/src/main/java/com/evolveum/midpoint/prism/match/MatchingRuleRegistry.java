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
package com.evolveum.midpoint.prism.match;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class MatchingRuleRegistry {
	
	public MatchingRule <? extends Object> defaultMatchingRule;
	public Map<QName, ? extends MatchingRule<?>> matchingRules = new HashMap<QName, MatchingRule<?>>();
	
	public MatchingRuleRegistry() {
		super();
		this.defaultMatchingRule = new DefaultMatchingRule<Object>();
	}
	
	public MatchingRuleRegistry(Collection<? extends MatchingRule<?>> matchingRules) {
		this();
		for (MatchingRule<?> matchingRule: matchingRules) {
			registerMatchingRule(matchingRule);
		}
	}

	public <T> MatchingRule<T> getMatchingRule(QName ruleName, QName typeQName) throws SchemaException {
		if (ruleName == null) {
			return (MatchingRule<T>) defaultMatchingRule;
		}
		MatchingRule<T> matchingRule = (MatchingRule<T>) matchingRules.get(ruleName);
		if (matchingRule == null) {
			//try match according to the localPart
			if (QNameUtil.matchAny(ruleName, matchingRules.keySet())){
				ruleName = QNameUtil.resolveNs(ruleName, matchingRules.keySet());
				matchingRule = (MatchingRule<T>) matchingRules.get(ruleName); 
			}
			if (matchingRule == null) {
				throw new SchemaException("Unknown matching rule for name " + ruleName);
			}
		}
		if (!matchingRule.isSupported(typeQName)) {
			throw new SchemaException("Matching rule "+ruleName+" does not support type "+typeQName);
		}
		return matchingRule;
	}
	
	public <T> void registerMatchingRule(MatchingRule<T> rule) {
		((Map)this.matchingRules).put(rule.getName(), rule);
	}

}
