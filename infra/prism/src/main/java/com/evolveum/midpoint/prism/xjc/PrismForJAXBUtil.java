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

package com.evolveum.midpoint.prism.xjc;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author lazyman
 */
public final class PrismForJAXBUtil {

    private PrismForJAXBUtil() {
    }

    public static <T> List<T> getPropertyValues(PrismContainer container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property;
		try {
			property = container.findOrCreateProperty(name);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
        return new PropertyArrayList<T>(property);
    }

    public static <T> T getPropertyValue(PrismContainerValue container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    public static <T> T getPropertyValue(PrismContainer container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    private static <T> T getPropertyValue(PrismProperty<?> property, Class<T> requestedType) {
        if (property == null) {
            return null;
        }

        PrismPropertyValue<?> pvalue = property.getValue();
        if (pvalue == null) {
            return null;
        }
        
        Object propertyRealValue = pvalue.getValue();
        
        if (propertyRealValue instanceof Element) {
        	if (requestedType.isAssignableFrom(Element.class)) {
        		return (T) propertyRealValue;
        	}
        	Field anyField = getAnyField(requestedType);
        	if (anyField == null) {
        		throw new IllegalArgumentException("Attempt to read raw property "+property+" while the requested class ("+requestedType+") does not have 'any' field");
        	}
        	anyField.setAccessible(true);
        	Collection<?> anyElementList = property.getRealValues();
        	T requestedTypeInstance;
			try {
				requestedTypeInstance = requestedType.newInstance();
				anyField.set(requestedTypeInstance, anyElementList);
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("Instantiate error while reading raw property "+property+", requested class ("+requestedType+"):"
						+e.getMessage(), e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("Illegal access error while reading raw property "+property+", requested class ("+requestedType+")"
						+", field "+anyField+": "+e.getMessage(), e);
			}
			return requestedTypeInstance;
        }
        
        return JaxbTypeConverter.mapPropertyRealValueToJaxb(propertyRealValue);
    }
    
    private static <T> Field getAnyField(Class<T> clazz) {
    	for (Field field: clazz.getDeclaredFields()) {
    		XmlAnyElement xmlAnyElementAnnotation = field.getAnnotation(XmlAnyElement.class);
    		if (xmlAnyElementAnnotation != null) {
    			return field;
    		}
    	}
    	return null;
    }
    
    public static <T> List<T> getPropertyValues(PrismContainerValue container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property;
		try {
			property = container.findOrCreateProperty(name);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
        return new PropertyArrayList<T>(property);
    }

    
    public static <T> void setPropertyValue(PrismContainerValue<?> container, QName name, T value) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        if (value == null) {
        	container.removeProperty(name);
        } else {
	        PrismProperty<?> property;
			try {
				property = container.findOrCreateProperty(name);
			} catch (SchemaException e) {
				// This should not happen. Code generator and compiler should take care of that.
				throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
			}
	    	Object propertyRealValue = JaxbTypeConverter.mapJaxbToPropertyRealValue(value);
	    	if (propertyRealValue == null) {
	    		container.removeProperty(name);
	    	} else {
	    		property.setValue(new PrismPropertyValue(propertyRealValue));
	    	}
        }
    }

    public static <T> void setPropertyValue(PrismContainer container, QName name, T value) {
    	setPropertyValue(container.getValue(), name, value);
    }

    public static <T extends Containerable> PrismContainerValue<T> getFieldContainerValue(PrismContainer<?> parent, QName fieldName) {
        Validate.notNull(parent, "Container must not be null.");
        Validate.notNull(fieldName, "Field QName must not be null.");

        return getFieldContainerValue(parent.getValue(), fieldName);
    }

    public static <T extends Containerable> PrismContainerValue<T> getFieldContainerValue(PrismContainerValue<?> parent, QName fieldName) {
        Validate.notNull(parent, "Container value must not be null.");
        Validate.notNull(fieldName, "Field QName must not be null.");

        PrismContainer<T> container = parent.findItem(fieldName, PrismContainer.class);
        return container != null ? container.getValue() : null;
    }

    public static <T extends Containerable> T getFieldSingleContainerable(PrismContainerValue<?> parent, QName fieldName, Class<T> fieldClass) {
    	PrismContainerValue<T> fieldContainerValue = getFieldContainerValue(parent, fieldName);
    	if (fieldContainerValue == null) {
    		return null;
    	}
    	return fieldContainerValue.asContainerable(fieldClass);
    }

    public static <T extends PrismContainer<?>> T getContainer(PrismContainerValue parentValue, QName name) {
        Validate.notNull(parentValue, "Parent container value must not be null.");
        Validate.notNull(name, "QName must not be null.");

        try {
            return (T) parentValue.findOrCreateContainer(name);
        } catch (SchemaException ex) {
            throw new SystemException(ex.getMessage(),  ex);
        }
    }

    public static <T extends PrismContainer<?>> T getContainer(PrismContainer<?> parent, QName name) {
        Validate.notNull(parent, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        try {
            return (T) parent.findOrCreateContainer(name);
        } catch (SchemaException ex) {
            throw new SystemException(ex.getMessage(),  ex);
        }
    }

    public static <T extends Containerable> boolean setFieldContainerValue(PrismContainerValue<?> parent, QName fieldName, 
    		PrismContainerValue<T> fieldContainerValue) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(fieldName, "QName must not be null.");

        try {
	        PrismContainer<T> fieldContainer = null;
	        if (fieldContainerValue == null) {
	        	parent.removeContainer(fieldName);
	        } else {
	        	if (fieldContainerValue.getParent() != null && fieldContainerValue.getParent() != parent) {
	        		// This value is already part of another prism. We need to clone it to add it here.
	        		fieldContainerValue = fieldContainerValue.clone();
	        	}
	            fieldContainer = new PrismContainer<T>(fieldName);
	            fieldContainer.add(fieldContainerValue);
	            if (parent.getParent() == null) {
	                parent.add(fieldContainer);
	            } else {
                    parent.addReplaceExisting(fieldContainer);
	            }
	        }
//	        // Make sure that the definition from parent is applied to new field container
//	        if (fieldContainer.getDefinition() == null) {
//	        	PrismContainer<?> parentContainer = parent.getContainer();
//	        	if (parentContainer != null) {
//		        	PrismContainerDefinition<?> parentDefinition = parentContainer.getDefinition();
//		        	if (parentDefinition != null) {
//		        		PrismContainerDefinition<T> fieldDefinition = parentDefinition.findContainerDefinition(fieldName);
//		        		fieldContainer.setDefinition(fieldDefinition);
//		        	}
//	        	}
//	        }
        } catch (SchemaException e) {
        	// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
        }
        return true;
    }

    public static boolean setFieldContainerValue(PrismContainer<?> parent, QName fieldName, PrismContainerValue<?> fieldContainerValue) {
        return setFieldContainerValue(parent.getValue(), fieldName, fieldContainerValue);
    }

    public static PrismReferenceValue getReferenceValue(PrismContainerValue<?> parent, QName name) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismReference reference = parent.findItem(name, PrismReference.class);
        return reference != null ? reference.getValue() : null;
    }

    public static PrismReferenceValue getReferenceValue(PrismContainer parent, QName name) {
        Validate.notNull(parent, "Prism container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismReference reference = getReference(parent, name);
        return reference != null ? reference.getValue() : null;
    }

    public static PrismReference getReference(PrismContainer parent, QName name) {
        Validate.notNull(parent, "Prism container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        return parent.findReference(name);
    }

    /**
     * This method must merge new value with potential existing value of the reference.
     * E.g. it is possible to call setResource(..) and then setResourceRef(..) with the
     * same OID. In that case the result should be one reference that has both OID/type/filter
     * and object.
     * Assumes single-value reference
     */
    public static void setReferenceValueAsRef(PrismContainerValue<?> parentValue, QName referenceName,
            PrismReferenceValue value) {
        Validate.notNull(parentValue, "Prism container value must not be null.");
        Validate.notNull(referenceName, "QName must not be null.");

        PrismReference reference;
		try {
			reference = parentValue.findOrCreateItem(referenceName, PrismReference.class);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		if (reference == null) {
        	throw new IllegalArgumentException("No reference "+referenceName+" in "+parentValue);
        }
        if (value == null) {
            parentValue.remove(reference);
        } else {
            if (reference.isEmpty()) {
                if (value.getParent() != null) {
                    value = value.clone();
                }
                reference.add(value);
            } else {
                reference.getValue().setOid(value.getOid());
                reference.getValue().setTargetType(value.getTargetType());
                reference.getValue().setFilter(value.getFilter());
                reference.getValue().setDescription(value.getDescription());
            }
        }
    }

    public static void setReferenceValueAsRef(PrismContainer parent, QName name, PrismReferenceValue value) {
        setReferenceValueAsRef(parent.getValue(), name, value);
    }

    /**
     * This method must merge new value with potential existing value of the reference.
     * E.g. it is possible to call setResource(..) and then setResourceRef(..) with the
     * same OID. In that case the result should be one reference that has both OID/type/filter
     * and object.
     * Assumes single-value reference
     */
    public static void setReferenceValueAsObject(PrismContainerValue parentValue, QName referenceQName, PrismObject targetObject) {
        Validate.notNull(parentValue, "Prism container value must not be null.");
        Validate.notNull(referenceQName, "QName must not be null.");

        PrismReference reference;
		try {
			reference = parentValue.findOrCreateReference(referenceQName);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
        if (reference == null) {
        	throw new IllegalArgumentException("No reference "+referenceQName+" in "+parentValue);
        }
        PrismReferenceValue referenceValue = reference.getValue();
        referenceValue.setObject(targetObject);
    }

    // Assumes single-value reference
    public static void setReferenceValueAsObject(PrismContainer parent, QName referenceQName, PrismObject targetObject) {
    	setReferenceValueAsObject(parent.getValue(), referenceQName, targetObject);
    }

    public static <T extends Objectable> PrismReferenceValue objectableAsReferenceValue(T objectable, PrismReference reference ) {
    	PrismObject<T> object = objectable.asPrismObject();
        for (PrismReferenceValue refValue: reference.getValues()) {
            if (object == refValue.getObject()) {
                return refValue;
            }
        }
        PrismReferenceValue referenceValue = new PrismReferenceValue();
        referenceValue.setObject(object);
        return referenceValue;
    }

    public static <T extends Containerable> List<PrismContainerValue<T>> getContainerValues(PrismContainerValue<T> parent, QName name, Class<T> clazz) {
        return getContainerValues(parent.getContainer(), name, clazz);
    }

    public static <T extends Containerable> List<PrismContainerValue<T>> getContainerValues(PrismContainer<T> parent, QName name, Class<T> clazz) {
        Validate.notNull(parent, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismContainer container;
		try {
			container = parent.findOrCreateContainer(name);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
        return container.getValues();
    }

    public static <T> List<T> getAny(PrismContainerValue value, Class<T> clazz) {
    	return new AnyArrayList(value);
    }

	public static PrismObject setupContainerValue(PrismObject prismObject, PrismContainerValue containerValue) {
		PrismContainerable parent = containerValue.getParent();
		if (parent != null && parent instanceof PrismObject) {
			return (PrismObject)parent;
		}
		try {
			prismObject.setValue(containerValue);
			return prismObject;
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}

	public static PrismReference getReference(PrismContainerValue parent, QName fieldName) {
		try {
			return parent.findOrCreateReference(fieldName);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}
	
	public static void setReferenceFilterElement(PrismReferenceValue rval, Element filterElement) {
		DomParser parser = getDomParser(rval);
		try {
			if (filterElement == null || DOMUtil.isEmpty(filterElement)){
				return;
			}
			MapXNode filterXNode = parser.parseElementAsMap(filterElement);
			SearchFilterType filter = new SearchFilterType();
            filter.parseFromXNode(filterXNode);
			rval.setFilter(filter);
		} catch (SchemaException e) {
			throw new SystemException("Error parsing filter: "+e.getMessage(),e);
		}
	}

    public static void setReferenceFilterClauseXNode(PrismReferenceValue rval, SearchFilterType filterType) {
        if (filterType == null) {
            return;
        }
        MapXNode filterClause = null;
        try {
            filterClause = filterType.getFilterClauseXNode(rval.getPrismContext());
        } catch (SchemaException e) {
            throw new SystemException("Error serializing filter: "+e.getMessage(),e);
        }
        if (filterClause == null || filterClause.isEmpty()) {
            return;
        }
        SearchFilterType filter = new SearchFilterType();
        filter.setFilterClauseXNode((MapXNode) filterClause.clone());
        rval.setFilter(filter);
    }

    public static Element getReferenceFilterElement(PrismReferenceValue rval) {
		SearchFilterType filter = rval.getFilter();
		if (filter == null) {
			return null;
		}
		Element filterElement;
		PrismContext prismContext = rval.getPrismContext();
		// We have to work even if prismContext is null. This is needed for
		// equals and hashcode and similar methods.
		DomParser parser = getDomParser(rval);
		try {
			MapXNode filterXmap = filter.serializeToXNode(prismContext);
			if (filterXmap.size() != 1) {
				// This is supposed to be a map with just a single entry. This is an internal error
				throw new IllegalArgumentException("Unexpected map in filter processing, it has "+filterXmap.size()+" entries");
			}
			Entry<QName, XNode> entry = filterXmap.entrySet().iterator().next();
			filterElement = parser.serializeXMapToElement((MapXNode) entry.getValue(), entry.getKey());
		} catch (SchemaException e) {
			throw new SystemException("Error serializing filter: "+e.getMessage(),e);
		}
		return filterElement;
	}

    public static MapXNode getReferenceFilterClauseXNode(PrismReferenceValue rval) {
        SearchFilterType filter = rval.getFilter();
        PrismContext prismContext = rval.getPrismContext();
        // We have to work even if prismContext is null. This is needed for
        // equals and hashcode and similar methods.

        if (filter == null || !filter.containsFilterClause()) {
            return null;
        }
        try {
            return (MapXNode) filter.getFilterClauseXNode(prismContext).clone();
        } catch (SchemaException e) {
            throw new SystemException("Error serializing filter: "+e.getMessage(),e);
        }
    }

    private static DomParser getDomParser(PrismValue pval) {
		PrismContext prismContext = pval.getPrismContext();
		if (prismContext != null) {
			return prismContext.getParserDom();
		} else {
			DomParser parser = new DomParser(null);
			return parser;
		}
	}

    public static <T extends ObjectType> T createTargetInstance(PrismReferenceValue value) {
        try {
            return (T) value.getTargetTypeCompileTimeClass().newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            throw new SystemException("Cannot instantiate item: " + e.getMessage(), e);
        }
    }
}
