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

/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.midpoint.test.util.TestUtil.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertProvisioningAccountShadow;
import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.TestConnectionCapabilityType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummySecurity extends AbstractDummyTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummySecurity.class);
	private String willIcfUid;

	@Test
	public void test100AddAccountDrink() throws Exception {
		final String TEST_NAME = "test100AddAccountDrink";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(new File(ACCOUNT_WILL_FILENAME));
		account.checkConsistence();
		
		setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, null, syncTask, result);
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}

	}
	
	private <T> void setAttribute(PrismObject<ShadowType> account, String attrName, T val) throws SchemaException {
		PrismContainer<Containerable> attrsCont = account.findContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttribute<T> attr = new ResourceAttribute<T>(
				dummyResourceCtl.getAttributeQName(attrName), null, prismContext);
		attr.setRealValue(val);
		attrsCont.add(attr); 		
	}

	@Test
	public void test199AddAccount() throws Exception {
		final String TEST_NAME = "test199AddAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task syncTask = taskManager.createTaskInstance(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummySecurity.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(new File(ACCOUNT_WILL_FILENAME));
		account.checkConsistence();
		
		setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
		setAttribute(account, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Eunuch");

		display("Adding shadow", account);

		// WHEN
		provisioningService.addObject(account, null, null, syncTask, result);
		
		// THEN
		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, syncTask, result);
		display("Account provisioning", accountProvisioning);
		willIcfUid = getIcfUid(accountProvisioning);

	}
	
	@Test
	public void test200ModifyAccountDrink() throws Exception {
		final String TEST_NAME = "test200ModifyAccountDrink";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), 
				prismContext, "RUM");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);
		
		delta.checkConsistence();
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid,
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");
		
		syncServiceMock.assertNotifySuccessOnly();
	}
	
	@Test
	public void test201ModifyAccountGossip() throws Exception {
		final String TEST_NAME = "test201ModifyAccountGossip";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME), 
				prismContext, "pirate");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);
		
		delta.checkConsistence();
		assertDummyAccountAttributeValues(ACCOUNT_WILL_USERNAME, willIcfUid, 
				DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "pirate");
		
		syncServiceMock.assertNotifySuccessOnly();
	}
	
	@Test
	public void test210ModifyAccountQuote() throws Exception {
		final String TEST_NAME = "test210ModifyAccountQuote";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class, 
				ACCOUNT_WILL_OID, 
				dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), 
				prismContext, "eh?");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		try {
			// WHEN
			provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
					new OperationProvisioningScriptsType(), null, task, result);
			
			AssertJUnit.fail("Unexpected success");
			
		} catch (SecurityViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
	}
	
	@Test
	public void test300GetAccount() throws Exception {
		final String TEST_NAME = "test300GetAccount";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);

		// WHEN
		ShadowType shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, 
				result).asObjectable();

		// THEN
		result.computeStatus();
		display("getObject result", result);
		TestUtil.assertSuccess(result);

		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);

		checkAccountWill(shadow, result);

		checkConsistency(shadow.asPrismObject());
	}
	
	@Test
	public void test310SearchAllShadows() throws Exception {
		final String TEST_NAME = "test310SearchAllShadows";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		ObjectQuery query = IntegrationTestTools.createAllShadowsQuery(resourceType,
				SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME, prismContext);
		display("All shadows query", query);

		// WHEN
		List<PrismObject<ShadowType>> allShadows = provisioningService.searchObjects(ShadowType.class,
				query, null, result);
		
		// THEN
		result.computeStatus();
		display("searchObjects result", result);
		TestUtil.assertSuccess(result);
		
		display("Found " + allShadows.size() + " shadows");

		assertFalse("No shadows found", allShadows.isEmpty());
		
		checkConsistency(allShadows);
		
		for (PrismObject<ShadowType> shadow: allShadows) {
			assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
			assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
		}
		
		assertEquals("Wrong number of results", 2, allShadows.size());
	}
	
	// TODO: search
	
	private void checkAccountWill(ShadowType shadow, OperationResult result) {
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "RUM");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "At the moment?");
		assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Will Turner");
		assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
		assertNoAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
		assertEquals("Unexpected number of attributes", 8, attributes.size());
	}
}
