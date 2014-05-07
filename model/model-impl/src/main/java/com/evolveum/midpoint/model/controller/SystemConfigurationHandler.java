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
package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.annotation.PostConstruct;

/**
 * @author semancik
 *
 */
@Component
public class SystemConfigurationHandler implements ChangeHook {
	
	private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationHandler.class);

    private static final String DOT_CLASS = SystemConfigurationHandler.class + ".";

    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/sysconfig-hook-1";

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
    }

    public void postInit(PrismObject<SystemConfigurationType> systemConfiguration, OperationResult parentResult) {
        LoggingConfigurationType loggingConfig = ProfilingConfigurationManager.checkSystemProfilingConfiguration(systemConfiguration);
        applyLoggingConfiguration(loggingConfig, systemConfiguration.asObjectable().getVersion(), parentResult);
        //applyLoggingConfiguration(systemConfiguration.asObjectable().getLogging(), systemConfiguration.asObjectable().getVersion(), parentResult);
    }

    private void applyLoggingConfiguration(LoggingConfigurationType loggingConfig, String version, OperationResult parentResult) {
        if (loggingConfig != null) {
            LoggingConfigurationManager.configure(loggingConfig, version, parentResult);
        }
    }

    @Override
    public <O extends ObjectType> HookOperationMode invoke(ModelContext<O> context, Task task, OperationResult parentResult) {

        ModelState state = context.getState();
        if (state != ModelState.FINAL) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("sysconfig handler called in state = " + state + ", exiting.");
            }
            return HookOperationMode.FOREGROUND;
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("sysconfig handler called in state = " + state + ", proceeding.");
            }
        }

        if (context.getFocusClass() != SystemConfigurationType.class) {
        	LOGGER.trace("invoke() EXITING: Changes not related to systemConfiguration");
            return HookOperationMode.FOREGROUND;
        }
        ModelContext<SystemConfigurationType> confContext = (ModelContext<SystemConfigurationType>)context;
        ModelElementContext<SystemConfigurationType> focusContext = confContext.getFocusContext();
        
        boolean isDeletion = false;     // is this config-related change a deletion?
        PrismObject<SystemConfigurationType> object = focusContext.getObjectNew();
        if (object == null) {
        	isDeletion = true;
            object = focusContext.getObjectOld();
        }
        if (object == null) {
            LOGGER.warn("Probably invalid projection context: both old and new objects are null");          // if the handler would not work because of this, for us to see the reason
        }

        LOGGER.trace("change relates to sysconfig, is deletion: {}", isDeletion);

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "invoke");
        try {
            if (isDeletion) {
                LoggingConfigurationManager.resetCurrentlyUsedVersion();        // because the new config (if any) will have version number probably starting at 1 - so to be sure to read it when it comes
                LOGGER.trace("invoke() EXITING because operation is DELETION");
                return HookOperationMode.FOREGROUND;
            }

            /*
             * Because we need to know actual version of the system configuration (generated by repo), we have to re-read
             * current configuration. (At this moment, it is already stored there.)
             */

            PrismObject<SystemConfigurationType> config = cacheRepositoryService.getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);

            LOGGER.trace("invoke() SystemConfig from repo: {}, ApplyingLoggingConfiguration", config.getVersion());

            //ProfilingConfigurationManager.checkSystemProfilingConfiguration(config);
            applyLoggingConfiguration(ProfilingConfigurationManager.checkSystemProfilingConfiguration(config), config.asObjectable().getVersion(), result);
            result.recordSuccessIfUnknown();

        } catch (ObjectNotFoundException e) {
            String message = "Cannot read system configuration because it does not exist in repository: " + e.getMessage();
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
        } catch (SchemaException e) {
            String message = "Cannot read system configuration because of schema exception: " + e.getMessage();
            LoggingUtils.logException(LOGGER, message, e);
            result.recordFatalError(message, e);
        }

        return HookOperationMode.FOREGROUND;
    }

    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
        // do nothing
    }
}
