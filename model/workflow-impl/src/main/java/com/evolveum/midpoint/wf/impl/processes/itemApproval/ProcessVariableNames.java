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

/**
 * @author mederly
 */
public class ProcessVariableNames {

    // A data structure that describes the request to approve something. Contains item that has to be approved (e.g. assignment) and approval schema.
    public static final String APPROVAL_REQUEST = "approvalRequest";            // of type ApprovalRequest

    // How the user task (work item) should be named.
    public static final String APPROVAL_TASK_NAME = "approvalTaskName";         // of type String

    // List of all decisions done within this process instance.
    public static final String ALL_DECISIONS = "allDecisions";                  // List<Decision>

    // Whether we have to stop approving at the current level (means the approval was rejected at this level).
    public static final String LOOP_LEVELS_STOP = "loopLevels_stop";            // Boolean

    // Information about currently active level of approval.
    public static final String LEVEL = "level";                                 // ApprovalLevel

    // Approvers that should be consulted within this level.
    public static final String APPROVERS_IN_LEVEL = "approversInLevel";         // List<LightweightObjectRef>

    // Current approver (one of APPROVERS_IN_LEVEL)
    public static final String APPROVER_REF = "approverRef";         // LightweightObjectRef

    // Oid of the approver if approverRef is a user (in that case, the task is assigned directly to the user)
    public static final String ASSIGNEE = "assignee";

    // Oid of the abstract role if approverRef is an abstract role (in that case, it is used as a candidate group)
    public static final String CANDIDATE_GROUPS = "candidateGroups";

    // List of decisions done in this level.
    public static final String DECISIONS_IN_LEVEL = "decisionsInLevel";         // List<Decision>

    // Whether we have to stop evaluating current level (e.g. because strategy was 'firstDecides' and the first person decided (approved or rejected).
    public static final String LOOP_APPROVERS_IN_LEVEL_STOP = "loopApproversInLevel_stop";  // Boolean
}
