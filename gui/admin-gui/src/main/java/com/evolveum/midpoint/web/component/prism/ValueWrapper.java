/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ValueWrapper<T> implements Serializable {

    private PropertyWrapper property;
    private PrismPropertyValue<T> value;
    private PrismPropertyValue<T> oldValue;
    private ValueStatus status;

    public ValueWrapper(PropertyWrapper property, PrismPropertyValue<T> value) {
        this(property, value, ValueStatus.NOT_CHANGED);
    }

    public ValueWrapper(PropertyWrapper property, PrismPropertyValue<T> value, ValueStatus status) {
        this(property, value, new PrismPropertyValue<T>(value.getValue(), value.getType(), value.getSource()), status);
    }

    public ValueWrapper(PropertyWrapper property, PrismPropertyValue<T> value, PrismPropertyValue<T> oldValue,
            ValueStatus status) {
        Validate.notNull(property, "Property wrapper must not be null.");
        Validate.notNull(value, "Property value must not be null.");
        Validate.notNull(value, "Old property value must not be null.");

        this.property = property;
        this.value = value;
        this.oldValue = oldValue;
        this.status = status;
    }

    public PropertyWrapper getProperty() {
        return property;
    }

    public ValueStatus getStatus() {
        return status;
    }

    public PrismPropertyValue<T> getValue() {
        return value;
    }

    public PrismPropertyValue<T> getOldValue() {
        return oldValue;
    }

    public void setStatus(ValueStatus status) {
        this.status = status;
    }

    public boolean hasValueChanged() {
        return oldValue != null ? !oldValue.equals(value) : value != null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("value: ");
        builder.append(value);
        builder.append(", old value: ");
        builder.append(oldValue);
        builder.append(", status: ");
        builder.append(status);

        return builder.toString();
    }
}