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

package com.evolveum.prism.xml.ns._public.types_3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import org.w3c.dom.Element;


/**
 * 
 *                 Defines a type for XPath-like item pointer. It points to a specific part
 *                 of the prism object.
 *             
 * 
 * <p>Java class for ItemPathType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ItemPathType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */

// TODO it is questionable whether to treat ItemPathType as XmlType any more (similar to RawType)
// however, unlike RawType, ItemPathType is still present in externally-visible schemas (XSD, WSDL)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemPathType")
public class ItemPathType implements Serializable, Equals, Cloneable {
	
	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ItemPathType");
	
	@XmlTransient
	private ItemPath itemPath;

    @Deprecated         // use one of the content-filling constructors instead
    public ItemPathType() {
    }
    
    public ItemPathType(ItemPath itemPath) {
		this.itemPath = itemPath;
	}

    public ItemPathType(String itemPath) {
        XPathHolder holder = new XPathHolder(itemPath);
        this.itemPath = holder.toItemPath();
    }

	public ItemPath getItemPath() {
        if (itemPath == null) {
            itemPath = ItemPath.EMPTY_PATH;
        }
		return itemPath;
	}
	
	public void setItemPath(ItemPath itemPath){
		this.itemPath = itemPath;
	}

    public ItemPathType clone() {
    	ItemPathType clone = new ItemPathType();
        if (itemPath != null) {
    	    clone.setItemPath(itemPath.clone());
        }
    	return clone;
    }
    
    @Override
    public boolean equals(Object obj) {
    	final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
    	return equals(null, null, obj, strategy);
    }

	@Override
	public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that,
			EqualsStrategy equalsStrategy) {
		
		if (!(that instanceof ItemPathType)){
    		return false;
    	}
    	
    	ItemPathType other = (ItemPathType) that;
    	
    	ItemPath thisPath = getItemPath();
    	ItemPath otherPath = other.getItemPath();

        return thisPath.equivalent(otherPath);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemPath == null) ? 0 : itemPath.hashCode());
		return result;
	}

    @Override
    public String toString() {
        return "ItemPathType{" +
                "itemPath=" + getItemPath() +
                '}';
    }
}
