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

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

/**
 * @author lazyman
 */
public enum ObjectTypeGuiDescriptor {

    @Deprecated
    ACCOUNT(ObjectTypes.ACCOUNT, "ObjectTypeGuiDescriptor.account", "silk-status_online"),

    CONNECTOR(ObjectTypes.CONNECTOR, "ObjectTypeGuiDescriptor.connector", "silk-link"),

    CONNECTOR_HOST(ObjectTypes.CONNECTOR_HOST, "ObjectTypeGuiDescriptor.connectorHost", "silk-driver_link"),

    GENERIC_OBJECT(ObjectTypes.GENERIC_OBJECT, "ObjectTypeGuiDescriptor.genericObject", "silk-page_white_code"),

    RESOURCE(ObjectTypes.RESOURCE, "ObjectTypeGuiDescriptor.resource", "silk-server"),

    USER(ObjectTypes.USER, "ObjectTypeGuiDescriptor.user", "silk-user"),

    OBJECT_TEMPLATE(ObjectTypes.OBJECT_TEMPLATE, "ObjectTypeGuiDescriptor.objectTemplate", "silk-layout"),

    SYSTEM_CONFIGURATION(ObjectTypes.SYSTEM_CONFIGURATION, "ObjectTypeGuiDescriptor.systemConfiguration", "silk-page_white_gear"),

    TASK(ObjectTypes.TASK, "ObjectTypeGuiDescriptor.task", "silk-script"),

    SHADOW(ObjectTypes.SHADOW, "ObjectTypeGuiDescriptor.shadow", "silk-status_online"),

    OBJECT(ObjectTypes.OBJECT, "ObjectTypeGuiDescriptor.object", "silk-page_white"),

    ROLE(ObjectTypes.ROLE, "ObjectTypeGuiDescriptor.role", "silk-medal_gold_3"),

    VALUE_POLICY(ObjectTypes.PASSWORD_POLICY, "ObjectTypeGuiDescriptor.valuePolicy", "silk-lock"),

    NODE(ObjectTypes.NODE, "ObjectTypeGuiDescriptor.node", "silk-computer"),

    ORG(ObjectTypes.ORG, "ObjectTypeGuiDescriptor.org", "silk-building"),

    ABSTRACT_ROLE(ObjectTypes.ABSTRACT_ROLE, "ObjectTypeGuiDescriptor.abstractRole", "silk-award_star_gold_3"),

    FOCUS(ObjectTypes.FOCUS_TYPE, "ObjectTypeGuiDescriptor.focus", ""),

    REPORT(ObjectTypes.REPORT, "ObjectTypeGuiDescriptor.report", ""),

    REPORT_OUTPUT(ObjectTypes.REPORT_OUTPUT, "ObjectTypeGuiDescriptor.reportOutput", ""),

    SECURITY_POLICY(ObjectTypes.SECURITY_POLICY, "ObjectTypeGuiDescriptor.securityPolicy", "");

    public static final String ERROR_ICON = "silk-error";
    public static final String ERROR_LOCALIZATION_KEY = "ObjectTypeGuiDescriptor.unknown";

    private ObjectTypes type;
    private String localizationKey;
    private String icon;

    private ObjectTypeGuiDescriptor(ObjectTypes type, String localizationKey, String icon) {
        this.icon = icon;
        this.localizationKey = localizationKey;
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public String getLocalizationKey() {
        return localizationKey;
    }

    public ObjectTypes getType() {
        return type;
    }

    public static ObjectTypeGuiDescriptor getDescriptor(Class type) {
        for (ObjectTypeGuiDescriptor descr : ObjectTypeGuiDescriptor.values()) {
            if (descr.getType().getClassDefinition().equals(type)) {
                return descr;
            }
        }

        return null;
    }

    public static ObjectTypeGuiDescriptor getDescriptor(ObjectTypes type) {
        for (ObjectTypeGuiDescriptor descr : ObjectTypeGuiDescriptor.values()) {
            if (descr.getType().equals(type)) {
                return descr;
            }
        }

        return null;
    }
}
