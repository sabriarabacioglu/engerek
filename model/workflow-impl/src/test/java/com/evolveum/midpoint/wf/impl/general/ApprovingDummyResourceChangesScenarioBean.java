/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.general;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.general.scenarios.BaseGcpScenarioBean;
import com.evolveum.midpoint.wf.impl.processors.general.scenarios.DefaultGcpScenarioBean;
import com.evolveum.midpoint.wf.impl.util.JaxbValueContainer;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessSpecificState;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.Map;

/**
 * This scenario bean simply puts "dummyResourceDelta" process variable into externalized state.
 *
 * @author mederly
 */
@Component
public class ApprovingDummyResourceChangesScenarioBean extends BaseGcpScenarioBean {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovingDummyResourceChangesScenarioBean.class);

    public static final QName DUMMY_RESOURCE_DELTA_QNAME = new QName(SchemaConstants.NS_WFCF, "dummyResourceDelta");

    @Autowired
    private PrismContext prismContext;

    @Override
    public ProcessSpecificState externalizeInstanceState(Map<String, Object> variables) throws SchemaException {
        PrismContainerDefinition<ProcessSpecificState> extDefinition = prismContext.getSchemaRegistry().findContainerDefinitionByType(ProcessSpecificState.COMPLEX_TYPE);
        PrismContainer<ProcessSpecificState> extStateContainer = extDefinition.instantiate();
        ProcessSpecificState extState = extStateContainer.createNewValue().asContainerable();

        PrismPropertyDefinition deltaDefinition = new PrismPropertyDefinition(
                DUMMY_RESOURCE_DELTA_QNAME,
                new QName(SchemaConstantsGenerated.NS_TYPES, "ObjectDeltaType"),
                prismContext);

        JaxbValueContainer<ObjectDeltaType> deltaInProcess = (JaxbValueContainer) variables.get("dummyResourceDelta");
        if (deltaInProcess != null) {
            deltaInProcess.setPrismContext(prismContext);
            PrismProperty deltaProperty = extStateContainer.getValue().findOrCreateItem(new ItemPath(DUMMY_RESOURCE_DELTA_QNAME), PrismProperty.class, deltaDefinition);
            deltaProperty.setRealValue(deltaInProcess.getValue());
            LOGGER.info("deltaProperty = {}", deltaProperty.debugDump());
        } else {
            LOGGER.warn("No dummyResourceDelta variable in process instance");
        }
        return extState;
    }
}
