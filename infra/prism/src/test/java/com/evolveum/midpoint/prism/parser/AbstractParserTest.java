/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.parser;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.foo.EventHandlerChainType;
import com.evolveum.midpoint.prism.foo.EventHandlerType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ActivationType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.ResourceType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class AbstractParserTest {
	
	private static final QName XSD_COMPLEX_TYPE_ELEMENT_NAME 
			= new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "complexType");

    public static final String EVENT_HANDLER_FILE_BASENAME = "event-handler";

    @BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	protected abstract String getSubdirName();
	
	protected abstract String getFilenameSuffix();
	
	protected File getCommonSubdir() {
		return new File(COMMON_DIR_PATH, getSubdirName());
	}
	
	protected File getFile(String baseName) {
		return new File(getCommonSubdir(), baseName+"."+getFilenameSuffix());
	}
	
	protected abstract Parser createParser();
	

	@Test
    public void testParseUserToPrism() throws Exception {
		final String TEST_NAME = "testParseUserToPrism";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse to xnode)
		XNode xnode = parser.parse(getFile(USER_JACK_FILE_BASENAME));
		System.out.println("XNode after parsing:");
		System.out.println(xnode.debugDump());
		
		// WHEN (parse to prism)
		PrismObject<UserType> user = processor.parseObject(xnode);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.debugDump());

		assertUserJackXNodeOrdering("serialized xnode", xnode);
		
		assertUserJack(user);		
		
	}

	@Test
    public void testParseUserRoundTrip() throws Exception {
		final String TEST_NAME = "testParseUserRoundTrip";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse)
		XNode xnode = parser.parse(getFile(USER_JACK_FILE_BASENAME));
		System.out.println("\nParsed xnode:");
		System.out.println(xnode.debugDump());
		PrismObject<UserType> user = processor.parseObject(xnode);
		
		// THEN
		System.out.println("\nParsed user:");
		System.out.println(user.debugDump());
		
		assertUserJack(user);
		
		// WHEN (re-serialize to XNode)
		XNode serializedXNode = processor.serializeObject(user, true);
		String serializedString = parser.serializeToString(serializedXNode, new QName(NS_FOO, "user"));
		
		// THEN
				System.out.println("\nXNode after re-serialization:");
				System.out.println(serializedXNode.debugDump());
				System.out.println("\nRe-serialized string:");
				System.out.println(serializedString);
		
//		try{
//			FileOutputStream out = new FileOutputStream(new File("D:/user-jack-prism.json"));
//			PrismJsonSerializer jsonSer = new PrismJsonSerializer();
//			String s = jsonSer.serializeToString((RootXNode) serializedXNode);
//			System.out.println("JSON: \n" + s);
//			
////			FileInputStream in = new FileInputStream(new File("D:/user-jack-prism.json"));
////			XNode afterJson = jsonSer.parseObject(in);
////			
////			// THEN
////					System.out.println("AFTER JSON XNode:");
////					System.out.println(afterJson.debugDump());
//			
//			} catch (Exception ex){
//				System.out.println( ex);
//				throw ex;
//			}
//		
		
		
		assertUserJackXNodeOrdering("serialized xnode", serializedXNode);
		
		validateUserSchema(serializedString, prismContext);
		
		// WHEN (re-parse)
		XNode reparsedXnode = parser.parse(serializedString);
		PrismObject<UserType> reparsedUser = processor.parseObject(reparsedXnode);
		
		// THEN
		System.out.println("\nXNode after re-parsing:");
		System.out.println(reparsedXnode.debugDump());
		System.out.println("\nRe-parsed user:");
		System.out.println(reparsedUser.debugDump());
		
		assertUserJackXNodeOrdering("serialized xnode", reparsedXnode);
				
		ObjectDelta<UserType> diff = DiffUtil.diff(user, reparsedUser);
		System.out.println("\nDiff:");
		System.out.println(diff.debugDump());
		
		PrismObject accountRefObjOrig = findObjectFromAccountRef(user);
		PrismObject accountRefObjRe = findObjectFromAccountRef(reparsedUser);
		
		ObjectDelta<UserType> accountRefObjDiff = DiffUtil.diff(accountRefObjOrig, accountRefObjRe);
		System.out.println("\naccountRef object diff:");
		System.out.println(accountRefObjDiff.debugDump());
		
		assertTrue("Re-parsed object in accountRef does not match: "+accountRefObjDiff, accountRefObjDiff.isEmpty());
		
		assertTrue("Re-parsed user does not match: "+diff, diff.isEmpty());
	}	
	
	@Test
    public void testParseResourceRumToPrism() throws Exception {
		final String TEST_NAME = "testParseResourceRumToPrism";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse to xnode)
		XNode xnode = parser.parse(getFile(RESOURCE_RUM_FILE_BASENAME));
		System.out.println("XNode after parsing:");
		System.out.println(xnode.debugDump());
		
		// WHEN (parse to prism)
		PrismObject<ResourceType> resource = processor.parseObject(xnode);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());
		
		assertResourceRum(resource);		
		
	}

	@Test
    public void testParseResourceRoundTrip() throws Exception {
		final String TEST_NAME = "testParseResourceRoundTrip";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse)
		XNode xnode = parser.parse(getFile(RESOURCE_RUM_FILE_BASENAME));
		PrismObject<ResourceType> resource = processor.parseObject(xnode);
		
		// THEN
		System.out.println("\nParsed resource:");
		System.out.println(resource.debugDump());
		
		assertResourceRum(resource);
		
		// WHEN (re-serialize to XNode)
		XNode serializedXNode = processor.serializeObject(resource, true);
		String serializedString = parser.serializeToString(serializedXNode, new QName(NS_FOO, "resource"));
				
		// THEN
		System.out.println("\nXNode after re-serialization:");
		System.out.println(serializedXNode.debugDump());
		System.out.println("\nRe-serialized string:");
		System.out.println(serializedString);
		
