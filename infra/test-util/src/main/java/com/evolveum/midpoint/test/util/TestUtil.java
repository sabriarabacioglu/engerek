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

package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Unit test utilities.
 *
 * @author Radovan Semancik
 */
public class TestUtil {
	
	public static final int MAX_EXCEPTION_MESSAGE_LENGTH = 500;
	
	public static final String TEST_LOG_PREFIX = "=====[ ";
	public static final String TEST_LOG_SUFFIX = " ]======================================";
	public static final String TEST_OUT_PREFIX = "\n\n=====[ ";
	public static final String TEST_OUT_SUFFIX = " ]======================================\n";
	public static final String TEST_OUT_SECTION_PREFIX = "\n\n----- ";
	public static final String TEST_OUT_SECTION_SUFFIX = " --------------------------------------\n";
	public static final String TEST_LOG_SECTION_PREFIX = "----- ";
	public static final String TEST_LOG_SECTION_SUFFIX = " --------------------------------------";
	
	public static boolean checkResults = true;
	
	private static final Trace LOGGER = TraceManager.getTrace(TestUtil.class);

    public static <T> void assertPropertyValueSetEquals(Collection<PrismPropertyValue<T>> actual, T... expected) {
        Set<T> set = new HashSet<T>();
        for (PrismPropertyValue<T> value : actual) {
            set.add(value.getValue());
        }
        assertSetEquals(set, expected);
    }

    public static <T> void assertSetEquals(Collection<T> actual, T... expected) {
        assertSetEquals(null, actual, expected);
    }

    public static <T> void assertSetEquals(String message, Collection<T> actual, T... expected) {
        Set<T> expectedSet = new HashSet<T>();
        expectedSet.addAll(Arrays.asList(expected));
        Set<T> actualSet = new HashSet<T>();
        actualSet.addAll(actual);
        if (message != null) {
            assertEquals(message, expectedSet, actualSet);
        } else {
            assertEquals(expectedSet, actualSet);
        }
    }
    
    public static String getNodeOid(Node node) {
		Node oidNode = null;
		if ((null == node.getAttributes())
				|| (null == (oidNode = node.getAttributes().getNamedItem(
						SchemaConstants.C_OID_ATTRIBUTE.getLocalPart())))
				|| (StringUtils.isEmpty(oidNode.getNodeValue()))) {
			return null;
		}
		String oid = oidNode.getNodeValue();
		return oid;
	}
    
    public static void setAttribute(PrismObject<ShadowType> account, QName attrName, QName typeName, 
			PrismContext prismContext, String value) throws SchemaException {
		PrismContainer<Containerable> attributesContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttributeDefinition attrDef = new ResourceAttributeDefinition(attrName, typeName, prismContext);
		ResourceAttribute attribute = attrDef.instantiate();
		attribute.setRealValue(value);
		attributesContainer.add(attribute);
	}

	public static void assertElement(List<Object> elements, QName elementQName, String value) {
		for (Object element: elements) {
			QName thisElementQName = JAXBUtil.getElementQName(element);
			if (elementQName.equals(thisElementQName)) {
				if (element instanceof Element) {
					String thisElementContent = ((Element)element).getTextContent();
					if (value.equals(thisElementContent)) {
						return;
					} else {
						AssertJUnit.fail("Wrong value for element with name "+elementQName+"; expected "+value+"; was "+thisElementContent);
					}
				} else {
					throw new IllegalArgumentException("Unexpected type of element "+elementQName+": "+element.getClass());
				}
			}
		}
		AssertJUnit.fail("No element with name "+elementQName);
	}

	public static void assertExceptionSanity(ObjectAlreadyExistsException e) {
		LOGGER.debug("Excpetion (expected)", e, e);
		System.out.println("Excpetion (expected)");
		System.out.println(ExceptionUtils.getFullStackTrace(e));
		assert !e.getMessage().isEmpty() : "Empty exception message";
		assert e.getMessage().length() < MAX_EXCEPTION_MESSAGE_LENGTH : "Exception message too long ("
				+e.getMessage().length()+" characters): "+e.getMessage();
	}

	public static void displayTestTile(String testName) {
		System.out.println(TEST_OUT_PREFIX + testName + TEST_OUT_SUFFIX);
		LOGGER.info(TEST_LOG_PREFIX + testName + TEST_LOG_SUFFIX);
	}

	public static void displayTestTile(Object testCase, String testName) {
		System.out.println(TEST_OUT_PREFIX + testCase.getClass().getSimpleName() + "." + testName + TEST_OUT_SUFFIX);
		LOGGER.info(TEST_LOG_PREFIX + testCase.getClass().getSimpleName() + "." + testName + TEST_LOG_SUFFIX);
	}

