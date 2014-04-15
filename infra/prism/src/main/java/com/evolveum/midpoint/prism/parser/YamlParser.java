package com.evolveum.midpoint.prism.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.parser.json.DomElementSerializer;
import com.evolveum.midpoint.prism.parser.json.ItemPathSerializer;
import com.evolveum.midpoint.prism.parser.json.PolyStringSerializer;
import com.evolveum.midpoint.prism.parser.json.QNameSerializer;
import com.evolveum.midpoint.prism.parser.json.XmlGregorialCalendarSerializer;
import com.evolveum.midpoint.prism.parser.yaml.MidpoinYAMLGenerator;
import com.evolveum.midpoint.prism.parser.yaml.MidpointYAMLFactory;
import com.evolveum.midpoint.prism.parser.yaml.MidpointYAMLParser;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
//import com.fasterxml.jackson.core.YAMLGenerator;

public class YamlParser extends AbstractParser{
	
	
	//------------------------END OF METHODS FOR SERIALIZATION -------------------------------
	
	@Override
	public boolean canParse(File file) throws IOException {
		if (file == null) {
			return false;
		}
		return file.getName().endsWith(".yaml");
	}

	@Override
	public boolean canParse(String dataString) {
		if (dataString == null) {
			return false;
		}
		return dataString.startsWith("---");
	}
	
	public YAMLGenerator createGenerator(StringWriter out) throws SchemaException{
		try {
			MidpointYAMLFactory factory = new MidpointYAMLFactory();
			MidpoinYAMLGenerator generator = (MidpoinYAMLGenerator) factory.createGenerator(out);
			generator.setPrettyPrinter(new DefaultPrettyPrinter());
			generator.setCodec(configureMapperForSerialization());
//			MidpoinYAMLGenerator myg = new MidpoinYAMLGenerator(generator., jsonFeatures, yamlFeatures, codec, out, version)
//			generator.
			generator.configure(com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.CANONICAL_OUTPUT, false);
			YAMLParser parser = factory.createParser(out.toString());
//			parser.
			return generator;
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		}

		
	}
	
	private ObjectMapper configureMapperForSerialization(){
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
//		mapper.enableDefaultTyping(DefaultTyping.NON_CONCRETE_AND_ARRAYS, As.EXISTING_PROPERTY);
//		mapper.configure(SerializationFeaCture.);
//		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.registerModule(createSerializerModule());
		mapper.enableDefaultTyping(DefaultTyping.NON_CONCRETE_AND_ARRAYS);
		mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.EXISTING_PROPERTY);
		mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.EXTERNAL_PROPERTY);
		mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.PROPERTY);
		
		return mapper;
	}
	
	private Module createSerializerModule(){
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
		module.addSerializer(ItemPath.class, new ItemPathSerializer());
//		module.addSerializer(JAXBElement.class, new JaxbElementSerializer());
		module.addSerializer(XMLGregorianCalendar.class, new XmlGregorialCalendarSerializer());
		module.addSerializer(Element.class, new DomElementSerializer());
		return module;
	}
	
	@Override
	protected MidpointYAMLParser createParser(File file) throws SchemaException, IOException {
        return createParser(new FileInputStream(file));
	}

    @Override
    protected MidpointYAMLParser createParser(InputStream stream) throws SchemaException, IOException {
        MidpointYAMLFactory factory = new MidpointYAMLFactory();
        try {
            MidpointYAMLParser p = (MidpointYAMLParser) factory.createParser(stream);
//			p.enable(Feature.BOGUS);
            String oid = p.getObjectId();
            p.enable(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_YAML_COMMENTS);
            return p;
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
	protected MidpointYAMLParser createParser(String dataString) throws SchemaException {
		MidpointYAMLFactory factory = new MidpointYAMLFactory();
		try {
			return (MidpointYAMLParser) factory.createParser(dataString);
		} catch (IOException e) {
			throw new SchemaException("Cannot create JSON parser: " + e.getMessage(), e);
		}
		
	}

	@Override
	protected boolean serializeExplicitType(PrimitiveXNode primitive, QName explicitType, JsonGenerator generator) throws JsonGenerationException, IOException {
		if (explicitType != null) {
			if (generator.canWriteTypeId()) {
				if (explicitType.equals(DOMUtil.XSD_STRING)) {
					generator.writeTypeId("http://www.w3.org/2001/XMLSchema/string");
				} else if (explicitType.equals(DOMUtil.XSD_INT)) {
					generator.writeTypeId("http://www.w3.org/2001/XMLSchema/int");
					generator.writeString(String.valueOf(primitive.getValue()));
					return true;
				}
			} 
		}
		return false;
		
	}

	@Override
	protected void writeExplicitType(QName explicitType, JsonGenerator generator) throws JsonGenerationException, IOException {
		generator.writeObjectField("@type", explicitType);
		//		if (generator.canWriteTypeId()){
//			String type = QNameUtil.qNameToUri(explicitType);
//			type = type.replace("#", "//");
//			generator.writeTypeId(type);
//		}
	}
	
}


