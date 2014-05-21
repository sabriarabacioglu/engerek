/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Class that statically instantiates the prism contexts and provides convenient static version of the PrismContext
 * and processor classes. 
 * 
 * This is usable for tests. DO NOT use this in the main code. Although it is placed in "main" for convenience,
 * is should only be used in tests.
 *
 * @author semancik
 */
public class PrismTestUtil {

    private static final Trace LOGGER = TraceManager.getTrace(PrismTestUtil.class);

    private static final QName DEFAULT_ELEMENT_NAME = new QName("http://midpoint.evolveum.com/xml/ns/test/whatever-1.xsd", "whatever");
    
    private static PrismContext prismContext;
    private static PrismContextFactory prismContextFactory;

    public static void resetPrismContext(PrismContextFactory newPrismContextFactory) throws SchemaException, SAXException, IOException {
    	if (prismContextFactory == newPrismContextFactory) {
    		// Exactly the same factory instance, nothing to do.
    		return;
    	}
    	setFactory(newPrismContextFactory);
    	resetPrismContext();
    }
    
	public static void setFactory(PrismContextFactory newPrismContextFactory) {
		PrismTestUtil.prismContextFactory = newPrismContextFactory;
	}

	public static void resetPrismContext() throws SchemaException, SAXException, IOException {
		prismContext = createInitializedPrismContext();
	}

	public static PrismContext createPrismContext() throws SchemaException, FileNotFoundException {
    	if (prismContextFactory == null) {
    		throw new IllegalStateException("Cannot create prism context, no prism factory is set");
    	}
        return prismContextFactory.createPrismContext();
    }

    public static PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
    	PrismContext newPrismContext = createPrismContext();
    	newPrismContext.initialize();
        return newPrismContext;
    }
    
    public static PrismContext getPrismContext() {
    	if (prismContext == null) {
    		throw new IllegalStateException("Prism context is not set in PrismTestUtil. Maybe a missing call to resetPrismContext(..) in test initialization?");
    	}
    	return prismContext;
    }
    
    public static SchemaRegistry getSchemaRegistry() {
    	return prismContext.getSchemaRegistry();
    }
    
    // ==========================
    // == parsing
    // ==========================
    
    public static <T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException, IOException {
    	return getPrismContext().parseObject(file);
    }
    
    public static <T extends Objectable> PrismObject<T> parseObject(String xmlString) throws SchemaException {
    	return getPrismContext().parseObject(xmlString);
    }
    
    @Deprecated
    public static <T extends Objectable> PrismObject<T> parseObject(Element element) throws SchemaException {
    	return getPrismContext().parseObject(element);
    }

    public static <T extends Objectable> T parseObjectable(File file, Class<T> clazz) throws SchemaException, IOException {
        return (T) parseObject(file).asObjectable();
    }


    public static List<PrismObject<? extends Objectable>> parseObjects(File file) throws SchemaException, IOException {
    	return getPrismContext().parseObjects(file);
    }
    
    // ==========================
    // == Serializing
    // ==========================

    public static String serializeObjectToString(PrismObject<? extends Objectable> object, String language) throws SchemaException {
    	return getPrismContext().serializeObjectToString(object, language);
    }

    public static String serializeObjectToString(PrismObject<? extends Objectable> object) throws SchemaException {
        return getPrismContext().serializeObjectToString(object, PrismContext.LANG_XML);
    }

    public static String serializeAtomicValue(Object object, QName elementName) throws SchemaException {
        return getPrismContext().serializeAtomicValue(object, elementName, PrismContext.LANG_XML);
    }

    public static String serializeAnyData(Object o, QName defaultRootElementName) throws SchemaException {
        return getPrismContext().serializeAnyData(o, defaultRootElementName, PrismContext.LANG_XML);
    }

    public static String serializeJaxbElementToString(JAXBElement element) throws SchemaException {
        return serializeAnyData(element.getValue(), element.getName());
    }

    public static String serializeAnyDataWrapped(Object o) throws SchemaException {
        return serializeAnyData(o, DEFAULT_ELEMENT_NAME);
    }



    // ==========================
    // == Here was parsing from JAXB.
    // ==========================

    public static <T> T parseAtomicValue(File file, QName type) throws SchemaException, IOException {
        return getPrismContext().parseAtomicValue(file, type);
    }

    public static <T> T parseAtomicValue(String data, QName type) throws SchemaException {
        return getPrismContext().parseAtomicValue(data, type);
    }

    public static <T> T parseAnyValue(File file) throws SchemaException, IOException {
        return getPrismContext().parseAnyValue(file);
    }

    public static <T extends Objectable> PrismObjectDefinition<T> getObjectDefinition(Class<T> compileTimeClass) {
		return getSchemaRegistry().findObjectDefinitionByCompileTimeClass(compileTimeClass);
	}

	public static PolyString createPolyString(String orig) {
		PolyString polyString = new PolyString(orig);
		polyString.recompute(getPrismContext().getDefaultPolyStringNormalizer());
		return polyString;
	}

	public static PolyStringType createPolyStringType(String string) {
		return new PolyStringType(createPolyString(string));
	}

	public static void displayTestTitle(String testName) {
		System.out.println("\n\n===[ "+testName+" ]===\n");
		LOGGER.info("===[ {} ]===",testName);
	}
	
	public static SearchFilterType unmarshalFilter(File file) throws Exception {
		return prismContext.parseAtomicValue(file, SearchFilterType.COMPLEX_TYPE);
	}
	
	public static ObjectFilter getFilterCondition(ObjectFilter filter, int index) {
		if (!(filter instanceof NaryLogicalFilter)) {
			throw new IllegalArgumentException("Filter not an instance of n-ary logical filter.");
		}
		return ((LogicalFilter) filter).getConditions().get(index);
	}
	
	public static void displayQuery(ObjectQuery query) {
		LOGGER.trace("object query: {}", query);
		System.out.println("object query: " + query);
		if (query != null) {
			LOGGER.trace("QUERY DUMP: {}", query.debugDump());
			System.out.println("QUERY DUMP: " + query.debugDump());
		}
	}

	public static void displayQueryType(QueryType queryType) {
		LOGGER.info(DOMUtil.serializeDOMToString(queryType.getFilter().getFilterClause()));
	}
}
