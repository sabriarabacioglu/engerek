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

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_2.RawType;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class TestDeltaConverter extends AbstractSchemaTest {
	
	private static final File TEST_DIR = new File("src/test/resources/deltaconverter");
	
	private static final ItemPath CREDENTIALS_PASSWORD_VALUE_PATH = 
		new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

    @Test
    public void testRefWithObject() throws SchemaException, IOException, JAXBException {
    	System.out.println("===[ testRefWithObject ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, "user-modify-add-account.xml"),
                ObjectModificationType.COMPLEX_TYPE);
    	
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	System.out.println("delta: " + objectDelta.debugDump());
  
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	ReferenceDelta accoutRefDelta = objectDelta.findReferenceModification(UserType.F_LINK_REF);
    	assertNotNull("No accountRef delta", accoutRefDelta);
    	Collection<PrismReferenceValue> valuesToAdd = accoutRefDelta.getValuesToAdd();
    	assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
    	PrismReferenceValue accountRefVal = valuesToAdd.iterator().next();
    	assertNotNull("No object in accountRef value", accountRefVal.getObject());
    	
    	objectDelta.assertDefinitions();
    }
    
    @Test
    public void testPasswordChange() throws Exception {
    	System.out.println("===[ testPasswordChange ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, "user-modify-password.xml"),
                ObjectModificationType.COMPLEX_TYPE);
    	
    	// WHEN
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	// THEN
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	PropertyDelta<ProtectedStringType> protectedStringDelta = objectDelta.findPropertyDelta(CREDENTIALS_PASSWORD_VALUE_PATH);
    	assertNotNull("No protectedString delta", protectedStringDelta);
    	Collection<PrismPropertyValue<ProtectedStringType>> valuesToReplace = protectedStringDelta.getValuesToReplace();
    	assertEquals("Wrong number of values to replace", 1, valuesToReplace.size());
    	PrismPropertyValue<ProtectedStringType> protectedStringVal = valuesToReplace.iterator().next();
    	assertNotNull("Null value in protectedStringDelta", protectedStringVal);
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
    	// apply to user
    	objectDelta.applyTo(user);
    	
    	PrismProperty<ProtectedStringType> protectedStringProperty = user.findProperty(CREDENTIALS_PASSWORD_VALUE_PATH);
    	PrismPropertyValue<ProtectedStringType> protectedStringPropertyValue = protectedStringProperty.getValue();
    	assertTrue("protectedString not equivalent", protectedStringPropertyValue.equalsRealValue(protectedStringVal));
    	
    	objectDelta.assertDefinitions();
    }
    
    @Test
    public void testModifyGivenName() throws Exception {
    	System.out.println("===[ testModifyGivenName ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, "user-modify-givenname.xml"),
                ObjectModificationType.COMPLEX_TYPE);
    	
    	// WHEN
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	// THEN
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	PropertyDelta<String> givenNameDelta = objectDelta.findPropertyDelta(UserType.F_GIVEN_NAME);
    	assertNotNull("No givenName delta", givenNameDelta);
    	Collection<PrismPropertyValue<String>> valuesToReplace = givenNameDelta.getValuesToReplace();
    	assertEquals("Wrong number of values to replace", 0, valuesToReplace.size());
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
    	// apply to user
    	objectDelta.applyTo(user);
    	
    	PrismProperty<String> protectedStringProperty = user.findProperty(UserType.F_GIVEN_NAME);
    	assertNull("givenName porperty sneaked in after delta was applied", protectedStringProperty);
    	
    	objectDelta.assertDefinitions();
    }
    
    
    @Test
    public void testAddAssignment() throws Exception {
    	System.out.println("===[ testAddAssignment ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, "user-modify-add-role-pirate.xml"),
                ObjectModificationType.COMPLEX_TYPE);
    	
    	// WHEN
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	System.out.println("Delta:");
    	System.out.println(objectDelta.debugDump());
    	
    	// THEN
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	ContainerDelta<AssignmentType> assignmentDelta = objectDelta.findContainerDelta(UserType.F_ASSIGNMENT);
    	assertNotNull("No assignment delta", assignmentDelta);
    	Collection<PrismContainerValue<AssignmentType>> valuesToAdd = assignmentDelta.getValuesToAdd();
    	assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
    	PrismContainerValue<AssignmentType> assignmentVal = valuesToAdd.iterator().next();
    	assertNotNull("Null value in protectedStringDelta", assignmentVal);
    	
    	PrismReference targetRef = assignmentVal.findReference(AssignmentType.F_TARGET_REF);
    	assertNotNull("No targetRef in assignment", targetRef);
    	PrismReferenceValue targetRefVal = targetRef.getValue();
    	assertNotNull("No targetRef value in assignment", targetRefVal);
    	assertEquals("Wrong OID in targetRef value", "12345678-d34d-b33f-f00d-987987987988", targetRefVal.getOid());
    	assertEquals("Wrong type in targetRef value", RoleType.COMPLEX_TYPE, targetRefVal.getTargetType());
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
    	
    	objectDelta.assertDefinitions("delta before test");
    	user.assertDefinitions("user before test");
    	
    	// apply to user
    	objectDelta.applyTo(user);
    	
    	objectDelta.assertDefinitions("delta after test");
    	user.assertDefinitions("user after test");

    	// TODO
    }
    
    @Test
    public void testAccountRefDelta() throws Exception {
    	System.out.println("===[ testAccountRefDelta ]====");
    	
    	// GIVEN
    	ObjectModificationType objectChange = new ObjectModificationType();
        objectChange.setOid("12345");
        ItemDeltaType modificationDeleteAccountRef = new ItemDeltaType();
        modificationDeleteAccountRef.setModificationType(ModificationTypeType.DELETE);
        RawType modificationValue = new RawType();
        ObjectReferenceType accountRefToDelete = new ObjectReferenceType();
        accountRefToDelete.setOid("54321");
        JAXBElement<ObjectReferenceType> accountRefToDeleteElement = new JAXBElement<ObjectReferenceType>(UserType.F_LINK_REF, ObjectReferenceType.class, accountRefToDelete);
        modificationValue.getContent().add(accountRefToDeleteElement);
        modificationDeleteAccountRef.getValue().add(modificationValue);
        objectChange.getItemDelta().add(modificationDeleteAccountRef);
        ItemPathType itemPathType = new ItemPathType();
        itemPathType.getContent().add(UserType.F_LINK_REF);
        modificationDeleteAccountRef.setPath(itemPathType);
        
        PrismObjectDefinition<UserType> objDef = PrismTestUtil.getObjectDefinition(UserType.class);
        
		// WHEN
        Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(objectChange, objDef);
        
        // THEN
        assertNotNull("Null modifications", modifications);
        assertFalse("Empty modifications", modifications.isEmpty());
        // TODO: more asserts
    }
    
    @Test
    public void testProtectedStringObjectDelta() throws Exception {
    	System.out.println("===[ testProtectedStringObjectDelta ]====");

    	// GIVEN
    	ItemPath path = CREDENTIALS_PASSWORD_VALUE_PATH;
    	ProtectedStringType protectedString = new ProtectedStringType();
    	protectedString.setClearValue("abrakadabra");
    	ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, "12345",
    			path, PrismTestUtil.getPrismContext(), protectedString);

    	System.out.println("ObjectDelta");
    	System.out.println(objectDelta.debugDump());

    	// WHEN
    	ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

    	// THEN
    	System.out.println("ObjectDeltaType (XML)");
    	System.out.println(JaxbTestUtil.marshalWrap(objectDeltaType));
    	
    	assertEquals("Wrong changetype", ChangeTypeType.MODIFY, objectDeltaType.getChangeType());
    	assertEquals("Wrong OID", "12345", objectDeltaType.getOid());
    	List<ItemDeltaType> modifications = objectDeltaType.getItemDelta();
    	assertNotNull("null modifications", modifications);
    	assertEquals("Wrong number of modifications", 1, modifications.size());
    	ItemDeltaType mod1 = modifications.iterator().next();
    	assertEquals("Wrong mod type", ModificationTypeType.REPLACE, mod1.getModificationType());
//    	XPathHolder xpath = new XPathHolder(mod1.getPath());
    	ItemPathType itemPathType = mod1.getPath();
    	assertNotNull("Wrong path (must not be null)", itemPathType);
    	assertEquals("Wrong path", path, itemPathType.getItemPath());
    	List<RawType> valueElements = mod1.getValue();
    	assertEquals("Wrong number of value elements", 1, valueElements.size());
    	RawType val = valueElements.get(0);
        MapXNode valXNode = (MapXNode) val.serializeToXNode();
        PrimitiveXNode clearValueNode = (PrimitiveXNode) valXNode.get(ProtectedStringType.F_CLEAR_VALUE);
        val.getParsedValue(null, null);
//        System.out.println("clear value " + clearValueNode);
        assertEquals("Wrong element value", protectedString.getClearValue(), clearValueNode.getParsedValue(DOMUtil.XSD_STRING));
//    	List<Object> values = val.getContent();
//    	assertEquals("Wrong number of values", 1, values.size());
//    	JAXBElement<ProtectedStringType> valueElement = (JAXBElement<ProtectedStringType>)values.iterator().next();
////    	assertEquals("Wrong element name", PasswordType.F_VALUE, valueElement.getName());
//    	assertEquals("Wrong element value", protectedString, valueElement.getValue());
    }
    
    @Test
    public void testObjectDeltaRoundtrip() throws Exception {
    	System.out.println("===[ testObjectDeltaRoundtrip ]====");

    	// GIVEN
    	final String OID = "13235545";
    	final String VALUE = "Very Costly Center";
    	ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, OID,
    			UserType.F_COST_CENTER, PrismTestUtil.getPrismContext(), VALUE);

    	System.out.println("ObjectDelta");
    	System.out.println(objectDelta.debugDump());

    	// WHEN
    	ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

    	// THEN
    	System.out.println("ObjectDeltaType (XML)");
