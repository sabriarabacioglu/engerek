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
import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.model.ModelWebService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.IHookCallBack;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class AbstractConfiguredModelIntegrationTest extends AbstractModelIntegrationTest {
			
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR_NAME, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final String USER_ADMINISTRATOR_FILENAME = COMMON_DIR_NAME + "/user-administrator.xml";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";
		
	protected static final String USER_TEMPLATE_FILENAME = COMMON_DIR_NAME + "/user-template.xml";
	protected static final String USER_TEMPLATE_OID = "10000000-0000-0000-0000-000000000002";
	
	protected static final String USER_TEMPLATE_COMPLEX_FILENAME = COMMON_DIR_NAME + "/user-template-complex.xml";
	protected static final String USER_TEMPLATE_COMPLEX_OID = "10000000-0000-0000-0000-000000000222";
	
	protected static final String USER_TEMPLATE_COMPLEX_INCLUDE_FILENAME = COMMON_DIR_NAME + "/user-template-complex-include.xml";
	protected static final String USER_TEMPLATE_COMPLEX_INCLUDE_OID = "10000000-0000-0000-0000-000000000223";
	
	protected static final String USER_TEMPLATE_SYNC_FILENAME = COMMON_DIR_NAME + "/user-template-sync.xml";
	protected static final String USER_TEMPLATE_SYNC_OID = "10000000-0000-0000-0000-000000000333";
	
	protected static final String CONNECTOR_LDAP_FILENAME = COMMON_DIR_NAME + "/connector-ldap.xml";
	
	protected static final String CONNECTOR_DBTABLE_FILENAME = COMMON_DIR_NAME + "/connector-dbtable.xml";
	
	protected static final String CONNECTOR_DUMMY_FILENAME = COMMON_DIR_NAME + "/connector-dummy.xml";
	
	protected static final String RESOURCE_OPENDJ_FILENAME = COMMON_DIR_NAME + "/resource-opendj.xml";
    protected static final String RESOURCE_OPENDJ_NAME = "Localhost OpenDJ";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
	protected static final File RESOURCE_DUMMY_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-deprecated.xml");
	protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	protected static final String RESOURCE_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004";
	
	// RED resource has STRONG mappings
	protected static final String RESOURCE_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/resource-dummy-red.xml";
	protected static final String RESOURCE_DUMMY_RED_OID = "10000000-0000-0000-0000-000000000104";
	protected static final String RESOURCE_DUMMY_RED_NAME = "red";
	protected static final String RESOURCE_DUMMY_RED_NAMESPACE = MidPointConstants.NS_RI;
	
	// BLUE resource has WEAK mappings
	protected static final File RESOURCE_DUMMY_BLUE_FILE = new File(COMMON_DIR, "resource-dummy-blue.xml");
	protected static final File RESOURCE_DUMMY_BLUE_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-blue-deprecated.xml");
	protected static final String RESOURCE_DUMMY_BLUE_OID = "10000000-0000-0000-0000-000000000204";
	protected static final String RESOURCE_DUMMY_BLUE_NAME = "blue";
	protected static final String RESOURCE_DUMMY_BLUE_NAMESPACE = MidPointConstants.NS_RI;
	
	// WHITE dummy resource has almost no configuration: no schema, no schemahandling, no synchronization, ...
	protected static final String RESOURCE_DUMMY_WHITE_FILENAME = COMMON_DIR_NAME + "/resource-dummy-white.xml";
	protected static final String RESOURCE_DUMMY_WHITE_OID = "10000000-0000-0000-0000-000000000304";
	protected static final String RESOURCE_DUMMY_WHITE_NAME = "white";
	protected static final String RESOURCE_DUMMY_WHITE_NAMESPACE = MidPointConstants.NS_RI;

    // YELLOW dummy resource is the same as default one but with strong asIs administrativeStatus mapping
    protected static final String RESOURCE_DUMMY_YELLOW_FILENAME = COMMON_DIR_NAME + "/resource-dummy-yellow.xml";
    protected static final String RESOURCE_DUMMY_YELLOW_OID = "10000000-0000-0000-0000-000000000704";
    protected static final String RESOURCE_DUMMY_YELLOW_NAME = "yellow";
    protected static final String RESOURCE_DUMMY_YELLOW_NAMESPACE = MidPointConstants.NS_RI;

    // Green dummy resource is authoritative
	protected static final File RESOURCE_DUMMY_GREEN_FILE = new File(COMMON_DIR, "resource-dummy-green.xml");
	protected static final File RESOURCE_DUMMY_GREEN_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-green-deprecated.xml");
	protected static final String RESOURCE_DUMMY_GREEN_OID = "10000000-0000-0000-0000-000000000404";
	protected static final String RESOURCE_DUMMY_GREEN_NAME = "green";
	protected static final String RESOURCE_DUMMY_GREEN_NAMESPACE = MidPointConstants.NS_RI;
	
	// Black dummy resource for testing tolerant attributes
	protected static final String RESOURCE_DUMMY_BLACK_FILENAME = COMMON_DIR_NAME + "/resource-dummy-black.xml";
	protected static final String RESOURCE_DUMMY_BLACK_OID = "10000000-0000-0000-0000-000000000305";
	protected static final String RESOURCE_DUMMY_BLACK_NAME = "black";
	protected static final String RESOURCE_DUMMY_BLACK_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String RESOURCE_DUMMY_SCHEMALESS_FILENAME = COMMON_DIR_NAME + "/resource-dummy-schemaless-no-schema.xml";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAME = "schemaless";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final String RESOURCE_DUMMY_FAKE_FILENAME = COMMON_DIR_NAME + "/resource-dummy-fake.xml";
	protected static final String RESOURCE_DUMMY_FAKE_OID = "10000000-0000-0000-0000-00000000000f";

	protected static final String ROLE_SUPERUSER_FILENAME = COMMON_DIR_NAME + "/role-superuser.xml";
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	protected static final File ROLE_PIRATE_FILE = new File(COMMON_DIR_NAME, "role-pirate.xml");
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";
    protected static final String ROLE_PIRATE_NAME = "Pirate";
    protected static final String ROLE_PIRATE_DESCRIPTION = "Scurvy Pirates";
	
	protected static final String ROLE_NICE_PIRATE_FILENAME = COMMON_DIR_NAME + "/role-nice-pirate.xml";
	protected static final String ROLE_NICE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556677";
	
	protected static final String ROLE_CAPTAIN_FILENAME = COMMON_DIR_NAME + "/role-captain.xml";
	protected static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-55555555cccc";

	// Excludes role "pirate"
	protected static final String ROLE_JUDGE_FILENAME = COMMON_DIR_NAME + "/role-judge.xml";
	protected static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";

	protected static final File USER_JACK_FILE = new File(COMMON_DIR_NAME, "user-jack.xml");
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";

	protected static final String USER_BARBOSSA_FILENAME = COMMON_DIR_NAME + "/user-barbossa.xml";
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	protected static final String USER_BARBOSSA_USERNAME = "barbossa";

	protected static final String USER_GUYBRUSH_FILENAME = COMMON_DIR_NAME + "/user-guybrush.xml";
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	
	// Largo does not have a full name set, employeeType=PIRATE
	protected static final File USER_LARGO_FILE = new File(COMMON_DIR, "user-largo.xml");
	protected static final String USER_LARGO_OID = "c0c010c0-d34d-b33f-f00d-111111111118";
	protected static final String USER_LARGO_USERNAME = "largo";
	
	// Rapp does not have a full name set, employeeType=COOK
	protected static final File USER_RAPP_FILE = new File(COMMON_DIR, "user-rapp.xml");
	protected static final String USER_RAPP_OID = "c0c010c0-d34d-b33f-f00d-11111111c008";
	protected static final String USER_RAPP_USERNAME = "rapp";

	// Herman has a validity dates set in the activation part
	protected static final File USER_HERMAN_FILE = new File(COMMON_DIR_NAME, "user-herman.xml");
	protected static final String USER_HERMAN_OID = "c0c010c0-d34d-b33f-f00d-111111111122";
	protected static final String USER_HERMAN_USERNAME = "herman";

	// Has null name, doesn not have given name, no employeeType
	protected static final String USER_THREE_HEADED_MONKEY_FILENAME = COMMON_DIR_NAME + "/user-three-headed-monkey.xml";
	protected static final String USER_THREE_HEADED_MONKEY_OID = "c0c010c0-d34d-b33f-f00d-110011001133";
	
	// Elaine has account on the dummy resources (default, red, blue)
	// The accounts are also assigned
	static final String USER_ELAINE_FILENAME = COMMON_DIR_NAME + "/user-elaine.xml";
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	// Captain Kate Capsize does not exist in the repo. This user is designed to be added. 
	// She has account on dummy resources (default, red, blue)
	// The accounts are also assigned
	static final File USER_CAPSIZE_FILE = new File(COMMON_DIR, "user-capsize.xml");
	protected static final String USER_CAPSIZE_OID = "c0c010c0-d34d-b33f-f00d-11c1c1c1c11c";
	protected static final String USER_CAPSIZE_USERNAME = "capsize";
	
	protected static final File USER_DRAKE_FILE = new File(COMMON_DIR_NAME, "user-drake.xml");
	protected static final String USER_DRAKE_OID = "c0c010c0-d34d-b33f-f00d-11d1d1d1d1d1";
	protected static final String USER_DRAKE_USERNAME = "drake";
	
	public static final File ACCOUNT_JACK_DUMMY_FILE = new File(COMMON_DIR, "account-jack-dummy.xml");
	public static final String ACCOUNT_JACK_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/account-jack-dummy-red.xml";
	public static final String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
	public static final String ACCOUNT_JACK_DUMMY_FULLNAME = "Jack Sparrow";
	
	public static final String ACCOUNT_HERMAN_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-herman-dummy.xml";
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";
	
	public static final String ACCOUNT_HERMAN_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-herman-opendj.xml";
	public static final String ACCOUNT_HERMAN_OPENDJ_OID = "22220000-2200-0000-0000-333300003333";
	
	public static final String ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-guybrush-dummy.xml";
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-guybrush-dummy.xml";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/account-guybrush-dummy-red.xml";
	
	public static final String ACCOUNT_SHADOW_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-jack-dummy.xml";
	
	public static final String ACCOUNT_DAVIEJONES_DUMMY_USERNAME = "daviejones";
	public static final String ACCOUNT_CALYPSO_DUMMY_USERNAME = "calypso";
	
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-22220004000e";
	public static final String ACCOUNT_ELAINE_DUMMY_USERNAME = USER_ELAINE_USERNAME;
	
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_RED_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy-red.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID = "c0c010c0-d34d-b33f-f00d-22220104000e";
	public static final String ACCOUNT_ELAINE_DUMMY_RED_USERNAME = USER_ELAINE_USERNAME;

	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/account-elaine-dummy-blue.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID = "c0c010c0-d34d-b33f-f00d-22220204000e";
	public static final String ACCOUNT_ELAINE_DUMMY_BLUE_USERNAME = USER_ELAINE_USERNAME;
	
	public static final File GROUP_PIRATE_DUMMY_FILE = new File(COMMON_DIR, "group-pirate-dummy.xml");
	public static final String GROUP_PIRATE_DUMMY_NAME = "pirate";
	public static final String GROUP_PIRATE_DUMMY_DESCRIPTION = "Scurvy pirates";
	
	public static final File SHADOW_GROUP_DUMMY_TESTERS_FILE = new File(COMMON_DIR, "group-testers-dummy.xml");
	public static final String SHADOW_GROUP_DUMMY_TESTERS_OID = "20000000-0000-0000-3333-000000000002";
	public static final String GROUP_DUMMY_TESTERS_NAME = "testers";
	public static final String GROUP_DUMMY_TESTERS_DESCRIPTION = "To boldly go where no pirate has gone before";
	
	protected static final String PASSWORD_POLICY_GLOBAL_FILENAME = COMMON_DIR_NAME + "/password-policy-global.xml";
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";
	
	protected static final String ORG_MONKEY_ISLAND_FILENAME = COMMON_DIR_NAME + "/org-monkey-island.xml";
	protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	protected static final String ORG_MINISTRY_OF_OFFENSE_OID = "00000000-8888-6666-0000-100000000003";
	protected static final String ORG_MINISTRY_OF_RUM_OID = "00000000-8888-6666-0000-100000000004";
	protected static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";
	protected static final String ORG_SAVE_ELAINE_OID = "00000000-8888-6666-0000-200000000001";
	
	protected static final String TASK_RECONCILE_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy.xml";
	protected static final String TASK_RECONCILE_DUMMY_OID = "10000000-0000-0000-5656-565600000004";
	
	protected static final String TASK_RECONCILE_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy-blue.xml";
	protected static final String TASK_RECONCILE_DUMMY_BLUE_OID = "10000000-0000-0000-5656-565600000204";
	
	protected static final String TASK_RECONCILE_DUMMY_GREEN_FILENAME = COMMON_DIR_NAME + "/task-reconcile-dummy-green.xml";
	protected static final String TASK_RECONCILE_DUMMY_GREEN_OID = "10000000-0000-0000-5656-565600000404";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_FILENAME = COMMON_DIR_NAME + "/task-dumy-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_OID = "10000000-0000-0000-5555-555500000004";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME = COMMON_DIR_NAME + "/task-dumy-blue-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_OID = "10000000-0000-0000-5555-555500000204";
	
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_FILENAME = COMMON_DIR_NAME + "/task-dumy-green-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_OID = "10000000-0000-0000-5555-555500000404";
	
	protected static final String TASK_VALIDITY_SCANNER_FILENAME = COMMON_DIR_NAME + "/task-validity-scanner.xml";
	protected static final String TASK_VALIDITY_SCANNER_OID = "10000000-0000-0000-5555-555505060400";
	
	protected static final File TASK_TRIGGER_SCANNER_FILE = new File(COMMON_DIR_NAME, "task-trigger-scanner.xml");
	protected static final String TASK_TRIGGER_SCANNER_OID = "00000000-0000-0000-0000-000000000007";
	
	protected static final File TASK_MOCK_JACK_FILE = new File(COMMON_DIR_NAME, "task-mock-jack.xml");
	protected static final String TASK_MOCK_JACK_OID = "10000000-0000-0000-5656-565674633311";
	
	protected static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
	protected static final QName PIRACY_SHIP = new QName(NS_PIRACY, "ship");
	protected static final QName PIRACY_TALES = new QName(NS_PIRACY, "tales");
	protected static final QName PIRACY_WEAPON = new QName(NS_PIRACY, "weapon");
	protected static final QName PIRACY_LOOT = new QName(NS_PIRACY, "loot");
	protected static final QName PIRACY_BAD_LUCK = new QName(NS_PIRACY, "badLuck");
	protected static final QName PIRACY_FUNERAL_TIMESTAMP = new QName(NS_PIRACY, "funeralTimestamp");
	protected static final QName PIRACY_SEA_QNAME = new QName(NS_PIRACY, "sea");
	protected static final QName PIRACY_COLORS = new QName(NS_PIRACY, "colors");

    protected static final ItemPath ROLE_EXTENSION_COST_CENTER_PATH = new ItemPath(RoleType.F_EXTENSION, new QName(NS_PIRACY, "costCenter"));

    protected static final String DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME = "sea";
	
	// Authorizations
	
	protected static final String NS_TEST_AUTZ = "http://midpoint.evolveum.com/xml/ns/test/authorization";
	protected static final QName AUTZ_LOOT_QNAME = new QName(NS_TEST_AUTZ, "loot");
	protected static final String AUTZ_LOOT_URL = QNameUtil.qNameToUri(AUTZ_LOOT_QNAME);
	protected static final QName AUTZ_COMMAND_QNAME = new QName(NS_TEST_AUTZ, "command");
	protected static final String AUTZ_COMMAND_URL = QNameUtil.qNameToUri(AUTZ_COMMAND_QNAME);
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractConfiguredModelIntegrationTest.class);
	
	protected PrismObject<UserType> userAdministrator;
		
	public AbstractConfiguredModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask,  OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
			
		modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(getSystemConfigurationFile(), SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		// Users
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, RoleType.class, initResult);
		login(userAdministrator);
	}
    	
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Override
	protected Task createTask(String operationName) {
		Task task = taskManager.createTaskInstance(operationName);
		task.setOwner(userAdministrator);
		return task;
	}
	

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> run start");
        super.run(callBack, testResult);
        LOGGER.info("###>>> run end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @AfterClass
    @Override
    protected void springTestContextAfterTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestClass start");
        super.springTestContextAfterTestClass();

        nullAllFields(this, getClass());

        LOGGER.info("###>>> springTestContextAfterTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    /**
     * This method null all fields which are not static, final or primitive type.
     *
     * All this is just to make GC work during DirtiesContext after every test class,
     * because memory consumption is too big. Test class instances can't be GCed
     * immediately. If they holds autowired fields like sessionFactory (for example
     * through SqlRepositoryService impl), their memory footprint is getting big.
     *
     * @param forClass
     * @throws Exception
     */
    public static void nullAllFields(Object object, Class forClass) throws Exception{
        if (forClass.getSuperclass() != null) {
            nullAllFields(object, forClass.getSuperclass());
        }

        for (Field field : forClass.getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers())
                    || Modifier.isStatic(field.getModifiers())
                    || field.getType().isPrimitive()) {
                continue;
            }

            nullField(object, field);
        }
    }

    private static void nullField(Object obj, Field field) throws Exception {
        LOGGER.info("Setting {} to null on {}.", new Object[]{field.getName(), obj.getClass().getSimpleName()});
        boolean accessible = field.isAccessible();
        if (!accessible) {
            field.setAccessible(true);
        }
        field.set(obj, null);
        field.setAccessible(accessible);
    }

    @AfterMethod
    @Override
    protected void springTestContextAfterTestMethod(Method testMethod) throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestMethod start");
        super.springTestContextAfterTestMethod(testMethod);
        LOGGER.info("###>>> springTestContextAfterTestMethod end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeClass
    @Override
    protected void springTestContextBeforeTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextBeforeTestClass start");
        super.springTestContextBeforeTestClass();
        LOGGER.info("###>>> springTestContextBeforeTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeMethod
    @Override
    protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextBeforeTestMethod start");
        super.springTestContextBeforeTestMethod(testMethod);
        LOGGER.info("###>>> springTestContextBeforeTestMethod end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeClass
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextPrepareTestInstance start");
        super.springTestContextPrepareTestInstance();
        LOGGER.info("###>>> springTestContextPrepareTestInstance end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }
    
    protected PrismSchema getPiracySchema() {
    	return prismContext.getSchemaRegistry().findSchemaByNamespace(NS_PIRACY);
    }
    
    protected void assertLastRecomputeTimestamp(String taskOid, XMLGregorianCalendar startCal, XMLGregorianCalendar endCal) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<TaskType> task = getTask(taskOid);
		display("Task", task);
        PrismContainer<?> taskExtension = task.getExtension();
        assertNotNull("No task extension", taskExtension);
        PrismProperty<XMLGregorianCalendar> lastRecomputeTimestampProp = taskExtension.findProperty(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
        assertNotNull("no lastRecomputeTimestamp property", lastRecomputeTimestampProp);
        XMLGregorianCalendar lastRecomputeTimestamp = lastRecomputeTimestampProp.getRealValue();
        assertNotNull("null lastRecomputeTimestamp", lastRecomputeTimestamp);
        IntegrationTestTools.assertBetween("lastRecomputeTimestamp", startCal, endCal, lastRecomputeTimestamp);
	}
}
