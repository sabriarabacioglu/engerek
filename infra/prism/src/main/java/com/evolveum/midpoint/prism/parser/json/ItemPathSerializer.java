package com.evolveum.midpoint.prism.parser.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class ItemPathSerializer extends JsonSerializer<ItemPath>{

	@Override
	public void serialize(ItemPath value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		XPathHolder xpath = new XPathHolder(value);
		String path = xpath.getXPathWithDeclarations(true);
//		value.
		jgen.writeObject(path);
		
	}
	
	@Override
	public void serializeWithType(ItemPath value, JsonGenerator jgen, SerializerProvider provider,
			TypeSerializer typeSer) throws IOException, JsonProcessingException {
		// TODO Auto-generated method stub
		serialize(value, jgen, provider);
	}

}
