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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DropDownMultiChoice;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.w3c.dom.html.HTMLTableElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class LoggingConfigPanel extends SimplePanel<LoggingDto> {

    private static final String DOT_CLASS = LoggingConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_LOGGING_CONFIGURATION = DOT_CLASS + "loadLoggingConfiguration";
    private static final String OPERATION_UPDATE_LOGGING_CONFIGURATION = DOT_CLASS + "updateLoggingConfiguration";

    private static final String ID_LOGGERS_TABLE = "loggersTable";
    private static final String ID_ROOT_LEVEL = "rootLevel";
    private static final String ID_ROOT_APPENDER = "rootAppender";
    private static final String ID_TABLE_APPENDERS = "appendersTable";
    private static final String ID_BUTTON_ADD_CONSOLE_APPENDER = "addConsoleAppender";
    private static final String ID_BUTTON_ADD_FILE_APPENDER = "addFileAppender";
    private static final String ID_BUTTON_DELETE_APPENDER = "deleteAppender";
    private static final String ID_BUTTON_ADD_STANDARD_LOGGER = "addStandardLogger";

    public LoggingConfigPanel(String id) {
        super(id, null);
    }

    @Override
    public IModel<LoggingDto> createModel() {
        return new LoadableModel<LoggingDto>(false) {

            @Override
            protected LoggingDto load() {
                return initLoggingModel();
            }
        };
    }

    private LoggingDto initLoggingModel() {
        LoggingDto dto = null;
        OperationResult result = new OperationResult(OPERATION_LOAD_LOGGING_CONFIGURATION);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOGGING_CONFIGURATION);

            PrismObject<SystemConfigurationType> config = getPageBase().getModelService().getObject(
                    SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
                    task, result);
            SystemConfigurationType systemConfiguration = config.asObjectable();
            LoggingConfigurationType logging = systemConfiguration.getLogging();
            dto = new LoggingDto(config, logging);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load logging configuration.", ex);
        }

        if (!result.isSuccess()) {
            getPageBase().showResult(result);
        }

        if (dto == null) {
            dto = new LoggingDto();
        }

        return dto;
    }

    @Override
    protected void initLayout() {
        initLoggers();
        initAudit();
        initProfiling();
        initAppenders();
    }

    private void initLoggers() {
        initRoot();

        ISortableDataProvider<LoggerConfiguration, String> provider = new ListDataProvider<LoggerConfiguration>(this,
                new PropertyModel<List<LoggerConfiguration>>(getModel(), "loggers"));
        TablePanel table = new TablePanel<LoggerConfiguration>(ID_LOGGERS_TABLE, provider, initLoggerColumns());
        table.setOutputMarkupId(true);
        table.setShowPaging(false);
        add(table);

        AjaxButton addStandardLogger = new AjaxButton(ID_BUTTON_ADD_STANDARD_LOGGER,
                createStringResource("LoggingConfigPanel.button.addStandardLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addStandardLoggerPerformed(target);
            }
        };
        add(addStandardLogger);

        AjaxButton addComponentLogger = new AjaxButton("addComponentLogger",
                createStringResource("LoggingConfigPanel.button.addComponentLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addComponentLoggerPerformed(target);
            }
        };
        add(addComponentLogger);

        AjaxButton addClassLogger = new AjaxButton("addClassLogger",
                createStringResource("LoggingConfigPanel.button.addClassLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addClassLoggerPerformed(target);
            }
        };
        add(addClassLogger);

        AjaxButton deleteLogger = new AjaxButton("deleteLogger",
                createStringResource("LoggingConfigPanel.button.deleteLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteLoggerPerformed(target);
            }
        };
        add(deleteLogger);

        //initProfiling();
    }

    private void initRoot() {
        DropDownChoice<LoggingLevelType> rootLevel = new DropDownChoice<LoggingLevelType>(ID_ROOT_LEVEL,
                new PropertyModel<LoggingLevelType>(getModel(), LoggingDto.F_ROOT_LEVEL),
                WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));

        add(rootLevel);

        DropDownChoice<String> rootAppender = new DropDownChoice<String>(ID_ROOT_APPENDER,
                new PropertyModel<String>(getModel(), LoggingDto.F_ROOT_APPENDER), createAppendersListModel());
        rootAppender.setNullValid(true);
        rootAppender.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                rootAppenderChangePerformed(target);
            }
        });
        add(rootAppender);
    }

    private void initAudit(){
        CheckBox auditLog = new CheckBox("auditLog", new PropertyModel<Boolean>(getModel(), "auditLog"));
        add(auditLog);

        CheckBox auditDetails = new CheckBox("auditDetails", new PropertyModel<Boolean>(getModel(), "auditDetails"));
        add(auditDetails);

        DropDownChoice<String> auditAppender = new DropDownChoice<String>("auditAppender", new PropertyModel<String>(
                getModel(), "auditAppender"), createAppendersListModel());
        auditAppender.setNullValid(true);
        add(auditAppender);
    }

    private void initProfiling(){
        //Entry-Exit profiling init
        DropDownChoice<ProfilingLevel> profilingLevel = new DropDownChoice<ProfilingLevel>("profilingLevel",
                new PropertyModel<ProfilingLevel>(getModel(), "profilingLevel"),
                WebMiscUtil.createReadonlyModelFromEnum(ProfilingLevel.class),
                new EnumChoiceRenderer(this));
        add(profilingLevel);

        DropDownChoice<String> profilingAppender = new DropDownChoice<String>("profilingAppender",
                new PropertyModel<String>(getModel(), "profilingAppender"), createAppendersListModel());
        profilingAppender.setNullValid(true);
        add(profilingAppender);

        //Subsystem and general profiling init
        CheckBox requestFilter = new CheckBox("requestFilter", new PropertyModel<Boolean>(getModel(), "requestFilter"));
        CheckBox performanceStatistics = new CheckBox("performanceStatistics", new PropertyModel<Boolean>(getModel(), "performanceStatistics"));
        CheckBox subsystemModel = new CheckBox("subsystemModel", new PropertyModel<Boolean>(getModel(), "subsystemModel"));
        CheckBox subsystemRepository = new CheckBox("subsystemRepository", new PropertyModel<Boolean>(getModel(), "subsystemRepository"));
        CheckBox subsystemProvisioning = new CheckBox("subsystemProvisioning", new PropertyModel<Boolean>(getModel(), "subsystemProvisioning"));
        CheckBox subsystemUcf = new CheckBox("subsystemUcf", new PropertyModel<Boolean>(getModel(), "subsystemUcf"));
        CheckBox subsystemResourceObjectChangeListener = new CheckBox("subsystemResourceObjectChangeListener", new PropertyModel<Boolean>(getModel(), "subsystemResourceObjectChangeListener"));
        CheckBox subsystemTaskManager = new CheckBox("subsystemTaskManager", new PropertyModel<Boolean>(getModel(), "subsystemTaskManager"));
        CheckBox subsystemWorkflow = new CheckBox("subsystemWorkflow", new PropertyModel<Boolean>(getModel(), "subsystemWorkflow"));
        add(requestFilter);
        add(performanceStatistics);
        add(subsystemModel);
        add(subsystemRepository);
        add(subsystemProvisioning);
        add(subsystemUcf);
        add(subsystemResourceObjectChangeListener);
        add(subsystemTaskManager);
        add(subsystemWorkflow);

        TextField<Integer> dumpInterval = new TextField<Integer>("dumpInterval", new PropertyModel<Integer>(getModel(),
                "dumpInterval"));
        add(dumpInterval);
    }

    private void addStandardLoggerPerformed(AjaxRequestTarget target){
        LoggingDto dto = getModel().getObject();
        StandardLogger logger = new StandardLogger(new ClassLoggerConfigurationType());
        logger.setEditing(true);
        dto.getLoggers().add(logger);

        target.add(getLoggersTable());
    }

    private void addComponentLoggerPerformed(AjaxRequestTarget target) {
        LoggingDto dto = getModel().getObject();
        ComponentLogger logger = new ComponentLogger(new ClassLoggerConfigurationType());
        logger.setEditing(true);
        dto.getLoggers().add(logger);

        target.add(getLoggersTable());
    }

    private void addClassLoggerPerformed(AjaxRequestTarget target) {
        LoggingDto dto = getModel().getObject();
        ClassLogger logger = new ClassLogger(new ClassLoggerConfigurationType());
        logger.setEditing(true);
        dto.getLoggers().add(logger);

        target.add(getLoggersTable());
    }

    private TablePanel getLoggersTable() {
        return (TablePanel) get(ID_LOGGERS_TABLE);
    }

    private TablePanel getAppendersTable(){
        return (TablePanel) get(ID_TABLE_APPENDERS);
    }

    private List<IColumn<LoggerConfiguration, String>> initLoggerColumns() {
        List<IColumn<LoggerConfiguration, String>> columns = new ArrayList<IColumn<LoggerConfiguration, String>>();
        IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
        columns.add(column);

        //name editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(
                createStringResource("LoggingConfigPanel.logger"), "name") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<LoggerConfiguration> model) {
                if(model.getObject() instanceof StandardLogger){
                    DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                            new PropertyModel(model, "logger"),
                            WebMiscUtil.createReadonlyModelFromEnum(StandardLoggerType.class),
                            new IChoiceRenderer<StandardLoggerType>() {

                                @Override
                                public Object getDisplayValue(StandardLoggerType item) {
                                    return createStringResource("StandardLoggerType." + item).getString();
                                }

                                @Override
                                public String getIdValue(StandardLoggerType item, int index) {
                                    return Integer.toString(index);
                                }
                            });

                    FormComponent<StandardLoggerType> input = dropDownChoicePanel.getBaseFormComponent();
                    input.add(new LoggerValidator());
                    input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                    return dropDownChoicePanel;

                } else if (model.getObject() instanceof ComponentLogger) {
                    DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                            new PropertyModel(model, "component"),
                            WebMiscUtil.createReadonlyModelFromEnum(LoggingComponentType.class),
                            new IChoiceRenderer<LoggingComponentType>() {

                                @Override
                                public Object getDisplayValue(LoggingComponentType item) {
                                    return getComponenLoggerDisplayValue(item);
                                }

                                @Override
                                public String getIdValue(LoggingComponentType item, int index) {
                                    return Integer.toString(index);
                                }
                            });

                    FormComponent<LoggingComponentType> input = dropDownChoicePanel.getBaseFormComponent();
                    input.add(new LoggerValidator());
                    input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                    return dropDownChoicePanel;

                } else {
                    TextPanel<String> textPanel = new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
                    FormComponent input = textPanel.getBaseFormComponent();
                    input.add(new AttributeAppender("style", "width: 100%"));
                    input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                    input.add(new InputStringValidator());
                    return textPanel;
                }
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }
        });

        //level editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(createStringResource("LoggingConfigPanel.loggersLevel"), "level") {

            @Override
            protected Component createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
                DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                        new PropertyModel(model, getPropertyExpression()),
                        WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));
                FormComponent<LoggingLevelType> input = dropDownChoicePanel.getBaseFormComponent();
                input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                input.add(new LevelValidator());
                return dropDownChoicePanel;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }

            @Override
            protected IModel<String> createLinkModel(IModel<LoggerConfiguration> rowModel) {
                LoggerConfiguration configuration = rowModel.getObject();
                return WebMiscUtil.createLocalizedModelForEnum(configuration.getLevel(), getPageBase());
            }
        });

        //appender editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(createStringResource("LoggingConfigPanel.loggersAppender"),
                "appenders") {

            @Override
            protected IModel<String> createLinkModel(IModel<LoggerConfiguration> rowModel){
                final LoggerConfiguration configuration = rowModel.getObject();

                if(configuration.getAppenders().isEmpty()){
                    return createStringResource("LoggingConfigPanel.appenders.Inherit");
                } else{
                    return new LoadableModel<String>() {

                        @Override
                        protected String load() {
                            StringBuilder builder = new StringBuilder();
                            for (String appender : configuration.getAppenders()) {
                                if (configuration.getAppenders().indexOf(appender) != 0) {
                                    builder.append(", ");
                                }
                                builder.append(appender);
                            }

                            return builder.toString();
                        }
                    };
                }
            }

            @Override
            protected InputPanel createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
                IModel<Map<String, String>> options = new Model(null);
                ListMultipleChoicePanel panel = new ListMultipleChoicePanel<String>(componentId,
                        new PropertyModel<List<String>>(model, getPropertyExpression()),
                        createNewLoggerAppendersListModel(), new IChoiceRenderer<String>() {

                    @Override
                    public String getDisplayValue(String o) {
                        return o;
                    }

                    @Override
                    public String getIdValue(String o, int index) {
                        return Integer.toString(index);
                    }
                }, options);

                FormComponent<AppenderConfigurationType> input = panel.getBaseFormComponent();
                input.add(new EmptyOnChangeAjaxFormUpdatingBehavior());

                return panel;
            }

            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }
        });

        return columns;
    }

    private String getComponenLoggerDisplayValue(LoggingComponentType item){
        //LoggingConfigPanel.this.getString("LoggingConfigPanel.logger." + item);
        return createStringResource("LoggingComponentType." + item).getString();
    }

    private IModel<List<String>> createNewLoggerAppendersListModel(){
        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                List<String> list = new ArrayList<String>();

                LoggingDto dto = getModel().getObject();

                //list.add(PageSystemConfiguration.ROOT_APPENDER_INHERITANCE_CHOICE);
                for(AppenderConfiguration appender: dto.getAppenders()){

                    if(!appender.getName().equals(dto.getRootAppender())){
                        list.add(appender.getName());
                    }
                }

                return list;
            }
        };
    }

    private IModel<List<String>> createAppendersListModel() {
        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                List<String> list = new ArrayList<String>();

                LoggingDto dto = getModel().getObject();
                for (AppenderConfiguration appender : dto.getAppenders()) {
                    list.add(appender.getName());
                }

                return list;
            }
        };
    }

    private void deleteLoggerPerformed(AjaxRequestTarget target) {
        Iterator<LoggerConfiguration> iterator = getModel().getObject().getLoggers().iterator();
        while (iterator.hasNext()) {
            LoggerConfiguration item = iterator.next();
            if (item.isSelected()) {
                iterator.remove();
            }
        }
        target.add(getLoggersTable());
    }

    private void initAppenders(){
        ISortableDataProvider<AppenderConfiguration, String> provider = new ListDataProvider<AppenderConfiguration>(
                this, new PropertyModel<List<AppenderConfiguration>>(getModel(), LoggingDto.F_APPENDERS));

        TablePanel table = new TablePanel<AppenderConfiguration>(ID_TABLE_APPENDERS, provider, initAppenderColumns());
        table.setOutputMarkupId(true);
        table.setShowPaging(false);
        add(table);

        AjaxButton addConsoleAppender = new AjaxButton(ID_BUTTON_ADD_CONSOLE_APPENDER,
                createStringResource("LoggingConfigPanel.button.addConsoleAppender")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addConsoleAppenderPerformed(target);
            }
        };
        add(addConsoleAppender);

        AjaxButton addFileAppender = new AjaxButton(ID_BUTTON_ADD_FILE_APPENDER,
                createStringResource("LoggingConfigPanel.button.addFileAppender")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addFileAppenderPerformed(target);
            }
        };
        add(addFileAppender);

        AjaxButton deleteAppender = new AjaxButton(ID_BUTTON_DELETE_APPENDER,
                createStringResource("LoggingConfigPanel.button.deleteAppender")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAppenderPerformed(target);
            }
        };
        add(deleteAppender);
    }

    private List<IColumn<AppenderConfiguration, String>> initAppenderColumns(){
        List<IColumn<AppenderConfiguration, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<AppenderConfiguration>();
        columns.add(column);

        //name columns (editable)
        column = new EditableLinkColumn<AppenderConfiguration>(createStringResource("LoggingConfigPanel.appenders.name"), "name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AppenderConfiguration> rowModel){
                appenderEditPerformed(target, rowModel);
            }

            @Override
            protected Component createInputPanel(String componentId, IModel<AppenderConfiguration> model){
                TextPanel<String> panel = new TextPanel<String>(componentId, new PropertyModel(model, getPropertyExpression()));
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                panel.add(new InputStringValidator());
                return panel;
            }

        };
        columns.add(column);

        //pattern column (editable)
        column = new EditablePropertyColumn(createStringResource("LoggingConfigPanel.appenders.pattern"),
                "pattern") {

            @Override
            protected InputPanel createInputPanel(String componentId, IModel model) {
                InputPanel panel = super.createInputPanel(componentId, model);
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                panel.add(new InputStringValidator());
                return panel;
            }
        };
        columns.add(column);

        //file path column (editable)
        column = new FileAppenderColumn(createStringResource("LoggingConfigPanel.appenders.filePath"), "filePath");
        columns.add(column);

        //file pattern column (editable)                                                                               jj
        column = new FileAppenderColumn(createStringResource("LoggingConfigPanel.appenders.filePattern"),
                "filePattern");
        columns.add(column);

        //max history column (editable)
        column = new FileAppenderColumn(createStringResource("LoggingConfigPanel.appenders.maxHistory"),
                "maxHistory") {

            @Override
            protected InputPanel createInputPanel(String componentId, IModel model) {
                TextPanel panel = new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
                FormComponent component = panel.getBaseFormComponent();
                component.add(new AttributeModifier("size", 5));
                component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                return panel;
            }
        };
        columns.add(column);

        //max file size column (editable)
        column = new FileAppenderColumn(createStringResource("LoggingConfigPanel.appenders.maxFileSize"),
                "maxFileSize") {

            @Override
            protected InputPanel createInputPanel(String componentId, IModel model) {
                TextPanel<String> panel = new TextPanel(componentId, new PropertyModel(model,
                        getPropertyExpression()));
                FormComponent component = panel.getBaseFormComponent();
                component.add(new AttributeModifier("size", 5));
                component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                component.add(new InputStringValidator());
                return panel;
            }
        };
        columns.add(column);

        CheckBoxColumn check = new EditableCheckboxColumn(createStringResource("LoggingConfigPanel.appenders.appending"),
                "appending") {

            @Override
            protected InputPanel createInputPanel(String componentId, IModel model) {
                InputPanel panel = super.createInputPanel(componentId, model);
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                panel.add(new InputStringValidator());
                return panel;
            }
        };
        check.setEnabled(false);
        columns.add(check);


        return columns;
    }

    private IModel<LoggingComponentType> createFilterModel(final IModel<FilterConfiguration> model) {
        return new Model<LoggingComponentType>() {

            @Override
            public LoggingComponentType getObject() {
                String name = model.getObject().getName();
                if (StringUtils.isEmpty(name)) {
                    return null;
                }
                return LoggingComponentType.fromValue(name);
            }

            @Override
            public void setObject(LoggingComponentType object) {
                model.getObject().setName(object.name());
            }
        };
    }

    private void loggerEditPerformed(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
        LoggerConfiguration config = rowModel.getObject();
        config.setEditing(true);
        target.add(getLoggersTable());
    }

    private static class EmptyOnChangeAjaxFormUpdatingBehavior extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnChangeAjaxFormUpdatingBehavior(){
            super("onChange");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target){

        }
    }

    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("onBlur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }

    private static class FileAppenderColumn<T extends Editable> extends EditablePropertyColumn<T> {

        private FileAppenderColumn(IModel<String> displayModel, String propertyExpression) {
            super(displayModel, propertyExpression);
        }

        @Override
        protected boolean isEditing(IModel<T> rowModel) {
            return super.isEditing(rowModel) && (rowModel.getObject() instanceof FileAppenderConfig);
        }

        @Override
        protected InputPanel createInputPanel(String componentId, IModel iModel) {
            InputPanel panel = super.createInputPanel(componentId, iModel);
            panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            panel.add(new InputStringValidator());
            return panel;
        }
    }

    private void rootAppenderChangePerformed(AjaxRequestTarget target){
        target.add(getLoggersTable());
    }

    private void addConsoleAppenderPerformed(AjaxRequestTarget target){
        LoggingDto dto = getModel().getObject();
        AppenderConfiguration appender = new AppenderConfiguration(new AppenderConfigurationType());
        appender.setEditing(true);
        dto.getAppenders().add(appender);

        target.add(getAppendersTable());
    }

    private void addFileAppenderPerformed(AjaxRequestTarget target){
        LoggingDto dto = getModel().getObject();
        FileAppenderConfig appender = new FileAppenderConfig(new FileAppenderConfigurationType());
        appender.setEditing(true);
        dto.getAppenders().add(appender);

        target.add(getAppendersTable());
    }

    private void deleteAppenderPerformed(AjaxRequestTarget target){
        Iterator<AppenderConfiguration> iterator = getModel().getObject().getAppenders().iterator();
        while (iterator.hasNext()) {
            AppenderConfiguration item = iterator.next();
            if (item.isSelected()) {
                iterator.remove();
            }
        }
        target.add(getAppendersTable());
    }

    private void appenderEditPerformed(AjaxRequestTarget target, IModel<AppenderConfiguration> model){
        AppenderConfiguration config = model.getObject();
        config.setEditing(true);
        target.add(getAppendersTable());
    }
}
