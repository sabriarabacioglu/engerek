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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;

/**
 * Adds "RUNNING" state to the TaskExecutionStatus (meaning the task is currently executing at a node).
 * And also "SUSPENDING" if it is running, but marked as suspended.
 *
 * @see TaskExecutionStatus
 * @author Pavol Mederly
 */
public enum TaskDtoExecutionStatus {

    RUNNING_OR_RUNNABLE,
    RUNNING,
    RUNNABLE,
    WAITING,
    SUSPENDED,
    SUSPENDING,
    CLOSED;

    public static TaskDtoExecutionStatus fromTaskExecutionStatus(TaskExecutionStatusType executionStatus, boolean running) {
        if (running) {
            if (executionStatus == TaskExecutionStatusType.SUSPENDED) {
                return SUSPENDING;
            } else {
                return TaskDtoExecutionStatus.RUNNING;
            }
        } else {
            if (executionStatus != null) {
                switch (executionStatus) {
                    case RUNNABLE: return RUNNABLE;
                    case WAITING: return WAITING;
                    case SUSPENDED: return SUSPENDED;
                    case CLOSED: return CLOSED;
                    default: throw new IllegalArgumentException("executionStatus = " + executionStatus);
                }
            } else {
                return null;
            }
        }
    }
}
