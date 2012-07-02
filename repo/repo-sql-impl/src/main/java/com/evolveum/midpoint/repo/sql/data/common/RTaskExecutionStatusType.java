/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.xml.ns._public.common.common_2.TaskExecutionStatusType;

/**
 * @author lazyman
 */
public enum RTaskExecutionStatusType {

    RUNNABLE(TaskExecutionStatusType.RUNNABLE),
    WAITING(TaskExecutionStatusType.WAITING),
    SUSPENDED(TaskExecutionStatusType.SUSPENDED),
    CLOSED(TaskExecutionStatusType.CLOSED);

    private TaskExecutionStatusType status;

    private RTaskExecutionStatusType(TaskExecutionStatusType status) {
        this.status = status;
    }

    public TaskExecutionStatusType getStatus() {
        return status;
    }

    public static RTaskExecutionStatusType toRepoType(TaskExecutionStatusType status) {
        if (status == null) {
            return null;
        }

        for (RTaskExecutionStatusType repo : RTaskExecutionStatusType.values()) {
            if (status.equals(repo.getStatus())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown task execution status type " + status);
    }
}