//    	System.out.println(PrismTestUtil.marshalWrap(objectDeltaType));
    	
    	assertEquals("Wrong changetype", ChangeTypeType.MODIFY, objectDeltaType.getChangeType());
    	assertEquals("Wrong OID", OID, objectDeltaType.getOid());
    	List<ItemDeltaType> modifications = objectDeltaType.getItemDelta();
    	assertNotNull("null modifications", modifications);
    	assertEquals("Wrong number of modifications", 1, modifications.size());
    	ItemDeltaType mod1 = modifications.iterator().next();
    	assertEquals("Wrong mod type", ModificationTypeType.REPLACE, mod1.getModificationType());
//    	XPathHolder xpath = new XPathHolder(mod1.getPath());
    	ItemPathType itemPathType = mod1.getPath();
    	assertNotNull("Wrong path (must not be null)", itemPathType);
//    	assertTrue("Wrong path: "+itemPathType, itemPathType.getItemPath().isEmpty());
    	assertEquals("Wrong path", itemPathType.getItemPath(), new ItemPath(UserType.F_COST_CENTER));
    	List<RawType> valueElements = mod1.getValue();
    	assertEquals("Wrong number of value elements", 1, valueElements.size());
    	RawType rawValue = valueElements.get(0);
    	List<Object> values = rawValue.getContent();
    	assertEquals("Wrong number of value elements", 1, values.size());
