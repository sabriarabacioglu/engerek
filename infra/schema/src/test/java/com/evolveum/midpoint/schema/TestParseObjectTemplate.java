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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
public class TestParseObjectTemplate {
	
	public static final File TEST_DIR = new File("src/test/resources/object-template");
	private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "object-template.xml");
	private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseObjectTemplateFile() throws Exception {
		roundTrip("testParseObjectTemplateFile", OBJECT_TEMPLATE_FILE, 
				new QName(SchemaConstantsGenerated.NS_COMMON, "objectTemplate"));
	}
	
	@Test
	public void testParseUserTemplateFile() throws Exception {
		roundTrip("testParseUserTemplateFile", USER_TEMPLATE_FILE, 
				new QName(SchemaConstantsGenerated.NS_COMMON, "userTemplate"));
	}
	
	private void roundTrip(final String TEST_NAME, File file, QName elementName) throws Exception {
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ObjectTemplateType> object = prismContext.parseObject(file);
		
		// THEN
		System.out.println("Parsed object:");
		System.out.println(object.debugDump());
		
		assertObjectTemplate(object, elementName);
		
		// WHEN
		String xml = prismContext.serializeObjectToString(object, PrismContext.LANG_XML);
		
		// THEN
		System.out.println("Serialized object:");
		System.out.println(xml);
		
		assertSerializedObject(xml, elementName);
		
		// WHEN
		PrismObject<ObjectTemplateType> reparsedObject = prismContext.parseObject(xml);
		
		// THEN
		System.out.println("Re-parsed object:");
		System.out.println(reparsedObject.debugDump());
		
		assertObjectTemplate(reparsedObject, elementName);
	}

	private void assertObjectTemplate(PrismObject<ObjectTemplateType> object, QName elementName) {
		object.checkConsistence();
		assertObjectTemplatePrism(object, elementName);
	}

	private void assertObjectTemplatePrism(PrismObject<ObjectTemplateType> object, QName elementName) {
		
		assertEquals("Wrong oid", "10000000-0000-0000-0000-000000000002", object.getOid());
		PrismObjectDefinition<ObjectTemplateType> objectDefinition = object.getDefinition();
		assertNotNull("No object definition", objectDefinition);
		PrismAsserts.assertObjectDefinition(objectDefinition, elementName,
				ObjectTemplateType.COMPLEX_TYPE, ObjectTemplateType.class);
		assertEquals("Wrong class", ObjectTemplateType.class, object.getCompileTimeClass());
		assertEquals("Wrong object item name", elementName, object.getElementName());
		ObjectTemplateType objectType = object.asObjectable();
		assertNotNull("asObjectable resulted in null", objectType);
		
		assertPropertyValue(object, "name", PrismTestUtil.createPolyString("Default User Template"));
		assertPropertyDefinition(object, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
		
		assertPropertyDefinition(object, "mapping", MappingType.COMPLEX_TYPE, 0, -1);
		
	}
	
	
	private void assertSerializedObject(String xml, QName elementName) {
		// TODO
	}

	

	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	public static <T> void assertPropertyValues(PrismContainer<?> container, String propName, T... expectedValues) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, expectedValues);
	}


}
