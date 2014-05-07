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
package com.evolveum.midpoint.model.common.mapping;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertTripleNoMinus;
import static com.evolveum.midpoint.prism.util.PrismAsserts.assertTripleZero;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * @author Radovan Semancik
 */
public class TestMappingDynamicSimple {
	
	private static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	private static final String PATTERN_NUMERIC = "^\\d+$";
	
	private MappingTestEvaluator evaluator;
	    
    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new MappingTestEvaluator();
    	evaluator.init();
    }
    
    @Test
    public void testValueSingleDeep() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-single-deep.xml",
    			"testValue",
    			"costCenter",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueSingleShallow() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-single-shallow.xml",
    			"testValue",
    			"costCenter",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueMultiDeep() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-multi-deep.xml",
    			"testValueMulti",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testValueMultiShallow() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-multi-shallow.xml",
    			"testValueMulti",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testValueSingleEnum() throws Exception {
        // WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ActivationStatusType>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-single-enum.xml",
    			"testValueSingleEnum",
    			new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),				// target
    			"employeeType",				// changed property
    			"CAPTAIN");					// changed values
    	
        // THEN
    	PrismAsserts.assertTripleZero(outputTriple, ActivationStatusType.ENABLED);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testAsIsAdd() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-asis.xml",
    			"testAsIsAdd",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "PIRATE");
    	PrismAsserts.assertTriplePlus(outputTriple, "CAPTAIN", "SWASHBUCKLER");
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testAsIsDelete() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicDelete(
    			"mapping-asis.xml",
    			"testAsIsDelete",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"PIRATE");					// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleMinus(outputTriple, "PIRATE");    	
    }
    
    
    @Test
    public void testAsIsStringToPolyString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-asis.xml",
    			"testAsIsStringToPolyString",
    			"fullName");				// target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("PIRATE"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testAsIsStringToProtectedString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateMapping(
    			"mapping-asis.xml",
    			"testAsIsStringToProtectedString",
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)); // target
    	
    	// THEN
    	evaluator.assertProtectedString("output zero set", outputTriple.getZeroSet(), "PIRATE");    	
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
        
    @Test
    public void testAsIsProtectedStringToProtectedString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateMapping(
    			"mapping-asis-password.xml",
    			"testAsIsProtectedStringToProtectedString",
    			new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)); // target
    	
    	// THEN
    	evaluator.assertProtectedString("output zero set", outputTriple.getZeroSet(), "d3adM3nT3llN0Tal3s");    	
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testAsIsProtectedStringToString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-asis-password.xml",
    			"testAsIsProtectedStringToString",
    			UserType.F_EMPLOYEE_NUMBER); // target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "d3adM3nT3llN0Tal3s");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testAsIsProtectedStringToPolyString() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMapping(
    			"mapping-asis-password.xml",
    			"testAsIsProtectedStringToPolyString",
    			UserType.F_FULL_NAME); // target
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("d3adM3nT3llN0Tal3s"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    
    @Test
    public void testPathVariables() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables.xml",
    			"testPathVariables",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathExtensionProperty() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
    			"mapping-path-extension-variable.xml",
    			"testPathExtensionProperty",
    			ShadowType.F_NAME				// target
    			);	// changed values
    	
    	// THEN
    	assertNull("expected null triple", outputTriple);