//    	System.out.println("value elements: " + valueElements);
    	String valueElement = (String) values.iterator().next();
//    	assertEquals("Wrong element name", ItemDeltaType.F_VALUE, DOMUtil.getQName(valueElement));
    	assertEquals("Wrong element value", VALUE, valueElement);
    	
    	// WHEN
    	ObjectDelta<Objectable> objectDeltaRoundtrip = DeltaConvertor.createObjectDelta(objectDeltaType, PrismTestUtil.getPrismContext());
    	
    	// THEN
    	System.out.println("ObjectDelta (roundtrip)");
    	System.out.println(objectDelta.debugDump());
    	
    	assertTrue("Roundtrip not equals", objectDelta.equals(objectDeltaRoundtrip));
    	
    	// TODO: more checks
    }

    @Test(enabled = false)
    public void testTaskExtensionDeleteDelta() throws Exception {
    	System.out.println("===[ testTaskExtensionDeleteDelta ]====");

    	// GIVEN
        PrismObject oldTask = PrismTestUtil.parseObject(
                new File(TEST_DIR, "task-old.xml"));
        PrismObject newTask = PrismTestUtil.parseObject(
                new File(TEST_DIR, "task-new.xml"));

        ObjectDelta<TaskType> delta = oldTask.diff(newTask, true, true);
        System.out.println("Delta:");
        System.out.println(delta.debugDump());

        final QName CUSTOM_OBJECT = new QName("http://delta.example.com", "object");

        PrismContext context = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

        // WHEN
        ObjectDeltaType xmlDelta = DeltaConvertor.toObjectDeltaType(delta);

        // THEN
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        String result = jaxbProcessor.marshalElementToString(new JAXBElement<Object>(CUSTOM_OBJECT,
                Object.class, xmlDelta), properties);
        assertNotNull(result);
    }
    
    @Test
    public void testItemDeltaReplace() throws Exception {
    	System.out.println("===[ testItemDeltaReplace ]====");

    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserDefinition();
    	PropertyDelta<String> deltaBefore = PropertyDelta.createReplaceEmptyDelta(userDef, UserType.F_COST_CENTER);
    	deltaBefore.setValueToReplace(new PrismPropertyValue<String>("foo"));
    	
		// WHEN
    	Collection<ItemDeltaType> itemDeltaTypes = DeltaConvertor.toPropertyModificationTypes(deltaBefore);
    	
    	// THEN
    	System.out.println("Serialized");
    	System.out.println(itemDeltaTypes);
    	
    	// WHEN
    	ItemDelta<PrismValue> deltaAfter = DeltaConvertor.createItemDelta(itemDeltaTypes.iterator().next(), userDef);
    	
    	// THEN
    	System.out.println("Parsed");
    	System.out.println(deltaAfter.debugDump());

    	assertEquals("Deltas do not match", deltaBefore, deltaAfter);
    }

    @Test
    public void testItemDeltaReplaceEmptyString() throws Exception {
    	System.out.println("===[ testItemDeltaReplaceEmptyString ]====");

    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserDefinition();
    	PropertyDelta<String> deltaBefore = PropertyDelta.createReplaceEmptyDelta(userDef, UserType.F_COST_CENTER);
//    	deltaBefore.setValueToReplace(new PrismPropertyValue<String>(""));
    	
		// WHEN
    	Collection<ItemDeltaType> itemDeltaTypes = DeltaConvertor.toPropertyModificationTypes(deltaBefore);
    	
    	// THEN
    	System.out.println("Serialized");
    	System.out.println(itemDeltaTypes);
    	
    	// WHEN
    	ItemDelta<PrismValue> deltaAfter = DeltaConvertor.createItemDelta(itemDeltaTypes.iterator().next(), userDef);

    	// THEN
    	System.out.println("Parsed");
    	System.out.println(deltaAfter.debugDump());
    	
    	assertEquals("Deltas do not match", deltaBefore, deltaAfter);
    }

    @Test
    public void testItemDeltaReplaceNil() throws Exception {
    	System.out.println("===[ testItemDeltaReplaceNil ]====");

    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserDefinition();
    	PropertyDelta<String> deltaBefore = PropertyDelta.createReplaceEmptyDelta(userDef, UserType.F_COST_CENTER);
    	// The delta remains empty
    	
		// WHEN
    	Collection<ItemDeltaType> itemDeltaTypes = DeltaConvertor.toPropertyModificationTypes(deltaBefore);
    	
    	// THEN
    	System.out.println("Serialized");
    	System.out.println(itemDeltaTypes);
    	ItemDeltaType itemDeltaType = itemDeltaTypes.iterator().next();
    	String xml = JaxbTestUtil.getInstance().marshalObjectToString(itemDeltaType, new QName("wherever","whatever"));
    	System.out.println(xml);
    	
    	// WHEN
    	ItemDelta<PrismValue> deltaAfter = DeltaConvertor.createItemDelta(itemDeltaType, userDef);
    	
    	// THEN
    	System.out.println("Parsed");
    	System.out.println(deltaAfter.debugDump());
    	
    	assertEquals("Deltas do not match", deltaBefore, deltaAfter);
    }

    @Test
    public void testModifyInducement() throws Exception {
        System.out.println("===[ testModifyInducement ]====");

        ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(new File(TEST_DIR, "role-modify-inducement.xml"),
                ObjectModificationType.COMPLEX_TYPE);

        // WHEN
        ObjectDelta<RoleType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, RoleType.class,
                PrismTestUtil.getPrismContext());

        System.out.println("Delta:");
        System.out.println(objectDelta.debugDump());

        // THEN
        assertNotNull("No object delta", objectDelta);
        objectDelta.checkConsistence();
        assertEquals("Wrong OID", "00000000-8888-6666-0000-100000000005", objectDelta.getOid());
        ReferenceDelta targetRefDelta = objectDelta.findReferenceModification(new ItemPath(
                new NameItemPathSegment(RoleType.F_INDUCEMENT),
                new IdItemPathSegment(5L),
                new NameItemPathSegment(AssignmentType.F_TARGET_REF)));
        assertNotNull("No targetRef delta", targetRefDelta);
        Collection<PrismReferenceValue> valuesToAdd = targetRefDelta.getValuesToAdd();
        assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
        PrismReferenceValue targetRefVal = valuesToAdd.iterator().next();
        assertNotNull("Null value in targetRef delta", targetRefVal);

        assertEquals("wrong OID in targetRef", "12345678-d34d-b33f-f00d-987987987987", targetRefVal.getOid());
        assertEquals("wrong target type in targetRef", RoleType.COMPLEX_TYPE, targetRefVal.getTargetType());
    }

}
