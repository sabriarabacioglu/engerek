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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

import java.lang.reflect.Array;
import java.util.*;


/**
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * <p/>
 * Properties may be single-valued or multi-valued
 * <p/>
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * <p/>
 * Property values are unordered, implementation may change the order of values
 * <p/>
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * <p/>
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * <p/>
 * Property is mutable.
 *
 * @author Radovan Semancik
 */
public class PrismProperty<T> extends Item<PrismPropertyValue<T>> {

    private static final long serialVersionUID = 6843901365945935660L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismProperty.class);

	private static final int MAX_SINGLELINE_LEN = 40;

    public PrismProperty(QName name) {
        super(name);
    }

    protected PrismProperty(QName name, PrismPropertyDefinition<T> definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

	/**
     * Returns applicable property definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    public PrismPropertyDefinition<T> getDefinition() {
        return (PrismPropertyDefinition<T>) definition;
    }

    /**
     * Sets applicable property definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismPropertyDefinition<T> definition) {
        this.definition = definition;
    }

    /**
     * Returns property values.
     * <p/>
     * The values are returned as set. The order of values is not significant.
     *
     * @return property values
     */
    public List<PrismPropertyValue<T>> getValues() {
        return (List<PrismPropertyValue<T>>) super.getValues();
    }
    
    public PrismPropertyValue<T> getValue() {
    	if (!isSingleValue()) {
    		throw new IllegalStateException("Attempt to get single value from property " + getElementName()
                    + " with multiple values");
    	}
        List<PrismPropertyValue<T>> values = getValues();
        if (values == null || values.isEmpty()) {
        	return null;
        }
        return values.get(0);
    }

	/**
     * Type override, also for compatibility.
     */
    public <X> List<PrismPropertyValue<X>> getValues(Class<X> type) {
        return (List) getValues();
    }

    public Collection<T> getRealValues() {
		Collection<T> realValues = new ArrayList<T>(getValues().size());
		for (PrismPropertyValue<T> pValue: getValues()) {
			realValues.add(pValue.getValue());
		}
		return realValues;
	}
    
    /**
     * Type override, also for compatibility.
     */
	public <X> Collection<X> getRealValues(Class<X> type) {
		Collection<X> realValues = new ArrayList<X>(getValues().size());
		for (PrismPropertyValue<T> pValue: getValues()) {
			realValues.add((X) pValue.getValue());
		}
		return realValues;
	}
	
	public T getAnyRealValue() {
		Collection<T> values = getRealValues();
		if (values.isEmpty()) {
			return null;
		}
		return values.iterator().next();
	}

    public PrismPropertyValue<T> getAnyValue() {
        Collection<PrismPropertyValue<T>> values = getValues();
        if (values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
    }

    public T getRealValue() {
		if (getValue() == null) {
            return null;
        }
		return getValue().getValue();
	}
	
	/**
     * Type override, also for compatibility.
     */
	public <X> X getRealValue(Class<X> type) {
        if (getValue() == null) {
            return null;
        }
		X value = (X) getValue().getValue();
		if (value == null) {
			return null;
		}
		if (type.isAssignableFrom(value.getClass())) {
			return (X)value;
		} else {
			throw new ClassCastException("Cannot cast value of property "+ getElementName()+" which is of type "+value.getClass()+" to "+type);
		}
	}

	/**
     * Type override, also for compatibility.
     */
	public <X> X[] getRealValuesArray(Class<X> type) {
		X[] valuesArrary = (X[]) Array.newInstance(type, getValues().size());
		for (int j = 0; j < getValues().size(); ++j) {
			Object avalue = getValues().get(j).getValue();
			Array.set(valuesArrary, j, avalue);
		}
		return valuesArrary;
	}

    /**
     * Type override, also for compatibility.
     */
    public <X> PrismPropertyValue<X> getValue(Class<X> type) {
    	if (getDefinition() != null) {
    		if (getDefinition().isMultiValue()) {
    			throw new IllegalStateException("Attempt to get single value from property " + elementName
                        + " with multiple values");
    		}
    	}
        if (getValues().size() > 1) {
            throw new IllegalStateException("Attempt to get single value from property " + elementName
                    + " with multiple values");
        }
        if (getValues().isEmpty()) {
            return null;
        }
        PrismPropertyValue<X> o = (PrismPropertyValue<X>) getValues().iterator().next();
        return o;
    }

    /**
     * Means as a short-hand for setting just a value for single-valued attributes.
     * Will remove all existing values.
     */
    public void setValue(PrismPropertyValue<T> value) {
    	getValues().clear();
        addValue(value);
    }

	public void setRealValue(Object realValue) {
		if (realValue == null) {
			// Just make sure there are no values
			clear();
		} else {
			setValue(new PrismPropertyValue(realValue));
		}
    }

    public void addValues(Collection<PrismPropertyValue<T>> pValuesToAdd) {
    	for (PrismPropertyValue<T> pValue: pValuesToAdd) {
    		addValue(pValue);
    	}
    }

    public void addValue(PrismPropertyValue<T> pValueToAdd) {
    	pValueToAdd.checkValue();
    	Iterator<PrismPropertyValue<T>> iterator = getValues().iterator();
    	while (iterator.hasNext()) {
    		PrismPropertyValue<T> pValue = iterator.next();
    		if (pValue.equalsRealValue(pValueToAdd)) {
    			LOGGER.warn("Adding value to property "+ getElementName()+" that already exists (overwriting), value: "+pValueToAdd);
    			iterator.remove();
    		}
    	}
    	pValueToAdd.setParent(this);
    	pValueToAdd.recompute();
    	getValues().add(pValueToAdd);
    }
    
    public void addRealValue(T valueToAdd) {
    	PrismPropertyValue<T> pval = new PrismPropertyValue<T>(valueToAdd);
    	addValue(pval);
    }
    
    public boolean deleteValues(Collection<PrismPropertyValue<T>> pValuesToDelete) {
        boolean changed = false;
    	for (PrismPropertyValue<T> pValue: pValuesToDelete) {
            if (!changed) {
    		    changed = deleteValue(pValue);
            } else {
                deleteValue(pValue);
            }
    	}
        return changed;
    }

    public boolean deleteValue(PrismPropertyValue<T> pValueToDelete) {
    	Iterator<PrismPropertyValue<T>> iterator = getValues().iterator();
    	boolean found = false;
    	while (iterator.hasNext()) {
    		PrismPropertyValue<T> pValue = iterator.next();
    		if (pValue.equalsRealValue(pValueToDelete)) {
    			iterator.remove();
    			pValue.setParent(null);
    			found = true;
    		}
    	}
    	if (!found) {
    		LOGGER.warn("Deleting value of property "+ getElementName()+" that does not exist (skipping), value: "+pValueToDelete);
    	}

        return found;
    }

    public void replaceValues(Collection<PrismPropertyValue<T>> valuesToReplace) {
    	getValues().clear();
        addValues(valuesToReplace);
    }

    public boolean hasValue(PrismPropertyValue<T> value) {
        return super.hasValue(value);
    }

    public boolean hasRealValue(PrismPropertyValue<T> value) {
        for (PrismPropertyValue<T> propVal : getValues()) {
            if (propVal.equalsRealValue(value)) {
                return true;
            }
        }

        return false;
    }
    
    @Override
	public PrismPropertyValue<T> getPreviousValue(PrismValue value) {
		return (PrismPropertyValue<T>) super.getPreviousValue(value);
	}

	@Override
	public PrismPropertyValue<T> getNextValue(PrismValue value) {
		return (PrismPropertyValue<T>) super.getNextValue(value);
	}

	public Class<T> getValueClass() {
    	if (getDefinition() != null) {
    		return getDefinition().getTypeClass();
    	}
    	if (!getValues().isEmpty()) {
    		PrismPropertyValue<T> firstPVal = getValues().get(0);
    		if (firstPVal != null) {
    			T firstVal = firstPVal.getValue();
    			if (firstVal != null) {
    				return (Class<T>) firstVal.getClass();
    			}
    		}
    	}
    	// TODO: How to determine value class?????
    	return PrismConstants.DEFAULT_VALUE_CLASS;
    }
    	
    @Override
	public PropertyDelta<T> createDelta() {
		return new PropertyDelta<T>(getPath(), getDefinition());
	}
    
    @Override
	public PropertyDelta<T> createDelta(ItemPath path) {
		return new PropertyDelta<T>(path, getDefinition());
	}
    
    @Override
	public Object find(ItemPath path) {
		if (path == null || path.isEmpty()) {
			return this;
		}
		if (!isSingleValue()) {
    		throw new IllegalStateException("Attempt to resolve sub-path '"+path+"' on multi-value property " + getElementName());
    	}
		PrismPropertyValue<T> value = getValue();
		return value.find(path);
	}

	@Override
	public <X extends PrismValue> PartiallyResolvedValue<X> findPartial(ItemPath path) {
		if (path == null || path.isEmpty()) {
			return new PartiallyResolvedValue<X>((Item<X>)this, null);
		}
		if (!isSingleValue()) {
    		throw new IllegalStateException("Attempt to resolve sub-path '"+path+"' on multi-value property " + getElementName());
    	}
		PrismPropertyValue<T> value = getValue();
		return value.findPartial(path);
	}

	public PropertyDelta<T> diff(PrismProperty<T> other) {
    	return diff(other, true, false);
    }
    
    public PropertyDelta<T> diff(PrismProperty<T> other, boolean ignoreMetadata, boolean isLiteral) {
    	List<? extends ItemDelta> deltas = new ArrayList<ItemDelta>();
    	diffInternal(other, deltas, ignoreMetadata, isLiteral);
    	if (deltas.isEmpty()) {
    		return null;
    	}
    	if (deltas.size() > 1) {
    		throw new IllegalStateException("Unexpected number of deltas from property diff: "+deltas);
    	}
    	return (PropertyDelta<T>)deltas.get(0);
    }

	@Override
	protected void checkDefinition(ItemDefinition def) {
		if (!(def instanceof PrismPropertyDefinition)) {
			throw new IllegalArgumentException("Definition "+def+" cannot be applied to property "+this);
		}
	}

	@Override
    public PrismProperty<T> clone() {
        PrismProperty<T> clone = new PrismProperty<T>(getElementName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismProperty<T> clone) {
        super.copyValues(clone);
        for (PrismPropertyValue<T> value : getValues()) {
            clone.addValue(value.clone());
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }
	
	@Override
	protected ItemDelta fixupDelta(ItemDelta delta, Item otherItem,
			boolean ignoreMetadata) {
		PrismPropertyDefinition def = getDefinition();
		if (def != null && def.isSingleValue() && !delta.isEmpty()) {
        	// Drop the current delta (it was used only to detect that something has changed
        	// Generate replace delta instead of add/delete delta
			PrismProperty<T> other = (PrismProperty<T>)otherItem;
			PropertyDelta<T> propertyDelta = (PropertyDelta<T>)delta;
			delta.clear();
    		Collection<PrismPropertyValue<T>> replaceValues = new ArrayList<PrismPropertyValue<T>>(other.getValues().size());
            for (PrismPropertyValue<T> value : other.getValues()) {
            	replaceValues.add(value.clone());
            }
            propertyDelta.setValuesToReplace(replaceValues);
			return propertyDelta;
        } else {
        	return super.fixupDelta(delta, otherItem, ignoreMetadata);
        }
	}

	@Override
    public String toString() {
        return getDebugDumpClassName() + "(" + PrettyPrinter.prettyPrint(getElementName()) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        if (DebugUtil.isDetailedDebugDump()) {
        	sb.append(getDebugDumpClassName()).append(": ");
        }
        sb.append(DebugUtil.formatElementName(getElementName())).append(": ");
        
        boolean isMultivalue = true;
        PrismPropertyDefinition def = getDefinition();
        if (def != null) {
        	isMultivalue = def.isMultiValue();
        }
        
        List<PrismPropertyValue<T>> values = getValues();
        if (values == null) {
            sb.append("null");
        } else if (values.isEmpty()) {
        	sb.append("[]");
        } else {
        	boolean multiline = false;
        	PrismPropertyValue<T> firstVal = values.iterator().next();
        	if (firstVal != null && !firstVal.isRaw() && firstVal.getValue() != null) {
        		if (DebugUtil.isDetailedDebugDump() && firstVal.getValue() instanceof DebugDumpable) {
        			multiline = true;
        		} else {
        			if (PrettyPrinter.prettyPrint(firstVal.getValue()).length() > MAX_SINGLELINE_LEN) {
        				multiline = true;
        			}
        		}
        	}
        	if (multiline) {
        		sb.append("\n");
	            Iterator<PrismPropertyValue<T>> iterator = getValues().iterator();
	            while(iterator.hasNext()) {
	            	PrismPropertyValue<T> value = iterator.next();
	            	if (value.isRaw()) {
	            		sb.append("(raw)");
	            	} else {
		            	T realValue = value.getValue();
		            	if (realValue instanceof DebugDumpable) {
		            		sb.append(((DebugDumpable)realValue).debugDump(indent + 1));
		            	} else {
		            		DebugUtil.indentDebugDump(sb, indent + 1);
			            	if (DebugUtil.isDetailedDebugDump()) {
			            		sb.append(PrettyPrinter.prettyPrint(value));
			            	} else {
			            		sb.append(PrettyPrinter.prettyPrint(value.getValue()));
			            	}
		            	}
	            	}
	                if (iterator.hasNext()) {
	                	sb.append("\n");
	                }
	            }
        	} else {
        		if (isMultivalue) {
        			sb.append("[ ");
        		}
	            Iterator<PrismPropertyValue<T>> iterator = getValues().iterator();
	            while(iterator.hasNext()) {
	            	PrismPropertyValue<T> value = iterator.next();
	            	if (value.isRaw()) {
	            		sb.append("(raw)");
	            	} else {
		            	if (DebugUtil.isDetailedDebugDump()) {
		            		sb.append(PrettyPrinter.prettyPrint(value));
		            	} else {
		            		sb.append(value.getValue());
		            	}
	            	}
	                if (iterator.hasNext()) {
	                	sb.append(", ");
	                }
	            }
	            if (isMultivalue) {
	            	sb.append(" ]");
	            }
        	}
        }
        
        if (def != null && DebugUtil.isDetailedDebugDump()) {
            sb.append(" def(");
            def.debugDumpShortToString(sb);
//            if (def.isIndexed() != null) {
//                sb.append(def.isIndexed() ? ",i+" : ",i-");
//            }
            sb.append(")");
        }
        return sb.toString();
    }
    
    public String toHumanReadableString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(PrettyPrinter.prettyPrint(getElementName())).append(" = ");
    	if (getValues() == null) {
            sb.append("null");
        } else {
            sb.append("[ ");
            Iterator<PrismPropertyValue<T>> iterator = getValues().iterator();
            while(iterator.hasNext()) {
            	PrismPropertyValue<T> value = iterator.next();
                sb.append(value.toHumanReadableString());
                if (iterator.hasNext()) {
                	sb.append(", ");
                }
            }
            sb.append(" ]");
        }
    	return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PP";
    }

}
