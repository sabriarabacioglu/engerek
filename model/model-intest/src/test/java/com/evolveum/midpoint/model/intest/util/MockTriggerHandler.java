/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.intest.util;

import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Radovan Semancik
 *
 */
public class MockTriggerHandler implements TriggerHandler {
	
	public static final String HANDLER_URI = SchemaConstants.NS_MIDPOINT_TEST + "/mock-trigger-handler";
	
	protected static final Trace LOGGER = TraceManager.getTrace(MockTriggerHandler.class);
	
	private PrismObject<?> lastObject;
	
	public PrismObject<?> getLastObject() {
		return lastObject;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.trigger.TriggerHandler#handle(com.evolveum.midpoint.prism.PrismObject)
	 */
	@Override
	public <O extends ObjectType> void handle(PrismObject<O> object, Task task, OperationResult result) {
		LOGGER.info("Mock trigger handler called with {}", object);
		lastObject = object.clone();
	}
	
	public void reset() {
		lastObject = null;
	}

}
