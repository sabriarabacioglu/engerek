package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ObjectWrapperUtil {

	
	public static <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName, String description, PrismObject<O> object, ContainerStatus status, PageBase pageBase) {
		try {
			Definition editedDefinition = null;
			if (isShadow(object)){
				PrismReference resourceRef = object.findReference(ShadowType.F_RESOURCE_REF);
                PrismObject<ResourceType> resource = resourceRef.getValue().getObject();
				editedDefinition = pageBase.getModelInteractionService().getEditObjectClassDefinition((PrismObject<ShadowType>) object, resource);
			} else {
		editedDefinition = pageBase.getModelInteractionService().getEditObjectDefinition(object);
			}
		ObjectWrapper wrapper = new ObjectWrapper(displayName, description, object, editedDefinition, status);
		return wrapper;
		} catch (SchemaException ex){
			throw new SystemException(ex);
		}
	}
	
	private static boolean isShadow(PrismObject object){
		return (object.getCompileTimeClass() != null && ShadowType.class.isAssignableFrom(object
				.getCompileTimeClass()))
				|| (object.getDefinition() != null && object.getDefinition().getName()
						.equals(ShadowType.COMPLEX_TYPE));
	}
}
