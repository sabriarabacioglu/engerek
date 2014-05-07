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

package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.AssertJUnit;
import org.testng.annotations.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.opends.server.core.AddOperation;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ResultCode;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestSynchronization extends AbstractIntegrationTest {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/object/resource-opendj.xml";
	private static final String FILENAME_LDAP_CONNECTOR = "src/test/resources/ucf/connector-ldap.xml";
	private static final String SYNC_TASK_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String FILENAME_SYNC_TASK = "src/test/resources/impl/sync-task-example.xml";
	private static final String LDIF_WILL_FILENAME = "src/test/resources/ucf/will.ldif";
	private static final String LDIF_CALYPSO_FILENAME = "src/test/resources/impl/opendj/calypso.ldif";
	private static final String FILENAME_USER_ADMIN = "src/test/resources/impl/admin.xml";
	private static final String ACCOUNT_WILL_NAME = "uid=wturner,ou=people,dc=example,dc=com";
	private static final String ACCOUNT_CALYPSO_NAME = "uid=calypso,ou=people,dc=example,dc=com";

	private ResourceType resourceType;
	
	@Autowired
	private ConnectorFactory manager;
	
	@Autowired
	private ProvisioningService provisioningService;

	@Autowired
	private ResourceObjectChangeListener syncServiceMock;

	private Task syncTask = null;
	
	@BeforeClass
	public static void startLdap() throws Exception {
		openDJController.startCleanServer();
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		openDJController.stop();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
		assertNotNull(manager);
		// let provisioning discover the connectors
		provisioningService.postInit(initResult);
		
		resourceType = addResourceFromFile(FILENAME_RESOURCE_OPENDJ, "org.identityconnectors.ldap.LdapConnector", initResult).asObjectable();
		
		//it is needed to declare the task owner, so we add the user admin to the reposiotry
		repoAddObjectFromFile(FILENAME_USER_ADMIN, UserType.class, initResult);
		
		repoAddObjectFromFile(FILENAME_SYNC_TASK, TaskType.class, initResult);
	}
	
	@Test
	public void test010Sanity() throws Exception {
		final String TEST_NAME = "test010Sanity";
		TestUtil.displayTestTile(TEST_NAME);
		final OperationResult result = new OperationResult(TestSynchronization.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceType.getOid(), null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertNotNull("Resource is null", resource);
		display("getObject(resource)", resource);
		
		result.computeStatus();
		display("getObject(resource) result", result);
		TestUtil.assertSuccess(result);
		
		// Make sure these were generated
		assertNotNull("No resource schema", resource.asObjectable().getSchema());
		assertNotNull("No native capabilities", resource.asObjectable().getCapabilities().getNative());

		Task syncTask = taskManager.getTask(SYNC_TASK_OID, result);
		AssertJUnit.assertNotNull(syncTask);
		assertSyncToken(syncTask, 0, result);
	}

	private void assertSyncToken(String syncTaskOid, Object expectedValue, OperationResult result) throws ObjectNotFoundException, SchemaException {
		Task task = taskManager.getTask(syncTaskOid, result);
		assertSyncToken(task, expectedValue, result);
	}
		
	private void assertSyncToken(Task task, Object expectedValue, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismProperty<Object> syncTokenProperty = task.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
		if (expectedValue == null && syncTokenProperty == null) {
			return;
		}
		assertEquals("Wrong sync token", expectedValue, syncTokenProperty.getRealValue());
	}

	@Test
	public void test100SyncAddWill() throws Exception {
		final String TEST_NAME = "test100SyncAddWill";
		TestUtil.displayTestTile(TEST_NAME);
		final OperationResult result = new OperationResult(TestSynchronization.class.getName()
				+ "." + TEST_NAME);

		Task syncTask = taskManager.getTask(SYNC_TASK_OID, result);
		AssertJUnit.assertNotNull(syncTask);
		assertSyncToken(syncTask, 0, result);
		((SynchornizationServiceMock)syncServiceMock).reset();

		// create add change in embeded LDAP
		LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_WILL_FILENAME);
		LDIFReader ldifReader = new LDIFReader(importConfig);
		Entry entry = ldifReader.readEntry();
		display("Entry from LDIF", entry);

		AddOperation addOperation = openDJController.getInternalConnection().processAdd(entry);

		AssertJUnit.assertEquals("LDAP add operation failed", ResultCode.SUCCESS,
				addOperation.getResultCode());

		// WHEN
		provisioningService.synchronize(resourceType.getOid(), ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType),
				syncTask, result);
		
		// THEN
		SynchornizationServiceMock mock = (SynchornizationServiceMock) syncServiceMock;
		
		assertEquals("Unexpected number of synchronization service calls", 1, mock.getCallCount());
		
		ResourceObjectShadowChangeDescription lastChange = mock.getLastChange();
//			ObjectDelta<? extends ShadowType> objectDelta = lastChange.getObjectDelta();
//			assertNotNull("Null object delta in change notification", objectDelta);
//			assertEquals("Wrong change type in delta in change notification", ChangeType.ADD, objectDelta.getChangeType());
		PrismObject<? extends ShadowType> currentShadow = lastChange.getCurrentShadow();
		assertNotNull("No current shadow in change notification", currentShadow);
		assertNotNull("No old shadow in change notification", lastChange.getOldShadow());
		
		assertEquals("Wrong shadow name", PrismTestUtil.createPolyStringType(ACCOUNT_WILL_NAME), currentShadow.asObjectable().getName());
		
		assertSyncToken(SYNC_TASK_OID, 1, result);

	}

	@Test
	public void test500SyncAddProtected() throws Exception {
		final String TEST_NAME = "test500SyncAddProtected";
		TestUtil.displayTestTile(TEST_NAME);
		final OperationResult result = new OperationResult(TestSynchronization.class.getName()
				+ "." + TEST_NAME);

		Task syncTask = taskManager.getTask(SYNC_TASK_OID, result);
		AssertJUnit.assertNotNull(syncTask);
		assertSyncToken(syncTask, 1, result);
		((SynchornizationServiceMock)syncServiceMock).reset();

		// create add change in embedded LDAP
		LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_CALYPSO_FILENAME);
		LDIFReader ldifReader = new LDIFReader(importConfig);
		Entry entry = ldifReader.readEntry();
		display("Entry from LDIF", entry);
		AddOperation addOperation = openDJController.getInternalConnection().processAdd(entry);

		AssertJUnit.assertEquals("LDAP add operation failed", ResultCode.SUCCESS,
				addOperation.getResultCode());

		// WHEN
		provisioningService.synchronize(resourceType.getOid(), ProvisioningTestUtil.getDefaultAccountObjectClass(resourceType),
				syncTask, result);
		
		// THEN
		SynchornizationServiceMock mock = (SynchornizationServiceMock) syncServiceMock;
		
		assertEquals("Unexpected number of synchronization service calls", 0, mock.getCallCount());
		
//		ResourceObjectShadowChangeDescription lastChange = mock.getLastChange();
//		PrismObject<? extends ShadowType> currentShadow = lastChange.getCurrentShadow();
//		assertNotNull("No current shadow in change notification", currentShadow);
//		assertNotNull("No old shadow in change notification", lastChange.getOldShadow());
//		
//		assertEquals("Wrong shadow name", PrismTestUtil.createPolyStringType(ACCOUNT_CALYPSO_NAME), currentShadow.asObjectable().getName());
//		
//		assertNotNull("Calypso is not protected", currentShadow.asObjectable().isProtectedObject());
//		assertTrue("Calypso is not protected", currentShadow.asObjectable().isProtectedObject());
		
		assertSyncToken(SYNC_TASK_OID, 2, result);

	}

}
