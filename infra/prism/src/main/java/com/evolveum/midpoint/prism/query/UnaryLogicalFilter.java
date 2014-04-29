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

import java.util.ArrayList;
import java.util.List;

public abstract class UnaryLogicalFilter extends LogicalFilter {

	public UnaryLogicalFilter() {
		super();
	}

	public UnaryLogicalFilter(ObjectFilter condition) {
		super();
		setFilter(condition);
	}

	public ObjectFilter getFilter() {
		if (conditions == null) {
			return null;
		}
		if (conditions.isEmpty()) {
			return null;
		}
		if (conditions.size() == 1) {
			return conditions.get(0);
		}
		throw new IllegalStateException("Unary logical filter can contains only one value, but contains "
				+ conditions.size());
	}
	
	public void setFilter(ObjectFilter filter){
		conditions = new ArrayList<ObjectFilter>();
		conditions.add(filter);
	}

}
