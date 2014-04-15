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

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.DebugUtil;

import javax.xml.namespace.QName;

public class RootXNode extends XNode {

	private QName rootElementName;
	private XNode subnode;
	
	public RootXNode() {
		super();
	}

	public RootXNode(QName rootElementName) {
		super();
		this.rootElementName = rootElementName;
	}

    public RootXNode(QName rootElementName, XNode subnode) {
        super();
        this.rootElementName = rootElementName;
        this.subnode = subnode;
    }


    public QName getRootElementName() {
		return rootElementName;
	}

	public void setRootElementName(QName rootElementName) {
		this.rootElementName = rootElementName;
	}

	public XNode getSubnode() {
		return subnode;
	}

	public void setSubnode(XNode subnode) {
		this.subnode = subnode;
	}

	@Override
	public boolean isEmpty() {
		return (subnode == null);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		if (subnode != null) {
			subnode.accept(visitor);
		}
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ROOT ").append(rootElementName);
		String dumpSuffix = dumpSuffix();
		if (dumpSuffix != null) {
			sb.append(dumpSuffix);
		}
		if (subnode == null) {
			sb.append(": null");
		} else {
			sb.append("\n");
			sb.append(subnode.debugDump(indent + 1));
		}
		return sb.toString();
	}
	
	@Override
	public String getDesc() {
		return "root";
	}
	
	@Override
	public String toString() {
		return "XNode(root:"+subnode+")";
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RootXNode rootXNode = (RootXNode) o;

        if (rootElementName != null ? !rootElementName.equals(rootXNode.rootElementName) : rootXNode.rootElementName != null)
            return false;
        if (subnode != null ? !subnode.equals(rootXNode.subnode) : rootXNode.subnode != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rootElementName != null ? rootElementName.hashCode() : 0;
        result = 31 * result + (subnode != null ? subnode.hashCode() : 0);
        return result;
    }
}
