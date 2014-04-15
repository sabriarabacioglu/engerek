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
package com.evolveum.midpoint.schema;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.MidPointSchemaDefinitionFactory;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class MidPointPrismContextFactory implements PrismContextFactory {

	private static final File TEST_EXTRA_SCHEMA_DIR = new File("src/test/resources/schema");

	public static final MidPointPrismContextFactory FACTORY = new MidPointPrismContextFactory(TEST_EXTRA_SCHEMA_DIR);
	
	private File extraSchemaDir;
		
	public MidPointPrismContextFactory() {
		this.extraSchemaDir = null;
	}

	public MidPointPrismContextFactory(File extraSchemaDir) {
		this.extraSchemaDir = extraSchemaDir;
	}

	@Override
	public PrismContext createPrismContext() throws SchemaException, FileNotFoundException {
		SchemaRegistry schemaRegistry = createSchemaRegistry();
		PrismContext context = PrismContext.create(schemaRegistry);
		context.setDefinitionFactory(createDefinitionFactory());
		return context;
	}
	
	public PrismContext createEmptyPrismContext() throws SchemaException, FileNotFoundException {
		SchemaRegistry schemaRegistry = createSchemaRegistry();
		PrismContext context = PrismContext.createEmptyContext(schemaRegistry);
		context.setDefinitionFactory(createDefinitionFactory());
		return context;
	}
	
	private SchemaDefinitionFactory createDefinitionFactory() {
		return new MidPointSchemaDefinitionFactory();
	}

	public PrismContext createInitializedPrismContext() throws SchemaException, SAXException, IOException {
		PrismContext context = createPrismContext();
		context.initialize();
		return context;
	}
	
	private SchemaRegistry createSchemaRegistry() throws SchemaException, FileNotFoundException {
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		schemaRegistry.setDefaultNamespace(SchemaConstantsGenerated.NS_COMMON);
		schemaRegistry.setNamespacePrefixMapper(new GlobalDynamicNamespacePrefixMapper());
		registerBuiltinSchemas(schemaRegistry);
        registerExtensionSchemas(schemaRegistry);
		return schemaRegistry;
	}
    
    protected void registerExtensionSchemas(SchemaRegistry schemaRegistry) throws SchemaException, FileNotFoundException {
    	if (extraSchemaDir != null && extraSchemaDir.exists()) {
    		schemaRegistry.registerPrismSchemasFromDirectory(extraSchemaDir);
    	}
    }
	
	private void registerBuiltinSchemas(SchemaRegistry schemaRegistry) throws SchemaException {
		// Note: the order of schema registration may affect the way how the schema files are located
		// (whether are pulled from the registry or by using a catalog file).
		
		// Standard schemas
		
		schemaRegistry.registerSchemaResource("xml/ns/standard/XMLSchema.xsd", "xsd");
		schemaRegistry.registerSchemaResource("xml/ns/standard/xmldsig-core-schema.xsd", "ds");
		schemaRegistry.registerSchemaResource("xml/ns/standard/xenc-schema.xsd", "enc");

		schemaRegistry.getNamespacePrefixMapper().registerPrefix(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi", false);

		
		// Prism Schemas
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/annotation-2.xsd", "a");

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/types-2.xsd", "t", 
				com.evolveum.prism.xml.ns._public.types_2.ObjectFactory.class.getPackage());

		schemaRegistry.registerPrismSchemaResource("xml/ns/public/query-2.xsd", "q", 
				com.evolveum.prism.xml.ns._public.query_2.ObjectFactory.class.getPackage());
		
		
		// midPoint schemas
		schemaRegistry.registerPrismDefaultSchemaResource("xml/ns/public/common/common-2a.xsd", "c", 
				com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory.class.getPackage());
		
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/common/api-types-2.xsd", "apti", 
				com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/annotation-2.xsd", "ra");
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/resource/capabilities-2.xsd", "cap",
				com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/connector-schema-2.xsd", "icfc",
				com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/resource-schema-2.xsd", "icfs",
				com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_2.ObjectFactory.class.getPackage());
		
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/import/extension-2.xsd", "impext");
		schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/extension-2.xsd", "mext");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/context/model-context-2.xsd", "m",
                com.evolveum.midpoint.xml.ns._public.model.model_context_2.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/workflow/extension-2.xsd", "wf");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/workflow/common-forms-2.xsd", "wfcf",
                com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/workflow/process-instance-state-2.xsd", "wfpis",
                com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/scripting/scripting-2.xsd", "s",
                com.evolveum.midpoint.xml.ns._public.model.scripting_2.ObjectFactory.class.getPackage());

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/task/noop-1.xsd", "noop");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/task/extension-2.xsd", "taskext");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/connector/icf-1/connector-extension-2.xsd", "connext");

        schemaRegistry.registerPrismSchemaResource("xml/ns/public/model/scripting/extension-2.xsd", "se");
    }
	
	private void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
	}

}
