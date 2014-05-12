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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.configuration.component.ImportOptionsPanel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author lazyman
 * @author mserbak
 */
@PageDescriptor(url = "/admin/config/import", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#configImport",
                label = "PageImportObject.auth.configImport.label", description = "PageImportObject.auth.configImport.description")})
public class PageImportObject extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageImportObject.class);
    private static final String DOT_CLASS = PageImportObject.class.getName() + ".";
    private static final String OPERATION_IMPORT_FILE = DOT_CLASS + "importFile";
    private static final String OPERATION_IMPORT_XML = DOT_CLASS + "importXml";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_IMPORT_OPTIONS = "importOptions";
    private static final String ID_IMPORT_RADIO_GROUP = "importRadioGroup";
    private static final String ID_FILE_RADIO = "fileRadio";
    private static final String ID_XML_RADIO = "xmlRadio";
    private static final String ID_IMPORT_FILE_BUTTON = "importFileButton";
    private static final String ID_IMPORT_XML_BUTTON = "importXmlButton";
    private static final String ID_INPUT = "input";
    private static final String ID_INPUT_ACE = "inputAce";
    private static final String ID_ACE_EDITOR = "aceEditor";
    private static final String ID_INPUT_FILE_LABEL = "inputFileLabel";
    private static final String ID_INPUT_FILE = "inputFile";
    private static final String ID_FILE_INPUT = "fileInput";

    private static final Integer INPUT_FILE = 1;
    private static final Integer INPUT_XML = 2;

    private LoadableModel<ImportOptionsType> model;
    private IModel<String> xmlEditorModel;

    public PageImportObject() {
        model = new LoadableModel<ImportOptionsType>(false) {

            @Override
            protected ImportOptionsType load() {
                return MiscSchemaUtil.getDefaultImportOptions();
            }
        };
        xmlEditorModel = new Model<String>(null);

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ImportOptionsPanel importOptions = new ImportOptionsPanel(ID_IMPORT_OPTIONS, model);
        mainForm.add(importOptions);

        final WebMarkupContainer input = new WebMarkupContainer(ID_INPUT);
        input.setOutputMarkupId(true);
        mainForm.add(input);

        final WebMarkupContainer buttonBar = new WebMarkupContainer(ID_BUTTON_BAR);
        buttonBar.setOutputMarkupId(true);
        mainForm.add(buttonBar);

        final IModel<Integer> groupModel = new Model<Integer>(INPUT_FILE);
        RadioGroup importRadioGroup = new RadioGroup(ID_IMPORT_RADIO_GROUP, groupModel);
        importRadioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(input);
                target.add(buttonBar);
            }
        });
        mainForm.add(importRadioGroup);

        Radio fileRadio = new Radio(ID_FILE_RADIO, new Model(INPUT_FILE), importRadioGroup);
        importRadioGroup.add(fileRadio);

        Radio xmlRadio = new Radio(ID_XML_RADIO, new Model(INPUT_XML), importRadioGroup);
        importRadioGroup.add(xmlRadio);

        WebMarkupContainer inputAce = new WebMarkupContainer(ID_INPUT_ACE);
        addVisibileForInputType(inputAce, INPUT_XML, groupModel);
        input.add(inputAce);

        AceEditor aceEditor = new AceEditor(ID_ACE_EDITOR, xmlEditorModel);
        aceEditor.setOutputMarkupId(true);
        inputAce.add(aceEditor);

        WebMarkupContainer inputFileLabel = new WebMarkupContainer(ID_INPUT_FILE_LABEL);
        addVisibileForInputType(inputFileLabel, INPUT_FILE, groupModel);
        input.add(inputFileLabel);

        WebMarkupContainer inputFile = new WebMarkupContainer(ID_INPUT_FILE);
        addVisibileForInputType(inputFile, INPUT_FILE, groupModel);
        input.add(inputFile);

        FileUploadField fileInput = new FileUploadField(ID_FILE_INPUT);
        inputFile.add(fileInput);

        initButtons(buttonBar, groupModel);
    }

    private void addVisibileForInputType(Component comp, final Integer type, final IModel<Integer> groupModel) {
        comp.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return type.equals(groupModel.getObject());
            }

        });
    }

    private void initButtons(WebMarkupContainer buttonBar, IModel<Integer> inputType) {
        AjaxSubmitButton saveFileButton = new AjaxSubmitButton(ID_IMPORT_FILE_BUTTON,
                createStringResource("PageImportObject.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                saveFilePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        addVisibileForInputType(saveFileButton, INPUT_FILE, inputType);
        buttonBar.add(saveFileButton);

        AjaxSubmitButton saveXmlButton = new AjaxSubmitButton(ID_IMPORT_XML_BUTTON,
                createStringResource("PageImportObject.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                saveXmlPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        addVisibileForInputType(saveXmlButton, INPUT_XML, inputType);
        buttonBar.add(saveXmlButton);
    }

    private void saveFilePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_IMPORT_FILE);

        FileUploadField file = (FileUploadField) get(createComponentPath(ID_MAIN_FORM, ID_INPUT, ID_INPUT_FILE, ID_FILE_INPUT));
        final FileUpload uploadedFile = file.getFileUpload();
        if (uploadedFile == null) {
            error(getString("pageImportObject.message.nullFile"));
            target.add(getFeedbackPanel());

            return;
        }

        InputStream stream = null;
        File newFile = null;
        try {
            // Create new file
            MidPointApplication application = getMidpointApplication();
            WebApplicationConfiguration config = application.getWebApplicationConfiguration();
            File folder = new File(config.getImportFolder());
            if (!folder.exists() || !folder.isDirectory()) {
                folder.mkdir();
            }

            newFile = new File(folder, uploadedFile.getClientFileName());
            // Check new file, delete if it already exists
            if (newFile.exists()) {
                newFile.delete();
            }
            // Save file
            Task task = createSimpleTask(OPERATION_IMPORT_FILE);
            newFile.createNewFile();
            uploadedFile.writeTo(newFile);

            InputStreamReader reader = new InputStreamReader(new FileInputStream(newFile), "utf-8");
            stream = new ReaderInputStream(reader, reader.getEncoding());
            getModelService().importObjectsFromStream(stream, model.getObject(), task, result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't import file.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't import file", ex);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
            if (newFile != null) {
                FileUtils.deleteQuietly(newFile);
            }
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void saveXmlPerformed(AjaxRequestTarget target) {
        String xml = xmlEditorModel.getObject();
        if (StringUtils.isEmpty(xml)) {
            error(getString("pageImportObject.message.emptyXml"));
            target.add(getFeedbackPanel());

            return;
        }

        OperationResult result = new OperationResult(OPERATION_IMPORT_XML);
        InputStream stream = null;
        try {
            Task task = createSimpleTask(OPERATION_IMPORT_XML);

            stream = IOUtils.toInputStream(xml, "utf-8");
            getModelService().importObjectsFromStream(stream, model.getObject(), task, result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't import object.", ex);
            LoggingUtils.logException(LOGGER, "Error occured during xml import", ex);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }

        if (result.isSuccess()) {
            xmlEditorModel.setObject(null);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
