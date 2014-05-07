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

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

/**
 * An interface through which external observers can be notified about work item related events.
 * Used e.g. for implementing workflow-related notifications.
 *
 * A tricky question is how to let the observer know how to deal with the process instance state
 * (e.g. how to construct a notification). Currently, the observer has to use the class of
 * the instance state prism object. It is up to the process implementer to provide appropriate
 * information through ChangeProcessor.externalizeInstanceState() method.
 *
 * EXPERIMENTAL. This interface may change in near future.
 *
 * @author mederly
 */
public interface WorkItemListener {

    /**
     * This method is called by wf module when a work item is created.
     *
     * @param workItemName name of the work item
     * @param assigneeOid OID of the user to which the work item is assigned
     * @param instanceState externalized process instance state
     */
    public void onWorkItemCreation(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState);

    /**
     * This method is called by wf module when a work item is completed.
     *
     * @param workItemName name of the work item
     * @param assigneeOid OID of the user to which the work item is assigned
     * @param instanceState externalized process instance state
     * @param decision decision of the user
     */
    public void onWorkItemCompletion(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState, String decision);
}
