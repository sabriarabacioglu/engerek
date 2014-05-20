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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This is testing the DEPRECATED functions of model API. It should be removed once the functions are phased out.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelCrudService extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/crud");
	public static final File TEST_CONTRACT_DIR = new File("src/test/resources/contract");

	public static final File RESOURCE_MAROON_FILE = new File(TEST_DIR, "resource-dummy-maroon.xml");
	public static final String RESOURCE_MAROON_OID = "10000000-0000-0000-0000-00000000e104";
	
	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";
	
	private static String accountOid;
	
	@Autowired(required = true)
	protected ModelCrudService modelCrudService;
			
	@Test
    public void test050AddResource() throws Exception {
		final String TEST_NAME = "test050AddResource";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Make sure that plain JAXB parser is used ... this is what a webservice stack would do
        ResourceType resourceType = prismContext.getJaxbDomHack().unmarshalObject(new FileInputStream(RESOURCE_MAROON_FILE));
        
        // WHEN
        PrismObject<ResourceType> object = resourceType.asPrismObject();
		prismContext.adopt(resourceType);
		modelCrudService.addObject(object, null, task, result);
        		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		// Make sure the resource has t:norm part of polystring name
		PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_MAROON_OID, null, task, result);
		assertEquals("Wrong orig in resource name", "Dummy Resource Maroon", resourceAfter.asObjectable().getName().getOrig());
		assertEquals("Wrong norm in resource name", "dummy resource maroon", resourceAfter.asObjectable().getName().getNorm());
	}
	
	@Test
    public void test100ModifyUserAddAccount() throws Exception {
        TestUtil.displayTestTile(this, "test100ModifyUserAddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test100ModifyUserAddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
	}
		
	@Test
    public void test119ModifyUserDeleteAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            IOException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        TestUtil.displayTestTile(this, "test119ModifyUserDeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test119ModifyUserDeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountOid);
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
        
		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        	AssertJUnit.fail("Shadow "+accountOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
	}
	
	@Test
    public void test120AddAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            IOException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        TestUtil.displayTestTile(this, "test120AddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test120AddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
		// WHEN
        accountOid = modelCrudService.addObject(account, null, task, result);
		
		// THEN
		// Check accountRef (should be none)
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
	}
	
	@Test
    public void test121ModifyUserAddAccountRef() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        TestUtil.displayTestTile(this, "test121ModifyUserAddAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test121ModifyUserAddAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount("jack", "Jack Sparrow", true);
	}


	
	@Test
    public void test128ModifyUserDeleteAccountRef() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            IOException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        TestUtil.displayTestTile(this, "test128ModifyUserDeleteAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test128ModifyUserDeleteAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        
        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountOid);
		modifications.add(accountDelta);
        
		// WHEN
		modelCrudService.modifyObject(UserType.class, USER_JACK_OID, modifications , null, task, result);
		
		// THEN
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);
		        
		// Check shadow (if it is unchanged)
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account (if it is unchanged)
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        // Check account in dummy resource (if it is unchanged)
        assertDummyAccount("jack", "Jack Sparrow", true);
	}
	
	@Test
    public void test129DeleteAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException, ConsistencyViolationException {
        TestUtil.displayTestTile(this, "test129DeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test129DeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
		// WHEN
        modelCrudService.deleteObject(ShadowType.class, accountOid, null, task, result);
		
		// THEN
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
		// Check is shadow is gone
        assertNoShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");
	}
	
	@Test
    public void test150AddUserBlackbeardWithAccount() throws Exception {
        TestUtil.displayTestTile(this, "test150AddUserBlackbeardWithAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test150AddUserBlackbeardWithAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_CONTRACT_DIR, "user-blackbeard-account-dummy.xml"));
                
		// WHEN
        modelCrudService.addObject(user , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
        
        // Check account in dummy resource
        assertDummyAccount("blackbeard", "Edward Teach", true);
	}

	
	@Test
    public void test210AddUserMorganWithAssignment() throws Exception {
        TestUtil.displayTestTile(this, "test210AddUserMorganWithAssignment");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelCrudService.class.getName() + ".test210AddUserMorganWithAssignment");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_CONTRACT_DIR, "user-morgan-assignment-dummy.xml"));
                
		// WHEN
        modelCrudService.addObject(user , null, task, result);
		
		// THEN
		// Check accountRef
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "morgan");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
        
        // Check account in dummy resource
        assertDummyAccount("morgan", "Sir Henry Morgan", true);
	}

}
