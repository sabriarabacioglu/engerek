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

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ResourceItemDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import javax.xml.namespace.QName;

import java.io.*;
import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/sync/accounts", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#configSyncAccounts",
                label = "PageAccounts.auth.configSyncAccounts.label", description = "PageAccounts.auth.configSyncAccounts.description")})
public class PageAccounts extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageAccounts.class);

    private static final String DOT_CLASS = PageAccounts.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_EXPORT = DOT_CLASS + "export";
    private static final String OPERATION_EXPORT_ACCOUNT = DOT_CLASS + "exportAccount";
    private static final String OPERATION_GET_TOTALS = DOT_CLASS + "getTotals";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_RESOURCES = "resources";
    private static final String ID_LIST_SYNC_DETAILS = "listSyncDetails";
    private static final String ID_EXPORT = "export";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_CLEAR_EXPORT = "clearExport";
    private static final String ID_FILES_CONTAINER = "filesContainer";
    private static final String ID_FILES = "files";
    private static final String ID_FILE = "file";
    private static final String ID_FILE_NAME = "fileName";
    private static final String ID_TOTALS = "totals";
    private static final String ID_TOTAL = "total";
    private static final String ID_DELETED = "deleted";
    private static final String ID_UNMATCHED = "unmatched";
    private static final String ID_DISPUTED = "disputed";
    private static final String ID_LINKED = "linked";
    private static final String ID_UNLINKED = "unlinked";
    private static final String ID_ACCOUNTS_CONTAINER = "accountsContainer";
    private static final String ID_NOTHING = "nothing";

    private IModel<List<ResourceItemDto>> resourcesModel;
    private LoadableModel<List<String>> filesModel;

    private IModel<ResourceItemDto> resourceModel = new Model<ResourceItemDto>();
    private LoadableModel<Integer> totalModel;
    private LoadableModel<Integer> deletedModel;
    private LoadableModel<Integer> unmatchedModel;
    private LoadableModel<Integer> disputedModel;
    private LoadableModel<Integer> linkedModel;
    private LoadableModel<Integer> unlinkedModel;
    private LoadableModel<Integer> nothingModel;

    private File downloadFile;

    public PageAccounts() {
        resourcesModel = new LoadableModel<List<ResourceItemDto>>() {

            @Override
            protected List<ResourceItemDto> load() {
                return loadResources();
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_MAIN_FORM);
        add(form);

        DropDownChoice<ResourceItemDto> resources = new DropDownChoice<ResourceItemDto>(
                ID_RESOURCES, resourceModel, resourcesModel,
                new IChoiceRenderer<ResourceItemDto>() {

                    @Override
                    public Object getDisplayValue(ResourceItemDto object) {
                        if (object == null) {
                            return "";
                        }

                        return object.getName();
                    }

                    @Override
                    public String getIdValue(ResourceItemDto object, int index) {
                        return Integer.toString(index);
                    }
                });
        form.add(resources);

        initLinks(form);
        initTotals(form);

        final AjaxDownloadBehaviorFromFile ajaxDownloadBehavior = new AjaxDownloadBehaviorFromFile(true) {

            @Override
            protected File initFile() {
                return downloadFile;
            }
        };
        ajaxDownloadBehavior.setRemoveFile(false);
        form.add(ajaxDownloadBehavior);

        WebMarkupContainer filesContainer = new WebMarkupContainer(ID_FILES_CONTAINER);
        filesContainer.setOutputMarkupId(true);
        form.add(filesContainer);

        filesModel = createFilesModel();
        ListView<String> files = new ListView<String>(ID_FILES, filesModel) {

            @Override
            protected void populateItem(final ListItem<String> item) {
                AjaxLink file = new AjaxLink(ID_FILE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        downloadPerformed(target, item.getModelObject(), ajaxDownloadBehavior);
                    }
                };
                file.add(new Label(ID_FILE_NAME, item.getModelObject()));
                item.add(file);
            }
        };
        files.setRenderBodyOnly(true);
        filesContainer.add(files);

        WebMarkupContainer accountsContainer = new WebMarkupContainer(ID_ACCOUNTS_CONTAINER);
        accountsContainer.setOutputMarkupId(true);
        form.add(accountsContainer);

        ObjectDataProvider provider = new ObjectDataProvider(this, ShadowType.class);
        provider.setOptions(SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS, provider, createAccountsColumns());
        accounts.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return resourceModel.getObject() != null;
            }
        });
        accounts.setItemsPerPage(50);
        accountsContainer.add(accounts);
    }

    private void initTotals(Form form) {
        WebMarkupContainer totals = new WebMarkupContainer(ID_TOTALS);
        totals.setOutputMarkupId(true);

        totalModel = createTotalModel();
        deletedModel = createTotalsModel(SynchronizationSituationType.DELETED);
        unmatchedModel = createTotalsModel(SynchronizationSituationType.UNMATCHED);
        disputedModel = createTotalsModel(SynchronizationSituationType.DISPUTED);
        linkedModel = createTotalsModel(SynchronizationSituationType.LINKED);
        unlinkedModel = createTotalsModel(SynchronizationSituationType.UNLINKED);
        nothingModel = createTotalsModel(null);

        totals.add(new Label(ID_TOTAL, totalModel));
        totals.add(new Label(ID_DELETED, deletedModel));
        totals.add(new Label(ID_UNMATCHED, unmatchedModel));
        totals.add(new Label(ID_DISPUTED, disputedModel));
        totals.add(new Label(ID_LINKED, linkedModel));
        totals.add(new Label(ID_UNLINKED, unlinkedModel));
        totals.add(new Label(ID_NOTHING, nothingModel));

        form.add(totals);
    }

    private LoadableModel<Integer> createTotalModel() {
        return new LoadableModel<Integer>(false) {

            @Override
            protected Integer load() {
                int total = 0;

                total += deletedModel.getObject();
                total += unmatchedModel.getObject();
                total += disputedModel.getObject();
                total += linkedModel.getObject();
                total += unlinkedModel.getObject();
                total += nothingModel.getObject();

                return total;
            }
        };
    }

    private void refreshSyncTotalsModels() {
        totalModel.reset();
        deletedModel.reset();
        unmatchedModel.reset();
        disputedModel.reset();
        linkedModel.reset();
        unlinkedModel.reset();
        nothingModel.reset();
    }

    private LoadableModel<Integer> createTotalsModel(final SynchronizationSituationType situation) {
        return new LoadableModel<Integer>(false) {

            @Override
            protected Integer load() {
                ObjectFilter resourceFilter = createResourceQueryFilter();
                if (resourceFilter == null) {
                    return 0;
                }

                Collection<SelectorOptions<GetOperationOptions>> options =
                        SelectorOptions.createCollection(GetOperationOptions.createRaw());
                Task task = createSimpleTask(OPERATION_GET_TOTALS);
                OperationResult result = new OperationResult(OPERATION_GET_TOTALS);
                try {
                    EqualFilter situationFilter = EqualFilter.createEqual(ShadowType.F_SYNCHRONIZATION_SITUATION, ShadowType.class,
                            getPrismContext(), null, situation);

                    AndFilter andFilter = AndFilter.createAnd(resourceFilter, situationFilter);
                    ObjectQuery query = ObjectQuery.createObjectQuery(andFilter);

                    return getModelService().countObjects(ShadowType.class, query, options, task, result);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't count shadows", ex);
                }

                return 0;
            }
        };
    }

    private LoadableModel<List<String>> createFilesModel() {
        return new LoadableModel<List<String>>(false) {

            @Override
            protected List<String> load() {
                String[] filesArray;
                try {
                    MidpointConfiguration config = getMidpointConfiguration();
                    File exportFolder = new File(config.getMidpointHome() + "/export");
                    filesArray = exportFolder.list(new FilenameFilter() {

                        @Override
                        public boolean accept(java.io.File dir, String name) {
                            if (name.endsWith("xml")) {
                                return true;
                            }

                            return false;
                        }
                    });
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't list files", ex);
                    getSession().error("Couldn't list files, reason: " + ex.getMessage());

                    throw new RestartResponseException(PageDashboard.class);
                }

                if (filesArray == null) {
                    return new ArrayList<String>();
                }

                List<String> list = Arrays.asList(filesArray);
                Collections.sort(list);

                return list;
            }
        };
    }

    private void initLinks(Form form) {
        AjaxSubmitLink listSyncDetails = new AjaxSubmitLink(ID_LIST_SYNC_DETAILS) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                listSyncDetailsPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(listSyncDetails);

        AjaxSubmitLink export = new AjaxSubmitLink(ID_EXPORT) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                exportPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(export);

        AjaxLink clearExport = new AjaxLink(ID_CLEAR_EXPORT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearExportPerformed(target);
            }
        };
        form.add(clearExport);
    }

    private List<IColumn> createAccountsColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.oid"),
                SelectableBean.F_VALUE + ".oid"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.name"),
                ShadowType.F_NAME.getLocalPart(), SelectableBean.F_VALUE + ".name"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationSituation"),
                ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart(), SelectableBean.F_VALUE + ".synchronizationSituation"));
        columns.add(new PropertyColumn(createStringResource("PageAccounts.accounts.synchronizationTimestamp"),
                ShadowType.F_SYNCHRONIZATION_TIMESTAMP.getLocalPart(), SelectableBean.F_VALUE + ".synchronizationTimestamp"));

        return columns;
    }

    private ObjectFilter createResourceQueryFilter() {
        ResourceItemDto dto = resourceModel.getObject();
        if (dto == null) {
            return null;
        }
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        String oid = dto.getOid();
        try {
            RefFilter resourceRef = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class,
                    getPrismContext(), oid);

            PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class, oid, null,
                    createSimpleTask(OPERATION_LOAD_ACCOUNTS), result);
            RefinedResourceSchema schema = RefinedResourceSchema.getRefinedSchema(resource);
            QName qname = null;
            for (RefinedObjectClassDefinition def : schema.getRefinedDefinitions(ShadowKindType.ACCOUNT)) {
                if (def.isDefault()) {
                    qname = def.getObjectClassDefinition().getTypeName();
                    break;
                }
            }

            if (qname == null) {
                error("Couldn't find default object class for resource '" + WebMiscUtil.getName(resource) + "'.");
                return null;
            }

            EqualFilter objectClass = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, getPrismContext(),
                    null, qname);

            return AndFilter.createAnd(resourceRef, objectClass);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create query", ex);
            error("Couldn't create query, reason: " + ex.getMessage());
        } finally {
            result.recomputeStatus();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }

        return null;
    }

    private List<ResourceItemDto> loadResources() {
        List<ResourceItemDto> resources = new ArrayList<ResourceItemDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        try {
            List<PrismObject<ResourceType>> objects = getModelService().searchObjects(ResourceType.class, null, null,
                    createSimpleTask(OPERATION_LOAD_RESOURCES), result);

            if (objects != null) {
                for (PrismObject<ResourceType> object : objects) {
                    resources.add(new ResourceItemDto(object.getOid(), WebMiscUtil.getName(object)));
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load resources", ex);
            result.recordFatalError("Couldn't load resources, reason: " + ex.getMessage(), ex);
        } finally {
            if (result.isUnknown()) {
                result.recomputeStatus();
            }
        }

        Collections.sort(resources);

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            throw new RestartResponseException(PageDashboard.class);
        }

        return resources;
    }

    private void listSyncDetailsPerformed(AjaxRequestTarget target) {
        refreshSyncTotalsModels();

        TablePanel table = (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS_CONTAINER, ID_ACCOUNTS));
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        provider.setQuery(ObjectQuery.createObjectQuery(createResourceQueryFilter()));
        table.getDataTable().setCurrentPage(0);

        target.add(get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS_CONTAINER)),
                get(createComponentPath(ID_MAIN_FORM, ID_TOTALS)), getFeedbackPanel());
    }

    private void exportPerformed(AjaxRequestTarget target) {
        String fileName = "accounts-" + WebMiscUtil.formatDate("yyyy-MM-dd-HH-mm-ss", new Date()) + ".xml";

        OperationResult result = new OperationResult(OPERATION_EXPORT);
        Writer writer = null;
        try {
            Task task = createSimpleTask(OPERATION_EXPORT);

            writer = createWriter(fileName);
            writeHeader(writer);

            final Writer handlerWriter = writer;
            ResultHandler handler = new AbstractSummarizingResultHandler() {

                @Override
                protected boolean handleObject(PrismObject object, OperationResult parentResult) {
                    OperationResult result = parentResult.createMinorSubresult(OPERATION_EXPORT_ACCOUNT);
                    try {
                        String xml = getPrismContext().serializeObjectToString(object, PrismContext.LANG_XML);
                        handlerWriter.write(xml);

                        result.computeStatus();
                    } catch (Exception ex) {
                        LoggingUtils.logException(LOGGER, "Couldn't serialize account", ex);
                        result.recordFatalError("Couldn't serialize account.", ex);

                        return false;
                    }

                    return true;
                }
            };

            try {
                ObjectQuery query = ObjectQuery.createObjectQuery(createResourceQueryFilter());
                getModelService().searchObjectsIterative(ShadowType.class, query, handler, null, task, result);
            } finally {
                writeFooter(writer);
            }

            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't export accounts", ex);
            error(getString("PageAccounts.exportException", ex.getMessage()));
        } finally {
            IOUtils.closeQuietly(writer);
        }

        filesModel.reset();
        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_FILES_CONTAINER)));
    }

    private Writer createWriter(String fileName) throws IOException {
        //todo improve!!!!

        MidpointConfiguration config = getMidpointConfiguration();
        File file = new File(config.getMidpointHome() + "/export/" + fileName);
        file.createNewFile();

        return new OutputStreamWriter(new FileOutputStream(file), "utf-8");
    }

    private void writeHeader(Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<objects xmlns=\"");
        sb.append(SchemaConstantsGenerated.NS_COMMON);
        sb.append("\">\n");

        writer.write(sb.toString());
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("</objects>\n");
    }

    private void clearExportPerformed(AjaxRequestTarget target) {
        try {
            MidpointConfiguration config = getMidpointConfiguration();
            File exportFolder = new File(config.getMidpointHome() + "/export");
            java.io.File[] files = exportFolder.listFiles();
            if (files == null) {
                return;
            }

            for (java.io.File file : files) {
                file.delete();
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't delete export files", ex);
            error("Couldn't delete export files, reason: " + ex.getMessage());
        }

        filesModel.reset();
        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_FILES_CONTAINER)));
    }

    private void downloadPerformed(AjaxRequestTarget target, String fileName,
                                   AjaxDownloadBehaviorFromFile downloadBehavior) {
        MidpointConfiguration config = getMidpointConfiguration();
        downloadFile = new File(config.getMidpointHome() + "/export/" + fileName);

        downloadBehavior.initiate(target);
    }
}
