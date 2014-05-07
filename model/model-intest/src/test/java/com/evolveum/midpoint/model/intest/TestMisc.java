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

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import com.evolveum.midpoint.prism.PrismContext;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisc extends AbstractInitializedModelIntegrationTest {
		
	public TestMisc() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Test
    public void test100GetRepositoryDiag() throws Exception {
		final String TEST_NAME = "test100GetRepositoryDiag";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
   
        // WHEN
        RepositoryDiag diag = modelDiagnosticService.getRepositoryDiag(task, result);
        
        // THEN
		display("Diag", diag);
		result.computeStatus();
        TestUtil.assertSuccess("getRepositoryDiag result", result);

        assertEquals("Wrong implementationShortName", "SQL", diag.getImplementationShortName());
        assertNotNull("Missing implementationDescription", diag.getImplementationDescription());
        // TODO
	}
	
	@Test
    public void test110RepositorySelfTest() throws Exception {
		final String TEST_NAME = "test110RepositorySelfTest";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
   
        // WHEN
        OperationResult testResult = modelDiagnosticService.repositorySelfTest(task);
        
        // THEN
		display("Repository self-test result", testResult);
        TestUtil.assertSuccess("Repository self-test result", testResult);

        // TODO: check the number of tests, etc.
	}
	
	@Test
    public void test200ExportUsers() throws Exception {
		final String TEST_NAME = "test200ExportUsers";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
   
        // WHEN
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, 
        		SelectorOptions.createCollection(new ItemPath(), GetOperationOptions.createRaw()), task, result);
        
        // THEN
        result.computeStatus();
		display("Search users result", result);
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected number of users", 5, users.size());
        for (PrismObject<UserType> user: users) {
        	display("Exporting user", user);
        	assertNotNull("Null definition in "+user, user.getDefinition());
        	display("Definition", user.getDefinition());
        	String xmlString = prismContext.serializeObjectToString(user, PrismContext.LANG_XML);
        	display("Exported user", xmlString);
        	
        	Document xmlDocument = DOMUtil.parseDocument(xmlString);
    		Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
    		Validator validator = javaxSchema.newValidator();
    		validator.setResourceResolver(prismContext.getSchemaRegistry());
    		validator.validate(new DOMSource(xmlDocument));
    		
    		PrismObject<Objectable> parsedUser = prismContext.parseObject(xmlString);
    		assertTrue("Re-parsed user is not equal to original: "+user, user.equals(parsedUser));
    		
        }
        
	}

}
