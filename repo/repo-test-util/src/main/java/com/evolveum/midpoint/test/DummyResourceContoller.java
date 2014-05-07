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
package com.evolveum.midpoint.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.ldap.AbstractResourceController;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class DummyResourceContoller extends AbstractResourceController {
	
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME = "fullname";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME = "title";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME = "location";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME = "loot";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME = "treasure";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME = "ship";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME = "weapon";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME = "drink";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME = "quote";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME = "gossip";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME = "water";
    
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME = "givenName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME = "sn";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME = "sAMAccountName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME = "userPrincipalName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME = "mail";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME = "userSharedFolderOther";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_DEPARTMENT_NAME = "department";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_FACSIMILE_TELEPHONE_NUMBER_NAME = "facsimileTelephoneNumber";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME = "telephoneNumber";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_MOBILE_NAME = "mobile";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_IP_PHONE_NAME = "ipPhone";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_PHYSICAL_DELIVERY_OFFICE_NAME_NAME = "physicalDeliveryOfficeName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_DESCRIPTION_NAME = "description";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_ACCOUNT_EXPIRES_NAME = "accountExpires";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_GROUPS_NAME = "groups";
    
    
    public static final String DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME = "members";
	public static final String DUMMY_GROUP_ATTRIBUTE_DESCRIPTION = "description";
    public static final String DUMMY_GROUP_ATTRIBUTE_CC = "cc";
	
	public static final String DUMMY_ENTITLEMENT_GROUP_NAME = "group";
	public static final String DUMMY_ENTITLEMENT_PRIVILEGE_NAME = "priv";
	
	public static final String CONNECTOR_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
	public static final String CONNECTOR_DUMMY_USELESS_STRING_NAME = "uselessString";
	public static final QName CONNECTOR_DUMMY_USELESS_STRING_QNAME = new QName(CONNECTOR_DUMMY_NS, CONNECTOR_DUMMY_USELESS_STRING_NAME);
	
	private DummyResource dummyResource;
	private boolean isExtendedSchema = false;
	private String instanceName;

	
	public static DummyResourceContoller create(String instanceName) {
		return create(instanceName, null);
	}

	public static DummyResourceContoller create(String instanceName, PrismObject<ResourceType> resource) {
		DummyResourceContoller ctl = new DummyResourceContoller();
		
		ctl.instanceName = instanceName;
		ctl.dummyResource = DummyResource.getInstance(instanceName);
		ctl.dummyResource.reset();
		
		ctl.resource = resource;
		
		return ctl;
	}
	
	public DummyResource getDummyResource() {
		return dummyResource;
	}

	public String getName() {
		return instanceName;
	}

	public void populateWithDefaultSchema() {
		dummyResource.populateWithDefaultSchema();
	}

	/**
	 * Extend schema in piratey fashion. Arr! This is used in many tests. Lots of attributes, various combination of types, etc. 
	 */
	public void extendSchemaPirate() throws ConnectException, FileNotFoundException {
		populateWithDefaultSchema();
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, String.class, false, false);
		DummyAttributeDefinition lootAttrDef =  addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, Integer.class, false, false);
		lootAttrDef.setReturnedByDefault(false);
		DummyAttributeDefinition treasureAttrDef = addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME, String.class, false, false);
		treasureAttrDef.setReturnedByDefault(false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, String.class, false, false);
		
		DummyObjectClass groupObjectClass = dummyResource.getGroupObjectClass();		
		addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, String.class, false, false);
        addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_CC, String.class, false, false);
		
		isExtendedSchema = true;
	}
	
	/**
	 * Extend dummy schema to look like AD
	 */
	public void extendSchemaAd() throws ConnectException, FileNotFoundException {
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_DEPARTMENT_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_FACSIMILE_TELEPHONE_NUMBER_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_MOBILE_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_IP_PHONE_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_PHYSICAL_DELIVERY_OFFICE_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_DESCRIPTION_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_ACCOUNT_EXPIRES_NAME, Long.class, false, false);
		// This should in fact be icfs:groups but this is OK for now
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_GROUPS_NAME, String.class, false, true);
		
		isExtendedSchema = true;
	}
	
	public DummyAttributeDefinition addAttrDef(DummyObjectClass accountObjectClass, String attrName, Class<?> type, boolean isRequired, boolean isMulti) {
		DummyAttributeDefinition attrDef = new DummyAttributeDefinition(attrName, type, isRequired, isMulti);
		accountObjectClass.add(attrDef);
		return attrDef;
	}
	
	public QName getAttributeFullnameQName() {
		return new QName(getNamespace(), DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
	}

	public ItemPath getAttributeFullnamePath() {
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeFullnameQName());
	}
	
	public QName getAttributeWeaponQName() {
		assertExtendedSchema();
		return new QName(getNamespace(), DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
	}

	public ItemPath getAttributeWeaponPath() {
		assertExtendedSchema();
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeWeaponQName());
	}
	
	private void assertExtendedSchema() {
		assert isExtendedSchema : "Resource "+resource+" does not have extended schema yet an extedned attribute was requested";
	}
	
	public void assertDummyResourceSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		IntegrationTestTools.assertIcfResourceSchemaSanity(resourceSchema, resourceType);
		
		// ACCOUNT
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("No ACCOUNT kind definition", accountDef);
		
		ResourceAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		// GROUP
		ObjectClassComplexTypeDefinition groupObjectClass = resourceSchema.findObjectClassDefinition(SchemaTestConstants.GROUP_OBJECT_CLASS_LOCAL_NAME);
		assertNotNull("No group objectClass", groupObjectClass);
		
		ResourceAttributeDefinition membersDef = groupObjectClass.findAttributeDefinition(DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME);
		assertNotNull("No definition for members", membersDef);
		assertEquals("Wrong maxOccurs", -1, membersDef.getMaxOccurs());
		assertEquals("Wrong minOccurs", 0, membersDef.getMinOccurs());
		assertTrue("No members create", membersDef.canAdd());
		assertTrue("No members update", membersDef.canModify());
		assertTrue("No members read", membersDef.canRead());
	}
	
	public void assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema, ResourceType resourceType) {
		assertDummyResourceSchemaSanity(resourceSchema, resourceType);
		
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);		
		assertEquals("Unexpected number of defnitions", 16, accountDef.getDefinitions().size());
		ResourceAttributeDefinition treasureDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME);
		assertFalse("Treasure IS returned by default and should not be", treasureDef.isReturnedByDefault());
	}

	public void assertRefinedSchemaSanity(RefinedResourceSchema refinedSchema) {
		
		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canAdd());
		assertFalse("UID has update",uidDef.canModify());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canAdd());
		assertTrue("No NAME update",nameDef.canModify());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS,"password")));
		
	}

	public void addAccount(String userId, String fullName) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException {
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		dummyResource.addAccount(account);
	}

	public void addAccount(String userId, String fullName, String location) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException {
		assertExtendedSchema();
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, location);
		dummyResource.addAccount(account);
	}

}
