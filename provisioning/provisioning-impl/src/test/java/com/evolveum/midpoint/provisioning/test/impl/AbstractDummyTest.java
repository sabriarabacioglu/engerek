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

import static org.testng.AssertJUnit.assertFalse;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.monitor.CachingStatistics;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ConnectorManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

/**
 * @author semancik
 *
 */
public abstract class AbstractDummyTest extends AbstractIntegrationTest {
	
	protected static final String TEST_DIR = "src/test/resources/impl/dummy/";
	
	public static final String RESOURCE_DUMMY_FILENAME = ProvisioningTestUtil.COMMON_TEST_DIR_FILENAME + "resource-dummy.xml";
	public static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	public static final String RESOURCE_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	
	protected static final String RESOURCE_DUMMY_NONEXISTENT_OID = "ef2bc95b-000-000-000-009900dddddd";

	protected static final String ACCOUNT_WILL_FILENAME = TEST_DIR + "account-will.xml";
	protected static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	protected static final String ACCOUNT_WILL_USERNAME = "Will";
	protected static final XMLGregorianCalendar ACCOUNT_WILL_ENABLE_TIMESTAMP = XmlTypeConverter.createXMLGregorianCalendar(2013, 5, 30, 12, 30, 42);

	protected static final String ACCOUNT_DAEMON_USERNAME = "daemon";
	protected static final String ACCOUNT_DAEMON_OID = "c0c010c0-dddd-dddd-dddd-dddddddae604";
	protected static final File ACCOUNT_DAEMON_FILE = new File(TEST_DIR, "account-daemon.xml");

	protected static final String ACCOUNT_DAVIEJONES_USERNAME = "daviejones";

	protected static final String ACCOUNT_MORGAN_FILENAME = TEST_DIR + "account-morgan.xml";
	protected static final String ACCOUNT_MORGAN_OID = "c0c010c0-d34d-b44f-f11d-444400008888";
	protected static final String ACCOUNT_MORGAN_NAME = "morgan";
	
	protected static final String ACCOUNT_LECHUCK_FILENAME = TEST_DIR + "account-lechuck.xml";
	protected static final String ACCOUNT_LECHUCK_OID = "c0c010c0-d34d-b44f-f11d-444400009aa9";
	protected static final String ACCOUNT_LECHUCK_NAME = "lechuck";
	
	protected static final String GROUP_PIRATES_FILENAME = TEST_DIR + "group-pirates.xml";
	protected static final String GROUP_PIRATES_OID = "c0c010c0-d34d-b44f-f11d-3332eeee0000";
	protected static final String GROUP_PIRATES_NAME = "pirates";
	
	protected static final String PRIVILEGE_PILLAGE_FILENAME = TEST_DIR + "privilege-pillage.xml";
	protected static final String PRIVILEGE_PILLAGE_OID = "c0c010c0-d34d-b44f-f11d-3332eeff0000";
	protected static final String PRIVILEGE_PILLAGE_NAME = "pillage";

	protected static final String FILENAME_ACCOUNT_SCRIPT = TEST_DIR + "account-script.xml";
	protected static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
	protected static final String FILENAME_ENABLE_ACCOUNT = TEST_DIR + "modify-will-enable.xml";
	protected static final String FILENAME_DISABLE_ACCOUNT = TEST_DIR + "modify-will-disable.xml";
	protected static final String FILENAME_MODIFY_ACCOUNT = TEST_DIR + "modify-will-fullname.xml";
	protected static final File FILE_SCRIPTS = new File(TEST_DIR, "scripts.xml");
	
	protected static final String NOT_PRESENT_OID = "deaddead-dead-dead-dead-deaddeaddead";
	
