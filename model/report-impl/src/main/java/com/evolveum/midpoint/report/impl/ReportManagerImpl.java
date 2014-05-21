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

package com.evolveum.midpoint.report.impl;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.util.ReportTypeUtil;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.hooks.ReadHook;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;


/**
 * @author lazyman, garbika
 */
@Service(value = "reportManager")
public class ReportManagerImpl implements ReportManager, ChangeHook, ReadHook {
	
    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/report-hook-1";
    
    private static final Trace LOGGER = TraceManager.getTrace(ReportManagerImpl.class);
    
    private static final String CLASS_NAME_WITH_DOT = ReportManagerImpl.class + ".";
    private static final String CLEANUP_REPORT_OUTPUTS = CLASS_NAME_WITH_DOT + "cleanupReportOutputs";
    private static final String DELETE_REPORT_OUTPUT = CLASS_NAME_WITH_DOT + "deleteReportOutput";
    private static final String REPORT_OUTPUT_DATA = CLASS_NAME_WITH_DOT + "getReportOutputData";
    
	@Autowired
    private HookRegistry hookRegistry;

	@Autowired
    private TaskManager taskManager;
	
	@Autowired
	private PrismContext prismContext;
	
	@Autowired
	private ModelService modelService;


	@PostConstruct
    public void init() {   	
        hookRegistry.registerChangeHook(HOOK_URI, this);
        hookRegistry.registerReadHook(HOOK_URI, this);
    }

    @Override
    public <T extends ObjectType> void invoke(PrismObject<T> object,
                                              Collection<SelectorOptions<GetOperationOptions>> options, Task task,
                                              OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {

        if (!ReportType.class.equals(object.getCompileTimeClass())) {
            return;
        }

        boolean raw = isRaw(options);
        if (!raw) {
            ReportTypeUtil.applyDefinition((PrismObject<ReportType>) object, prismContext);
        }
    }

    private boolean isRaw(Collection<SelectorOptions<GetOperationOptions>> options) {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        return rootOptions == null ? false : GetOperationOptions.isRaw(rootOptions);
    }

    /**
     * Creates and starts task with proper handler, also adds necessary information to task
     * (like ReportType reference and so on).
     *
     * @param object
     * @param task
     * @param parentResult describes report which has to be created
     */
    
    @Override
    public void runReport(PrismObject<ReportType> object, Task task, OperationResult parentResult) {    	
        task.setHandlerUri(ReportCreateTaskHandler.REPORT_CREATE_TASK_URI);
        task.setObjectRef(object.getOid(), ReportType.COMPLEX_TYPE);

        task.setThreadStopAction(ThreadStopActionType.CLOSE);
    	task.makeSingle();
    	
    	taskManager.switchToBackground(task, parentResult);
    }
    /**
     * Transforms change:
     * 1/ ReportOutputType DELETE to MODIFY some attribute to mark it for deletion.
     * 2/ ReportType ADD and MODIFY should compute jasper design and styles if necessary
     *
     * @param context
     * @param task
     * @param result
     * @return
     * @throws UnsupportedEncodingException 
     */
    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult parentResult)  {
    	ModelState state = context.getState();
         if (state != ModelState.FINAL) {
             if (LOGGER.isTraceEnabled()) {
                 LOGGER.trace("report manager called in state = " + state + ", exiting.");
             }
             return HookOperationMode.FOREGROUND;
         } else {
             if (LOGGER.isTraceEnabled()) {
                 LOGGER.trace("report manager called in state = " + state + ", proceeding.");
             }
         }

         boolean relatesToReport = false;
         boolean isDeletion = false; 
         PrismObject<?> object = null;
         for (Object o : context.getProjectionContexts()) {     
             boolean deletion = false;
             object = ((ModelElementContext<?>) o).getObjectNew();
             if (object == null) {
                 deletion = true;
                 object = ((ModelElementContext<?>) o).getObjectOld();
             }
             if (object == null) {
                 LOGGER.warn("Probably invalid projection context: both old and new objects are null");  
             } else if (object.getCompileTimeClass().isAssignableFrom(ReportType.class)) {
                 relatesToReport = true;
                 isDeletion = deletion;
             }
         }

         if (LOGGER.isTraceEnabled()) {
             LOGGER.trace("change relates to report: " + relatesToReport + ", is deletion: " + isDeletion);
         }

         if (!relatesToReport) {
             LOGGER.trace("invoke() EXITING: Changes not related to report");
             return HookOperationMode.FOREGROUND;
         }
         
         if (isDeletion) {
             LOGGER.trace("invoke() EXITING because operation is DELETION");
             return HookOperationMode.FOREGROUND;
         }

         OperationResult result = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "invoke");
         try {
             ReportType reportType = (ReportType) object.asObjectable();
             JasperDesign jasperDesign = null;
             if (reportType.getTemplate() == null)
             {
            	 PrismSchema reportSchema = null;
            	 PrismContainer<Containerable> parameterConfiguration = null;  
            	 try
            	 {
            		reportSchema = ReportUtils.getParametersSchema(reportType, prismContext);
            		parameterConfiguration = ReportUtils.getParametersContainer(reportType, reportSchema);
             		
            	 } catch (Exception ex){
            		 String message = "Cannot create parameter configuration: " + ex.getMessage();
            		 LOGGER.error(message);
            		 result.recordFatalError(message, ex);
            	 }
            	 
            	 jasperDesign = ReportUtils.createJasperDesign(reportType, parameterConfiguration, reportSchema) ;
            	 LOGGER.trace("create jasper design : {}", jasperDesign);
             }
             else
             {
            	 byte[] reportTemplateBase64 = reportType.getTemplate();
            	 byte[] reportTemplate = Base64.decodeBase64(reportTemplateBase64);
            	 InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate);
            	 jasperDesign = JRXmlLoader.load(inputStreamJRXML);
            	 LOGGER.trace("load jasper design : {}", jasperDesign);
             }
             // Compile template
             JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
             LOGGER.trace("compile jasper design, create jasper report : {}", jasperReport);
             
