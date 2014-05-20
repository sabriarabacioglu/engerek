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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.BaseProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.util.ApprovalUtils;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

public class PrepareResult implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(PrepareResult.class);

    public void execute(DelegateExecution execution) {

        Boolean loopLevelsStop = (Boolean) execution.getVariable(ProcessVariableNames.LOOP_LEVELS_STOP);
        Validate.notNull(loopLevelsStop, "loopLevels_stop is undefined");
        boolean approved = !loopLevelsStop;

        execution.setVariable(BaseProcessMidPointInterface.VARIABLE_WF_ANSWER, ApprovalUtils.approvalStringValue(approved));
        execution.setVariable(BaseProcessMidPointInterface.VARIABLE_WF_STATE, "Final decision is " + (approved ? "APPROVED" : "REFUSED"));

        SpringApplicationContextHolder.getActivitiInterface().notifyMidpointAboutProcessFinishedEvent(execution);
    }

}
