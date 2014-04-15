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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAttributeDefinitionType;

import com.evolveum.prism.xml.ns._public.types_2.RawType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class ACAttributeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_VALUES = "values";

    private PrismPropertyDefinition definition;
    private ResourceAttributeDefinitionType construction;
    private List<ACValueConstructionDto> values;

    private ACAttributeDto(PrismPropertyDefinition definition, ResourceAttributeDefinitionType construction) {
        Validate.notNull(definition, "Prism property definition must not be null.");
        Validate.notNull(construction, "Value construction must not be null.");

        this.definition = definition;
        this.construction = construction;
    }

    public static ACAttributeDto createACAttributeDto(PrismPropertyDefinition definition, ResourceAttributeDefinitionType construction,
                                                      PrismContext context) throws SchemaException {
        ACAttributeDto dto = new ACAttributeDto(definition, construction);
        dto.values = dto.createValues(context);

        return dto;
    }

    public List<ACValueConstructionDto> getValues() {
        if (values.isEmpty()) {
            values.add(new ACValueConstructionDto(this, null));
        }
        return values;
    }

    private List<ACValueConstructionDto> createValues(PrismContext context) throws SchemaException {
        List<ACValueConstructionDto> values = new ArrayList<ACValueConstructionDto>();
        MappingType outbound = construction.getOutbound();
        if (outbound == null || outbound.getExpression() == null) {
            return values;
        }
        ExpressionType expression = outbound.getExpression();
        if (expression.getExpressionEvaluator() != null) {
            List<JAXBElement<?>> elements = expression.getExpressionEvaluator();
            List<PrismValue> valueList = getExpressionValues(elements, context);
            for (PrismValue value : valueList) {
                if (!(value instanceof PrismPropertyValue)) {
                    //todo ignoring other values for now
                    continue;
                }
                values.add(new ACValueConstructionDto(this, ((PrismPropertyValue) value).getValue()));
            }
        }
        return values;
    }

    private List<PrismValue> getExpressionValues(List<JAXBElement<?>> elements, PrismContext context) throws SchemaException {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        JAXBElement<?> fistElement = elements.iterator().next();
        Element firstDomElement = (Element) fistElement.getValue();
        if (!DOMUtil.isElementName(firstDomElement, SchemaConstants.C_VALUE)) {
            return null;
        }

        Item item = StaticExpressionUtil.parseValueElements(elements, definition, "gui", context);
        return item.getValues();
    }

    public PrismPropertyDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        String name = definition.getDisplayName();
        return StringUtils.isNotEmpty(name) ? name : definition.getName().getLocalPart();
    }

    public boolean isEmpty() {
        List<ACValueConstructionDto> values = getValues();
        if (values.isEmpty()) {
            return true;
        }

        for (ACValueConstructionDto dto : values) {
            if (dto.getValue() != null) {
                return false;
            }
        }

        return true;
    }

    public ResourceAttributeDefinitionType getConstruction() throws SchemaException {
        if (isEmpty()) {
            return null;
        }

        ResourceAttributeDefinitionType attrConstruction = new ResourceAttributeDefinitionType();
        attrConstruction.setRef(definition.getName());
        MappingType outbound = new MappingType();
        attrConstruction.setOutbound(outbound);

        ExpressionType expression = new ExpressionType();
        outbound.setExpression(expression);

        List<ACValueConstructionDto> values = getValues();
        PrismProperty property = definition.instantiate();
        for (ACValueConstructionDto dto : values) {
            if (dto.getValue() == null) {
                continue;
            }

            property.add(new PrismPropertyValue(dto.getValue()));
        }

        List evaluators = expression.getExpressionEvaluator();
        List<JAXBElement<RawType>> collection = StaticExpressionUtil.serializeValueElements(property, null);
        ObjectFactory of = new ObjectFactory();
        for (JAXBElement<RawType> evaluator : collection) {
            evaluators.add(evaluator);
        }

        if (evaluators.isEmpty()) {
            return null;
        }

        return attrConstruction;
    }
}
