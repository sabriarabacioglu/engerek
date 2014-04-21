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

package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * Test delta operation on real midpoint schema. Similar to TestDelta in prism, but this is using the
 * real thing and not just testing schema.
 * 
 * @author Radovan Semancik
 */
public class TestSchemaDelta extends AbstractSchemaTest {
    
    @Test
    public void testAssignmentSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testAssignmentSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);
    	
		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);

		//Delta
    	PrismContainerValue<AssignmentType> assignmentValue = new PrismContainerValue<AssignmentType>();
    	// The value id is null
    	assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "jamalalicha patlama paprtala");
    	
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_JACK_OID, 
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue);
				
		// WHEN
        userDelta.applyTo(user);
        
        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong OID", USER_JACK_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 2, assignment.size());
    }

    @Test
    public void testAddInducementConstructionSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testAddInducementConstructionSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);
    	
		// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
    	PrismContainerValue<AssignmentType> inducementValue = new PrismContainerValue<AssignmentType>();
    	// The value id is null
    	inducementValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "jamalalicha patlama paprtala");
    	
		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, ROLE_CONSTRUCTION_OID, 
				RoleType.F_INDUCEMENT, PrismTestUtil.getPrismContext(), inducementValue);
				
		// WHEN
        roleDelta.applyTo(role);
        
        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull("No inducement", assignment);
        assertEquals("Unexpected number of inducement values", 2, assignment.size());
    }
    
    @Test
    public void testDeleteInducementValidIdSameValueApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementValidIdSameValueApplyToObject";
    	displayTestTile(TEST_NAME);
    	
    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(ROLE_CONSTRUCTION_RESOURCE_OID);
		construction.setResourceRef(resourceRef);
		AssignmentType inducement = new AssignmentType();
		inducement.setConstruction(construction);
		inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID, 
        		RoleType.F_INDUCEMENT, PrismTestUtil.getPrismContext(), inducement);
				
		// WHEN
		roleDelta.applyTo(role);
        
        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNull("Unexpected inducement", assignment);
    }

    @Test
    public void testDeleteInducementValidIdEmptyValueApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementValidIdEmptyValueApplyToObject";
    	displayTestTile(TEST_NAME);
    	
    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		AssignmentType inducement = new AssignmentType();
		inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID, 
        		RoleType.F_INDUCEMENT, PrismTestUtil.getPrismContext(), inducement);
				
		// WHEN
		roleDelta.applyTo(role);
        
        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNull("Unexpected inducement", assignment);
    }
    
    @Test
    public void testDeleteInducementValidIdEmptyValueApplyToObjectStatic() throws Exception {
    	final String TEST_NAME = "testDeleteInducementValidIdEmptyValueApplyToObjectStatic";
    	displayTestTile(TEST_NAME);
    	
    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);
		System.out.println("Role before delta application:");
        System.out.println(role.debugDump());

		//Delta
		AssignmentType inducement = new AssignmentType();
		inducement.setId(ROLE_CONSTRUCTION_INDUCEMENT_ID);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID, 
        		RoleType.F_INDUCEMENT, PrismTestUtil.getPrismContext(), inducement);
				
		// WHEN
        PropertyDelta.applyTo(roleDelta.getModifications(), role);
        
        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> assignment = role.findContainer(RoleType.F_INDUCEMENT);
        assertNull("Unexpected inducement", assignment);
    }

    @Test
    public void testDeleteInducementConstructionSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementConstructionSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);
    	
    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(ROLE_CONSTRUCTION_RESOURCE_OID);
        resourceRef.setType(ObjectTypes.RESOURCE.getTypeQName());
		construction.setResourceRef(resourceRef);
		// No container ID
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID, 
        		new ItemPath(
        				new NameItemPathSegment(RoleType.F_INDUCEMENT),
        				new IdItemPathSegment(ROLE_CONSTRUCTION_INDUCEMENT_ID),
        				new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
        		PrismTestUtil.getPrismContext(), construction);
				
		// WHEN
		roleDelta.applyTo(role);
        
        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> inducementContainer = role.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull("No inducement", inducementContainer);
        assertEquals("Unexpected number of inducement values", 1, inducementContainer.size());
        PrismContainerValue<AssignmentType> inducementValue = inducementContainer.getValues().iterator().next();
        AssignmentType inducement = inducementValue.asContainerable();
        ConstructionType constructionAfter = inducement.getConstruction();
        // construction should be gone (the error is that it is empty and not gone)
        assertNull("Construction is not gone", constructionAfter);
        
    }
    
    @Test
    public void testDeleteInducementActivationSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteInducementActivationSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);
    	
    	// GIVEN
		PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_CONSTRUCTION_FILE);

		//Delta
		ActivationType activationType = new ActivationType();
		activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
		// No container ID
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, ROLE_CONSTRUCTION_OID, 
        		new ItemPath(
        				new NameItemPathSegment(RoleType.F_INDUCEMENT),
        				new IdItemPathSegment(ROLE_CONSTRUCTION_INDUCEMENT_ID),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
        		PrismTestUtil.getPrismContext(), activationType);
				
		// WHEN
		roleDelta.applyTo(role);
        
        // THEN
        System.out.println("Role after delta application:");
        System.out.println(role.debugDump());
        assertEquals("Wrong OID", ROLE_CONSTRUCTION_OID, role.getOid());
        PrismAsserts.assertPropertyValue(role, UserType.F_NAME, PrismTestUtil.createPolyString("Construction"));
        PrismContainer<AssignmentType> inducementContainer = role.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull("No inducement", inducementContainer);
        assertEquals("Unexpected number of inducement values", 1, inducementContainer.size());
        PrismContainerValue<AssignmentType> inducementValue = inducementContainer.getValues().iterator().next();
        AssignmentType inducement = inducementValue.asContainerable();
        ActivationType activation = inducement.getActivation();
        // activation should be gone (the error is that it is empty and not gone)
        assertNull("Activation is not gone", activation);
    }
    
    @Test
    public void testDeleteUserAssignmentActivationSameNullIdApplyToObject() throws Exception {
    	final String TEST_NAME = "testDeleteUserAssignmentActivationSameNullIdApplyToObject";
    	displayTestTile(TEST_NAME);
    	
    	// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);

		//Delta
		ActivationType activationType = new ActivationType();
		activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
		// No container ID
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_JACK_OID, 
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(USER_JACK_ASSIGNMENT_ID),
        				new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
        		PrismTestUtil.getPrismContext(), activationType);
				
		// WHEN
		userDelta.applyTo(user);
        
        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong OID", USER_JACK_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_NAME));
        PrismContainer<AssignmentType> assignmentContainer = user.findContainer(RoleType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignmentContainer);
        assertEquals("Unexpected number of assignment values", 1, assignmentContainer.size());
        PrismContainerValue<AssignmentType> assignmentValue = assignmentContainer.getValues().iterator().next();
        AssignmentType assignment = assignmentValue.asContainerable();
        ActivationType assignmentActivation = assignment.getActivation();
        // activation should be gone (the error is that it is empty and not gone)
        assertNull("Assignment activation is not gone", assignmentActivation);
        
        ActivationType activation = user.asObjectable().getActivation();
        assertNotNull("Activation missing", activation);
        assertEquals("Wrong activation administrativeStatus", ActivationStatusType.ENABLED, activation.getAdministrativeStatus());
    }

}
