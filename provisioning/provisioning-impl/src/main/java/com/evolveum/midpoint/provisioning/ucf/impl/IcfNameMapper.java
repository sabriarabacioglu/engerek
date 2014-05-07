package com.evolveum.midpoint.provisioning.ucf.impl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class IcfNameMapper {
	
	private static final String CUSTOM_OBJECTCLASS_PREFIX = "Custom";
	private static final String CUSTOM_OBJECTCLASS_SUFFIX = "ObjectClass";
	
	private Map<String,QName> specialAttributeMapIcf = new HashMap<String,QName>();
	private Map<QName,String> specialAttributeMapMp = new HashMap<QName,String>();

	public void initialize() {
		addSpecialAttributeMapping(Name.NAME, ConnectorFactoryIcfImpl.ICFS_NAME);
		addSpecialAttributeMapping(Uid.NAME, ConnectorFactoryIcfImpl.ICFS_UID);
		
		addOperationalAttributeMapping(OperationalAttributeInfos.CURRENT_PASSWORD);
		addOperationalAttributeMapping(OperationalAttributeInfos.DISABLE_DATE);
		addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE);
		addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE_DATE);
		addOperationalAttributeMapping(OperationalAttributeInfos.LOCK_OUT);
		addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD);
		addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
		addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRED);
		
		addOperationalAttributeMapping(SecretIcfOperationalAttributes.DESCRIPTION);
		addOperationalAttributeMapping(SecretIcfOperationalAttributes.GROUPS);
		addOperationalAttributeMapping(SecretIcfOperationalAttributes.LAST_LOGIN_DATE);
	}
	
	private void addSpecialAttributeMapping(String icfName, QName qname) {
		specialAttributeMapIcf.put(icfName, qname);
		specialAttributeMapMp.put(qname, icfName);
	}
	
	private void addOperationalAttributeMapping(
			SecretIcfOperationalAttributes opAttr) {
		addOperationalAttributeMapping(opAttr.getName());
	}
	
	private void addOperationalAttributeMapping(AttributeInfo attrInfo) {
		addOperationalAttributeMapping(attrInfo.getName());
	}
	
	private void addOperationalAttributeMapping(String icfName) {
		QName qName = convertUnderscoreAttributeNameToQName(icfName);
		addSpecialAttributeMapping(icfName, qName);
	}

	public QName convertAttributeNameToQName(String icfAttrName, String schemaNamespace) {
		if (specialAttributeMapIcf.containsKey(icfAttrName)) {
			return specialAttributeMapIcf.get(icfAttrName);
		}
		QName attrXsdName = new QName(schemaNamespace, icfAttrName,
				ConnectorFactoryIcfImpl.NS_ICF_RESOURCE_INSTANCE_PREFIX);
		return attrXsdName;
	}
	
	public String convertAttributeNameToIcf(QName attrQName, String resourceSchemaNamespace)
			throws SchemaException {
		if (specialAttributeMapMp.containsKey(attrQName)) {
			return specialAttributeMapMp.get(attrQName);
		}
		
		if (!attrQName.getNamespaceURI().equals(resourceSchemaNamespace)) {
			throw new SchemaException("No mapping from QName " + attrQName + " to an ICF attribute in resource schema namespace: " + resourceSchemaNamespace);
		}

		return attrQName.getLocalPart();
		
	}
	
	private boolean isUnderscoreSyntax(String icfAttrName) {
		return icfAttrName.startsWith("__") && icfAttrName.endsWith("__");
	}
	
	private QName convertUnderscoreAttributeNameToQName(String icfAttrName) {
		// Strip leading and trailing underscores
		String inside = icfAttrName.substring(2, icfAttrName.length()-2);
		
		StringBuilder sb = new StringBuilder();
		int lastIndex = 0;
		while (true) {
			int nextIndex = inside.indexOf("_", lastIndex);
			if (nextIndex < 0) {
				String upcase = inside.substring(lastIndex, inside.length());
				sb.append(toCamelCase(upcase, lastIndex == 0));
				break;
			}
			String upcase = inside.substring(lastIndex, nextIndex);
			sb.append(toCamelCase(upcase, lastIndex == 0));
			lastIndex = nextIndex + 1;
		}
		
		return new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, sb.toString());
	}

	private String toCamelCase(String upcase, boolean lowCase) {
		if (lowCase) {
			return StringUtils.lowerCase(upcase);
		} else {
			return StringUtils.capitalize(StringUtils.lowerCase(upcase));
		}
	}

	/**
	 * Maps ICF native objectclass name to a midPoint QName objctclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	public QName objectClassToQname(String icfObjectClassString, String schemaNamespace) {
		if (ObjectClass.ACCOUNT_NAME.equals(icfObjectClassString)) {
			return new QName(schemaNamespace, ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME,
					ConnectorFactoryIcfImpl.NS_ICF_SCHEMA_PREFIX);
		} else if (ObjectClass.GROUP_NAME.equals(icfObjectClassString)) {
			return new QName(schemaNamespace, ConnectorFactoryIcfImpl.GROUP_OBJECT_CLASS_LOCAL_NAME,
					ConnectorFactoryIcfImpl.NS_ICF_SCHEMA_PREFIX);
		} else {
			return new QName(schemaNamespace, CUSTOM_OBJECTCLASS_PREFIX + icfObjectClassString
					+ CUSTOM_OBJECTCLASS_SUFFIX, ConnectorFactoryIcfImpl.NS_ICF_RESOURCE_INSTANCE_PREFIX);
		}
	}

	public ObjectClass objectClassToIcf(PrismObject<? extends ShadowType> object, String schemaNamespace, ConnectorType connectorType) {

		ShadowType shadowType = object.asObjectable();
		QName qnameObjectClass = shadowType.getObjectClass();
		if (qnameObjectClass == null) {
			ResourceAttributeContainer attrContainer = ShadowUtil
					.getAttributesContainer(shadowType);
			if (attrContainer == null) {
				return null;
			}
			ResourceAttributeContainerDefinition objectClassDefinition = attrContainer.getDefinition();
			qnameObjectClass = objectClassDefinition.getTypeName();
		}

		return objectClassToIcf(qnameObjectClass, schemaNamespace, connectorType);
	}

	/**
	 * Maps a midPoint QName objctclass to the ICF native objectclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	public ObjectClass objectClassToIcf(ObjectClassComplexTypeDefinition objectClassDefinition, String schemaNamespace, ConnectorType connectorType) {
		QName qnameObjectClass = objectClassDefinition.getTypeName();
		return objectClassToIcf(qnameObjectClass, schemaNamespace, connectorType);
	}

	private ObjectClass objectClassToIcf(QName qnameObjectClass, String schemaNamespace, ConnectorType connectorType) {
		if (!schemaNamespace.equals(qnameObjectClass.getNamespaceURI())) {
			throw new IllegalArgumentException("ObjectClass QName " + qnameObjectClass
					+ " is not in the appropriate namespace for "
					+ connectorType + ", expected: " + schemaNamespace);
		}
		String lname = qnameObjectClass.getLocalPart();
		if (ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME.equals(lname)) {
			return ObjectClass.ACCOUNT;
		} else if (ConnectorFactoryIcfImpl.GROUP_OBJECT_CLASS_LOCAL_NAME.equals(lname)) {
			return ObjectClass.GROUP;
		} else if (lname.startsWith(CUSTOM_OBJECTCLASS_PREFIX) && lname.endsWith(CUSTOM_OBJECTCLASS_SUFFIX)) {
			String icfObjectClassName = lname.substring(CUSTOM_OBJECTCLASS_PREFIX.length(), lname.length()
					- CUSTOM_OBJECTCLASS_SUFFIX.length());
			return new ObjectClass(icfObjectClassName);
		} else {
			throw new IllegalArgumentException("Cannot recognize objectclass QName " + qnameObjectClass
					+ " for " + ObjectTypeUtil.toShortString(connectorType) + ", expected: "
					+ schemaNamespace);
		}
	}

}
