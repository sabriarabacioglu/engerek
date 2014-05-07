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
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public interface ModelElementContext<O extends ObjectType> extends Serializable, DebugDumpable {

    Class<O> getObjectTypeClass();

    public PrismObject<O> getObjectOld();

	public void setObjectOld(PrismObject<O> objectOld);
	
	public PrismObject<O> getObjectNew();
	
	public void setObjectNew(PrismObject<O> objectNew);
	
	public ObjectDelta<O> getPrimaryDelta();
	
	public void setPrimaryDelta(ObjectDelta<O> primaryDelta);
	
	public ObjectDelta<O> getSecondaryDelta();
	
	public void setSecondaryDelta(ObjectDelta<O> secondaryDelta);

    public List<? extends ObjectDeltaOperation> getExecutedDeltas();

    public String getOid();

}
