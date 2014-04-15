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

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class TestCompare {
	
	protected abstract String getSubdirName();
	
	protected abstract String getFilenameSuffix();
	
	protected File getCommonSubdir() {
		return new File(COMMON_DIR_PATH, getSubdirName());
	}
	
	protected File getFile(String baseName) {
		return new File(getCommonSubdir(), baseName+"."+getFilenameSuffix());
	}
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	/**
	 * Parse the same files twice, compare the results.
	 */
	@Test
	public void testCompareJack() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testCompareJack ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
//		Document document = DOMUtil.parseFile(USER_JACK_FILE_XML);
//		Element userElement = DOMUtil.getFirstChildElement(document);
		
		PrismObject<UserType> user1 = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
		PrismObject<UserType> user2 = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
		
		// WHEN, THEN
		
		assertTrue("Users not the same (PrismObject)(1)", user1.equals(user2));
		assertTrue("Users not the same (PrismObject)(2)", user2.equals(user1));
		
		// Following line won't work here. We don't have proper generated classes here. 
		// It is tested in the "schema" project that has a proper code generation
		// assertTrue("Users not the same (Objectable)", user1.asObjectable().equals(user2.asObjectable()));
		
		assertTrue("Users not equivalent (1)", user1.equivalent(user2));
		assertTrue("Users not equivalent (2)", user2.equivalent(user1));
	}
	
	/**
	 * Parse original jack and modified Jack. Diff and assert if the resulting
	 * delta is OK.
	 */
	@Test
	public void testDiffJack() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testDiffJack ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
//		Document document = DOMUtil.parseFile(USER_JACK_FILE_XML);
//		Element userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackOriginal = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
		
//		document = DOMUtil.parseFile(USER_JACK_MODIFIED_FILE);
//		userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackModified = prismContext.parseObject(getFile(USER_JACK_MODIFIED_FILE_BASENAME));
		
		// WHEN
		ObjectDelta<UserType> jackDelta = jackOriginal.diff (jackModified);
		
		// THEN
		System.out.println("Jack delta:");
		System.out.println(jackDelta.debugDump());
		
		jackDelta.assertDefinitions();
		jackDelta.checkConsistence(true, true, true);
		
		assertEquals("Wrong delta type", ChangeType.MODIFY, jackDelta.getChangeType());
		assertEquals("Wrong delta OID", USER_JACK_OID, jackDelta.getOid());
		assertEquals("Wrong number of modificaitions", 10, jackDelta.getModifications().size());
		
		PrismAsserts.assertPropertyReplace(jackDelta, USER_FULLNAME_QNAME, "Jack Sparrow");
		
		PrismAsserts.assertPropertyDelete(jackDelta, new ItemPath(USER_EXTENSION_QNAME, EXTENSION_MULTI_ELEMENT), "dva");
		PrismAsserts.assertPropertyAdd(jackDelta, new ItemPath(USER_EXTENSION_QNAME, EXTENSION_MULTI_ELEMENT), "osem");
		// TODO: assert BAR
		
		PrismAsserts.assertPropertyDelete(jackDelta, USER_ADDITIONALNAMES_QNAME, "Captain");
		
		PrismAsserts.assertPropertyAdd(jackDelta, USER_LOCALITY_QNAME, "World's End");
		
		PrismAsserts.assertPropertyReplace(jackDelta, USER_ENABLED_PATH, false);
		PrismAsserts.assertPropertyDelete(jackDelta, USER_VALID_FROM_PATH, USER_JACK_VALID_FROM);
		
		PrismAsserts.assertPropertyReplace(jackDelta, 
				new ItemPath(
						new NameItemPathSegment(USER_ASSIGNMENT_QNAME),
						new IdItemPathSegment(USER_ASSIGNMENT_2_ID),
						new NameItemPathSegment(USER_DESCRIPTION_QNAME)), 
				"Assignment II");
		
		ContainerDelta<?> assignment3Delta = PrismAsserts.assertContainerAdd(jackDelta, new ItemPath(USER_ASSIGNMENT_QNAME));
		PrismContainerValue<?> assignment3DeltaAddValue = assignment3Delta.getValuesToAdd().iterator().next();
		assertEquals("Assignment 3 wrong ID", USER_ASSIGNMENT_3_ID, assignment3DeltaAddValue.getId());
		
		// TODO assert assignment[i1112]/accountConstruction
	}
	
	@Test
	public void testDiffPatchRoundTrip() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testDiffPatchRoundTrip ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
//		Document document = DOMUtil.parseFile(USER_JACK_FILE_XML);
//		Element userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackOriginal = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
		
//		document = DOMUtil.parseFile(USER_JACK_MODIFIED_FILE);
//		userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackModified = prismContext.parseObject(getFile(USER_JACK_MODIFIED_FILE_BASENAME));
		
		ObjectDelta<UserType> jackDelta = jackOriginal.diff(jackModified);

