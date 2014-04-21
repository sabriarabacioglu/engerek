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
package com.evolveum.midpoint.prism.xnode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class MapXNode extends XNode implements Map<QName,XNode>, Serializable {
	
	// We want to maintain ordering, hence the List
	private List<Entry> subnodes = new ArrayList<Entry>();

	public int size() {
		return subnodes.size();
	}

	public boolean isEmpty() {
		return subnodes.isEmpty();
	}

	public boolean containsKey(Object key) {
		if (!(key instanceof QName)) {
			throw new IllegalArgumentException("Key must be QName, but it is "+key);
		}
		return findEntry((QName)key) != null;
	}

	public boolean containsValue(Object value) {
		if (!(value instanceof XNode)) {
			throw new IllegalArgumentException("Value must be XNode, but it is "+value);
		}
		return findEntry((XNode)value) != null;
	}

	public XNode get(Object key) {
		if (!(key instanceof QName)) {
			throw new IllegalArgumentException("Key must be QName, but it is "+key);
		}
		Entry entry = findEntry((QName)key);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}

    public XNode put(Map.Entry<QName, XNode> entry) {
        return put(entry.getKey(), entry.getValue());
    }

	public XNode put(QName key, XNode value) {
		removeEntry(key);
		subnodes.add(new Entry(key, value));
		return value;
	}

	public XNode remove(Object key) {
		if (!(key instanceof QName)) {
			throw new IllegalArgumentException("Key must be QName, but it is "+key);
		}
		return removeEntry((QName)key);
	}

	public void putAll(Map<? extends QName, ? extends XNode> m) {
		for (Map.Entry<?, ?> entry: m.entrySet()) {
			put((QName)entry.getKey(), (XNode)entry.getValue());
		}
	}

	public void clear() {
		subnodes.clear();
	}

	public Set<QName> keySet() {
		Set<QName> keySet = new HashSet<QName>();
		for (Entry entry: subnodes) {
			keySet.add(entry.getKey());
		}
		return keySet;
	}

	public Collection<XNode> values() {
		Collection<XNode> values = new ArrayList<XNode>(subnodes.size());
		for (Entry entry: subnodes) {
			values.add(entry.getValue());
		}
		return values;
	}
	
	public java.util.Map.Entry<QName, XNode> getSingleSubEntry(String errorContext) throws SchemaException {
		if (isEmpty()) {
			return null;
		}
		
		if (size() > 1) {
			throw new SchemaException("More than one element in " + errorContext +" : "+dumpKeyNames());
		}
		
		return subnodes.get(0);
	}
	
	public Entry getSingleEntryThatDoesNotMatch(QName... excludedKeys) throws SchemaException {
		Entry found = null;
		OUTER: for (Entry subentry: subnodes) {
			for (QName excludedKey: excludedKeys) {
				if (QNameUtil.match(subentry.getKey(), excludedKey)) {
					continue OUTER;
				}
			}
			if (found != null) {
				throw new SchemaException("More than one extension subnode found under "+this+": "+found.getKey()+" and "+subentry.getKey());
			} else {
				found = subentry;
			}
		}
		return found;
	}

	public Set<java.util.Map.Entry<QName, XNode>> entrySet() {
		Set<java.util.Map.Entry<QName, XNode>> entries = new Set<Map.Entry<QName,XNode>>() {

			@Override
			public int size() {
				return subnodes.size();
			}

			@Override
			public boolean isEmpty() {
				return subnodes.isEmpty();
			}

			@Override
			public boolean contains(Object o) {
				return subnodes.contains(o);
			}

			@Override
			public Iterator<java.util.Map.Entry<QName, XNode>> iterator() {
				return (Iterator)subnodes.iterator();
			}

			@Override
			public Object[] toArray() {
				return subnodes.toArray();
			}
			@Override
			public <T> T[] toArray(T[] a) {
				return subnodes.toArray(a);
			}
			@Override
			public boolean add(java.util.Map.Entry<QName, XNode> e) {
				throw new UnsupportedOperationException();
//				put(e.getKey(), e.getValue());
//				return true;
			}

			@Override
			public boolean remove(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean containsAll(Collection<?> c) {
				return subnodes.containsAll(c);
			}

			@Override
			public boolean addAll(Collection<? extends java.util.Map.Entry<QName, XNode>> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean retainAll(Collection<?> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean removeAll(Collection<?> c) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException();
			}
		};
		return entries;
	}
	
	public <T> T getParsedPrimitiveValue(QName key, QName typeName) throws SchemaException {
		XNode xnode = get(key);
		if (xnode == null) {
			return null;
		}
		if (!(xnode instanceof PrimitiveXNode<?>)) {
			throw new SchemaException("Expected that field "+key+" will be primitive, but it is "+xnode.getDesc());
		}
		PrimitiveXNode<T> xprim = (PrimitiveXNode<T>)xnode;
		return xprim.getParsedValue(typeName);
	}
	
	public void merge(MapXNode other) {
		for (java.util.Map.Entry<QName, XNode> otherEntry: other.entrySet()) {
			QName otherKey = otherEntry.getKey();
			XNode otherValue = otherEntry.getValue();
            merge (otherKey, otherValue);
        }
    }

    public void merge(QName otherKey, XNode otherValue) {
        XNode myValue = get(otherKey);
        if (myValue == null) {
            put(otherKey, otherValue);
        } else {
            ListXNode myList;
            if (myValue instanceof ListXNode) {
                myList = (ListXNode)myValue;
            } else {
                myList = new ListXNode();
                myList.add(myValue);
                put(otherKey, myList);
            }
            if (otherValue instanceof ListXNode) {
                myList.addAll((ListXNode)otherValue);
            } else {
                myList.add(otherValue);
            }
        }
    }

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		for(Entry subentry: subnodes) {
			subentry.value.accept(visitor);
		}
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof MapXNode)){
			return false;
		}
		MapXNode other = (MapXNode) o;
		return MiscUtil.unorderedCollectionEquals(this.values(), other.values());
	}

	public int hashCode() {
		int result = 0xCAFEBABE;
        for (XNode node : this.values()) {
        	if (node != null){
        		result = result ^ node.hashCode();          // using XOR instead of multiplying and adding in order to achieve commutativity
        	}
        }
		return result;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpMapMultiLine(sb, this, indent, true, dumpSuffix());
		return sb.toString();
	}

	@Override
	public String getDesc() {
		return "map";
	}
	
	@Override
	public String toString() {
		return "XNode(map:"+subnodes.size()+" entries)";
	}

	private Entry findEntry(QName qname) {
		for (Entry entry: subnodes) {
			if (QNameUtil.match(qname,entry.getKey())) {
				return entry;
			}
		}
		return null;
	}

	private Entry findEntry(XNode xnode) {
		for (Entry entry: subnodes) {
			if (entry.getValue().equals(xnode)) {
				return entry;
			}
		}
		return null;
	}

	private XNode removeEntry(QName key) {
		Iterator<Entry> iterator = subnodes.iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			if (QNameUtil.match(key,entry.getKey())) {
				iterator.remove();
				return entry.getValue();
			}
		}
		return null;
	}
	
	public String dumpKeyNames() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry> iterator = subnodes.iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			sb.append(PrettyPrinter.prettyPrint(entry.getKey()));
			if (iterator.hasNext()) {
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	private class Entry implements Map.Entry<QName, XNode>, Serializable {

		private QName key;
		private XNode value;
		
		public Entry(QName key) {
			super();
			this.key = key;
		}

		public Entry(QName key, XNode value) {
			super();
			this.key = key;
			this.value = value;
		}

		@Override
		public QName getKey() {
			return key;
		}

		@Override
		public XNode getValue() {
			return value;
		}

		@Override
		public XNode setValue(XNode value) {
			this.value = value;
			return value;
		}

		@Override
		public String toString() {
			return "E(" + key + ": " + value + ")";
		}
		
	}

}