             //result.computeStatus();
             result.recordSuccessIfUnknown();

         }
         catch (JRException ex) {
             String message = "Cannot load or compile jasper report: " + ex.getMessage();
             LOGGER.error(message);
             result.recordFatalError(message, ex);
         } 
        

        return HookOperationMode.FOREGROUND;
    }
    
    
    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
    	
    }
  
    @Override
    public void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult) {
    	OperationResult result = parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS);

        if (cleanupPolicy.getMaxAge() == null) {
            return;
        }

        Duration duration = cleanupPolicy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date deleteReportOutputsTo = new Date();
        duration.addTo(deleteReportOutputsTo);

        LOGGER.info("Starting cleanup for report outputs deleting up to {} (duration '{}').",
                new Object[]{deleteReportOutputsTo, duration});

        XMLGregorianCalendar timeXml = XmlTypeConverter.createXMLGregorianCalendar(deleteReportOutputsTo.getTime());
        
        List<PrismObject<ReportOutputType>> obsoleteReportOutputs = new ArrayList<PrismObject<ReportOutputType>>();
        try {
            ObjectQuery obsoleteReportOutputsQuery = ObjectQuery.createObjectQuery(LessFilter.createLess(
            		new ItemPath(ReportOutputType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), 
            		ReportOutputType.class, prismContext, timeXml, true));
            obsoleteReportOutputs = modelService.searchObjects(ReportOutputType.class, obsoleteReportOutputsQuery, null, null, result);
        } catch (Exception e) {
            throw new SystemException("Couldn't get the list of obsolete report outputs: " + e.getMessage(), e);
        }

        LOGGER.debug("Found {} report output(s) to be cleaned up", obsoleteReportOutputs.size());

        boolean interrupted = false;
        int deleted = 0;
        int problems = 0;
        
        for (PrismObject<ReportOutputType> reportOutputPrism : obsoleteReportOutputs){
        	ReportOutputType reportOutput = reportOutputPrism.asObjectable();
        	
        	LOGGER.trace("Removing report output {} along with {} file.", reportOutput.getName().getOrig(), 
        			reportOutput.getFilePath());
        	boolean problem = false;
        	try {
                    deleteReportOutput(reportOutput, result);
                } catch (Exception e) {
                	LoggingUtils.logException(LOGGER, "Couldn't delete obsolete report output {} due to a exception", e, reportOutput);
                    problem = true;
                }

                if (problem) {
                    problems++;
                } else {
                    deleted++;
            }
        }
        result.computeStatusIfUnknown();

        LOGGER.info("Report cleanup procedure " + 
        (interrupted ? "was interrupted" : "finished") + 
        ". Successfully deleted {} report outputs; there were problems with deleting {} report ouptuts.", deleted, problems);
        String suffix = interrupted ? " Interrupted." : "";
        if (problems == 0) {
            parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS + ".statistics").recordStatus(OperationResultStatus.SUCCESS,
            		"Successfully deleted " + deleted + " report output(s)." + suffix);
        } else {
            parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS + ".statistics").recordPartialError("Successfully deleted " + 
        deleted + " report output(s), "
                    + "there was problems with deleting " + problems + " report outputs.");
        }
    }
    
    private void deleteReportOutput(ReportOutputType reportOutput, OperationResult parentResult) throws Exception {
    	String oid = reportOutput.getOid();

    	Task task = taskManager.createTaskInstance(DELETE_REPORT_OUTPUT);
    	parentResult.addSubresult(task.getResult());
    	OperationResult result = parentResult.createSubresult(DELETE_REPORT_OUTPUT);

        result.addParam("oid", oid);
        try {
            File reportFile = new File(reportOutput.getFilePath());
            reportFile.delete();
            
			ObjectDelta<ReportOutputType> delta = ObjectDelta.createDeleteDelta(ReportOutputType.class, oid, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

			modelService.executeChanges(deltas, null, task, result);	
            
            result.recordSuccessIfUnknown();
        }
        catch (Exception e) {
        	result.recordFatalError("Cannot delete the report output because of a exception.", e);
            throw e;
        }
    }
	
   
    @Override
    public InputStream getReportOutputData(String reportOutputOid, OperationResult parentResult) {
    	Task task = taskManager.createTaskInstance(REPORT_OUTPUT_DATA);

    	OperationResult result = parentResult.createSubresult(REPORT_OUTPUT_DATA);
        result.addParam("oid", reportOutputOid);

    	InputStream reportData = null;
        try {
        	ReportOutputType reportOutput = modelService.getObject(ReportOutputType.class, reportOutputOid, null,
                    task, result).asObjectable();

            String filePath = reportOutput.getFilePath();
            if (StringUtils.isEmpty(filePath)) {
                parentResult.recordFatalError("Report output file path is not defined.");
                return null;
            }
            File file = new File(filePath);
            reportData = FileUtils.openInputStream(file);

            result.recordSuccessIfUnknown();
        } catch (Exception e) {
            LOGGER.trace("Cannot read the report data : {}", e.getMessage());
        	result.recordFatalError("Cannot read the report data.", e);
        } finally {
            result.computeStatusIfUnknown();
        }

        return reportData;
    }
}
