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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIteration extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/iteration");
	
	protected static final File USER_XAVIER_FILE = new File(TEST_DIR, "user-xavier.xml");
	protected static final String USER_XAVIER_OID = "c0c010c0-d34d-b33f-f00d-11111111aaa1";
	
	protected static final File RESOURCE_DUMMY_PINK_FILE = new File(TEST_DIR, "resource-dummy-pink.xml");
	protected static final String RESOURCE_DUMMY_PINK_OID = "10000000-0000-0000-0000-00000000a104";
	protected static final String RESOURCE_DUMMY_PINK_NAME = "pink";
	protected static final String RESOURCE_DUMMY_PINK_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-violet.xml");
	protected static final String RESOURCE_DUMMY_VIOLET_OID = "10000000-0000-0000-0000-00000000a204";
	protected static final String RESOURCE_DUMMY_VIOLET_NAME = "violet";
	protected static final String RESOURCE_DUMMY_VIOLET_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_DARK_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-dark-violet.xml");
	protected static final String RESOURCE_DUMMY_DARK_VIOLET_OID = "10000000-0000-0000-0000-0000000da204";
	protected static final String RESOURCE_DUMMY_DARK_VIOLET_NAME = "darkViolet";
	protected static final String RESOURCE_DUMMY_DARK_VIOLET_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_MAGENTA_FILE = new File(TEST_DIR, "resource-dummy-magenta.xml");
	protected static final String RESOURCE_DUMMY_MAGENTA_OID = "10000000-0000-0000-0000-00000000a304";
	protected static final String RESOURCE_DUMMY_MAGENTA_NAME = "magenta";
	protected static final String RESOURCE_DUMMY_MAGENTA_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_FILE = new File(TEST_DIR, "task-dumy-dark-violet-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID = "10000000-0000-0000-5555-555500da0204";
	
	protected static final File USER_TEMPLATE_ITERATION_FILE = new File(TEST_DIR, "user-template-iteration.xml");
	protected static final String USER_TEMPLATE_ITERATION_OID = "10000000-0000-0000-0000-0000000d0002";

	protected static final File USER_TEMPLATE_ITERATION_RANDOM_FILE = new File(TEST_DIR, "user-template-iteration-random.xml");
	protected static final String USER_TEMPLATE_ITERATION_RANDOM_OID = "10000000-0000-0000-0000-0000000d0002"; // SAME OID as USER_TEMPLATE_ITERATION
	
	private static final String USER_ANGELICA_NAME = "angelica";
	private static final String ACCOUNT_SPARROW_NAME = "sparrow";
	
	private static final String USER_DEWATT_NAME = "dewatt";
	private static final String ACCOUNT_DEWATT_NAME = "DeWatt";
	
	private static final String DESCRIPTION_RUM = "Where's the rum?";

	private static final String USER_JACK_RENAMED_NAME = "cptjack";

	private static final String ACCOUNT_LECHUCK_USERNAME = "lechuck";
	private static final String LECHUCK_FULLNAME = "LeChuck";
	private static final String ACCOUNT_CHARLES_USERNAME = "charles";
	private static final String ACCOUNT_SHINETOP_USERNAME = "shinetop";
	private static final String CHUCKIE_FULLNAME = "Chuckie";
	
	private static final String ACCOUNT_MATUSALEM_USERNAME = "matusalem";
	private static final String ACCOUNT_DIPLOMATICO_USERNAME = "diplomatico";
	private static final String ACCOUNT_MILLONARIO_USERNAME = "millonario";
	private static final String RUM_FULLNAME = "Rum";
	private static final String RON_FULLNAME = "Ron";
	
	protected static DummyResource dummyResourcePink;
	protected static DummyResourceContoller dummyResourceCtlPink;
	protected ResourceType resourceDummyPinkType;
	protected PrismObject<ResourceType> resourceDummyPink;
	
	protected static DummyResource dummyResourceViolet;
	protected static DummyResourceContoller dummyResourceCtlViolet;
	protected ResourceType resourceDummyVioletType;
	protected PrismObject<ResourceType> resourceDummyViolet;
	
	protected static DummyResource dummyResourceDarkViolet;
	protected static DummyResourceContoller dummyResourceCtlDarkViolet;
	protected ResourceType resourceDummyDarkVioletType;
	protected PrismObject<ResourceType> resourceDummyDarkViolet;
	
	protected static DummyResource dummyResourceMagenta;
	protected static DummyResourceContoller dummyResourceCtlMagenta;
	protected ResourceType resourceDummyMagentaType;
	protected PrismObject<ResourceType> resourceDummyMagenta;
	
	String iterationTokenDiplomatico;
	String iterationTokenMillonario;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlPink = DummyResourceContoller.create(RESOURCE_DUMMY_PINK_NAME, resourceDummyPink);
		dummyResourceCtlPink.extendSchemaPirate();
		dummyResourcePink = dummyResourceCtlPink.getDummyResource();
		resourceDummyPink = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_PINK_FILE, RESOURCE_DUMMY_PINK_OID, initTask, initResult); 
		resourceDummyPinkType = resourceDummyPink.asObjectable();
		dummyResourceCtlPink.setResource(resourceDummyPink);
		
		dummyResourceCtlViolet = DummyResourceContoller.create(RESOURCE_DUMMY_VIOLET_NAME, resourceDummyViolet);
		dummyResourceCtlViolet.extendSchemaPirate();
		dummyResourceViolet = dummyResourceCtlViolet.getDummyResource();
		resourceDummyViolet = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_VIOLET_FILE, RESOURCE_DUMMY_VIOLET_OID, initTask, initResult); 
		resourceDummyVioletType = resourceDummyViolet.asObjectable();
		dummyResourceCtlViolet.setResource(resourceDummyViolet);
		
		dummyResourceCtlDarkViolet = DummyResourceContoller.create(RESOURCE_DUMMY_DARK_VIOLET_NAME, resourceDummyViolet);
		dummyResourceCtlDarkViolet.extendSchemaPirate();
		dummyResourceDarkViolet = dummyResourceCtlDarkViolet.getDummyResource();
		resourceDummyDarkViolet = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_DARK_VIOLET_FILE, RESOURCE_DUMMY_DARK_VIOLET_OID, initTask, initResult); 
		resourceDummyDarkVioletType = resourceDummyDarkViolet.asObjectable();
		dummyResourceCtlDarkViolet.setResource(resourceDummyDarkViolet);
		dummyResourceDarkViolet.setSyncStyle(DummySyncStyle.SMART);
		
		dummyResourceCtlMagenta = DummyResourceContoller.create(RESOURCE_DUMMY_MAGENTA_NAME, resourceDummyMagenta);
		dummyResourceCtlMagenta.extendSchemaPirate();
		dummyResourceMagenta = dummyResourceCtlMagenta.getDummyResource();
		resourceDummyMagenta = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_MAGENTA_FILE, RESOURCE_DUMMY_MAGENTA_OID, initTask, initResult); 
		resourceDummyMagentaType = resourceDummyMagenta.asObjectable();
		dummyResourceCtlMagenta.setResource(resourceDummyMagenta);
		
		addObject(USER_TEMPLATE_ITERATION_FILE);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
	}

	/**
	 * The default dummy instance will not iterate. It has correlation rule which will link the account instead.
	 */
	@Test
    public void test100JackAssignAccountDummyConflicting() throws Exception {
		final String TEST_NAME = "test100JackAssignAccountDummyConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Tortuga");
		dummyResource.addAccount(account);
		repoAddObject(ShadowType.class, createShadow(resourceDummy, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");
        
        // Check account in dummy resource
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
		
	@Test
    public void test200JackAssignAccountDummyPinkConflicting() throws Exception {
		final String TEST_NAME = "test200JackAssignAccountDummyPinkConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Pinky");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Red Sea");
		dummyResourcePink.addAccount(account);
		repoAddObject(ShadowType.class, createShadow(resourceDummyPink, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_PINK_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 2);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		
		String accountPinkOid = getLinkRefOid(userJack, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountPinkShadow = repositoryService.getObject(ShadowType.class, accountPinkOid, null, result);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, "jack1", resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, "jack1", resourceDummyPinkType);
        
        // Check account in dummy resource
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Pinky", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "jack1", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Test the normal case. Just to be sure the default iteration algorithm works well.
	 */
	@Test
    public void test210GuybrushAssignAccountDummyPink() throws Exception {
		final String TEST_NAME = "test210GuybrushAssignAccountDummyPink";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
                        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_PINK_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 2);
		// Guybrush had dummy account before
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		
		String accountPinkOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountPinkShadow = repositoryService.getObject(ShadowType.class, accountPinkOid, null, result);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyPinkType);
        
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test220DeWattAssignAccountDummyPinkCaseIgnore() throws Exception {
		final String TEST_NAME = "test220DeWattAssignAccountDummyPinkCaseIgnore";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userDeWatt = createUser(USER_DEWATT_NAME, "Augustus DeWatt", true);
        addObject(userDeWatt);
        String userDeWattkOid = userDeWatt.getOid();
        
        PrismObject<ShadowType> accountDeWatt = createAccount(resourceDummyPink, ACCOUNT_DEWATT_NAME, true);
        addAttributeToShadow(accountDeWatt, resourceDummyPink, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,  "Augustus DeWatt");
        addObject(accountDeWatt);

        // precondition
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_DEWATT_NAME, "Augustus DeWatt", true);
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(userDeWattkOid,
        		RESOURCE_DUMMY_PINK_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        dummyAuditService.clear();
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userDeWattAfter = getUser(userDeWattkOid);
		display("User after change execution", userDeWattAfter);
		assertUser(userDeWattAfter, userDeWattkOid, USER_DEWATT_NAME, "Augustus DeWatt", null, null);
		assertLinks(userDeWattAfter, 1);
		assertAccount(userDeWattAfter, RESOURCE_DUMMY_PINK_OID);
		
		String accountPinkOid = getLinkRefOid(userDeWattAfter, RESOURCE_DUMMY_PINK_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountPinkShadow = repositoryService.getObject(ShadowType.class, accountPinkOid, null, result);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, USER_DEWATT_NAME+"1", resourceDummyPinkType);
        
        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, USER_DEWATT_NAME+"1", resourceDummyPinkType);
        
        // Old account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_DEWATT_NAME, "Augustus DeWatt", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_DEWATT_NAME+"1", "Augustus DeWatt", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}

	
	@Test
    public void test300JackAssignAccountDummyVioletConflicting() throws Exception {
		final String TEST_NAME = "test300JackAssignAccountDummyVioletConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Violet");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Sea of Lavender");
		dummyResourceViolet.addAccount(account);
		repoAddObject(ShadowType.class, createShadow(resourceDummyViolet, ACCOUNT_JACK_DUMMY_USERNAME), result);
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 3);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
		
		String accountVioletOid = getLinkRefOid(userJack, RESOURCE_DUMMY_VIOLET_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountVioletShadow = repositoryService.getObject(ShadowType.class, accountVioletOid, null, result);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "jack.1", resourceDummyVioletType);
        
        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "jack.1", resourceDummyVioletType);
        
        // Check account in dummy resource
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "jack.1", "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test350GuybrushAssignAccountDummyViolet() throws Exception {
		final String TEST_NAME = "test350GuybrushAssignAccountDummyViolet";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 3);
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
		
		String accountVioletOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountVioletShadow = repositoryService.getObject(ShadowType.class, accountVioletOid, null, result);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "guybrush.3", resourceDummyVioletType);
        
        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "guybrush.3", resourceDummyVioletType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test360HermanAssignAccountDummyViolet() throws Exception {
		final String TEST_NAME = "test360HermanAssignAccountDummyViolet";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        addObject(USER_HERMAN_FILE);
        
        dummyAuditService.clear();
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_HERMAN_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userHerman = getUser(USER_HERMAN_OID);
		display("User after change execution", userHerman);
		assertUser(userHerman, USER_HERMAN_OID, "herman", "Herman Toothrot", "Herman", "Toothrot");
		assertLinks(userHerman, 1);
		assertAccount(userHerman, RESOURCE_DUMMY_VIOLET_OID);
		
		String accountVioletOid = getLinkRefOid(userHerman, RESOURCE_DUMMY_VIOLET_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountVioletShadow = repositoryService.getObject(ShadowType.class, accountVioletOid, null, result);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "herman.1", resourceDummyVioletType);
        
        assertIteration(accountVioletShadow, 1, ".1");
        
        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "herman.1", resourceDummyVioletType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "herman");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "herman.1", "Herman Toothrot", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test400RenameAngelicaConflicting() throws Exception {
		final String TEST_NAME = "test400RenameAngelicaConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userLechuck = createUser(USER_ANGELICA_NAME, "Angelica", true);
        userLechuck.asObjectable().getAssignment().add(createAccountAssignment(RESOURCE_DUMMY_PINK_OID, null));
        addObject(userLechuck);
        String userLechuckOid = userLechuck.getOid();
        
        PrismObject<ShadowType> accountCharles = createAccount(resourceDummyPink, ACCOUNT_SPARROW_NAME, true);
        addObject(accountCharles);
        
        // preconditions
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_ANGELICA_NAME, "Angelica", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME, null, true);
        
        // WHEN
        modifyUserReplace(userLechuckOid, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString(ACCOUNT_SPARROW_NAME));
        
        // THEN
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME, null, true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME+"1", "Angelica", true);
        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_ANGELICA_NAME);
	}
	
	/**
	 * No conflict. Just make sure the iteration condition is not triggered.
	 */
	@Test
    public void test500JackAssignAccountDummyMagenta() throws Exception {
		final String TEST_NAME = "test500JackAssignAccountDummyMagenta";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
		assertLinks(userJack, 4);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userJack, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userJack, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "jack", resourceDummyMagentaType);
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "jack", resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 0, "");
        
        // Check account in dummy resource
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "jack.1", "Jack Sparrow", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "jack", "Jack Sparrow", true);
        
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Conflict on quote attribute
	 */
	@Test
    public void test510DrakeAssignAccountDummyMagenta() throws Exception {
		final String TEST_NAME = "test510DrakeAssignAccountDummyMagenta";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userDrake = PrismTestUtil.parseObject(USER_DRAKE_FILE);
        userDrake.asObjectable().setDescription(DESCRIPTION_RUM);
        userDrake.asObjectable().setLocality(PrismTestUtil.createPolyStringType("Jamaica"));
        addObject(userDrake);
        
        dummyAuditService.clear();
        
        // Make sure there are some dummy accounts without quote. So if the code tries to search for null
        // it will get something and the test fails
        dummyResourceCtlMagenta.addAccount("afettucini", "Alfredo Fettucini");
        dummyResourceCtlMagenta.addAccount("bfettucini", "Bill Fettucini");
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_DRAKE_OID, 
        		RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
		display("User after change execution", userDrakeAfter);
		assertUser(userDrakeAfter, USER_DRAKE_OID, "drake", "Francis Drake", "Fancis", "Drake");
		assertLinks(userDrakeAfter, 1);
		assertAccount(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 1, "001");
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", "Francis Drake", false);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Francis Drake");
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Jamaica");
        
        PrismAsserts.assertPropertyValue(userDrakeAfter, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM + " -- Francis Drake"));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/*
	 * Modify a property that has nothing to do with iteration
	 */
	@Test
    public void test520DrakeModifyLocality() throws Exception {
		final String TEST_NAME = "test520DrakeModifyLocality";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();        
        dummyAuditService.clear();
                                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_DRAKE_OID, UserType.F_LOCALITY, task, result, PrismTestUtil.createPolyString("London"));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
		display("User after change execution", userDrakeAfter);
		assertUser(userDrakeAfter, USER_DRAKE_OID, "drake", "Francis Drake", "Fancis", "Drake");
		assertLinks(userDrakeAfter, 1);
		assertAccount(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 1, "001");
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "drake001", resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", "Francis Drake", false);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "London");
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Francis Drake");
        
        PrismAsserts.assertPropertyValue(userDrakeAfter, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM + " -- Francis Drake"));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Nothing special in this test. Just plain assignment. No conflicts. It just prepares the ground for the next
	 * test and also tests the normal case.
	 */
	@Test
    public void test530GuybrushAssignAccountDummyMagenta() throws Exception {
		final String TEST_NAME = "test530GuybrushAssignAccountDummyMagenta";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
                
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 4);
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 0, "");
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // old account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
                
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	/**
	 * Change Guybrushe's description so it conflicts with Jack's description in magenta resource.
	 * As the iterator is also bound to the account identifier (ICF NAME) the guybrushe's account will
	 * also be renamed.
	 */
	@Test
    public void test532GuybrushModifyDescription() throws Exception {
		final String TEST_NAME = "test532GuybrushModifyDescription";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, task, result, DESCRIPTION_RUM);
                  		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
		display("User after change execution", userGuybrush);
		assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
		assertLinks(userGuybrush, 4);
		assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", resourceDummyMagentaType);
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", resourceDummyMagentaType);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        
        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // Renamed
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME+ "001", "Guybrush Threepwood", true);
        
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME+ "001", 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Guybrush Threepwood");
        
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM + " -- Guybrush Threepwood"));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test600JackRename() throws Exception {
		final String TEST_NAME = "test600JackRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result, 
        		PrismTestUtil.createPolyString(USER_JACK_RENAMED_NAME));
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack, USER_JACK_RENAMED_NAME, "Jack Sparrow", "Jack", "Sparrow", "Caribbean");
		assertLinks(userJack, 4);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
		assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
		assertAccount(userJack, RESOURCE_DUMMY_MAGENTA_OID);
		
		String accountMagentaOid = getLinkRefOid(userJack, RESOURCE_DUMMY_MAGENTA_OID);
        
		// Check shadow
        PrismObject<ShadowType> accountMagentaShadow = repositoryService.getObject(ShadowType.class, accountMagentaOid, null, result);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, USER_JACK_RENAMED_NAME, resourceDummyMagentaType);
        
        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, USER_JACK_RENAMED_NAME, resourceDummyMagentaType);
        
        assertIteration(accountMagentaShadow, 0, "");
        
        assertDummyAccount(USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION, 
        		PrismTestUtil.createPolyString(DESCRIPTION_RUM));
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(5);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
	}
	
	@Test
    public void test700DarkVioletSyncTask() throws Exception {
		final String TEST_NAME = "test700DarkVioletSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_FILE);
        
        // THEN
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, false);
	}

	/*
	 * Create account with fullname LeChuck. User with name LeChuck should be created (no conflict yet).
	 */
	@Test
    public void test710DarkVioletAddLeChuck() throws Exception {
		final String TEST_NAME = "test710DarkVioletAddLeChuck";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_LECHUCK_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
	}
	
	/*
	 * Create account with fullname LeChuck. User with name LeChuck.1 should be created (conflict).
	 */
	@Test
    public void test712DarkVioletAddCharles() throws Exception {
		final String TEST_NAME = "test712DarkVioletAddCharles";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_CHARLES_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
	}
	
	/*
	 * Create account with fullname LeChuck. User with name LeChuck.2 should be created (second conflict).
	 */
	@Test
    public void test714DarkVioletAddShinetop() throws Exception {
		final String TEST_NAME = "test714DarkVioletAddShinetop";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_SHINETOP_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2", "Melee Island");
	}
	
	/*
	 * Create account with fullname LeChuck. User with name LeChuck.2 should be created (second conflict).
	 */
	@Test
    public void test716DarkVioletDeleteCharles() throws Exception {
		final String TEST_NAME = "test716DarkVioletDeleteCharles";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		dummyResourceDarkViolet.deleteAccountByName(ACCOUNT_CHARLES_USERNAME);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2", "Melee Island");
	}
	
	@Test
    public void test720DarkVioletModifyShinetopLocation() throws Exception {
		final String TEST_NAME = "test720DarkVioletModifyShinetopLocation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_SHINETOP_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2", "Monkey Island");
	}
	
	@Test
    public void test722DarkVioletModifyShinetopFullName() throws Exception {
		final String TEST_NAME = "test722DarkVioletModifyShinetopFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_SHINETOP_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, CHUCKIE_FULLNAME);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
		assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".1");
		assertUserNick(ACCOUNT_SHINETOP_USERNAME, CHUCKIE_FULLNAME, CHUCKIE_FULLNAME, "Monkey Island");
		assertNoUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME+".2");
	}
	
	/*
	 * Create account with fullname barbossa. But user barbossa already exists.
	 *  User with name barbossa.1 should be created (conflict).
	 */
	@Test
    public void test730DarkVioletAddBarbossa() throws Exception {
		final String TEST_NAME = "test730DarkVioletAddBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(USER_BARBOSSA_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_BARBOSSA_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME+".1");
	}
	
	@Test
    public void test750DarkVioletAddMatusalem() throws Exception {
		final String TEST_NAME = "test750DarkVioletAddMatusalem";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        // IMPORTANT! Change of user template!
        deleteObject(ObjectTemplateType.class, USER_TEMPLATE_ITERATION_OID, task, result);
        addObject(USER_TEMPLATE_ITERATION_RANDOM_FILE);
        
        DummyAccount account = new DummyAccount(ACCOUNT_MATUSALEM_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
	}
	
	/*
	 * Create account with fullname Rum. User with name Rum.xxx should be created (conflict).
	 */
	@Test
    public void test752DarkVioletAddDiplomatico() throws Exception {
		final String TEST_NAME = "test752DarkVioletAddDiplomatico";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_DIPLOMATICO_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		
		iterationTokenDiplomatico = lookupIterationTokenByAdditionalName(ACCOUNT_DIPLOMATICO_USERNAME);
		assertUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
	}

	/*
	 * Create account with fullname Rum. User with name Rum.yyy should be created (second conflict).
	 */
	@Test
    public void test754DarkVioletAddMilionario() throws Exception {
		final String TEST_NAME = "test754DarkVioletAddMilionario";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = new DummyAccount(ACCOUNT_MILLONARIO_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Peru");
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
        display("Adding dummy account", account.debugDump());
		dummyResourceDarkViolet.addAccount(account);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		
		iterationTokenMillonario = lookupIterationTokenByAdditionalName(ACCOUNT_MILLONARIO_USERNAME);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario, "Peru");
	}
	
	@Test
    public void test756DarkVioletDeleteDiplomatico() throws Exception {
		final String TEST_NAME = "test756DarkVioletDeleteDiplomatico";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		dummyResourceDarkViolet.deleteAccountByName(ACCOUNT_DIPLOMATICO_USERNAME);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario, "Peru");
	}
	
	@Test
    public void test760DarkVioletModifyMillonarioLocation() throws Exception {
		final String TEST_NAME = "test760DarkVioletModifyMillonarioLocation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_MILLONARIO_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Northern Peru");
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario, "Northern Peru");
	}
	
	/**
	 * Rename to an identifier that is free. Empty iterationToken is expected.
	 */
	@Test
    public void test762DarkVioletModifyMillonarioFullName() throws Exception {
		final String TEST_NAME = "test762DarkVioletModifyMillonarioFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_MILLONARIO_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RON_FULLNAME);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RON_FULLNAME, RON_FULLNAME, "Northern Peru");
		assertNoUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario);
	}
	
	/**
	 * Rename to an identifier that is taken. New random iterationToken is expected.
	 */	
	@Test
    public void test764DarkVioletModifyMatusalemFullName() throws Exception {
		final String TEST_NAME = "test764DarkVioletModifyMatusalemFullName";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        DummyAccount account = dummyResourceDarkViolet.getAccountByUsername(ACCOUNT_MATUSALEM_USERNAME);
        
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RON_FULLNAME);
		
		waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID, true);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		displayAllUsers();
		assertNoUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
		String iterationTokenMatusalem = lookupIterationTokenByAdditionalName(ACCOUNT_MATUSALEM_USERNAME);
		assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RON_FULLNAME, RON_FULLNAME+iterationTokenMatusalem);
		assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenDiplomatico);
		assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RON_FULLNAME, RON_FULLNAME, "Northern Peru");
		assertNoUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME+iterationTokenMillonario);
	}
	
	private void assertUserNick(String accountName, String accountFullName, String expectedUserName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertUserNick(accountName, accountFullName, expectedUserName, null);
	}
	
	private void assertUserNick(String accountName, String accountFullName, String expectedUserName, String expectedLocality) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = findUserByUsername(expectedUserName);
		assertNotNull("No user for "+accountName+" ("+expectedUserName+")", user);
		display("Created user for "+accountName, user);
		assertEquals("Wrong nickname in user created for "+accountName, accountFullName, user.asObjectable().getNickName().getOrig());
		assertEquals("Wrong additionalName in user created for "+accountName, accountName, user.asObjectable().getAdditionalName().getOrig());
		PolyStringType locality = user.asObjectable().getLocality();
		if (locality == null) {
			assertEquals("Wrong locality in user created for "+accountName, expectedLocality, null);
		} else {
			assertEquals("Wrong locality in user created for "+accountName, expectedLocality, locality.getOrig());
		}
	}
	
	private void assertNoUserNick(String accountName, String accountFullName, String expectedUserName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = findUserByUsername(expectedUserName);
		display("User for "+accountName, user);
		assertNull("User for "+accountName+" ("+expectedUserName+") exists but it should be gone", user);
	}
	
	private String lookupIterationTokenByAdditionalName(String additionalName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(TestIteration.class.getName() + ".lookupIterationTokenByAdditionalName");
        OperationResult result = task.getResult();
        EqualsFilter filter = EqualsFilter.createEqual(UserType.F_ADDITIONAL_NAME, UserType.class, prismContext, null, PrismTestUtil.createPolyString(additionalName));
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		List<PrismObject<UserType>> objects = modelService.searchObjects(UserType.class, query, null, task, result);
		if (objects.isEmpty()) {
			return null;
		}
		assert objects.size() == 1 : "Too many objects found for additional name "+additionalName+": "+objects;
		PrismObject<UserType> user = objects.iterator().next();
		return user.asObjectable().getIterationToken();
	}
}
