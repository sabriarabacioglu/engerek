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
package com.evolveum.midpoint.provisioning;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.test.impl.TestDummy;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

/**
 * @author semancik
 *
 */
public class ProvisioningTestUtil {
	
	public static final String COMMON_TEST_DIR_FILENAME = "src/test/resources/object/";
	
	
	public static final String RESOURCE_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-9999dddddddd";

	public static void assertConnectorSchemaSanity(ConnectorType conn, PrismContext prismContext) throws SchemaException {
		XmlSchemaType xmlSchemaType = conn.getSchema();
		assertNotNull("xmlSchemaType is null",xmlSchemaType);
		Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
		assertNotNull("No schema", connectorXsdSchemaElement);
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);
		assertNotNull("No xsd:schema element in xmlSchemaType",xsdElement);
		display("XSD schema of "+conn, DOMUtil.serializeDOMToString(xsdElement));
		// Try to parse the schema
		PrismSchema schema = null;
		try {
			schema = PrismSchema.parse(xsdElement, true, "schema of "+conn, prismContext);
		} catch (SchemaException e) {
			throw new SchemaException("Error parsing schema of "+conn+": "+e.getMessage(),e);
		}
		assertConnectorSchemaSanity(schema, conn.toString());
	}
	
	public static void assertConnectorSchemaSanity(PrismSchema schema, String connectorDescription) {
		assertNotNull("Cannot parse connector schema of "+connectorDescription,schema);
		assertFalse("Empty connector schema in "+connectorDescription,schema.isEmpty());
		display("Parsed connector schema of "+connectorDescription,schema);
		
		// Local schema namespace is used here.
		PrismContainerDefinition configurationDefinition = 
			schema.findItemDefinition(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(), PrismContainerDefinition.class);
		assertNotNull("Definition of <configuration> property container not found in connector schema of "+connectorDescription,
				configurationDefinition);
		assertFalse("Empty definition of <configuration> property container in connector schema of "+connectorDescription,
				configurationDefinition.isEmpty());
		
		// ICFC schema is used on other elements
		PrismContainerDefinition configurationPropertiesDefinition = 
			configurationDefinition.findContainerDefinition(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("Definition of <configurationProperties> property container not found in connector schema of "+connectorDescription,
				configurationPropertiesDefinition);
		assertFalse("Empty definition of <configurationProperties> property container in connector schema of "+connectorDescription,
				configurationPropertiesDefinition.isEmpty());
		assertFalse("No definitions in <configurationProperties> in "+connectorDescription, configurationPropertiesDefinition.getDefinitions().isEmpty());

		// TODO: other elements
	}
	
	public static void checkRepoAccountShadow(PrismObject<ShadowType> repoShadow) {
		checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT);
	}
	
	public static void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
		checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT);
	}
	
	public static void checkRepoShadow(PrismObject<ShadowType> repoShadow, ShadowKindType kind) {
		ShadowType repoShadowType = repoShadow.asObjectable();
		assertNotNull("No OID in repo shadow "+repoShadow, repoShadowType.getOid());
		assertNotNull("No name in repo shadow "+repoShadow, repoShadowType.getName());
		assertNotNull("No objectClass in repo shadow "+repoShadow, repoShadowType.getObjectClass());
		assertEquals("Wrong kind in repo shadow "+repoShadow, kind, repoShadowType.getKind());
		PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("No attributes in repo shadow "+repoShadow, attributesContainer);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
		assertFalse("Empty attributes in repo shadow "+repoShadow, attributes.isEmpty());
		assertEquals("Unexpected number of attributes in repo shadow "+repoShadow, 2, attributes.size());
	}
	
	public static QName getDefaultAccountObjectClass(ResourceType resourceType) {
		String namespace = ResourceTypeUtil.getResourceNamespace(resourceType);
		return new QName(namespace, ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
	}
	
	public static <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName, 
			T... expectedValues) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		assertAttribute(resource, shadow, attrQname, expectedValues);
	}
	
	public static <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname, 
			T... expectedValues) {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, actualValues, expectedValues);
	}
	
	public static <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, MatchingRule<T> matchingRule, 
			QName attrQname, T... expectedValues) {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, matchingRule, actualValues, expectedValues);
	}
	
	public static void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname) {
		ResourceAttribute attribute = ShadowUtil.getAttribute(shadow.asPrismContainer(), attrQname);
		assertNull("Unexpected attribute "+attrQname+" in "+shadow+": "+attribute, attribute);
	}
	
	public static void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		ResourceAttribute attribute = ShadowUtil.getAttribute(shadow.asPrismContainer(), attrQname);
		assertNull("Unexpected attribute "+attrQname+" in "+shadow+": "+attribute, attribute);
	}

}
