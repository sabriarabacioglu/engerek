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
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class TestProjectorAddUser extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/addUser");

	private static final Trace LOGGER = TraceManager.getTrace(TestProjectorAddUser.class);
	
	@Autowired(required = true)
	private Projector projector;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	
	@Autowired(required = true)
	private ProvisioningService provisioning;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void addUserWithSimpleTemplate() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(AbstractLensTest.USER_DRAKE_FILE);
		UserType userType = user.asObjectable();
		PrismObject<ObjectTemplateType> userTemplate = PrismTestUtil.parseObject(new File(AbstractInternalModelIntegrationTest.USER_TEMPLATE_FILENAME));
		ObjectTemplateType userTemplateType = userTemplate.asObjectable();
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(AbstractInternalModelIntegrationTest.RESOURCE_DUMMY_FILENAME));
		ResourceType resourceType = resource.asObjectable();

		final String userOid = "10000000-0000-0000-0000-000000000001";
		final String accountOid = "10000000-0000-0000-0000-000000000004";

		when(
				provisioning.getObject(eq(ResourceType.class), eq(AbstractInternalModelIntegrationTest.RESOURCE_DUMMY_OID), 
						any(Collection.class),
						any(Task.class), any(OperationResult.class))).thenReturn(
				resourceType.asPrismObject());
		when(
				provisioning.addObject(any(PrismObject.class), any(OperationProvisioningScriptsType.class),
						any(ProvisioningOperationOptions.class), any(Task.class), any(OperationResult.class))).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				ShadowType account = (ShadowType) invocation.getArguments()[0];
				PrismAsserts.assertEquals(new File(TEST_FOLDER, "expected-account.xml"), account);

				return accountOid;
			}
		});
		when(repository.addObject(any(PrismObject.class), any(RepoAddOptions.class), any(OperationResult.class))).thenAnswer(
				new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation) throws Throwable {
						UserType user = (UserType) invocation.getArguments()[0];
						PrismAsserts.assertEquals(new File(TEST_FOLDER, "expected-user.xml"), user);
						return userOid;
					}
				});

		Task task = taskManager.createTaskInstance("Add User With Template");
		OperationResult result = task.getResult();
		
		LensContext<UserType> syncContext = new LensContext<UserType>(UserType.class, PrismTestUtil.getPrismContext(), provisioning);
		LensFocusContext<UserType> focusContext = syncContext.createFocusContext();

		ObjectDelta<UserType> objectDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD, prismContext);
		objectDelta.setObjectToAdd(user);
		
		focusContext.setObjectOld(null);
		focusContext.setObjectCurrent(null);
		focusContext.setObjectNew(user);
		focusContext.setPrimaryDelta(objectDelta);
		
		syncContext.setFocusTemplate(userTemplateType);
		
		syncContext.checkConsistence();

		try {
			LOGGER.info("provisioning: " + provisioning.getClass());
			LOGGER.info("repo" + repository.getClass());
						
			// WHEN
			projector.project(syncContext, "test", task, result);
			
		} finally {
			LOGGER.info(result.debugDump());
		}
		
		// THEN
		
		display("Context after sync",syncContext);
		
		// TODO
	}
}
