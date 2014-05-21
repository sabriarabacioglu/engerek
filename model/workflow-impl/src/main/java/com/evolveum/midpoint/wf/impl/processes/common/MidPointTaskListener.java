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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiInterface;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * @author mederly
 */
public class MidPointTaskListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointTaskListener.class);
    private static final String DOT_CLASS = MidPointTaskListener.class.getName() + ".";

    @Override
    public void notify(DelegateTask delegateTask) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("notify called; event name = {}, name = {}", delegateTask.getEventName(), delegateTask.getName());
        }

        ActivitiInterface activitiInterface = SpringApplicationContextHolder.getActivitiInterface();
        activitiInterface.notifyMidpointAboutTaskEvent(delegateTask);

    }


}
