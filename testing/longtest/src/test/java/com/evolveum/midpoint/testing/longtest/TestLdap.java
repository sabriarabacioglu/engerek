package com.evolveum.midpoint.testing.longtest;
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


import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdap extends AbstractModelIntegrationTest {
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_NAME + "/system-configuration.xml";
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final String USER_ADMINISTRATOR_FILENAME = COMMON_DIR_NAME + "/user-administrator.xml";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";
	
	protected static final String ROLE_SUPERUSER_FILENAME = COMMON_DIR_NAME + "/role-superuser.xml";
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
	
	protected static final String RESOURCE_OPENDJ_FILENAME = COMMON_DIR_NAME + "/resource-opendj.xml";
    protected static final String RESOURCE_OPENDJ_NAME = "Localhost OpenDJ";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	
	private static final String USER_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_CHARLES_NAME = "charles";
	private static final int NUM_LDAP_ENTRIES = 1000;
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	
    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        openDJController.stop();
    }

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		// Users
		PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, RoleType.class, initResult);
		login(userAdministrator);
		
		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILENAME, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
	}
    
	@Test
    public void test400RenameLeChuckConflicting() throws Exception {
		final String TEST_NAME = "test400RenameLeChuckConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userLechuck = createUser(USER_LECHUCK_NAME, "LeChuck", true);
        userLechuck.asObjectable().getAssignment().add(createAccountAssignment(RESOURCE_OPENDJ_OID, null));
        userLechuck.asObjectable().setFamilyName(PrismTestUtil.createPolyStringType("LeChuck"));
        addObject(userLechuck);
        String userLechuckOid = userLechuck.getOid();
        
        PrismObject<ShadowType> accountCharles = createAccount(resourceOpenDj, toDn(ACCOUNT_CHARLES_NAME), true);
        addAttributeToShadow(accountCharles, resourceOpenDj, "sn", "Charles");
        addAttributeToShadow(accountCharles, resourceOpenDj, "cn", "Charles L. Charles");
        addObject(accountCharles);
        
        // preconditions
        assertOpenDjAccount(ACCOUNT_LECHUCK_NAME, "LeChuck", true);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME, "Charles L. Charles", true);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(userLechuckOid, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString(ACCOUNT_CHARLES_NAME));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME, "Charles L. Charles", true);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME + "1", "LeChuck", true);
        assertNoOpenDjAccount(ACCOUNT_LECHUCK_NAME);
	}
	
	@Test
    public void test800BigImport() throws Exception {
		final String TEST_NAME = "test800BigImport";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        
        long ldapPopStart = System.currentTimeMillis();
        
        for(int i=0; i < NUM_LDAP_ENTRIES; i++) {
        	String name = "user"+i;
        	Entry entry = createEntry("u"+i, name);
        	openDJController.addEntry(entry);
        }
        
        long ldapPopEnd = System.currentTimeMillis();
        
        display("Loaded "+NUM_LDAP_ENTRIES+" LDAP entries in "+((ldapPopEnd-ldapPopStart)/1000)+" seconds");
        
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_OPENDJ_OID, 
        		new QName(RESOURCE_OPENDJ_NAMESPACE, "AccountObjectClass"), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", NUM_LDAP_ENTRIES + 6, userCount);
	}
	
	private Entry createEntry(String uid, String name) throws IOException, LDIFException {
		StringBuilder sb = new StringBuilder();
		String dn = "uid="+uid+","+openDJController.getSuffixPeople();
		sb.append("dn: ").append(dn).append("\n");
		sb.append("objectClass: inetOrgPerson\n");
		sb.append("uid: ").append(uid).append("\n");
		sb.append("cn: ").append(name).append("\n");
		sb.append("sn: ").append(name).append("\n");
		LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb.toString(), "utf-8"));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry ldifEntry = ldifReader.readEntry();
		return ldifEntry;
	}
	
	private String toDn(String username) {
		return "uid="+username+","+OPENDJ_PEOPLE_SUFFIX;
	}
}
