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
package com.evolveum.midpoint.model.impl.migrator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


/**
 * @author semancik
 *
 */
public class Migrator {
	
	public <I extends ObjectType, O extends ObjectType> PrismObject<O> migrate(PrismObject<I> original) {
		Class<I> origType = original.getCompileTimeClass();
		if (ObjectTemplateType.class.isAssignableFrom(origType)) {
			PrismObject<ObjectTemplateType> out = migrateObjectTemplate((PrismObject<ObjectTemplateType>) original);
			return (PrismObject<O>) out;
		}
		if (ResourceType.class.isAssignableFrom(origType)) {
			PrismObject<ResourceType> out = migrateResource((PrismObject<ResourceType>) original);
			return (PrismObject<O>) out;
		}
		if (FocusType.class.isAssignableFrom(origType)) {
			PrismObject<FocusType> out = migrateFocus((PrismObject<FocusType>) original);
			original = (PrismObject<I>) out;
		}
		if (UserType.class.isAssignableFrom(origType)) {
			PrismObject<UserType> out = migrateUser((PrismObject<UserType>) original);
			return (PrismObject<O>) out;
		}
		return (PrismObject<O>) original;
	}
	
	private PrismObject<ObjectTemplateType> migrateObjectTemplate(PrismObject<ObjectTemplateType> orig) {
		QName elementName = orig.getElementName();
		if (elementName.equals(SchemaConstants.C_OBJECT_TEMPLATE)) {
			return orig;
		}
		PrismObject<ObjectTemplateType> migrated = orig.clone();
		migrated.setElementName(SchemaConstants.C_OBJECT_TEMPLATE);
		return migrated;
	}

	private PrismObject<ResourceType> migrateResource(PrismObject<ResourceType> orig) {
		return orig;
	}
	
	private void migrateObjectSynchronization(ObjectSynchronizationType sync) {
		if (sync == null || sync.getReaction() == null){
			return;
		}
		
		List<SynchronizationReactionType> migratedReactions = new ArrayList<SynchronizationReactionType>();
		for (SynchronizationReactionType reaction : sync.getReaction()){
			if (reaction.getAction() == null){
				continue;
			}
			List<SynchronizationActionType> migratedAction = new ArrayList<SynchronizationActionType>();
			for (SynchronizationActionType action : reaction.getAction()){
				migratedAction.add(migrateAction(action));
			}
			SynchronizationReactionType migratedReaction = reaction.clone();
			migratedReaction.getAction().clear();
			migratedReaction.getAction().addAll(migratedAction);
			migratedReactions.add(migratedReaction);
		}
		
		sync.getReaction().clear();
		sync.getReaction().addAll(migratedReactions);
	}

	private SynchronizationActionType migrateAction(SynchronizationActionType action){
		if (action.getUserTemplateRef() == null){
			return action;
		}
		
		action.setObjectTemplateRef(action.getUserTemplateRef());
		
		return action;
	}
	
	private PrismObject<FocusType> migrateFocus(PrismObject<FocusType> orig) {
		return orig;
	}
	
	private PrismObject<UserType> migrateUser(PrismObject<UserType> orig) {
		return orig;
	}

}
