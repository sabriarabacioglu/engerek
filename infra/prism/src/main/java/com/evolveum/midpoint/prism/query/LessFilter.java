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

package com.evolveum.midpoint.prism.query;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class LessFilter<T> extends ComparativeFilter<T> {

	LessFilter(ItemPath parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals) {
		super(parentPath, definition, value, equals);
	}
	
	public LessFilter() {
	}
	
	public static <T, O extends Objectable> LessFilter createLess(QName parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals){
		return new LessFilter(new ItemPath(parentPath), definition, value, equals);
	}
	
	
	public static <T, O extends Objectable> LessFilter createLess(ItemPath parentPath, PrismPropertyDefinition definition, PrismPropertyValue<T> value, boolean equals){
		return new LessFilter(parentPath, definition, value, equals);
	}
	
	public static <T, O extends Objectable> LessFilter createLess(ItemPath parentPath, PrismObjectDefinition<O> containerDef,
			PrismPropertyValue<T> value, boolean equals) throws SchemaException {
		PrismPropertyDefinition def = (PrismPropertyDefinition) findItemDefinition(parentPath, containerDef);
		return createLess(parentPath, def, value, equals);
	}

	public static <T> LessFilter createLess(QName parentPath, PrismPropertyDefinition itemDefinition, T realValue, boolean equals) throws SchemaException{
		return createLess(new ItemPath(parentPath), itemDefinition, realValue, equals);
	}
	
	public static <T> LessFilter createLess(ItemPath parentPath, PrismPropertyDefinition itemDefinition, T realValue, boolean equals) throws SchemaException{
		PrismPropertyValue<T> value = createPropertyValue(itemDefinition, realValue);
		
		if (value == null){
			// create null filter
		}
		
		return createLess(parentPath, itemDefinition, value, equals);
	}

	public static <T, O extends Objectable> LessFilter createLess(ItemPath parentPath, PrismObjectDefinition<O> containerDef,
			T realValue, boolean equals) throws SchemaException {
		PrismPropertyDefinition def = (PrismPropertyDefinition) findItemDefinition(parentPath, containerDef);
		return createLess(parentPath, def, realValue, equals);
	}

	public static <T, O extends Objectable> LessFilter createLess(QName propertyName, Class<O> type, PrismContext prismContext, T realValue, boolean equals)
			throws SchemaException {
		return createLess(new ItemPath(propertyName), type, prismContext, realValue, equals);
	}
	
	public static <T, O extends Objectable> LessFilter createLess(ItemPath path, Class<O> type, PrismContext prismContext, T realValue, boolean equals)
			throws SchemaException {
	
		PrismPropertyDefinition def = (PrismPropertyDefinition) findItemDefinition(path, type, prismContext);
		
		return createLess(path, def, realValue, equals);
	}	
	@Override
	public LessFilter clone() {
		return new LessFilter(getFullPath(), getDefinition(), (PrismPropertyValue<T>) getValues().get(0), isEquals());
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("LESS:");
		return debugDump(indent, sb);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("LESS: ");
		return toString(sb);
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry) {
		throw new UnsupportedOperationException("Matching object and greater filter not supported yet");
	}
	
	@Override
	public PrismPropertyDefinition getDefinition() {
		return (PrismPropertyDefinition) super.getDefinition();
	}

	@Override
	public QName getElementName() {
		return getDefinition().getName();
	}

	@Override
	public PrismContext getPrismContext() {
		return getDefinition().getPrismContext();
	}

	@Override
	public ItemPath getPath() {
		return getFullPath();
	}

}
