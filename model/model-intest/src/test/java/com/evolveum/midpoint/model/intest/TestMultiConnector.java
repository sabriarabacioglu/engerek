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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiConnector extends AbstractConfiguredModelIntegrationTest {
			
	private String connectorDummyOid;
	private String connectorDummyFakeOid;
	
	private PrismObject<ResourceType> resourceDummy;
	private PrismObject<ResourceType> resourceDummyFake;

	private DummyResourceContoller dummyResourceCtl;
	protected static DummyResource dummyResource;

	public TestMultiConnector() throws JAXBException {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// Make sure that the connectors are discovered
		modelService.postInit(initResult);
		
		// Make sure to call postInit first. This add system config to repo.
		// If system is initialized after that then the logging config from system config
		// will be used instead of test logging config
		super.initSystem(initTask, initResult);
		
		dummyResourceCtl = DummyResourceContoller.create(null, resourceDummy);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();
		
		dummyResourceCtl.addAccount(ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot");
		dummyResourceCtl.addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood");
		dummyResourceCtl.addAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");
	}
	
	@Test
    public void test010ListConnectors() throws Exception {
        TestUtil.displayTestTile(this, "test010ListConnectors");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test010ListConnectors");
        OperationResult result = task.getResult();
        
		// WHEN
        List<PrismObject<ConnectorType>> connectors = modelService.searchObjects(ConnectorType.class, null, null, task, result);
		
		// THEN
        display("Connectors", connectors);
        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);
        
        assertEquals("Unexpected number of connectors", 5, connectors.size());
        for(PrismObject<ConnectorType> connector: connectors) {
        	display("Connector", connector);
        	ConnectorType connectorType = connector.asObjectable();
        	if (CONNECTOR_DUMMY_TYPE.equals(connectorType.getConnectorType())) {
        		String connectorVersion = connectorType.getConnectorVersion();
        		if (connectorVersion.contains("fake")) {
        			display("Fake Dummy Connector OID", connector.getOid());
        			connectorDummyFakeOid = connector.getOid();
        		} else {
        			display("Dummy Connector OID", connector.getOid());
        			connectorDummyOid = connector.getOid();
        		}
        	}
        }
        
        assertNotNull("No dummy connector", connectorDummyOid);
        assertNotNull("No fake dummy connector", connectorDummyFakeOid);

	}
	
	@Test
    public void test020ImportFakeResource() throws Exception {
        TestUtil.displayTestTile(this, "test020ImportFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test020ImportFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        importObjectFromFile(RESOURCE_DUMMY_FAKE_FILENAME, result);
		
		// THEN
        result.computeStatus();
        display("Import result", result);
        TestUtil.assertSuccess("import result", result, 2);
        
        resourceDummyFake = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, task, result);
        display("Imported resource", resourceDummyFake);
        assertNotNull("Null fake resource after getObject", resourceDummyFake);
        assertEquals("Wrong connectorRef in fake resource", connectorDummyFakeOid, 
        		resourceDummyFake.asObjectable().getConnectorRef().getOid());

	}
	
	@Test
    public void test021TestFakeResource() throws Exception {
        TestUtil.displayTestTile(this, "test021TestFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test021TestFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_FAKE_OID, task);
		
		// THEN
 		display("testResource result", testResult);
        TestUtil.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test022ListAccountsFakeResource() throws Exception {
        TestUtil.displayTestTile(this, "test022ListAccountsFakeResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test022ListAccountsFakeResource");
        OperationResult result = task.getResult();
        
		// WHEN
        Collection<PrismObject<ShadowType>> accounts = listAccounts(resourceDummyFake, task, result);
		
		// THEN
        result.computeStatus();
 		display("listAccounts result", result);
        TestUtil.assertSuccess("listAccounts result", result);
        
        assertEquals("Unexpected number of accounts: "+accounts, 1, accounts.size());
	}

	@Test
    public void test030ImportDummyResource() throws Exception {
        TestUtil.displayTestTile(this, "test030ImportDummyResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test030ImportDummyResource");
        OperationResult result = task.getResult();
        
		// WHEN
        importObjectFromFile(RESOURCE_DUMMY_FILE, result);
		
		// THEN
        result.computeStatus();
        display("Import result", result);
        TestUtil.assertSuccess("import result", result, 2);
        
        resourceDummy = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        display("Imported resource", resourceDummy);
        assertNotNull("Null fake resource after getObject", resourceDummy);
        assertEquals("Wrong connectorRef in fake resource", connectorDummyOid, 
        		resourceDummy.asObjectable().getConnectorRef().getOid());

	}
	
	@Test
    public void test031TestDummyResource() throws Exception {
        TestUtil.displayTestTile(this, "test031TestDummyResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test031TestDummyResource");
        OperationResult result = task.getResult();
        
		// WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);
		
		// THEN
 		display("testResource result", testResult);
        TestUtil.assertSuccess("testResource result", testResult);
	}
	
	@Test
    public void test032ListAccountsDummyResource() throws Exception {
        TestUtil.displayTestTile(this, "test032ListAccountsDummyResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test032ListAccountsDummyResource");
        OperationResult result = task.getResult();
        
		// WHEN
        Collection<PrismObject<ShadowType>> accounts = listAccounts(resourceDummy, task, result);
		
		// THEN
        result.computeStatus();
 		display("listAccounts result", result);
        TestUtil.assertSuccess("listAccounts result", result);
        
        assertEquals("Unexpected number of accounts: "+accounts, 3, accounts.size());
	}
	
	@Test
    public void test100UpgradeModelAddDelete() throws Exception {
        TestUtil.displayTestTile(this, "test100UpgradeModelAddDelete");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test100UpgradeModelAddDelete");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        PrismReference connectorRef = resourceDummyFake.findReference(ResourceType.F_CONNECTOR_REF);
        ReferenceDelta connectorRefDeltaDel = ReferenceDelta.createModificationDelete(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorRef.getValue().clone());
        resourceDelta.addModification(connectorRefDeltaDel);
        ReferenceDelta connectorRefDeltaAdd = ReferenceDelta.createModificationAdd(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
		resourceDelta.addModification(connectorRefDeltaAdd);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertUpgrade(dummyResourceModelBefore);
	}
	
		
	@Test
    public void test150DowngradeModelAddDelete() throws Exception {
        TestUtil.displayTestTile(this, "test150DowngradeModelAddDelete");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test150DowngradeModelAddDelete");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaDel = ReferenceDelta.createModificationDelete(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
        resourceDelta.addModification(connectorRefDeltaDel);
        ReferenceDelta connectorRefDeltaAdd = ReferenceDelta.createModificationAdd(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyFakeOid);
		resourceDelta.addModification(connectorRefDeltaAdd);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertDowngrade(dummyResourceModelBefore);        
	}
	
	@Test
    public void test200UpgradeModelReplace() throws Exception {
        TestUtil.displayTestTile(this, "test200UpgradeModelReplace");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test200UpgradeModelReplace");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaReplace = ReferenceDelta.createModificationReplace(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
		resourceDelta.addModification(connectorRefDeltaReplace);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertUpgrade(dummyResourceModelBefore);
	}
	
		
	@Test
    public void test250DowngradeModelReplace() throws Exception {
        TestUtil.displayTestTile(this, "test250DowngradeModelReplace");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test250DowngradeModelReplace");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaReplace = ReferenceDelta.createModificationReplace(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyFakeOid);
		resourceDelta.addModification(connectorRefDeltaReplace);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
		// WHEN
        modelService.executeChanges(deltas, null, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertDowngrade(dummyResourceModelBefore);        
	}
	
	@Test
    public void test300UpgradeRawAddDelete() throws Exception {
        TestUtil.displayTestTile(this, "test100UpgradeModelAddDelete");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test100UpgradeModelAddDelete");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaDel = ReferenceDelta.createModificationDelete(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyFakeOid);
        resourceDelta.addModification(connectorRefDeltaDel);
        ReferenceDelta connectorRefDeltaAdd = ReferenceDelta.createModificationAdd(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
		resourceDelta.addModification(connectorRefDeltaAdd);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
		ModelExecuteOptions options = ModelExecuteOptions.createRaw();
		
		// WHEN
        modelService.executeChanges(deltas, options, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertUpgrade(dummyResourceModelBefore);
	}
	
		
	@Test
    public void test350DowngradeRawAddDelete() throws Exception {
        TestUtil.displayTestTile(this, "test150DowngradeModelAddDelete");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test150DowngradeModelAddDelete");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaDel = ReferenceDelta.createModificationDelete(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
        resourceDelta.addModification(connectorRefDeltaDel);
        ReferenceDelta connectorRefDeltaAdd = ReferenceDelta.createModificationAdd(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyFakeOid);
		resourceDelta.addModification(connectorRefDeltaAdd);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
		
		ModelExecuteOptions options = ModelExecuteOptions.createRaw();
		
		// WHEN
        modelService.executeChanges(deltas, options, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertDowngrade(dummyResourceModelBefore);        
	}
	
	@Test
    public void test400UpgradeRawReplace() throws Exception {
        TestUtil.displayTestTile(this, "test400UpgradeRawReplace");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test400UpgradeRawReplace");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaReplace = ReferenceDelta.createModificationReplace(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyOid);
		resourceDelta.addModification(connectorRefDeltaReplace);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
		
		ModelExecuteOptions options = ModelExecuteOptions.createRaw();
        
		// WHEN
        modelService.executeChanges(deltas, options, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertUpgrade(dummyResourceModelBefore);
	}
	
		
	@Test
    public void test450DowngradeRawReplace() throws Exception {
        TestUtil.displayTestTile(this, "test450DowngradeRawReplace");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".test450DowngradeRawReplace");
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> dummyResourceModelBefore = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createEmptyModifyDelta(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, 
        		prismContext);
        ReferenceDelta connectorRefDeltaReplace = ReferenceDelta.createModificationReplace(ResourceType.F_CONNECTOR_REF, 
        		getResourceDefinition(), connectorDummyFakeOid);
		resourceDelta.addModification(connectorRefDeltaReplace);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
		
		ModelExecuteOptions options = ModelExecuteOptions.createRaw();
        
		// WHEN
        modelService.executeChanges(deltas, options, task, result);
		
		// THEN
        result.computeStatus();
 		display("executeChanges result", result);
        TestUtil.assertSuccess("executeChanges result", result);
        
        assertDowngrade(dummyResourceModelBefore);        
	}

	private void assertUpgrade(PrismObject<ResourceType> dummyResourceModelBefore) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".assertUpgrade");
        OperationResult result = task.getResult();
                
        // Check if the changes went well in the repo
        PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, result);
        display("Upgraded fake resource (repo)", repoResource);
        assertNotNull("Null fake resource after getObject (repo)", repoResource);
        assertEquals("Oooops. The OID of fake resource mysteriously changed. Call the police! (repo)", RESOURCE_DUMMY_FAKE_OID, repoResource.getOid());
        assertEquals("Wrong connectorRef in fake resource (repo)", connectorDummyOid, 
        		repoResource.asObjectable().getConnectorRef().getOid());
        
        // Check if resource view of the model has changed as well
        resourceDummyFake = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, task, result);
        display("Upgraded fake resource (model)", resourceDummyFake);
        assertNotNull("Null fake resource after getObject (model)", resourceDummyFake);
        assertEquals("Oooops. The OID of fake resource mysteriously changed. Call the police! (model)", RESOURCE_DUMMY_FAKE_OID, resourceDummyFake.getOid());
        assertEquals("Wrong connectorRef in fake resource (model)", connectorDummyOid, 
        		resourceDummyFake.asObjectable().getConnectorRef().getOid());
        
        // Check if the other resource is still untouched
        PrismObject<ResourceType> dummyResourceModelAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        dummyResourceModelBefore.asObjectable().setFetchResult(null);
        dummyResourceModelAfter.asObjectable().setFetchResult(null);
        ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(dummyResourceModelBefore, dummyResourceModelAfter);
        display("Dummy resource diff", dummyResourceDiff);
        assertTrue("Ha! Someone touched the other resource! Off with his head! diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());
        
        testResources(3,3);
	}

	private void assertDowngrade(PrismObject<ResourceType> dummyResourceModelBefore) throws ObjectNotFoundException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".assertDowngrade");
        OperationResult result = task.getResult();
        // Check if the changes went well in the repo
        PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, result);
        display("Upgraded fake resource (repo)", repoResource);
        assertNotNull("Null fake resource after getObject (repo)", repoResource);
        assertEquals("Oooops. The OID of fake resource mysteriously changed. Call the police! (repo)", RESOURCE_DUMMY_FAKE_OID, repoResource.getOid());
        assertEquals("Wrong connectorRef in fake resource (repo)", connectorDummyFakeOid, 
        		repoResource.asObjectable().getConnectorRef().getOid());
        
        // Check if resource view of the model has changed as well
        resourceDummyFake = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_FAKE_OID, null, task, result);
        display("Upgraded fake resource (model)", resourceDummyFake);
        assertNotNull("Null fake resource after getObject (model)", resourceDummyFake);
        assertEquals("Oooops. The OID of fake resource mysteriously changed. Call the police! (model)", RESOURCE_DUMMY_FAKE_OID, resourceDummyFake.getOid());
        assertEquals("Wrong connectorRef in fake resource (model)", connectorDummyFakeOid, 
        		resourceDummyFake.asObjectable().getConnectorRef().getOid());
        
        // Check if the other resource is still untouched
        PrismObject<ResourceType> dummyResourceModelAfter = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        dummyResourceModelBefore.asObjectable().setFetchResult(null);
        dummyResourceModelAfter.asObjectable().setFetchResult(null);
        ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(dummyResourceModelBefore, dummyResourceModelAfter);
        display("Dummy resource diff", dummyResourceDiff);
        assertTrue("Ha! Someone touched the other resource! Off with his head! diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());
        
        testResources(3,1);
	}

	private void testResources(int numDummyAccounts, int numFakeAccounts) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".testResources");
        
        // We have to purge fake resource schema here. As the new connector provides a different schema
        purgeResourceSchema(RESOURCE_DUMMY_FAKE_OID);
        
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_FAKE_OID, task);
 		display("testResource fake result", testResult);
        TestUtil.assertSuccess("testResource fake result", testResult);
        
        testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);
 		display("testResource dummy result", testResult);
        TestUtil.assertSuccess("testResource dummy result", testResult);
        
        assertResourceAccounts(resourceDummy, numDummyAccounts);
        assertResourceAccounts(resourceDummyFake, numFakeAccounts);
	}
	
	private void assertResourceAccounts(PrismObject<ResourceType> resource, int numAccounts) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(TestMultiConnector.class.getName() + ".assertResourceAccounts");
        OperationResult result = task.getResult();
        
		// WHEN
        Collection<PrismObject<ShadowType>> accounts = listAccounts(resource, task, result);
		
		// THEN
        result.computeStatus();
 		display("listAccounts result "+resource, result);
        TestUtil.assertSuccess("listAccounts result "+resource, result);
        
        assertEquals("Unexpected number of accounts on "+resource+": "+accounts, numAccounts, accounts.size());
	}


	
}