//    	PrismAsserts.assertTripleNoZero(outputTriple);
//    	PrismAsserts.assertTripleNoPlus(outputTriple);
//    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesNamespace() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-namespace.xml",
    			"testPathVariablesNamespace",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    
    @Test
    public void testPathVariablesPolyStringShort() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-short.xml",
    			"testPathVariablesPolyStringShort",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesPolyStringToStringShort() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-short.xml",
    			"testPathVariablesPolyStringToStringShort",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Jack Sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesExtension() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-long.xml",
    			"testPathVariablesPolyStringToStringLong",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testPathVariablesPolyStringToStringLong() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-path-system-variables-polystring-long.xml",
    			"testPathVariablesPolyStringToStringLong",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptSimpleXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-simple-xpath.xml",
    			"testScriptSimpleXPath",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "fooBAR");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptSimpleGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-simple-groovy.xml",
    			"testScriptSimpleXPath",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "fooBAR");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-variables-xpath.xml",
    			"testScriptVariablesXPath",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-variables-groovy.xml",
    			"testScriptVariablesGroovy",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testScriptVariablesPolyStringXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-xpath.xml",
    			"testScriptVariablesPolyStringXPath",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow", "captain jack sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain Jack Sparrow", "captain jack sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovyOp() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy-op.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, new PolyString("Captain J. Sparrow", "captain j sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovyOrig() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy-orig.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain J");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
        
    @Test
    public void testScriptVariablesPolyStringGroovyNorm() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-system-variables-polystring-groovy-norm.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Captain j");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }

    @Test
    public void testScriptVariablesPolyStringGroovyNormReplace() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-polystring-groovy-norm.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"fullName",				// changed property
    			PrismTestUtil.createPolyString("Barbossa"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, "Captain b");
    	PrismAsserts.assertTripleMinus(outputTriple, "Captain j");    	
    }
    
    @Test
    public void testScriptVariablesPolyStringGroovyNormReplaceNull() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-system-variables-polystring-groovy-norm.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"fullName"				// changed property
    			);	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleMinus(outputTriple, "Captain j");    	
    }

    @Test
    public void testAsIsVariablesPolyStringNorm() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-asis-system-variables-polystring-norm.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "jack sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }
    
    @Test
    public void testAsIsVariablesPolyStringOrig() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-asis-system-variables-polystring-orig.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"description",					// target
    			"employeeType",				// changed property
    			"CAPTAIN", "SWASHBUCKLER");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "Jack Sparrow");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);    	
    }


    @Test
    public void testScriptExtraVariablesRef() throws Exception {
    	// GIVEN
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-extra-variables.xml", 
    			"testScriptExtraVariablesRef", "employeeType", null);
    	
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	ObjectReferenceType ref = MiscSchemaUtil.createObjectReference(
        	"c0c010c0-d34d-b33f-f00d-111111111112",
        	UserType.COMPLEX_TYPE);
        vars.put(new QName(SchemaConstants.NS_C, "sailor"), ref);
        mapping.addVariableDefinitions(vars);
        
    	OperationResult opResult = new OperationResult("testScriptExtraVariablesRef");
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptExtraVariablesJaxb() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptExtraVariablesJaxb";
    	TestUtil.displayTestTile(TEST_NAME);
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-extra-variables.xml", 
    			TEST_NAME, "employeeType", null);
    	
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	UserType userType = (UserType) PrismTestUtil.parseObject(
                new File(MidPointTestConstants.OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111112.xml")).asObjectable();
        vars.put(new QName(SchemaConstants.NS_C, "sailor"), userType);
        mapping.addVariableDefinitions(vars);
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, "Captain barbossa");
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testScriptFullNameNoChange() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMapping(
    			"mapping-script-fullname.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName");					// target
    			
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testScriptFullNameReplaceGivenName() throws Exception {
    	final String TEST_NAME = "testScriptFullNameReplaceGivenName";
    	TestUtil.displayTestTile(TEST_NAME);
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-fullname.xml",
    			TEST_NAME,
    			"fullName",					// target
    			"givenName",				// changed property
    			PrismTestUtil.createPolyString("Jackie"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie Sparrow"));
    	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }
    
    @Test
    public void testScriptFullNameDeleteGivenName() throws Exception {
    	final String TEST_NAME = "testScriptFullNameDeleteGivenName";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_GIVEN_NAME, evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
    	Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
    			"mapping-script-fullname.xml",
    			TEST_NAME,
    			"fullName",					// target
    			delta);
    	
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Sparrow"));
    	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }
    
    @Test
    public void testScriptFullNameDeleteGivenNameFromNull() throws Exception {
    	final String TEST_NAME = "testScriptFullNameDeleteGivenNameFromNull";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_GIVEN_NAME, evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
    	PrismObject<UserType> userOld = evaluator.getUserOld();
		userOld.asObjectable().setGivenName(null);
    	
    	Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
    			"mapping-script-fullname.xml",
    			TEST_NAME,
    			"fullName",					// target
    			delta, userOld);
    	
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Sparrow"));
    	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }
    
    @Test
    public void testScriptFullNameDeleteGivenNameFamilyName() throws Exception {
    	final String TEST_NAME = "testScriptFullNameDeleteGivenNameFamilyName";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_GIVEN_NAME, evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	delta.addModificationDeleteProperty(UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString("Sparrow"));
    	
    	Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
    			"mapping-script-fullname.xml",
    			TEST_NAME,
    			"fullName",					// target
    			delta);
    	
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("John Doe"));
    	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }
    
    /**
     * Change an unrelated property. See that it does not die.
     */
    @Test
    public void testScriptFullNameReplaceEmployeeNumber() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-fullname.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"employeeNumber",				// changed property
    			"666");	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    /**
     * Return type is a date, script returns string.
     */
   @Test
   public void testScriptDateGroovy() throws Exception {
   	// WHEN
   	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
   			"mapping-script-date-groovy.xml",
   			"testScriptDateGroovy",
   			new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALID_FROM),	// target
   			"employeeNumber",				// changed property
   			"1975-05-30");	// changed values
   	
   	// THEN
   	PrismAsserts.assertTripleZero(outputTriple);
   	PrismAsserts.assertTriplePlus(outputTriple, XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0));
   	PrismAsserts.assertTripleNoMinus(outputTriple);
   }


    @Test
    public void testScriptRootNodeRef() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptRootNodeRef";
    	TestUtil.displayTestTile(TEST_NAME);
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-root-node.xml", 
    			TEST_NAME, "locality", null);
    	
        mapping.setRootNode(MiscSchemaUtil.createObjectReference(
            	"c0c010c0-d34d-b33f-f00d-111111111111",
            	UserType.COMPLEX_TYPE));
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, new PolyString("Black Pearl", "black pearl"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptRootNodeJaxb() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptRootNodeJaxb";
    	TestUtil.displayTestTile(TEST_NAME);
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-root-node.xml", 
    			TEST_NAME, "locality", null);
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(new File(MidPointTestConstants.OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"));
        mapping.setRootNode(user.asObjectable());
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, new PolyString("Black Pearl", "black pearl"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptListRelativeXPath() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-list-relative-xpath.xml",
    			"testScriptListRelativeXPath",
    			"organizationalUnit",					// target
    			"organizationalUnit",				// changed property
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple,
    			PrismTestUtil.createPolyString("The Guild of Brethren of the Coast"), 
    			PrismTestUtil.createPolyString("The Guild of Davie Jones' Locker"));
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The Guild of Antropomorphic Personifications"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptListRelativeGroovy() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-script-list-relative-groovy.xml",
    			"testScriptListRelativeXPath",
    			"organizationalUnit",					// target
    			"organizationalUnit",				// changed property
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple,
    			PrismTestUtil.createPolyString("The Guild of Brethren of the Coast"), 
    			PrismTestUtil.createPolyString("The Guild of Davie Jones' Locker"));
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The Guild of Antropomorphic Personifications"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    
    @Test
    public void testScriptListAbsoluteXPath() throws Exception {
    	testScriptListAbsolute("mapping-script-list-absolute-xpath.xml");
    }
    
    @Test
    public void testScriptListAbsoluteGroovy() throws Exception {
    	testScriptListAbsolute("mapping-script-list-absolute-groovy.xml");
    }
    

    public void testScriptListAbsolute(String fileName) throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			fileName,
    			"testScriptListAbsolute",
    			"organizationalUnit",					// target
    			"organizationalUnit",				// changed property
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, 
    			PrismTestUtil.createPolyString("Brethren of the Coast"), 
    			PrismTestUtil.createPolyString("Davie Jones' Locker"));
    	PrismAsserts.assertTriplePlus(outputTriple,
    			PrismTestUtil.createPolyString("Antropomorphic Personifications"));
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
        
    @Test
    public void testValueConditionTrue() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-condition-true.xml",
    			"testValueConditionTrue",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"DRUNKARD");				// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleZero(outputTriple, "foobar");
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testValueConditionFalse() throws Exception {
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicAdd(
    			"mapping-value-condition-false.xml",
    			"testValueConditionFalse",
    			"employeeType",				// target
    			"employeeType",				// changed property
    			"DRUNKARD");				// changed values
    	
    	// THEN
    	assertNull("Unexpected value in outputTriple", outputTriple);
    }
    
    
    
    @Test
    public void testConditionNonEmptyCaptain() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testConditionNonEmptyCaptain";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("CAPTAIN");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-condition-nonempty.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The CAPTAIN"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testConditionNonEmptyEmpty() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testConditionNonEmptyEmpty";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-condition-nonempty.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple", outputTriple);
    }
    
    @Test
    public void testConditionNonEmptyNoValue() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testConditionNonEmptyNoValue";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-condition-nonempty.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple", outputTriple);
    }
    
    public void testScriptTransformMulti() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrueXPath";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, evaluator.USER_OLD_OID, evaluator.getPrismContext());
    	PropertyDelta<String> propDelta = delta.createPropertyModification(evaluator.toPath("employeeType"));
    	propDelta.addValueToAdd(new PrismPropertyValue<String>("CAPTAIN"));
    	propDelta.addValueToDelete(new PrismPropertyValue<String>("LANDLUBER"));
    	delta.addModification(propDelta);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-script-transform.xml", 
    			TEST_NAME, "organizationalUnit", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("LANDLUBER");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(null, opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("The pirate deck"));
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The captain deck"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("The landluber deck"));
    }
    
    @Test
    public void testInboundMapping() throws Exception{
    	final String TEST_NAME = "testInboundMapping";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(MappingTestEvaluator.TEST_DIR + "/account-inbound-mapping.xml"));
    	Item oldItem = account.findItem(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaTestConstants.ICFS_NAME));
    	ItemDelta delta = PropertyDelta.createModificationAddProperty(SchemaTestConstants.ICFS_NAME_PATH, (PrismPropertyDefinition) oldItem.getDefinition(), ((PrismPropertyValue) oldItem.getValue(0)).getValue());
    	
    	PrismObject<UserType> user = evaluator.getUserDefinition().instantiate();
    	
    	Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createInboudMapping("mapping-inbound.xml", TEST_NAME, delta, user.asObjectable(), account.asObjectable(), null, null);
    	
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	mapping.evaluate(null, opResult);
    	
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
    	assertTripleZero(outputTriple, PrismTestUtil.createPolyString("pavolr"));
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testGenerateDefault() throws Exception {
    	final String TEST_NAME = "testGenerateDefault";
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	final StringPolicyType stringPolicy = evaluator.getStringPolicy();
    	// GIVEN
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-generate.xml", 
    			TEST_NAME, stringPolicy, "employeeNumber", null);
    	
