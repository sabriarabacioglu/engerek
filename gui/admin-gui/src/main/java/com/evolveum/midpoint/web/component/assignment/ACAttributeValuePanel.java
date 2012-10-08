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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.PasswordPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.ThreeStateCheckPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class ACAttributeValuePanel extends BasePanel<ACValueConstructionDto> {

    private static final String ID_INPUT = "input";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";

    public ACAttributeValuePanel(String id, IModel<ACValueConstructionDto> iModel) {
        super(id, iModel);

        initPanel();
    }

    private void initPanel() {
        ACValueConstructionDto dto = getModel().getObject();
        PrismPropertyDefinition definition = dto.getAttribute().getDefinition();

        InputPanel input = createTypedInputComponent(ID_INPUT, definition);
        add(input);

        AjaxLink addLink = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add(addLink);
        addLink.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddVisible();
            }
        });

        AjaxLink removeLink = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target);
            }
        };
        add(removeLink);
        removeLink.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveVisible();
            }
        });
    }

    private InputPanel createTypedInputComponent(String id, PrismPropertyDefinition definition) {
        QName valueType = definition.getTypeName();

        final String baseExpression = ACValueConstructionDto.F_VALUE;

        InputPanel panel;
        if (DOMUtil.XSD_DATETIME.equals(valueType)) {
            panel = new DatePanel(id, new PropertyModel<XMLGregorianCalendar>(getModel(), baseExpression));
        } else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
            panel = new PasswordPanel(id, new PropertyModel<String>(getModel(), baseExpression + ".clearValue"));
        } else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
            panel = new ThreeStateCheckPanel(id, new PropertyModel<Boolean>(getModel(), baseExpression));
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueType)) {
            panel = new TextPanel<String>(id, new PropertyModel<String>(getModel(), baseExpression + ".orig"), String.class);
        } else {
            Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
            if (type != null && type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }
            panel = new TextPanel<String>(id, new PropertyModel<String>(getModel(), baseExpression),
                    type);

            if (SchemaConstantsGenerated.C_NAME.equals(definition.getName())) {
                panel.getBaseFormComponent().setRequired(true);
            }
        }

        return panel;
    }

    private boolean isAddVisible() {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        PrismPropertyDefinition def = attributeDto.getDefinition();

        List<ACValueConstructionDto> values = attributeDto.getValues();
        if (def.getMaxOccurs() != -1 && values.size() >= def.getMaxOccurs()) {
            return false;
        }

        //we want to show add on last item only
        if (values.indexOf(dto) + 1 != values.size()) {
            return false;
        }

        return true;
    }

    private boolean isRemoveVisible() {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        PrismPropertyDefinition def = attributeDto.getDefinition();

        List<ACValueConstructionDto> values = attributeDto.getValues();
        if (values.size() <= 1) {
            return false;
        }

        if (values.size() <= def.getMinOccurs()) {
            return false;
        }

        return true;
    }

    private void addPerformed(AjaxRequestTarget target) {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        attributeDto.getValues().add(new ACValueConstructionDto(attributeDto, null));

        target.add(findParent(ACAttributePanel.class).getParent());

        //todo implement add to account construction
    }

    private void removePerformed(AjaxRequestTarget target) {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        attributeDto.getValues().remove(dto);
        //todo implement remove from acctount construction

        target.add(findParent(ACAttributePanel.class).getParent());
    }
}