//		try{
////			FileOutputStream out = new FileOutputStream(new File("D:/user-jack-prism.json"));
//			PrismJsonSerializer jsonSer = new PrismJsonSerializer();
//			String s = jsonSer.serializeToString((RootXNode) serializedXNode);
//			System.out.println("JSON: \n" + s);
//			
////			FileOutputStream out = new FileOutputStream(new File("D:/user-jack-prism.json"));
//			YamlParser yamlParser = new YamlParser();
//			s = yamlParser.serializeToString((RootXNode) serializedXNode);
//			System.out.println("YAML: \n" + s);
//			
////			FileInputStream in = new FileInputStream(new File("D:/user-jack-prism.json"));
////			XNode afterJson = jsonSer.parseObject(in);
////			
////			// THEN
////					System.out.println("AFTER JSON XNode:");
////					System.out.println(afterJson.debugDump());
//			
//			} catch (Exception ex){
//				System.out.println( ex);
//				throw ex;
//			}
		
		validateResourceSchema(serializedString, prismContext);
		
		// WHEN (re-parse)
		XNode reparsedXnode = parser.parse(serializedString);
		PrismObject<ResourceType> reparsedResource = processor.parseObject(reparsedXnode);
		
		// THEN
		System.out.println("\nXNode after re-parsing:");
		System.out.println(reparsedXnode.debugDump());
		System.out.println("\nRe-parsed resource:");
		System.out.println(reparsedResource.debugDump());
		
		ObjectDelta<ResourceType> diff = DiffUtil.diff(resource, reparsedResource);
		System.out.println("\nDiff:");
		System.out.println(diff.debugDump());
				
		assertTrue("Re-parsed user does not match: "+diff, diff.isEmpty());
	}	


	private void assertResourceRum(PrismObject<ResourceType> resource) throws SchemaException {
		resource.checkConsistence();
		resource.assertDefinitions("test");
		
		assertEquals("Wrong oid", RESOURCE_RUM_OID, resource.getOid());
		PrismAsserts.assertObjectDefinition(resource.getDefinition(), RESOURCE_QNAME, RESOURCE_TYPE_QNAME, ResourceType.class);
		PrismAsserts.assertParentConsistency(resource);
		
		assertPropertyValue(resource, "name", new PolyString("Rum Delivery System", "rum delivery system"));
		assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
		
		PrismProperty<SchemaDefinitionType> propSchema = resource.findProperty(ResourceType.F_SCHEMA);
		assertNotNull("No schema property in resource", propSchema);
		PrismPropertyDefinition<SchemaDefinitionType> propSchemaDef = propSchema.getDefinition();
		assertNotNull("No definition of schema property in resource", propSchemaDef);
		SchemaDefinitionType schemaDefinitionType = propSchema.getRealValue();
		assertNotNull("No value of schema property in resource", schemaDefinitionType);
		
		Element schemaElement = schemaDefinitionType.getSchema();
		assertNotNull("No schema element in schema property in resource", schemaElement);
		System.out.println("Resource schema:");
		System.out.println(DOMUtil.serializeDOMToString(schemaElement));
		assertEquals("Bad schema element name", DOMUtil.XSD_SCHEMA_ELEMENT, DOMUtil.getQName(schemaElement));
		Element complexTypeElement = DOMUtil.getChildElement(schemaElement, XSD_COMPLEX_TYPE_ELEMENT_NAME);
		assertNotNull("No complexType element in schema element in schema property in resource", complexTypeElement);
	}

	private PrismObject findObjectFromAccountRef(PrismObject<UserType> user) {
		for (PrismReferenceValue rval: user.findReference(UserType.F_ACCOUNT_REF).getValues()) {
			if (rval.getObject() != null) {
				return rval.getObject();
			}
		}
		return null;
	}

	protected <X extends XNode> X getAssertXNode(String message, XNode xnode, Class<X> expectedClass) {
		assertNotNull(message+" is null", xnode);
		assertTrue(message+", expected "+expectedClass.getSimpleName()+", was "+xnode.getClass().getSimpleName(),
				expectedClass.isAssignableFrom(xnode.getClass()));
		return (X) xnode;
	}
	
	protected <X extends XNode> X getAssertXMapSubnode(String message, MapXNode xmap, QName key, Class<X> expectedClass) {
		XNode xsubnode = xmap.get(key);
		assertNotNull(message+" no key "+key, xsubnode);
		return getAssertXNode(message+" key "+key, xsubnode, expectedClass);
	}
	
	protected void assertUserJackXNodeOrdering(String message, XNode xnode) {
		if (xnode instanceof RootXNode) {
			xnode = ((RootXNode)xnode).getSubnode();
		}
		MapXNode xmap = getAssertXNode(message+": top", xnode, MapXNode.class);
		Set<Entry<QName, XNode>> reTopMapEntrySet = xmap.entrySet();
		Iterator<Entry<QName, XNode>> reTopMapEntrySetIter = reTopMapEntrySet.iterator();
		Entry<QName, XNode> reTopMapEntry0 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 0, the xnodes were shuffled", "oid", reTopMapEntry0.getKey().getLocalPart());
		Entry<QName, XNode> reTopMapEntry1 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 1, the xnodes were shuffled", "version", reTopMapEntry1.getKey().getLocalPart());
		Entry<QName, XNode> reTopMapEntry2 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 2, the xnodes were shuffled", UserType.F_NAME, reTopMapEntry2.getKey());
		Entry<QName, XNode> reTopMapEntry3 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 3, the xnodes were shuffled", UserType.F_DESCRIPTION, reTopMapEntry3.getKey());

	}
	
	protected void validateUserSchema(String dataString, PrismContext prismContext) throws SAXException, IOException {
		// Nothing to do by default
	}
	
	protected void validateResourceSchema(String dataString, PrismContext prismContext) throws SAXException, IOException {
		// Nothing to do by default
	}

    // The following is not supported now (and probably won't be in the future).
    // Enable it if that changes.
    @Test(enabled = false)
    public void testParseEventHandler() throws Exception {
        final String TEST_NAME = "testParseEventHandler";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Parser parser = createParser();
        XNodeProcessor processor = new XNodeProcessor();
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        processor.setPrismContext(prismContext);

        // WHEN (parse to xnode)
        RootXNode xnode = (RootXNode) parser.parse(getFile(EVENT_HANDLER_FILE_BASENAME));
        System.out.println("XNode after parsing:");
        System.out.println(xnode.debugDump());

        // WHEN (parse to prism)
        EventHandlerType eventHandlerType = processor.getPrismContext().getBeanConverter().unmarshall((MapXNode) xnode.getSubnode(), EventHandlerChainType.class);

        // THEN
        System.out.println("Parsed object:");
        System.out.println(eventHandlerType);

        // WHEN2 (marshalling)
        MapXNode marshalled = (MapXNode) processor.getPrismContext().getBeanConverter().marshall(eventHandlerType);

        System.out.println("XNode after unmarshalling and marshalling back:");
        System.out.println(marshalled.debugDump());

    }

}
