/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.mapping;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.prism.util.PrismAsserts.*;
import static com.evolveum.midpoint.common.mapping.MappingTestEvaluator.*;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 */
public class TestMappingDynamic {
	
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
    	PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMappingDynamicAdd(
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
		mapping.evaluate(opResult);
    	
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
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-extra-variables.xml", 
    			TEST_NAME, "employeeType", null);
    	
    	Map<QName, Object> vars = new HashMap<QName, Object>();
    	JAXBElement<UserType> userTypeElement = PrismTestUtil.unmarshalElement(
                new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111112.xml"), UserType.class);
        UserType userType = userTypeElement.getValue();
        vars.put(new QName(SchemaConstants.NS_C, "sailor"), userType);
        mapping.addVariableDefinitions(vars);
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	// WHEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = evaluator.evaluateMappingDynamicReplace(
    			"mapping-script-fullname.xml",
    			"testScriptVariablesPolyStringGroovy",
    			"fullName",					// target
    			"givenName",				// changed property
    			PrismTestUtil.createPolyString("Jackie"));	// changed values
    	
    	// THEN
    	PrismAsserts.assertTripleNoZero(outputTriple);
    	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Jackie Sparrow"));
    	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Jack Sparrow"));
    }


    @Test
    public void testScriptRootNodeRef() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptRootNodeRef";
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-root-node.xml", 
    			TEST_NAME, "locality", null);
    	
        mapping.setRootNode(MiscSchemaUtil.createObjectReference(
            	"c0c010c0-d34d-b33f-f00d-111111111111",
            	UserType.COMPLEX_TYPE));
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-script-root-node.xml", 
    			TEST_NAME, "locality", null);
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(new File(OBJECTS_DIR, "c0c010c0-d34d-b33f-f00d-111111111111.xml"));
        mapping.setRootNode(user.asObjectable());
        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
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
    public void testScriptSystemVariablesConditionTrueToTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionTrueToTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrueXPath";
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), "Jack");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain Jack"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }

    @Test
    public void testScriptSystemVariablesConditionFalseToFalseGroovy() throws Exception {
    	testScriptSystemVariablesConditionFalseToFalse("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionFalseToFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionFalseToFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionFalseToFalse";
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), "Jack");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionFalseToTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionFalseToTrue";
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), "Jack");
    	delta.addModificationAddProperty(evaluator.toPath("employeeType"), "CAPTAIN");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain Jack"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToFalseGroovy() throws Exception {
    	testScriptSystemVariablesConditionTrueToFalse("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionTrueToFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionTrueToFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToFalse";
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), "Jack");
    	delta.addModificationDeleteProperty(evaluator.toPath("employeeType"), "CAPTAIN");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }
    
    public void testScriptTransformMulti() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrueXPath";
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, evaluator.USER_OLD_OID, evaluator.getPrismContext());
    	PropertyDelta<String> propDelta = delta.createPropertyModification(evaluator.toPath("employeeType"));
    	propDelta.addValueToAdd(new PrismPropertyValue<String>("CAPTAIN"));
    	propDelta.addValueToDelete(new PrismPropertyValue<String>("LANDLUBER"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-script-transform.xml", 
    			TEST_NAME, "organizationalUnit", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("LANDLUBER");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleZero(outputTriple, PrismTestUtil.createPolyString("The pirate deck"));
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("The captain deck"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("The landluber deck"));
    }
    
    @Test
    public void testGenerate() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testGenerate";
    	Mapping<PrismPropertyValue<String>> mapping = evaluator.createMapping("mapping-generate.xml", 
    			TEST_NAME, "employeeNumber", null);
    	
    	final StringPolicyType stringPolicy = evaluator.getStringPolicy();
    	
    	StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private PropertyPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(PropertyPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				// No path. The the path is default
//				assertNotNull("Null outputPath", outputPath);
				assertNotNull("Null outputDefinition", outputDefinition);
				return stringPolicy;
			}
		};
		mapping.setStringPolicyResolver(stringPolicyResolver);
    	
		OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN (1)
    	mapping.evaluate(opResult);

		// THEN (1)
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = mapping.getOutputTriple();
		String value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		System.out.println("Generated value (1): " + value1);
		assertGeneratedValue(value1, stringPolicy);

		// WHEN (2)
		mapping.evaluate(opResult);

		// THEN (2)
		outputTriple = mapping.getOutputTriple();
		String value2 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
		System.out.println("Generated value (2): " + value2);
		assertGeneratedValue(value2, stringPolicy);
		PrismAsserts.assertTripleNoPlus(outputTriple);
		PrismAsserts.assertTripleNoMinus(outputTriple);

		assertFalse("Generated the same value", value1.equals(value2));
    }
    
	private void assertGeneratedValue(String value, StringPolicyType stringPolicy) {
		assertTrue("Value too short", value.length() >= stringPolicy.getLimitations().getMinLength());
		assertTrue("Value too long", value.length() <= stringPolicy.getLimitations().getMaxLength());
		// TODO: better validation
	}


	@Test
    public void testGenerateProtectedString() throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testGenerateProtectedString";
    	Mapping<PrismPropertyValue<ProtectedStringType>> mapping = evaluator.createMapping("mapping-generate.xml", 
    			TEST_NAME, SchemaConstants.PATH_PASSWORD_VALUE, null);
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	
		// WHEN
    	mapping.evaluate(opResult);

		// THEN
    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = mapping.getOutputTriple();
    	ProtectedStringType value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
    	PrismAsserts.assertTripleNoPlus(outputTriple);
    	PrismAsserts.assertTripleNoMinus(outputTriple);
    	
        System.out.println("Generated excrypted value: "+value1);
        assertNotNull(value1);
        assertNotNull(value1.getEncryptedData());
    }
    
    // TODO
    
    

//    @Test
//    public void testConstructionGenerateProtectedString() throws JAXBException, ExpressionEvaluationException, ObjectNotFoundException, SchemaException, FileNotFoundException, EncryptionException {
//    	ProtectedStringType addedPs = evaluator.createProtectedString("apple");
//    	ProtectedStringType oldPs = evaluator.createProtectedString("rock");
//		// WHEN
//    	PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = evaluator.evaluateMappingDynamicAdd(ProtectedStringType.class, 
//    			"construction-generate.xml", SchemaConstants.PATH_PASSWORD_VALUE, oldPs, null, "testConstructionGenerateProtectedString",
//    			addedPs);
//    	
//    	// THEN
//    	ProtectedStringType value1 = MappingTestEvaluator.getSingleValue("plus set", outputTriple.getZeroSet());
//    	PrismAsserts.assertTripleNoPlus(outputTriple);
//    	PrismAsserts.assertTripleNoMinus(outputTriple);
//    	
//        System.out.println("Generated excrypted value: "+value1);
//        assertNotNull(value1);
//        assertNotNull(value1.getEncryptedData());
//    }
//    
    
}