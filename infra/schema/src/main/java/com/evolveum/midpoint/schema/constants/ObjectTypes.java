/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.schema.constants;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public enum ObjectTypes {

    @Deprecated
    ACCOUNT(SchemaConstants.C_ACCOUNT_SHADOW_TYPE, SchemaConstants.C_ACCOUNT,
            AccountShadowType.class, ObjectManager.PROVISIONING, "accounts"),

    CONNECTOR(SchemaConstants.C_CONNECTOR_TYPE, SchemaConstants.C_CONNECTOR,
            ConnectorType.class, ObjectManager.PROVISIONING, "connectors"),

    CONNECTOR_HOST(SchemaConstants.C_CONNECTOR_HOST_TYPE,
            SchemaConstants.C_CONNECTOR_HOST, ConnectorHostType.class, ObjectManager.PROVISIONING, "connectorHosts"),

    GENERIC_OBJECT(SchemaConstants.C_GENERIC_OBJECT_TYPE,
            SchemaConstants.C_GENERIC_OBJECT, GenericObjectType.class, ObjectManager.MODEL, "genericObjects"),

    RESOURCE(SchemaConstants.C_RESOURCE_TYPE, SchemaConstants.C_RESOURCE,
            ResourceType.class, ObjectManager.PROVISIONING, "resources"),

    USER(SchemaConstants.C_USER_TYPE, SchemaConstants.C_USER, UserType.class,
            ObjectManager.MODEL, "users"),

    OBJECT_TEMPLATE(SchemaConstants.C_OBJECT_TEMPLATE_TYPE,
            SchemaConstants.C_OBJECT_TEMPLATE, ObjectTemplateType.class, ObjectManager.MODEL, "objectTemplates"),

    SYSTEM_CONFIGURATION(SchemaConstants.C_SYSTEM_CONFIGURATION_TYPE, SchemaConstants.C_SYSTEM_CONFIGURATION,
            SystemConfigurationType.class, ObjectManager.MODEL, "systemConfigurations"),

    TASK(SchemaConstants.C_TASK_TYPE, SchemaConstants.C_TASK, TaskType.class, ObjectManager.TASK_MANAGER, "tasks"),

    SHADOW(SchemaConstants.C_SHADOW_TYPE, SchemaConstants.C_SHADOW, ShadowType.class, ObjectManager.PROVISIONING, "shadows"),

    ROLE(RoleType.COMPLEX_TYPE, SchemaConstantsGenerated.C_ROLE, RoleType.class, ObjectManager.MODEL, "roles"),

    PASSWORD_POLICY(ValuePolicyType.COMPLEX_TYPE, SchemaConstantsGenerated.C_VALUE_POLICY, ValuePolicyType.class,
            ObjectManager.MODEL, "valuePolicies"),

    NODE(NodeType.COMPLEX_TYPE, SchemaConstantsGenerated.C_NODE, NodeType.class, ObjectManager.TASK_MANAGER, "nodes"),

    ORG(OrgType.COMPLEX_TYPE, SchemaConstantsGenerated.C_ORG, OrgType.class, ObjectManager.MODEL, "orgs"),

    ABSTRACT_ROLE(AbstractRoleType.COMPLEX_TYPE, SchemaConstants.C_ABSTRACT_ROLE, AbstractRoleType.class,
            ObjectManager.MODEL, "abstractRoles"),

    FOCUS_TYPE(FocusType.COMPLEX_TYPE, SchemaConstants.C_FOCUS, FocusType.class, ObjectManager.MODEL, "focus"),

    REPORT(ReportType.COMPLEX_TYPE, SchemaConstants.C_REPORT, ReportType.class, ObjectManager.MODEL, "reports"),

    REPORT_OUTPUT(ReportOutputType.COMPLEX_TYPE, SchemaConstants.C_REPORT_OUTPUT, ReportOutputType.class,
                  ObjectManager.MODEL, "reportOutputs"),
                  
    SECURITY_POLICY(SecurityPolicyType.COMPLEX_TYPE, SchemaConstants.C_SECURITY_POLICY, SecurityPolicyType.class,
    			ObjectManager.MODEL, "securityPolicies"),

    // as for now, this has to remain disabled (they could be used e.g. in audit logs, when the repo will support them)
    //WORK_ITEM(WorkItemType.COMPLEX_TYPE, SchemaConstants.C_WORK_ITEM, WorkItemType.class, ObjectManager.WORKFLOW, "workItems"),
    //WF_PROCESS_INSTANCE(WfProcessInstanceType.COMPLEX_TYPE, SchemaConstants.C_WF_PROCESS_INSTANCE, WfProcessInstanceType.class, ObjectManager.WORKFLOW, "wfProcessInstances"),

    // this should be at end, because otherwise it presents itself as entry for all subtypes of ObjectType
    OBJECT(SchemaConstants.C_OBJECT_TYPE, SchemaConstants.C_OBJECT, ObjectType.class, ObjectManager.MODEL, "objects");
    
    public static enum ObjectManager {
        PROVISIONING, TASK_MANAGER, WORKFLOW, MODEL, REPOSITORY;
    }

    private QName type;
    private QName name;
    private Class<? extends ObjectType> classDefinition;
    private ObjectManager objectManager;
    private String restType;

    private ObjectTypes(QName type, QName name, Class<? extends ObjectType> classDefinition,
                        ObjectManager objectManager, String restType) {
        this.type = type;
        this.name = name;
        this.classDefinition = classDefinition;
        this.objectManager = objectManager;
        this.restType = restType;
    }

    public boolean isManagedByProvisioning() {
        return objectManager == ObjectManager.PROVISIONING;
    }

    public boolean isManagedByTaskManager() {
        return objectManager == ObjectManager.TASK_MANAGER;
    }

    public boolean isManagedByWorkflow() {
        return objectManager == ObjectManager.WORKFLOW;
    }

    public String getValue() {
        return type.getLocalPart();
    }

    public QName getQName() {
        return name;
    }

    public QName getTypeQName() {
        return type;
    }

    public Class<? extends ObjectType> getClassDefinition() {
        return classDefinition;
    }

    public String getRestType() {
        return restType;
    }

    public void setRestType(String restType) {
        this.restType = restType;
    }

    public String getObjectTypeUri() {
        return QNameUtil.qNameToUri(getTypeQName());
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    public static ObjectTypes getObjectType(String objectType) {
        for (ObjectTypes type : values()) {
            if (type.getValue().equals(objectType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    public static ObjectTypes getObjectTypeFromTypeQName(QName typeQName) {
        // HACK WARNING! FIXME
        // UGLY HORRIBLE TERRIBLE AWFUL HACK FOLLOWS
        // The JAXB fails to correctly process QNames in default namespace (no prefix)
        // e.g it will not understand this: type="RoleType", even if defatult namespace
        // is set, it will parse it as null namespace.
        // Therefore substitute null namespace with common namespace
        if (typeQName.getNamespaceURI() == null || typeQName.getNamespaceURI().isEmpty()) {
            typeQName = new QName(SchemaConstants.NS_C, typeQName.getLocalPart());
        }
        // END OF UGLY HACK

        for (ObjectTypes type : values()) {
            if (type.getTypeQName().equals(typeQName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type qname " + typeQName);
    }

    public static ObjectTypes getObjectTypeFromUri(String objectTypeUri) {
        for (ObjectTypes type : values()) {
            if (type.getObjectTypeUri().equals(objectTypeUri)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported object type uri " + objectTypeUri);
    }

    public static String getObjectTypeUri(String objectType) {
        return getObjectType(objectType).getObjectTypeUri();
    }

    public static Class<? extends ObjectType> getObjectTypeClass(String objectType) {
        for (ObjectTypes type : values()) {
            if (type.getValue().equals(objectType)) {
                return type.getClassDefinition();
            }
        }

        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    @SuppressWarnings("unchecked")
    public static ObjectTypes getObjectType(Class<? extends ObjectType> objectType) {
        for (ObjectTypes type : values()) {
            if (type.getClassDefinition().equals(objectType)) {
                return type;
            }
        }
        // No match. Try with superclass.
        Class<?> superclass = objectType.getSuperclass();
        if (superclass != null && !superclass.equals(ObjectType.class)) {
            return getObjectType((Class<? extends ObjectType>) superclass);
        }

        throw new IllegalArgumentException("Unsupported object type " + objectType);
    }

    public static boolean isManagedByProvisioning(ObjectType object) {
        Validate.notNull(object, "Object must not be null.");

        return isClassManagedByProvisioning(object.getClass());
    }

    public static boolean isClassManagedByProvisioning(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().isAssignableFrom(clazz)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static boolean isObjectTypeManagedByProvisioning(Class<? extends ObjectType> objectType) {
        Validate.notNull(objectType, "Object type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().equals(objectType)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static boolean isObjectTypeManagedByProvisioning(String objectType) {
        Validate.notEmpty(objectType, "Object type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getTypeQName().getLocalPart().equals(objectType)) {
                return type.isManagedByProvisioning();
            }
        }

        return false;
    }

    public static ObjectManager getObjectManagerForClass(Class<? extends ObjectType> clazz) {
        Validate.notNull(clazz, "Class must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getClassDefinition().isAssignableFrom(clazz)) {
                return type.getObjectManager();
            }
        }

        return null;
    }

    public static Class getClassFromRestType(String restType) {
        Validate.notNull(restType, "Rest type must not be null.");

        for (ObjectTypes type : ObjectTypes.values()) {
            if (type.getRestType().equals(restType)) {
                return type.getClassDefinition();
            }
        }

        throw new IllegalArgumentException("Not suitable class found for rest type: " + restType);
    }

    public static List<Class<? extends ObjectType>> getAllObjectTypes() {
        List<Class<? extends ObjectType>> list = new ArrayList<>();
        for (ObjectTypes t : ObjectTypes.values()) {
            list.add(t.getClassDefinition());
        }

        return list;
    }
}

