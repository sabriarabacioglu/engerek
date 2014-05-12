package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InternalsConfigDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import javax.xml.datatype.XMLGregorianCalendar;

@PageDescriptor(url = "/admin/config/internals", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#configInternals",
                label = "PageInternals.auth.configInternals.label", description = "PageInternals.auth.configInternals.description")})
public class PageInternals extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageInternals.class);

    private static final String ID_DEBUG_UTIL_FORM = "debugUtilForm";
    private static final String ID_SAVE_DEBUG_UTIL = "saveDebugUtil";
    private static final String ID_INTERNALS_CONFIGFORM = "internalsConfigForm";
    private static final String ID_UPDATE_INTERNALS_CONFIG = "updateInternalsConfig";
    private static final String ID_CONSISTENCY_CHECKS = "consistencyChecks";
    private static final String ID_ENCRYPTION_CHECKS = "encryptionChecks";
    private static final String ID_READ_ENCRYPTION_CHECKS = "readEncryptionChecks";
    private static final String ID_DETAILED_DEBUG_DUMP = "detailedDebugDump";

    private static final String LABEL_SIZE = "col-md-4";
    private static final String INPUT_SIZE = "col-md-8";

    @SpringBean(name = "clock")
    private Clock clock;

    private IModel<XMLGregorianCalendar> model;
    private IModel<InternalsConfigDto> internalsModel;

    public PageInternals() {
        initLayout();
    }

    private void initLayout() {
        model = new LoadableModel<XMLGregorianCalendar>() {

            @Override
            protected XMLGregorianCalendar load() {
                return clock.currentTimeXMLGregorianCalendar();
            }
        };
        internalsModel = new Model<>(new InternalsConfigDto());

        Form mainForm = new Form("mainForm");
        add(mainForm);

        DatePanel offset = new DatePanel("offset", model);
        mainForm.add(offset);

        AjaxSubmitButton saveButton = new AjaxSubmitButton("save", createStringResource("PageInternals.button.changeTime")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);

        initDebugUtilForm();
        initInternalsConfigForm();
    }

    private void initDebugUtilForm() {
        Form form = new Form(ID_DEBUG_UTIL_FORM);
        add(form);

        CheckFormGroup detailed = new CheckFormGroup(ID_DETAILED_DEBUG_DUMP,
                new PropertyModel<Boolean>(internalsModel, InternalsConfigDto.F_DETAILED_DEBUG_DUMP),
                createStringResource("PageInternals.detailedDebugDump"), LABEL_SIZE, INPUT_SIZE);
        form.add(detailed);

        AjaxSubmitButton update = new AjaxSubmitButton(ID_SAVE_DEBUG_UTIL,
                createStringResource("PageBase.button.update")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                internalsModel.getObject().saveDebugUtil();

                LOGGER.trace("Updated debug util, detailedDebugDump={}", DebugUtil.isDetailedDebugDump());
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(update);
    }

    private void initInternalsConfigForm() {
        Form form = new Form(ID_INTERNALS_CONFIGFORM);
        add(form);

        CheckFormGroup consistency = new CheckFormGroup(ID_CONSISTENCY_CHECKS,
                new PropertyModel<Boolean>(internalsModel, InternalsConfigDto.F_CONSISTENCY_CHECKS),
                createStringResource("PageInternals.checkConsistency"), LABEL_SIZE, INPUT_SIZE);
        form.add(consistency);
        CheckFormGroup encryption = new CheckFormGroup(ID_ENCRYPTION_CHECKS,
                new PropertyModel<Boolean>(internalsModel, InternalsConfigDto.F_ENCRYPTION_CHECKS),
                createStringResource("PageInternals.checkEncryption"), LABEL_SIZE, INPUT_SIZE);
        form.add(encryption);
        CheckFormGroup encryptionRead = new CheckFormGroup(ID_READ_ENCRYPTION_CHECKS,
                new PropertyModel<Boolean>(internalsModel, InternalsConfigDto.F_READ_ENCRYPTION_CHECKS),
                createStringResource("PageInternals.checkReadEncrypion"), LABEL_SIZE, INPUT_SIZE);
        form.add(encryptionRead);

        AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE_INTERNALS_CONFIG,
                createStringResource("PageBase.button.update")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                internalsModel.getObject().saveInternalsConfig();

                LOGGER.trace("Updated internals config, consistencyChecks={},encryptionChecks={},readEncryptionChecks={}",
                        new Object[]{InternalsConfig.consistencyChecks, InternalsConfig.encryptionChecks,
                                InternalsConfig.readEncryptionChecks});
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        form.add(update);
    }

    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(PageInternals.class.getName() + ".changeTime");
        XMLGregorianCalendar offset = model.getObject();
        if (offset != null) {
            clock.override(offset);
        }

        result.recordSuccess();
        showResult(result);
        target.add(getFeedbackPanel());

    }
}