	protected static final String OBJECTCLAS_PRIVILEGE_LOCAL_NAME = "CustomprivilegeObjectClass";
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractDummyTest.class);

	private static final QName ASSOCIATION_GROUP_NAME = new QName(RESOURCE_DUMMY_NS, "group");
	private static final QName ASSOCIATION_PRIV_NAME = new QName(RESOURCE_DUMMY_NS, "priv");
	
	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	
	@Autowired(required = true)
	protected ProvisioningService provisioningService;

	// Used to make sure that the connector is cached
	@Autowired(required = true)
	protected ConnectorManager connectorManager;

	@Autowired(required = true)
	protected SynchornizationServiceMock syncServiceMock;
	
	@Autowired(required = true) 
	protected TaskManager taskManager;
	
	@Autowired(required = true)
	protected MatchingRuleRegistry matchingRuleRegistry;
	
	// Values used to check if something is unchanged or changed properly
	private Long lastResourceVersion = null;
	private ConnectorInstance lastConfiguredConnectorInstance;
	private CachingMetadataType lastCachingMetadata;
	private ResourceSchema lastResourceSchema = null;
	private RefinedResourceSchema lastRefinedResourceSchema;

	protected String daemonIcfUid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// We need to switch off the encryption checks. Some values cannot be encrypted as we do
		// not have a definition here
		InternalsConfig.encryptionChecks = false;
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(getResourceDummyFilename(), IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();

		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.setResource(resource);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();

		DummyAccount dummyAccountDaemon = new DummyAccount(ACCOUNT_DAEMON_USERNAME);
		dummyAccountDaemon.setEnabled(true);
		dummyAccountDaemon.addAttributeValues("fullname", "Evil Daemon");
		dummyResource.addAccount(dummyAccountDaemon);
		daemonIcfUid = dummyAccountDaemon.getId();

		PrismObject<ShadowType> shadowDaemon = PrismTestUtil.parseObject(ACCOUNT_DAEMON_FILE);
		if (!isIcfNameUidSame()) {
			setIcfUid(shadowDaemon, dummyAccountDaemon.getId());
		}
		repositoryService.addObject(shadowDaemon, null, initResult);
	}
	
	protected void setIcfUid(PrismObject<ShadowType> shadow, String icfUid) {
		PrismProperty<String> icfUidAttr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_UID));
		icfUidAttr.setRealValue(icfUid);
	}
	
	protected String getIcfUid(ShadowType shadowType) {
		return getIcfUid(shadowType.asPrismObject());
	}
	
	protected String getIcfUid(PrismObject<ShadowType> shadow) {
		PrismProperty<String> icfUidAttr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_UID));
		return icfUidAttr.getRealValue();
	}
	
	protected String getIcfName(PrismObject<ShadowType> shadow) {
		PrismProperty<String> icfUidAttr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_NAME));
		return icfUidAttr.getRealValue();
	}

	protected String getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILENAME;
	}
	
	protected File getAccountWillFile() {
		return new File(ACCOUNT_WILL_FILENAME);
	}
	
	protected boolean supportsActivation() {
		return true;
	}

	protected <T extends ShadowType> void checkConsistency(Collection<PrismObject<T>> shadows) throws SchemaException {
		for (PrismObject<T> shadow: shadows) {
			checkConsistency(shadow);
		}
	}
	
	protected void checkConsistency(PrismObject<? extends ShadowType> object) throws SchemaException {

		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".checkConsistency");
		
		PrismPropertyDefinition itemDef = ShadowUtil.getAttributesContainer(object).getDefinition().findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		
		LOGGER.info("item definition: {}", itemDef.debugDump());
		//TODO: matching rule
		EqualFilter equal = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, itemDef.getName()), itemDef, getWillRepoIcfName());
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		
		System.out.println("Looking for shadows of \"" + getWillRepoIcfName() + "\" with filter "
				+ query.debugDump());
		display("Looking for shadows of \"" + getWillRepoIcfName() + "\" with filter "
				+ query.debugDump());
		
		List<PrismObject<ShadowType>> objects = repositoryService.searchObjects(ShadowType.class, query,
				null, result);

		
		assertEquals("Wrong number of repo shadows for ICF NAME \"" + getWillRepoIcfName() + "\"", 1, objects.size());

	}
	
	protected <T> void assertAttribute(ShadowType shadow, String attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, attrName, expectedValues);
	}
	
	protected <T> void assertAttribute(ShadowType shadow, QName attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, attrName, expectedValues);
	}
	
	protected <T> void assertAttribute(ShadowType shadow, MatchingRule<T> matchingRule, QName attrName, T... expectedValues) {
		ProvisioningTestUtil.assertAttribute(resource, shadow, matchingRule, attrName, expectedValues);
	}
	
	protected <T> void assertNoAttribute(ShadowType shadow, String attrName) {
		ProvisioningTestUtil.assertNoAttribute(resource, shadow, attrName);
	}
	
	protected <T> void assertNoAttribute(PrismObject<ShadowType> shadow, String attrName) {
		ProvisioningTestUtil.assertNoAttribute(resource, shadow.asObjectable(), attrName);
	}
	
	protected void assertSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType);
	}
	
	protected DummyAccount getDummyAccount(String icfName, String icfUid) throws ConnectException, FileNotFoundException {
//		if (isNameUnique()) {
		if (isIcfNameUidSame()) {
			return dummyResource.getAccountByUsername(icfName);
		} else {
			 return dummyResource.getAccountById(icfUid);
		}
	}
	
	protected DummyAccount getDummyAccountAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException {
//		if (isNameUnique()) {
		if (isIcfNameUidSame()) {
			return dummyResource.getAccountByUsername(icfName);
		} else {
			 DummyAccount account = dummyResource.getAccountById(icfUid);
			 assertNotNull("No dummy account with ICF UID "+icfUid+" (expected name "+icfName+")", account);
			 assertEquals("Unexpected name in "+account, icfName, account.getName());
			 return account;
		}
	}
	
	protected DummyGroup getDummyGroup(String icfName, String icfUid) throws ConnectException, FileNotFoundException {
//		if (isNameUnique()) {
		if (isIcfNameUidSame()) {
			return dummyResource.getGroupByName(icfName);
		} else {
			 return dummyResource.getGroupById(icfUid);
		}
	}
	
	protected DummyGroup getDummyGroupAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException {
//		if (isNameUnique()) {
		if (isIcfNameUidSame()) {
			return dummyResource.getGroupByName(icfName);
		} else {
			 DummyGroup group = dummyResource.getGroupById(icfUid);
			 assertNotNull("No dummy group with ICF UID "+icfUid+" (expected name "+icfName+")", group);
			 assertEquals("Unexpected name in "+group, icfName, group.getName());
			 return group;
		}
	}
	
	protected DummyPrivilege getDummyPrivilege(String icfName, String icfUid) throws ConnectException, FileNotFoundException {
//		if (isNameUnique()) {
		if (isIcfNameUidSame()) {
			return dummyResource.getPrivilegeByName(icfName);
		} else {
			 return dummyResource.getPrivilegeById(icfUid);
		}
	}

	protected DummyPrivilege getDummyPrivilegeAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException {
//		if (isNameUnique()) {
		if (isIcfNameUidSame()) {
			return dummyResource.getPrivilegeByName(icfName);
		} else {
			 DummyPrivilege priv = dummyResource.getPrivilegeById(icfUid);
			 assertNotNull("No dummy privilege with ICF UID "+icfUid+" (expected name "+icfName+")", priv);
			 assertEquals("Unexpected name in "+priv, icfName, priv.getName());
			 return priv;
		}
	}

	protected <T> void assertDummyAccountAttributeValues(String accountName, String accountUid, String attributeName, T... expectedValues) throws ConnectException, FileNotFoundException {
		DummyAccount dummyAccount = getDummyAccountAssert(accountName, accountUid);
		assertNotNull("No account '"+accountName+"'", dummyAccount);
		assertDummyAttributeValues(dummyAccount, attributeName, expectedValues);
	}
	
	protected <T> void assertDummyAttributeValues(DummyObject object, String attributeName, T... expectedValues) {
		Set<T> attributeValues = (Set<T>) object.getAttributeValues(attributeName, expectedValues[0].getClass());
		assertNotNull("No attribute "+attributeName+" in "+object.getShortTypeName()+" "+object, attributeValues);
		TestUtil.assertSetEquals("Wroung values of attribute "+attributeName+" in "+object.getShortTypeName()+" "+object, attributeValues, expectedValues);
	}
	
	protected String getWillRepoIcfName() {
		return ACCOUNT_WILL_USERNAME;
	}
	
	protected boolean isIcfNameUidSame() {
		return true;
	}
	
	protected boolean isNameUnique() {
		return true;
	}
	
	protected void assertMember(DummyGroup group, String accountId) {
		IntegrationTestTools.assertGroupMember(group, accountId);
	}

	protected void assertNoMember(DummyGroup group, String accountId) {
		IntegrationTestTools.assertNoGroupMember(group, accountId);
	}
	
	protected void assertEntitlementGroup(PrismObject<ShadowType> account, String entitlementOid) {
		IntegrationTestTools.assertAssociation(account, ASSOCIATION_GROUP_NAME, entitlementOid);
	}
	
	protected void assertEntitlementPriv(PrismObject<ShadowType> account, String entitlementOid) {
		IntegrationTestTools.assertAssociation(account, ASSOCIATION_PRIV_NAME, entitlementOid);
	}
	
	protected <T extends ObjectType> void assertVersion(PrismObject<T> object, String expectedVersion) {
		assertEquals("Wrong version of "+object, expectedVersion, object.asObjectable().getVersion());
	}

	protected void rememberResourceVersion(String version) {
		lastResourceVersion = parseVersion(version);
	}
	
	protected void assertResourceVersionIncrement(PrismObject<ResourceType> resource, int expectedIncrement) {
		assertResourceVersionIncrement(resource.getVersion(), expectedIncrement);
	}
	
	protected void assertResourceVersionIncrement(String currentVersion, int expectedIncrement) {
		long currentVersionLong = parseVersion(currentVersion);
		long actualIncrement = currentVersionLong - lastResourceVersion;
		assertEquals("Unexpected increment in resource version", (long)expectedIncrement, actualIncrement);
		lastResourceVersion = currentVersionLong;
	}
	
	private long parseVersion(String stringVersion) {
		if (stringVersion == null) {
			AssertJUnit.fail("Version is null");
		}
		if (stringVersion.isEmpty()) {
			AssertJUnit.fail("Version is empty");
		}
		return Long.parseLong(stringVersion);
	}

	protected CachingMetadataType getSchemaCachingMetadata(PrismObject<ResourceType> resource) {
		ResourceType resourceType = resource.asObjectable();
		XmlSchemaType xmlSchemaTypeAfter = resourceType.getSchema();
		assertNotNull("No schema", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceType);
		assertNotNull("No schema XSD element", resourceXsdSchemaElementAfter);
		return xmlSchemaTypeAfter.getCachingMetadata();
	}
	
	protected void rememberSchemaMetadata(PrismObject<ResourceType> resource) {
		lastCachingMetadata = getSchemaCachingMetadata(resource);
	}
	
	protected void assertSchemaMetadataUnchanged(PrismObject<ResourceType> resource) {
		CachingMetadataType current = getSchemaCachingMetadata(resource);
		assertEquals("Schema caching metadata changed", lastCachingMetadata, current);
	}
	
	protected void rememberResourceSchema(ResourceSchema resourceSchema) {
		lastResourceSchema = resourceSchema;
	}
	
	protected void assertResourceSchemaUnchanged(ResourceSchema currentResourceSchema) {
		// We really want == there. We want to make sure that this is actually the same instance and that
		// it was properly cached
		assertTrue("Resource schema has changed", lastResourceSchema == currentResourceSchema);
	}
	
	protected void rememberRefinedResourceSchema(RefinedResourceSchema rResourceSchema) {
		lastRefinedResourceSchema = rResourceSchema;
	}
	
	protected void assertRefinedResourceSchemaUnchanged(RefinedResourceSchema currentRefinedResourceSchema) {
		// We really want == there. We want to make sure that this is actually the same instance and that
		// it was properly cached
		assertTrue("Refined resource schema has changed", lastRefinedResourceSchema == currentRefinedResourceSchema);
	}

	protected void assertHasSchema(PrismObject<ResourceType> resource, String desc) throws SchemaException {
		ResourceType resourceType = resource.asObjectable();
		display("Resource "+desc, resourceType);

		XmlSchemaType xmlSchemaTypeAfter = resourceType.getSchema();
		assertNotNull("No schema in "+desc, xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceType);
		assertNotNull("No schema XSD element in "+desc, resourceXsdSchemaElementAfter);

		String resourceXml = prismContext.serializeObjectToString(resource, PrismContext.LANG_XML);
//		display("Resource XML", resourceXml);                            

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata in "+desc, cachingMetadata);
		assertNotNull("No retrievalTimestamp in "+desc, cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber in "+desc, cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resource.toString(), prismContext);
		assertNotNull("No schema after parsing in "+desc, parsedSchema);
	}

	protected void rememberConnectorInstance(PrismObject<ResourceType> resource) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".rememberConnectorInstance");
		rememberConnectorInstance(connectorManager.getConfiguredConnectorInstance(resource, false, result));
	}
	
	protected void rememberConnectorInstance(ConnectorInstance currentConnectorInstance) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		lastConfiguredConnectorInstance = currentConnectorInstance;
	}
	
	protected void assertConnectorInstanceUnchanged(PrismObject<ResourceType> resource) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".rememberConnectorInstance");
		ConnectorInstance currentConfiguredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertTrue("Connector instance has changed", lastConfiguredConnectorInstance == currentConfiguredConnectorInstance);
	}
	
	protected void assertConnectorInstanceChanged(PrismObject<ResourceType> resource) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(TestDummyResourceAndSchemaCaching.class.getName()
				+ ".rememberConnectorInstance");
		ConnectorInstance currentConfiguredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertTrue("Connector instance has NOT changed", lastConfiguredConnectorInstance != currentConfiguredConnectorInstance);
		lastConfiguredConnectorInstance = currentConfiguredConnectorInstance;
	}
	
	protected void assertSteadyResource() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		assertResourceSchemaFetchIncrement(0);
		assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorSchemaParseIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertResourceSchemaParseCountIncrement(0);
		assertResourceVersionIncrement(resource, 0);
		assertSchemaMetadataUnchanged(resource);
		assertConnectorInstanceUnchanged(resource);
		
		display("Resource cache", InternalMonitor.getResourceCacheStats());
		// We do not assert hits, there may be a lot of them
		assertResourceCacheMissesIncrement(0);
	}

}