	public static void displayWhen(String testName) {
		System.out.println(TEST_OUT_SECTION_PREFIX + " WHEN " + testName + TEST_OUT_SECTION_SUFFIX);
		LOGGER.info(TEST_LOG_SECTION_PREFIX + " WHEN " + testName + TEST_LOG_SECTION_SUFFIX);
	}

	public static void displayThen(String testName) {
		System.out.println(TEST_OUT_SECTION_PREFIX + " THEN " + testName + TEST_OUT_SECTION_SUFFIX);
		LOGGER.info(TEST_LOG_SECTION_PREFIX + " THEN " + testName + TEST_LOG_SECTION_SUFFIX);
	}

	public static void assertSuccess(String message, OperationResult result, OperationResult originalResult, int stopLevel, int currentLevel, boolean warningOk) {
		if (!checkResults) {
			return;
		}
		if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
			String logmsg = message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
			System.out.println(logmsg + "\n" + originalResult.debugDump());
			fail(logmsg);
		}
		
		if (result.isHandledError()) {
			// There may be errors deeper in this result, even fatal errors. that's ok, we can ignore them.
			return;
		} else if (result.isSuccess() || result.isNotApplicable()) {
			// OK ... expected error is as good as success
		} else if (warningOk && result.getStatus() == OperationResultStatus.WARNING) {
			// OK
		} else {
			String logmsg = message + ": " + result.getStatus() + ": " + result.getMessage();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
			System.out.println(logmsg + "\n" + originalResult.debugDump());
			assert false : logmsg;	
		}
		