//    	StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
//			private ItemPath outputPath;
//			private ItemDefinition outputDefinition;
//			@Override
//			public void setOutputPath(ItemPath outputPath) {
//				this.outputPath = outputPath;
//			}
//			
//			@Override
//			public void setOutputDefinition(ItemDefinition outputDefinition) {
//				this.outputDefinition = outputDefinition;
//			}
//			
//			@Override
//			public StringPolicyType resolve() {
//				// No path. The the path is default
////				assertNotNull("Null outputPath", outputPath);
//				assertNotNull("Null outputDefinition", outputDefinition);
//				return stringPolicy;
//			}
//		};
    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(null, opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertGeneratedValue(value1, stringPolicy, null, false);

		// WHEN (2)
		mapping.evaluate(null, opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertGeneratedValue(value2, stringPolicy, null, false);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
    }

    @Test
    public void testGeneratePolicy() throws Exception {
    	final String TEST_NAME = "testGeneratePolicy";
    	generatePolicy(TEST_NAME, "mapping-generate-policy.xml", "c0c010c0-d34d-b33f-f00d-999888111111.xml", null);
    }
    
    @Test
    public void testGeneratePolicyBad() throws Exception {
    	final String TEST_NAME = "testGeneratePolicy";
    	try {
    		generatePolicy(TEST_NAME, "mapping-generate-policy-bad.xml", "c0c010c0-d34d-b33f-f00d-999888111113.xml", null);
    		AssertJUnit.fail("Unexpected success");
    	} catch (ExpressionEvaluationException e) {
    		// This is expected, the policy is broken
    	}
    }
    
    @Test
    public void testGeneratePolicyNumericString() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericString";
    	generatePolicy(TEST_NAME, "mapping-generate-policy-numeric.xml", "c0c010c0-d34d-b33f-f00d-999888111112.xml", 
    			PATTERN_NUMERIC);
    }
    	
    private void generatePolicy(final String TEST_NAME, String mappingFileName, String policyFileName, String pattern)
    		throws Exception {
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	// This is just for validation. The expression has to resolve reference of its own
    	PrismObject<ValuePolicyType> valuePolicy = PrismTestUtil.parseObject(
    			new File(MidPointTestConstants.OBJECTS_DIR, policyFileName));
    	final StringPolicyType stringPolicy = valuePolicy.asObjectable().getStringPolicy();
    	// GIVEN
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping(mappingFileName, 
    			TEST_NAME, stringPolicy, "employeeNumber", null);
    	    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(null, opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertNotNull("Generated null value", value1);
		assertGeneratedValue(value1, stringPolicy, pattern, false);

		// WHEN (2)
		mapping.evaluate(null, opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertNotNull("Generated null value", value2);
		assertGeneratedValue(value2, stringPolicy, pattern, false);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
    }
        
	private void assertGeneratedValue(String value, StringPolicyType stringPolicy, String pattern, boolean ignoreMin) {
		if (stringPolicy == null) {
			assertEquals("Unexpected generated value length", GenerateExpressionEvaluator.DEFAULT_LENGTH, value.length());
		} else {
			if (!ignoreMin) {
				assertTrue("Value '"+value+"' too short, minLength="+stringPolicy.getLimitations().getMinLength()+", length="+value.length(), value.length() >= stringPolicy.getLimitations().getMinLength());
			}
			assertTrue("Value '"+value+"' too long, maxLength="+stringPolicy.getLimitations().getMaxLength()+", length="+value.length(), value.length() <= stringPolicy.getLimitations().getMaxLength());
			// TODO: better validation
		}
		if (pattern != null) {
			assertTrue("Value '"+value+"' does not match pattern '"+pattern+"'", value.matches(pattern));
		}
	}

	@Test
    public void testGeneratePolicyNumericInt() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericInt";
    	generatePolicyNumeric(TEST_NAME, "mapping-generate-policy-numeric.xml", 
    			"c0c010c0-d34d-b33f-f00d-999888111112.xml", "intType", Integer.class);
    }
	
	@Test
    public void testGeneratePolicyNumericInteger() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericInt";
    	generatePolicyNumeric(TEST_NAME, "mapping-generate-policy-numeric.xml", 
    			"c0c010c0-d34d-b33f-f00d-999888111112.xml", "integerType", Integer.class);
    }
	
	@Test
    public void testGeneratePolicyNumericLong() throws Exception {
    	final String TEST_NAME = "testGeneratePolicyNumericInt";
    	generatePolicyNumeric(TEST_NAME, "mapping-generate-policy-numeric.xml", 
    			"c0c010c0-d34d-b33f-f00d-999888111112.xml", "longType", Long.class);
    }
    	
    private <T> void generatePolicyNumeric(final String TEST_NAME, String mappingFileName,
    		String policyFileName, String extensionPropName, Class<T> clazz) throws Exception {
    	TestUtil.displayTestTile(TEST_NAME);
    	
    	// This is just for validation. The expression has to resolve reference of its own
    	PrismObject<ValuePolicyType> valuePolicy = PrismTestUtil.parseObject(
    			new File(MidPointTestConstants.OBJECTS_DIR, policyFileName));
    	final StringPolicyType stringPolicy = valuePolicy.asObjectable().getStringPolicy();
    	// GIVEN
    	Mapping<PrismPropertyValue<T>> mapping = evaluator.createMapping(mappingFileName, 
    			TEST_NAME, stringPolicy, new ItemPath(
    					UserType.F_EXTENSION,
    					new QName(NS_EXTENSION, extensionPropName)), null);
    	    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(null, opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
		T value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertNotNull("Generated null value", value1);
		// We need to ignore the minLength. Conversion string -> number -> string may lose the leading zeroes
		assertGeneratedValue(value1.toString(), stringPolicy, PATTERN_NUMERIC, true);

		// WHEN (2)
		mapping.evaluate(null, opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		T value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertNotNull("Generated null value", value2);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
		assertGeneratedValue(value1.toString(), stringPolicy, PATTERN_NUMERIC, true);
    }

	@Test
    public void testGenerateProtectedString() throws Exception {
    	final String TEST_NAME = "testGenerateProtectedString";
    	TestUtil.displayTestTile(TEST_NAME);
    	// GIVEN
    	Mapping<PrismPropertyValue<ProtectedStringType>> mapping = evaluator.createMapping("mapping-generate.xml", 
    			TEST_NAME, SchemaConstants.PATH_PASSWORD_VALUE, null);
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN
    	mapping.evaluate(null, opResult);

		// THEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = mapping.getOutputTriple();
    	ProtectedStringType value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    	
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedDataType());
    }
        
}
