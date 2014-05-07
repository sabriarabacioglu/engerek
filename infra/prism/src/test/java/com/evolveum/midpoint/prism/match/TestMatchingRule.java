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
package com.evolveum.midpoint.prism.match;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class TestMatchingRule {

	private static MatchingRuleRegistry matchingRuleRegistry;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
		
		matchingRuleRegistry = MatchingRuleRegistryFactory.createRegistry();
	}
	
	@Test
	public void testStringDefault() throws Exception {
		// GIVEN
		MatchingRule<String> rule = matchingRuleRegistry.getMatchingRule(null, DOMUtil.XSD_STRING);
		// WHEN, THEN		
		assertMatch(rule, "foo", "foo");
		assertNoMatch(rule, "foo", "bar");
		assertNoMatch(rule, "foo", "Foo");
		assertNoMatch(rule, "FOO", "Foo");
		assertNormalized(rule, "Foo", "Foo");
		assertNormalized(rule, "baR", "baR");
	}
	
	@Test
	public void testStringCaseInsensitive() throws Exception {
		// GIVEN
		MatchingRule<String> rule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, 
				DOMUtil.XSD_STRING);
		// WHEN, THEN		
		assertMatch(rule, "foo", "foo");
		assertNoMatch(rule, "foo", "bar");
		assertMatch(rule, "foo", "Foo");
		assertMatch(rule, "FOO", "Foo");
		assertNormalized(rule, "bar", "baR");
		assertNormalized(rule, "foo", "FoO");
		assertNormalized(rule, "foobar", "foobar");
	}
	
	@Test
	public void testPolyStringStrict() throws Exception {
		// GIVEN
		MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PolyStringStrictMatchingRule.NAME, 
				PolyStringType.COMPLEX_TYPE);
		// WHEN, THEN		
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
		assertNoMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
	}
	
	@Test
	public void testPolyStringOrig() throws Exception {
		// GIVEN
		MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PolyStringOrigMatchingRule.NAME, 
				PolyStringType.COMPLEX_TYPE);
		// WHEN, THEN		
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
		assertNoMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
	}
	
	@Test
	public void testPolyStringNorm() throws Exception {
		// GIVEN
		MatchingRule<PolyString> rule = matchingRuleRegistry.getMatchingRule(PolyStringNormMatchingRule.NAME, 
				PolyStringType.COMPLEX_TYPE);
		// WHEN, THEN		
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "bar"));
		assertMatch(rule, new PolyString("BAR", "bar"), new PolyString("Foo", "bar"));
		assertMatch(rule, new PolyString("Bar", "bar"), new PolyString("bAR", "bar"));
		assertNoMatch(rule, new PolyString("Bar", "bar"), new PolyString("Bar", "barbar"));
	}

	private <T> void assertMatch(MatchingRule<T> rule, T a, T b) {
		assertTrue("Values '"+a+"' and '"+b+"' does not match; rule: "+rule, rule.match(a, b));
	}

	private <T> void assertNoMatch(MatchingRule<T> rule, T a, T b) {
		assertFalse("Values '"+a+"' and '"+b+"' DOES match but they should not; rule: "+rule, rule.match(a, b));
	}

	private void assertNormalized(MatchingRule<String> rule, String expected, String original) {
		assertEquals("Normalized value does not match", expected, rule.normalize(original));
	}
}
