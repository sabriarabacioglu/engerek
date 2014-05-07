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

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *  On this place, we take care about profilingConfiguration part of midPoint systemConfiguration
 *
 *  @author shood
 * */
public class ProfilingConfigurationManager {

    /* Class Attributes */
    private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationManager.class);
    private static final String DOT_CLASS = ProfilingConfigurationManager.class + ".";

    private static final String REQUEST_FILTER_LOGGER_CLASS_NAME = "com.evolveum.midpoint.web.util.MidPointProfilingServletFilter";
    private static final String SUBSYSTEM_PROFILING_LOGGER = "com.evolveum.midpoint.util.aspect.ProfilingDataManager";
    private static final String APPENDER_IDM_PROFILE = "IDM-PROFILE_LOG";

    //Subsystems
    public static final String SUBSYSTEM_REPOSITORY = "REPOSITORY";
    public static final String SUBSYSTEM_TASKMANAGER = "TASKMANAGER";
    public static final String SUBSYSTEM_PROVISIONING = "PROVISIONING";
    public static final String SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = "RESOURCEOBJECTCHANGELISTENER";
    public static final String SUBSYSTEM_MODEL = "MODEL";
    public static final String SUBSYSTEM_UCF = "UCF";
    public static final String SUBSYSTEM_WORKFLOW = "WORKFLOW";
    public static final String SUBSYSTEM_WEB = "WEB";

    /* Object Attributes */

    /* Methods - STATIC */

    /**
     *  In this method, we perform the check of systemConfiguration object, searching for any changes
     *  related to profilingConfiguration
     * */
    public static LoggingConfigurationType checkSystemProfilingConfiguration(PrismObject<SystemConfigurationType> systemConfigurationPrism){
        SystemConfigurationType systemConfig = systemConfigurationPrism.asObjectable();
        ProfilingConfigurationType profilingConfig = systemConfig.getProfilingConfiguration();
        boolean isSubsystemConfig = false;

        if(profilingConfig == null || !profilingConfig.isEnabled())
            return systemConfig.getLogging();
        else{
            isSubsystemConfig = applySubsystemProfiling(systemConfig);
            return applyProfilingConfiguration(systemConfigurationPrism, profilingConfig, isSubsystemConfig);
        }
    }   //checkSystemProfilingConfiguration

    /*
     *  Checks systemConfig profiling configuration and performs necessary actions
     * */
    private static LoggingConfigurationType applyProfilingConfiguration(PrismObject<SystemConfigurationType> systemConfigurationPrism, ProfilingConfigurationType profilingConfig, boolean subsystemProfiling){
        SystemConfigurationType systemConfig = systemConfigurationPrism.asObjectable();

        LoggingConfigurationType loggingConfig = systemConfig.getLogging();

        if(loggingConfig != null){
            //LOGGER.info("entering profiling config applyProfilingConfiguration()");
            if(checkXsdBooleanValue(profilingConfig.isRequestFilter())){
                ClassLoggerConfigurationType requestFilterLogger = new ClassLoggerConfigurationType();
                requestFilterLogger.setPackage(REQUEST_FILTER_LOGGER_CLASS_NAME);
                requestFilterLogger.setLevel(LoggingLevelType.TRACE);
                requestFilterLogger.getAppender().clear();
                requestFilterLogger.getAppender().add(APPENDER_IDM_PROFILE);

                loggingConfig.getClassLogger().add(requestFilterLogger);
            }
            if(subsystemProfiling){
                ClassLoggerConfigurationType subsystemLogger = new ClassLoggerConfigurationType();
                subsystemLogger.setPackage(SUBSYSTEM_PROFILING_LOGGER);
                subsystemLogger.setLevel(LoggingLevelType.DEBUG);
                subsystemLogger.getAppender().clear();
                subsystemLogger.getAppender().add(APPENDER_IDM_PROFILE);

                loggingConfig.getClassLogger().add(subsystemLogger);
            }
        }

        LOGGER.info("Applying profiling configuration.");
        return  loggingConfig;
    }   //applyProfilingConfiguration

    /*
    *   Checks for subsystem profiling in system configuration.
    * */
    private static boolean applySubsystemProfiling(SystemConfigurationType systemConfig){
        ProfilingConfigurationType profilingConfig = systemConfig.getProfilingConfiguration();

        Map<String, Boolean> profiledSubsystems = new HashMap<String, Boolean>();
        int dumpInterval = 0;
        boolean subSystemProfiling = false;
        boolean performanceProfiling = false;
        boolean requestProfiling = false;

        profiledSubsystems.put(SUBSYSTEM_PROVISIONING, checkXsdBooleanValue(profilingConfig.isProvisioning()));
        profiledSubsystems.put(SUBSYSTEM_REPOSITORY, checkXsdBooleanValue(profilingConfig.isRepository()));
        profiledSubsystems.put(SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER, checkXsdBooleanValue(profilingConfig.isResourceObjectChangeListener()));
        profiledSubsystems.put(SUBSYSTEM_TASKMANAGER, checkXsdBooleanValue(profilingConfig.isTaskManager()));
        profiledSubsystems.put(SUBSYSTEM_UCF, checkXsdBooleanValue(profilingConfig.isUcf()));
        profiledSubsystems.put(SUBSYSTEM_MODEL, checkXsdBooleanValue(profilingConfig.isModel()));
        profiledSubsystems.put(SUBSYSTEM_WORKFLOW, checkXsdBooleanValue(profilingConfig.isWorkflow()));
        profiledSubsystems.put(SUBSYSTEM_WEB, checkXsdBooleanValue(profilingConfig.isRequestFilter()));

        for(Boolean b: profiledSubsystems.values()){
            if(b != null && b){
                subSystemProfiling = true;
                break;
            }
        }

        //Check the dump interval
        if(profilingConfig.getDumpInterval() != null)
            dumpInterval = profilingConfig.getDumpInterval();

        performanceProfiling = checkXsdBooleanValue(profilingConfig.isPerformanceStatistics());
        requestProfiling = checkXsdBooleanValue(profilingConfig.isRequestFilter());

        if (subSystemProfiling || performanceProfiling || requestProfiling){
            ProfilingDataManager.getInstance().configureProfilingDataManager(profiledSubsystems, dumpInterval, subSystemProfiling, performanceProfiling, requestProfiling);
            return true;
        }
        else {
            ProfilingDataManager.getInstance().configureProfilingDataManager(profiledSubsystems, dumpInterval, subSystemProfiling, performanceProfiling, requestProfiling);
            return false;
        }
    }   //applySubsystemProfiling

    /*
    *   Check value of Boolean in xsd
    * */
    private static boolean checkXsdBooleanValue(Boolean value){
        if(value == null || !value)
            return false;
        else
            return true;
    }   //checkXsdBooleanValue

}   //ProfilingConfigurationManager
