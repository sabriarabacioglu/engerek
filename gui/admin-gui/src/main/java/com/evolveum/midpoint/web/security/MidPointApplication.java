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

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskListener;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.component.GuiComponents;
import com.evolveum.midpoint.web.component.atmosphere.NotifyMessage;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.page.error.PageError401;
import com.evolveum.midpoint.web.page.error.PageError403;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.MidPointPageParametersEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowManager;

import org.apache.commons.configuration.Configuration;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.atmosphere.EventBus;
import org.apache.wicket.atmosphere.config.AtmosphereLogLevel;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.markup.head.PriorityFirstComparator;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.settings.IApplicationSettings;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.lang.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author lazyman
 */
@Component("midpointApplication")
public class MidPointApplication extends AuthenticatedWebApplication {

    /**
     * Max. photo size for user/jpegPhoto
     */
    public static final Bytes USER_PHOTO_MAX_FILE_SIZE = Bytes.kilobytes(192);

    private static final String WEB_APP_CONFIGURATION = "midpoint.webApplication";

    private static final Trace LOGGER = TraceManager.getTrace(MidPointApplication.class);

    private EventBus eventBus;

    @Autowired
    transient ModelService model;
    @Autowired
    transient ModelInteractionService modelInteractionService;
    @Autowired
    transient TaskService taskService;
    @Autowired
    transient PrismContext prismContext;
    @Autowired
    transient TaskManager taskManager;
    @Autowired
    transient private WorkflowManager workflowManager;
    @Autowired
    transient MidpointConfiguration configuration;
    @Autowired(required = true)
    transient Protector protector;
    private WebApplicationConfiguration webApplicationConfiguration;

    @Override
    protected void onDestroy() {
        GuiComponents.destroy();

        super.onDestroy();
    }

    @Override
    public Class<PageDashboard> getHomePage() {
        return PageDashboard.class;
    }

    @Override
    public void init() {
        super.init();

        getComponentInstantiationListeners().add(new SpringComponentInjector(this));

        IResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.setHeaderItemComparator(new PriorityFirstComparator(true));

        resourceSettings.setThrowExceptionOnMissingResource(false);
        getMarkupSettings().setStripWicketTags(true);
        getMarkupSettings().setDefaultBeforeDisabledLink("");
        getMarkupSettings().setDefaultAfterDisabledLink("");

        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getDebugSettings().setAjaxDebugModeEnabled(true);
            getDebugSettings().setDevelopmentUtilitiesEnabled(true);
        }

        //pretty url for resources (e.g. images)
        mountFiles(ImgResources.BASE_PATH, ImgResources.class);

        //exception handling an error pages
        IApplicationSettings appSettings = getApplicationSettings();
        appSettings.setAccessDeniedPage(PageError401.class);
        appSettings.setInternalErrorPage(PageError.class);
        appSettings.setPageExpiredErrorPage(PageError.class);

        mount(new MountedMapper("/error",PageError.class, MidPointPageParametersEncoder.ENCODER));
        mount(new MountedMapper("/error/401",PageError401.class, MidPointPageParametersEncoder.ENCODER));
        mount(new MountedMapper("/error/403",PageError403.class, MidPointPageParametersEncoder.ENCODER));
        mount(new MountedMapper("/error/404",PageError404.class, MidPointPageParametersEncoder.ENCODER));

        getRequestCycleListeners().add(new AbstractRequestCycleListener() {

            @Override
            public IRequestHandler onException(RequestCycle cycle, Exception ex) {
                return new RenderPageRequestHandler(new PageProvider(new PageError(ex)));
            }
        });

// todo wicket atmosphere was disabled because of form file upload and redirection url problems.
//        //ajax push (just an experiment)
//        eventBus = new EventBus(this);
//        eventBus.getParameters().setLogLevel(AtmosphereLogLevel.DEBUG);
//
//        //enable simple task notifications here
//        taskManager.registerTaskListener(new TaskListener() {
//
//            @Override
//            public void onTaskStart(Task task) {
//                EventBus bus = getEventBus();
//                bus.post(new NotifyMessage("Task start", WebMiscUtil.getOrigStringFromPoly(task.getName()) + " started.",
//                        OperationResultStatus.SUCCESS));
//            }
//
//            @Override
//            public void onTaskFinish(Task task, TaskRunResult runResult) {
//                EventBus bus = getEventBus();
//                bus.post(new NotifyMessage("Task finish", WebMiscUtil.getOrigStringFromPoly(task.getName()) + " finished.",
//                        OperationResultStatus.parseStatusType(task.getResultStatus())));
//            }
//
//            @Override
//            public void onTaskThreadStart(Task task, boolean isRecovering) {
//
//            }
//
//            @Override
//            public void onTaskThreadFinish(Task task) {
//
//            }
//        });

        //descriptor loader, used for customization
        new DescriptorLoader().loadData(this);
    }

    private void mountFiles(String path, Class<?> clazz) {
        try {
            String absPath = getServletContext().getRealPath("WEB-INF/classes") + "/"
                    + clazz.getPackage().getName().replace('.', '/');

            File folder = new File(absPath);
            mountFiles(path, clazz, folder);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't mount files", ex);
        }
    }

    private void mountFiles(String path, Class<?> clazz, File folder) {
        File[] files = folder.listFiles(new ResourceFileFilter());
        for (File file : files) {
            if (!file.exists()) {
                LOGGER.warn("Couldn't mount resource {}.", new Object[]{file.getPath()});
                continue;
            }
            if (file.isDirectory()) {
                mountFiles(path + "/" + file.getName(), clazz, file);
            } else {
                mountResource(path + "/" + file.getName(), new SharedResourceReference(clazz, file.getName()));
            }
        }
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        if (webApplicationConfiguration == null) {
            Configuration config = configuration.getConfiguration(WEB_APP_CONFIGURATION);
            webApplicationConfiguration = new WebApplicationConfiguration(config);
        }
        return webApplicationConfiguration;
    }

    public ModelService getModel() {
        return model;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public Protector getProtector() {
        return protector;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return PageLogin.class;
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return MidPointAuthWebSession.class;
    }

    public WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    private static class ResourceFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File parent, String name) {
            if (name.endsWith("png") || name.endsWith("gif")) {
                return true;
            }

            return false;
        }
    }
}
