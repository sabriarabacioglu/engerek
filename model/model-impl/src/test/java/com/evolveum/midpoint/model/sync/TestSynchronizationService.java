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
package com.evolveum.midpoint.model.sync;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.util.mock.MockLensDebugListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSynchronizationService extends AbstractInternalModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/sync");
	
	public static final File SHADOW_PIRATES_DUMMY_FILE = new File(TEST_DIR, "shadow-pirates-dummy.xml");
	public static final String GROUP_PIRATES_DUMMY_NAME = "pirates";

	private static final String INTENT_GROUP = "group";
		
	@Autowired(required = true)
	SynchronizationService synchronizationService;
	
	@Autowired(required = true)
	Clockwork clockwork;
	
	private String accountShadowJackDummyOid = null;
	private String accountShadowCalypsoDummyOid = null;
	
	@Test
    public void test010AddedAccountJack() throws Exception {
		final String TEST_NAME = "test010AddedAccountJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, ShadowType.class, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        provisioningService.applyDefinition(accountShadowJack, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		dummyResource.addAccount(dummyAccount);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());
		
		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
		//it this really expected?? delta was already executed, should we expect it in the secondary delta?
//		assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
//		assertIterationDelta(accCtx.getSecondaryDelta(), 0, "");
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	@Test
    public void test020ModifyLootAbsolute() throws Exception {
		final String TEST_NAME = "test020ModifyLootAbsolute";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, "999");
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 3, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_COST_CENTER, "999");
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		
		PrismAsserts.assertNoDelta("account primary delta", accCtx.getPrimaryDelta());
		PrismAsserts.assertNoDelta("account secondary delta", accCtx.getSecondaryDelta());
		
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
		
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertEquals("Unexpected used constCenter", "999", user.asObjectable().getCostCenter());
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	@Test
    public void test021ModifyLootAbsoluteEmpty() throws Exception {
		final String TEST_NAME = "test021ModifyLootAbsoluteEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        
        display("SENDING CHANGE NOTIFICATION", change);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 3, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_COST_CENTER);
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		
		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
		PrismAsserts.assertNoDelta("Unexpected account secondary delta", accCtx.getSecondaryDelta());
		
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
		
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertEquals("Unexpected used constCenter", null, user.asObjectable().getCostCenter());
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	/**
	 * Sending empty delta, this is what reconciliation does.
	 */
	@Test
    public void test030Reconcile() throws Exception {
		final String TEST_NAME = "test030Reconcile";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_RECON_URI);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertNull("Unexpected user secondary delta", context.getFocusContext().getSecondaryDelta());
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), 
        		ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		
		PrismAsserts.assertNoDelta("account primary delta", accCtx.getPrimaryDelta());
		PrismAsserts.assertNoDelta("account secondary delta", accCtx.getSecondaryDelta());
		
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());
		
		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	
	@Test
    public void test039DeletedAccountJack() throws Exception {
		final String TEST_NAME = "test039DeletedAccountJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);

        dummyResource.deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(shadow);
        change.setResource(resourceDummy);
        ObjectDelta<ShadowType> syncDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountShadowJackDummyOid, prismContext);
		change.setObjectDelta(syncDelta);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertNull("Unexpected user secondary delta", context.getFocusContext().getSecondaryDelta());
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), 
        		ShadowKindType.ACCOUNT, null, true);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.DELETED, accCtx.getSynchronizationSituationDetected());
		
		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
		
		assertNotLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);
		
		shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.DELETED);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	/**
	 * Calypso is protected, no reaction should be applied.
	 */
	@Test
    public void test050AddedAccountCalypso() throws Exception {
		final String TEST_NAME = "test050AddedAccountCalypso";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        PrismObject<ShadowType> accountShadowCalypso = repoAddObjectFromFile(ACCOUNT_SHADOW_CALYPSO_DUMMY_FILENAME, ShadowType.class, result);
        accountShadowCalypsoDummyOid = accountShadowCalypso.getOid();
        provisioningService.applyDefinition(accountShadowCalypso, result);
        assertNotNull("No oid in shadow", accountShadowCalypso.getOid());
        // Make sure that it is properly marked as protected. This is what provisioning would normally do
        accountShadowCalypso.asObjectable().setProtectedObject(true);
        
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        dummyAccount.setPassword("h1ghS3AS");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");
		dummyResource.addAccount(dummyAccount);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowCalypso);
        change.setResource(resourceDummy);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNull("Unexpected lens context", context);
        		
		PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
		assertNull("Unexpected user "+userCalypso, userCalypso);
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        assertSituation(shadow, null);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	/**
	 * Calypso is protected, no reaction should be applied.
	 */
	@Test
    public void test051CalypsoRecon() throws Exception {
		final String TEST_NAME = "test051CalypsoRecon";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        // Lets make this a bit more interesting by setting up a fake situation in the shadow
        ObjectDelta<ShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountShadowCalypsoDummyOid, 
        		resourceDummy, new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION), SynchronizationSituationType.DISPUTED);
        repositoryService.modifyObject(ShadowType.class, accountShadowCalypsoDummyOid, objectDelta.getModifications(), result);
        
        PrismObject<ShadowType> accountShadowCalypso = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        // Make sure that it is properly marked as protected. This is what provisioning would normally do
        accountShadowCalypso.asObjectable().setProtectedObject(true);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowCalypso);
        change.setResource(resourceDummy);
        
        display("Change notification", change);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        LensContext<UserType> context = mockListener.getLastSyncContext();

        display("Resulting context (as seen by debug listener)", context);
        assertNull("Unexpected lens context", context);
        		
		PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
		assertNull("Unexpected user "+userCalypso, userCalypso);
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        assertSituation(shadow, null);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	@Test
    public void test210AddedGroupPirates() throws Exception {
		final String TEST_NAME = "test210AddedGroupPirates";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        MockLensDebugListener mockListener = new MockLensDebugListener();
        clockwork.setDebugListener(mockListener);
        
        PrismObject<ShadowType> shadowPirates = repoAddObjectFromFile(SHADOW_PIRATES_DUMMY_FILE, ShadowType.class, result);
        provisioningService.applyDefinition(shadowPirates, result);
        assertNotNull("No oid in shadow", shadowPirates.getOid());
        DummyGroup dummyGroup = new DummyGroup();
        dummyGroup.setName(GROUP_PIRATES_DUMMY_NAME);
        dummyGroup.setEnabled(true);
        dummyGroup.addAttributeValues(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Scurvy Pirates");
		dummyResource.addGroup(dummyGroup);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(shadowPirates);
        change.setResource(resourceDummy);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        TestUtil.displayWhen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        LensContext<UserType> context = mockListener.getLastSyncContext();
        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);
        
        assertNotNull("No focus primary delta", context.getFocusContext().getPrimaryDelta());
        assertNotNull("No focus secondary delta", context.getFocusContext().getSecondaryDelta());
        
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceDummy.getOid(), 
        		ShadowKindType.ENTITLEMENT, INTENT_GROUP);
		LensProjectionContext projCtx = context.findProjectionContext(rat);
		assertNotNull("No projection sync context for "+rat, projCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNMATCHED, projCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, projCtx.getSynchronizationSituationResolved());
		
		PrismAsserts.assertNoDelta("Unexpected projection primary delta", projCtx.getPrimaryDelta());
		//it this really expected?? delta was already executed, should we expect it in the secondary delta?
//		assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
//		assertIterationDelta(accCtx.getSecondaryDelta(), 0, "");
		
		assertLinked(RoleType.class, context.getFocusContext().getOid(), shadowPirates.getOid());
		
		PrismObject<ShadowType> shadow = getShadowModelNoFetch(shadowPirates.getOid());
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);
        
	}

}
