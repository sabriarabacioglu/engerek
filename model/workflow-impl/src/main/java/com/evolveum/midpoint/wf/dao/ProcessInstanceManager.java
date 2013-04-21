package com.evolveum.midpoint.wf.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WorkflowServiceImpl;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class ProcessInstanceManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceManager.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    private static final String DOT_CLASS = WorkflowServiceImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowService.class.getName() + ".";

    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_INTERFACE + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_INTERFACE + "deleteProcessInstance";

    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_STOP_PROCESS_INSTANCE);

        RuntimeService rs = activitiEngine.getRuntimeService();
        try {
            rs.deleteProcessInstance(instanceId, "Process instance stopped on the request of " + username);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Process instance couldn't be stopped", e);
            LoggingUtils.logException(LOGGER, "Process instance {} couldn't be stopped", e);
        }
    }

    public void deleteProcessInstance(String instanceId, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_PROCESS_INSTANCE);

        HistoryService hs = activitiEngine.getHistoryService();
        try {
            hs.deleteHistoricProcessInstance(instanceId);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Process instance couldn't be deleted", e);
            LoggingUtils.logException(LOGGER, "Process instance {} couldn't be deleted", e);
        }
    }

}