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

package com.evolveum.midpoint.wf.impl.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.addrole.AddRoleVariableNames;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is a preliminary version of 'password approval' process aspect. The idea is that in some cases, a user may request
 * changing (resetting) his password, but if not enough authentication information is available, the change may be
 * subject to manual approval.
 *
 * Exact conditions should be coded into getApprovalRequestList method. Currently, we only test for ANY password change
 * request and push it into approval process.
 *
 * DO NOT USE THIS ASPECT IN PRODUCTION UNLESS YOU KNOW WHAT YOU ARE DOING
 *
 * @author mederly
 */
@Component
public class ChangePasswordAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordAspect.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException {

        List<ApprovalRequest<String>> approvalRequestList = new ArrayList<ApprovalRequest<String>>();
        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();

        if (change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }

        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        ItemPath passwordPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();

            // this needs to be customized and enhanced; e.g. to start wf process only when not enough authentication info is present in the request
            // also, what if we replace whole 'credentials' container?
            if (passwordPath.equivalent(delta.getPath())) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Found password-changing delta, moving it into approval request. Delta = " + delta.debugDump());
                }
                ApprovalRequest<String> approvalRequest = createApprovalRequest(delta);
                approvalRequestList.add(approvalRequest);
                instructions.add(createStartProcessInstruction(modelContext, delta, approvalRequest, taskFromModel, result));
                deltaIterator.remove();
            }
        }
        return instructions;
    }

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) {
        return null;        // todo implement this
    }

    @Override
    public PrismObject<? extends ObjectType> prepareRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) {
        return null;        // todo implement this
    }

    private ApprovalRequest<String> createApprovalRequest(ItemDelta delta) {

        ObjectReferenceType approverRef = new ObjectReferenceType();
        approverRef.setOid(SystemObjectsType.USER_ADMINISTRATOR.value());
        approverRef.setType(UserType.COMPLEX_TYPE);

        List<ObjectReferenceType> approvers = new ArrayList<ObjectReferenceType>();
        approvers.add(approverRef);

        return new ApprovalRequestImpl("Password change", null, approvers, null, null, prismContext);
    }

    private PcpChildJobCreationInstruction createStartProcessInstruction(ModelContext<?> modelContext, ItemDelta delta, ApprovalRequest approvalRequest, Task taskFromModel, OperationResult result) throws SchemaException {

        String userName = MiscDataUtil.getFocusObjectName(modelContext);
        String objectOid = primaryChangeAspectHelper.getObjectOid(modelContext);
        PrismObject<UserType> requester = primaryChangeAspectHelper.getRequester(taskFromModel, result);

        // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
        PcpChildJobCreationInstruction instruction =
                PcpChildJobCreationInstruction.createInstruction(getChangeProcessor());

        // set some common task/process attributes
        instruction.prepareCommonAttributes(this, modelContext, objectOid, requester);

        // prepare and set the delta that has to be approved
        instruction.setDeltaProcessAndTaskVariables(itemDeltaToObjectDelta(objectOid, delta));

        // set the names of midPoint task and activiti process instance
        instruction.setTaskName("Workflow for approving password change for " + userName);
        instruction.setProcessInstanceName("Changing password for " + userName);

        // setup general item approval process
        String approvalTaskName = "Approve changing password for " + userName;
        itemApprovalProcessInterface.prepareStartInstruction(instruction, approvalRequest, approvalTaskName);

        // set some aspect-specific variables
        instruction.addProcessVariable(AddRoleVariableNames.USER_NAME, userName);

        return instruction;
    }

    private ObjectDelta<Objectable> itemDeltaToObjectDelta(String objectOid, ItemDelta delta) {
        return (ObjectDelta<Objectable>) (ObjectDelta) ObjectDelta.createModifyDelta(objectOid, delta, UserType.class, prismContext);
    }

}