		if (stopLevel == currentLevel) {
			return;
		}
		List<OperationResult> partialResults = result.getSubresults();
		for (OperationResult subResult : partialResults) {
			assertSuccess(message, subResult, originalResult, stopLevel, currentLevel + 1, warningOk);
		}
	}

	/**
	 * level=-1 - check all levels
	 * level=0 - check only the top-level
	 * level=1 - check one level below top-level
	 * ...
	 * 
	 * @param message
	 * @param result
	 * @param level
	 */
	public static void assertSuccess(String message, OperationResult result, int level) {
		assertSuccess(message, result, result, level, 0, false);
	}

	public static void assertSuccess(String message, OperationResult result) {
		assertSuccess(message, result,-1);
	}

	public static void assertSuccess(OperationResultType result) {
		assertSuccess(result.getOperation(), result);
	}
	
	public static void assertSuccess(String message, OperationResultType result) {
		if (!checkResults) {
			return;
		}
		assertNotNull(message + ": null result", result);
		// Ignore top-level if the operation name is not set
		if (result.getOperation()!=null) {
			if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
				fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
			}
			if (result.getStatus() != OperationResultStatusType.SUCCESS
	                && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE
	                && result.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
				fail(message + ": " + result.getMessage() + " ("+result.getStatus()+")");
			}
		}
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			if (subResult==null) {
				fail(message+": null subresult under operation "+result.getOperation());
			}
			if (subResult.getOperation()==null) {
				fail(message+": null subresult operation under operation "+result.getOperation());
			}
			assertSuccess(message, subResult);
		}
	}

	public static void assertSuccess(OperationResult result) {
		assertSuccess("Operation "+result.getOperation()+" result", result);
	}
	
	public static void assertSuccess(OperationResult result, int depth) {
		assertSuccess("Operation "+result.getOperation()+" result", result, depth);
	}
	
	public static void assertStatus(OperationResult result, OperationResultStatus expectedStatus) {
		assertEquals("Operation "+result.getOperation()+" result", expectedStatus, result.getStatus());		
	}

	public static void assertStatus(OperationResultType result, OperationResultStatusType expectedStatus) {
		assertEquals("Operation "+result.getOperation()+" result", expectedStatus, result.getStatus());		
	}

	public static boolean hasWarningAssertSuccess(String message, OperationResultType result) {
		boolean hasWarning = false;
		// Ignore top-level if the operation name is not set
		if (result.getOperation()!=null) {
			if (result.getStatus() == OperationResultStatusType.WARNING) {
				// Do not descent into warnings. There may be lions inside. Or errors.
				return true;
			} else {
				if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
					fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
				} 
				if (result.getStatus() != OperationResultStatusType.SUCCESS
	                    && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE
	                    && result.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
					fail(message + ": " + result.getMessage() + " ("+result.getStatus()+")");
				}
			}
		}
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			if (subResult==null) {
				fail(message+": null subresult under operation "+result.getOperation());
			}
			if (subResult.getOperation()==null) {
				fail(message+": null subresult operation under operation "+result.getOperation());
			}
			if (hasWarningAssertSuccess(message, subResult)) {
				hasWarning = true;
			}
		}
		return hasWarning;
	}

	public static void assertWarning(String message, OperationResultType result) {
		if (!checkResults) {
			return;
		}
		assert hasWarningAssertSuccess(message, result) : message + ": does not have warning";
	}

	public static void assertFailure(String message, OperationResult result) {
		assertTrue(message, result.isError());
		assertNoUnknown(result);
	}

	public static void assertFailure(OperationResult result) {
		if (!result.isError()) {
			String message = "Expected that operation "+result.getOperation()+" fails, but the result was "+result.getStatus();
			System.out.println(message);
			System.out.println(result.debugDump());
			LOGGER.error("{}",message);
			LOGGER.error("{}",result.debugDump());
			AssertJUnit.fail(message);
		}
		assertNoUnknown(result);
	}

	public static void assertPartialError(OperationResult result) {
		assertTrue("Expected that operation "+result.getOperation()+" fails partially, but the result was "+result.getStatus(), result.getStatus() == OperationResultStatus.PARTIAL_ERROR);
		assertNoUnknown(result);
	}

	public static void assertResultStatus(OperationResult result, OperationResultStatus expectedStatus) {
		assertTrue("Expected that operation "+result.getOperation()+" will result with "+expectedStatus+", but the result was "+result.getStatus(), result.getStatus() == expectedStatus);
		assertNoUnknown(result);
	}

	public static void assertFailure(OperationResultType result) {
		assertFailure(null, result);
	}

	public static void assertFailure(String message, OperationResultType result) {
		assertTrue((message == null ? "" : message + ": ") + 
				"Expected that operation "+result.getOperation()+" fails, but the result was "+result.getStatus(), 
				OperationResultStatusType.FATAL_ERROR == result.getStatus() || 
				OperationResultStatusType.PARTIAL_ERROR == result.getStatus()) ;
		assertNoUnknown(result);
	}

	public static void assertNoUnknown(OperationResult result) {
		if (result.isUnknown()) {
			AssertJUnit.fail("Unkwnown status for operation "+result.getOperation());
		}
		for (OperationResult subresult: result.getSubresults()) {
			assertNoUnknown(subresult);
		}
	}

	public static void assertNoUnknown(OperationResultType result) {
		if (result.getStatus() == OperationResultStatusType.UNKNOWN) {
			AssertJUnit.fail("Unkwnown status for operation "+result.getOperation());
		}
		for (OperationResultType subresult: result.getPartialResults()) {
			assertNoUnknown(subresult);
		}
	}

	public static void assertSuccessOrWarning(String message, OperationResult result, int level) {
		assertSuccess(message, result, result, level, 0, true);
	}

	public static void assertSuccessOrWarning(String message, OperationResult result) {
		assertSuccess(message, result, result, -1, 0, true);
	}

	public static void assertWarning(String message, OperationResult result) {
		assertWarning(message, result, -1, 0);
	}

	public static boolean hasWarningAssertSuccess(String message, OperationResult result, OperationResult originalResult, int stopLevel, int currentLevel) {
		if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
			String logmsg = message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
			System.out.println(logmsg + "\n" + originalResult.debugDump());
			fail(logmsg);
		}
		
		if (result.isWarning()) {
			// Do not descent into warnings. There may be lions inside. Or errors.
			return true;
		}
		
		if (result.isSuccess() || result.isHandledError() || result.isNotApplicable()) {
			// OK ... expected error is as good as success
		} else {
			String logmsg = message + ": " + result.getStatus() + ": " + result.getMessage();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
			System.out.println(logmsg + "\n" + originalResult.debugDump());
			assert false : logmsg;	
		}
		
		if (stopLevel == currentLevel) {
			return false;
		}
		boolean hasWarning = false;
		List<OperationResult> partialResults = result.getSubresults();
		for (OperationResult subResult : partialResults) {
			if (hasWarningAssertSuccess(message, subResult, originalResult, stopLevel, currentLevel + 1)) {
				hasWarning = true;
			}
		}
		return hasWarning;
	}

	public static void assertWarning(String message, OperationResult result, int stopLevel, int currentLevel) {
		if (!checkResults) {
			return;
		}
		hasWarningAssertSuccess(message, result, result, -1, 0);
	}

	public static void assertInProgress(String message, OperationResult result) {
		assertTrue("Expected result IN_PROGRESS but it was "+result.getStatus()+" in "+message,
				result.getStatus() == OperationResultStatus.IN_PROGRESS);
	}

	public static String getErrorMessage(OperationResult result) {
		if (result.isError()) {
			return result.getMessage();
		}
		for (OperationResult subresult: result.getSubresults()) {
			String message = getErrorMessage(subresult);
			if (message != null) {
				return message;
			}
		}
		return null;
	}
	
}
