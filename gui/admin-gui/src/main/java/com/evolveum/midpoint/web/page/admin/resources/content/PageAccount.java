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

package com.evolveum.midpoint.web.page.admin.resources.content;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/resources/account", encoder = OnePageParameterEncoder.class, action = {
        PageAdminResources.AUTHORIZATION_RESOURCE_ALL,
        AuthorizationConstants.NS_AUTHORIZATION + "#resourcesAccount"})
public class PageAccount extends PageAdminResources {

    private static final Trace LOGGER = TraceManager.getTrace(PageAccount.class);
    private static final String DOT_CLASS = PageAccount.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE_ACCOUNT = DOT_CLASS + "saveAccount";

    private IModel<ObjectWrapper> accountModel;

    public PageAccount() {
        accountModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadAccount();
            }
        };
        initLayout();
    }

    private ObjectWrapper loadAccount() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNT);
        PrismObject<ShadowType> account = null;
        try {
            Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                    ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

            StringValue userOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
            account = getModelService().getObject(ShadowType.class, userOid.toString(), options, task, result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
        }

        if (!result.isSuccess()) {
            showResultInSession(result);
        }

        if (account == null) {
            getSession().error(getString("pageAccount.message.cantEditAccount"));
            throw new RestartResponseException(PageResources.class);
        }

        ObjectWrapper wrapper = new ObjectWrapper(null, null, account, ContainerStatus.MODIFYING);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }
        wrapper.setShowEmpty(false);
        return wrapper;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        PrismObjectPanel userForm = new PrismObjectPanel("account", accountModel, new PackageResourceReference(
                ImgResources.class, ImgResources.HDD_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageAccount.description");
            }
        };
        mainForm.add(userForm);

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton("save", createStringResource("pageAccount.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

        AjaxButton back = new AjaxButton("back", createStringResource("pageAccount.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);
    }

    @Override
    protected IModel<String> createPageSubTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                PrismObject<ShadowType> account = accountModel.getObject().getObject();

                ResourceType resource = account.asObjectable().getResource();
                String name = WebMiscUtil.getName(resource);

                return new StringResourceModel("page.subTitle", PageAccount.this, null, null, name).getString();
            }
        };
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving account changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_ACCOUNT);
        ObjectWrapper wrapper = accountModel.getObject();
        try {
            ObjectDelta<ShadowType> delta = wrapper.getObjectDelta();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Account delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
            }

            if (delta == null || delta.isEmpty()) {
                return;
            }
            WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());

            Task task = createSimpleTask(OPERATION_SAVE_ACCOUNT);
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
            deltas.add(delta);

            getModelService().executeChanges(deltas, null, task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save account.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't save account", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
            showResultInSession(result);

            returnToAccountList();
        }
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        returnToAccountList();
    }

    private void returnToAccountList() {
        PrismObject<ShadowType> account = accountModel.getObject().getObject();
        ResourceType resource = account.asObjectable().getResource();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, resource.getOid());
        setResponsePage(PageContentAccounts.class, parameters);
    }
}