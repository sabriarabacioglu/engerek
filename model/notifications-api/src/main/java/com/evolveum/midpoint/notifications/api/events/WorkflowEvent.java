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

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
abstract public class WorkflowEvent extends BaseEvent {

    private String processInstanceName;
    private PrismObject<? extends ObjectType> processInstanceState;
    private String operationStatusCustom;                // exact string representation of the status (useful for work items that return custom statuses)
    private ChangeType changeType;                      // ADD = process/task start, DELETE = process/task finish (for now)

    public WorkflowEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, ChangeType changeType) {
        super(lightweightIdentifierGenerator);

        Validate.notNull(changeType, "changeType is null");
        this.changeType = changeType;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public PrismObject<? extends ObjectType> getProcessInstanceState() {
        return processInstanceState;
    }

    public void setProcessInstanceState(PrismObject<? extends ObjectType> processInstanceState) {
        this.processInstanceState = processInstanceState;
    }

    public OperationStatus getOperationStatus() {
        return resultToStatus(changeType, operationStatusCustom);
    }

    public String getOperationStatusCustom() {
        return operationStatusCustom;
    }

    public void setOperationStatusCustom(String operationStatusCustom) {
        this.operationStatusCustom = operationStatusCustom;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        return getOperationStatus().matchesEventStatusType(eventStatusType);
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return changeTypeMatchesOperationType(changeType, eventOperationType);
    }

    public boolean isResultKnown() {
        return !isInProgress();         // for now
    }

    public boolean isApproved() {
        return isSuccess();             // for now
    }

    public boolean isRejected() {
        return isFailure();             // for now
    }

    private OperationStatus resultToStatus(ChangeType changeType, String decision) {
        if (changeType != ChangeType.DELETE) {
            return OperationStatus.SUCCESS;
        } else {
            if (decision == null) {
                return OperationStatus.IN_PROGRESS;
            } else if (decision.equals(ApprovalUtils.DECISION_APPROVED)) {
                return OperationStatus.SUCCESS;
            } else if (decision.equals(ApprovalUtils.DECISION_REJECTED)) {
                return OperationStatus.FAILURE;
            } else {
                return OperationStatus.OTHER;
            }
        }
    }


    @Override
    public String toString() {
        return "WorkflowEvent{" +
                "event=" + super.toString() +
                ", processInstanceName='" + processInstanceName + '\'' +
                ", changeType=" + changeType +
                ", operationStatusCustom=" + operationStatusCustom +
                '}';
    }

}