//        System.out.println("jackOriginal:\n" + prismContext.getXnodeProcessor().serializeObject(jackOriginal).debugDump(1));
//        System.out.println("jackModified:\n" + prismContext.getXnodeProcessor().serializeObject(jackModified).debugDump(1));
//        System.out.println("jackDelta:\n" + jackDelta.debugDump());

		jackDelta.assertDefinitions();
		jackDelta.checkConsistence(true, true, true);
		
		// WHEN
		jackDelta.applyTo(jackOriginal);

//        System.out.println("jackOriginal after applying delta:\n" + prismContext.getXnodeProcessor().serializeObject(jackOriginal).debugDump(1));

        // THEN
		assertTrue("Roundtrip failed", jackOriginal.equivalent(jackModified));
	}
	
	@Test
	public void testEqualsReferenceValues() throws Exception {
		System.out.println("===[ testEqualsReferenceValues ]===");
		
		PrismContext prismContext = constructInitializedPrismContext();
		
		PrismReferenceValue val11 = new PrismReferenceValue("oid1");
		val11.setTargetType(ACCOUNT_TYPE_QNAME);
		
		PrismReferenceValue val12 = new PrismReferenceValue("oid1");
		val12.setTargetType(ACCOUNT_TYPE_QNAME);
		
		PrismReferenceValue val13 = new PrismReferenceValue("oid1");
		// No type
		
		PrismReferenceValue val21 = new PrismReferenceValue();
		val21.setTargetType(ACCOUNT_TYPE_QNAME);
		
		PrismReferenceValue val22 = new PrismReferenceValue();
		val22.setTargetType(ACCOUNT_TYPE_QNAME);
		
		PrismReferenceValue val23 = new PrismReferenceValue();
		// No type
		
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
		
		PrismReferenceValue val31 = new PrismReferenceValue();
		val31.setObject(user);
		
		PrismReferenceValue val32 = new PrismReferenceValue();
		val32.setObject(user.clone());
				
		PrismReferenceValue val33 = new PrismReferenceValue();
		// No type, no object
		
		PrismReferenceValue val34 = new PrismReferenceValue();
		PrismObject<UserType> differentUser = user.clone();
		differentUser.setOid(null);
		differentUser.setPropertyRealValue(UserType.F_FULL_NAME, "Jack Different");
		val34.setObject(differentUser);
		
		PrismReferenceValue val35 = new PrismReferenceValue();
		PrismObject<UserType> yetAnotherDifferentUser = user.clone();
		yetAnotherDifferentUser.setOid(null);
		yetAnotherDifferentUser.setPropertyRealValue(UserType.F_FULL_NAME, "John J Random");
		val35.setObject(yetAnotherDifferentUser);

		
		assertTrue("val11 - val11", val11.equals(val11));
		assertTrue("val11 - val12", val11.equals(val12));
		assertTrue("val12 - val11", val12.equals(val11));
		assertFalse("val11 - val13", val11.equals(val13));
		assertFalse("val13 - val11", val13.equals(val11));
		
		assertTrue("val21 - val21", val21.equals(val21));
		assertTrue("val21 - val22", val21.equals(val22));
		assertTrue("val22 - val21", val22.equals(val21));
		assertFalse("val21 - val23", val21.equals(val23));
		assertFalse("val23 - val21", val23.equals(val21));
		
		assertTrue("val31 - val31", val31.equals(val31));
		assertTrue("val31 - val32", val31.equals(val32));
		assertTrue("val32 - val31", val32.equals(val31));
		assertFalse("val31 - val33", val31.equals(val33));
		assertFalse("val33 - val31", val33.equals(val31));
		assertFalse("val31 - val34", val31.equals(val34));
		assertFalse("val34 - val31", val34.equals(val31));
		assertFalse("val31 - val35", val31.equals(val35));
		assertFalse("val35 - val31", val35.equals(val31));
		assertFalse("val34 - val35", val34.equals(val35));
		assertFalse("val35 - val34", val35.equals(val34));
		
	}
	
	@Test
	public void testEqualsBrokenAssignmentActivation() throws Exception {
		System.out.println("===[ testEqualsReferenceValues ]===");
		
		// GIVEN
		PrismObjectDefinition<UserType> userDef = PrismInternalTestUtil.getUserTypeDefinition();
		PrismContainerDefinition<AssignmentType> assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> goodAssignment = assignmentDef.instantiate(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> brokenAssignment = goodAssignment.clone();
		assertEquals("Not equals after clone", goodAssignment, brokenAssignment);
		// lets break one of these ...
		PrismContainerValue<AssignmentType> emptyValue = new PrismContainerValue<AssignmentType>();
		brokenAssignment.add(emptyValue);
		
		// WHEN
		assertFalse("Unexpected equals", goodAssignment.equals(brokenAssignment));
		
		brokenAssignment.normalize();
		assertEquals("Not equals after normalize(bad)", goodAssignment, brokenAssignment);
		
		goodAssignment.normalize();
		assertEquals("Not equals after normalize(good)", goodAssignment, brokenAssignment);
		
	}
}
