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
package com.evolveum.midpoint.test.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class MidPointAsserts {
	
	public static <F extends FocusType> void assertAssigned(PrismObject<F> user, String targetOid, QName refType) {
		F userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					if (targetOid.equals(targetRef.getOid())) {
						return;
					}
				}
			}
		}
		AssertJUnit.fail(user + " does not have assigned "+refType.getLocalPart()+" "+targetOid);
	}
	
	public static void assertAssigned(PrismObject<UserType> user, String targetOid, QName refType, QName relation) {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					if (targetOid.equals(targetRef.getOid()) &&
							MiscSchemaUtil.compareRelation(targetRef.getRelation(), relation)) {
						return;
					}
				}
			}
		}
		AssertJUnit.fail(user + " does not have assigned "+refType.getLocalPart()+" "+targetOid+ ", relation "+relation);
	}
	
	public static <F extends FocusType> void assertNotAssigned(PrismObject<F> user, String targetOid, QName refType) {
		F userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					if (targetOid.equals(targetRef.getOid())) {
						AssertJUnit.fail(user + " does have assigned "+refType.getLocalPart()+" "+targetOid+" while not expecting it");
					}
				}
			}
		}
	}
	
	public static <F extends FocusType> void assertAssignments(PrismObject<F> user, int expectedNumber) {
		F userType = user.asObjectable();
		assertEquals("Unexepected number of assignments in "+user+": "+userType.getAssignment(), expectedNumber, userType.getAssignment().size());
	}
	
	public static <F extends FocusType> void assertAssignments(PrismObject<F> user, Class expectedType, int expectedNumber) {
		F userType = user.asObjectable();
		int actualAssignments = 0;
		List<AssignmentType> assignments = userType.getAssignment();
		for (AssignmentType assignment: assignments) {
			ObjectReferenceType targetRef = assignment.getTargetRef();
			if (targetRef != null) {
				QName type = targetRef.getType();
				if (type != null) {
					Class<? extends ObjectType> assignmentTargetClass = ObjectTypes.getObjectTypeFromTypeQName(type).getClassDefinition();
					if (expectedType.isAssignableFrom(assignmentTargetClass)) {
						actualAssignments++;
					}
				}
			}
		}
		assertEquals("Unexepected number of assignments of type "+expectedType+" in "+user+": "+userType.getAssignment(), expectedNumber, actualAssignments);
	}
	
	public static <F extends FocusType> void assertAssignedRole(PrismObject<F> user, String roleOid) {
		assertAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
	}
	
	public static <F extends FocusType> void assertNotAssignedRole(PrismObject<F> user, String roleOid) {
		assertNotAssigned(user, roleOid, RoleType.COMPLEX_TYPE);
	}
	
	public static void assertAssignedOrg(PrismObject<UserType> user, String orgOid) {
		assertAssigned(user, orgOid, OrgType.COMPLEX_TYPE);
	}
	
	public static void assertAssignedOrg(PrismObject<UserType> user, String orgOid, QName relation) {
		assertAssigned(user, orgOid, OrgType.COMPLEX_TYPE, relation);
	}
	
	public static <O extends ObjectType> void assertHasOrg(PrismObject<O> object, String orgOid) {
		for (ObjectReferenceType orgRef: object.asObjectable().getParentOrgRef()) {
			if (orgOid.equals(orgRef.getOid())) {
				return;
			}
		}
		AssertJUnit.fail(object + " does not have org " + orgOid);
	}
	
	public static <O extends ObjectType> void assertHasOrg(PrismObject<O> user, String orgOid, QName relation) {
		for (ObjectReferenceType orgRef: user.asObjectable().getParentOrgRef()) {
			if (orgOid.equals(orgRef.getOid()) &&
					MiscSchemaUtil.compareRelation(orgRef.getRelation(), relation)) {
				return;
			}
		}
		AssertJUnit.fail(user + " does not have org " + orgOid + ", relation "+relation);
	}
	
	public static <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user) {
		assertTrue(user + " does have orgs "+user.asObjectable().getParentOrgRef()+" while not expecting them", user.asObjectable().getParentOrgRef().isEmpty());
	}

	public static <O extends ObjectType> void assertHasOrgs(PrismObject<O> user, int expectedNumber) {
		O userType = user.asObjectable();
		assertEquals("Unexepected number of orgs in "+user+": "+userType.getParentOrgRef(), expectedNumber, userType.getParentOrgRef().size());
	}
	
    
    public static <O extends ObjectType> void assertVersionIncrease(PrismObject<O> objectOld, PrismObject<O> objectNew) {
		Long versionOld = parseVersion(objectOld);
		Long versionNew = parseVersion(objectNew);
		assertTrue("Version not increased (from "+versionOld+" to "+versionNew+")", versionOld < versionNew);
	}

    public static <O extends ObjectType> Long parseVersion(PrismObject<O> object) {
		String version = object.getVersion();
		if (version == null) {
			return null;
		}
		return Long.valueOf(version);
	}

	public static <O extends ObjectType> void assertVersion(PrismObject<O> object, int expectedVersion) {
		assertVersion(object, Integer.toString(expectedVersion));
	}
	
	public static <O extends ObjectType> void assertVersion(PrismObject<O> object, String expectedVersion) {
		assertEquals("Wrong version for "+object, expectedVersion, object.getVersion());
	}
	
	public static <O extends ObjectType> void assertOid(PrismObject<O> object, String expectedOid) {
		assertEquals("Wrong OID for "+object, expectedOid, object.getOid());
	}

	public static void assertContainsCaseIgnore(String message, Collection<String> actualValues, String expectedValue) {
		AssertJUnit.assertNotNull(message+", expected "+expectedValue+", got null", actualValues);
		for (String actualValue: actualValues) {
			if (StringUtils.equalsIgnoreCase(actualValue, expectedValue)) {
				return;
			}
		}
		AssertJUnit.fail(message+", expected "+expectedValue+", got "+actualValues);
	}
	
	public static void assertNotContainsCaseIgnore(String message, Collection<String> actualValues, String expectedValue) {
		if (actualValues == null) {
			return;
		}
		for (String actualValue: actualValues) {
			if (StringUtils.equalsIgnoreCase(actualValue, expectedValue)) {
				AssertJUnit.fail(message+", expected that value "+expectedValue+" will not be present but it is");
			}
		}
	}
}
