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

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author lazyman
 */
public final class WebMiscUtil {

    private static final Trace LOGGER = TraceManager.getTrace(WebMiscUtil.class);
    private static DatatypeFactory df = null;

    static {
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining Datatype Factory instance", dce);
        }
    }

    private WebMiscUtil() {
    }

    public static boolean isAuthorized(String... action) {
        if (action == null) {
            return true;
        }
        List<String> actions = Arrays.asList(action);
        Roles roles = new Roles(AuthorizationConstants.AUTZ_ALL_URL);
        roles.addAll(actions);
        if (((AuthenticatedWebApplication) AuthenticatedWebApplication.get()).hasAnyRole(roles)) {
            return true;
        }
        return false;
    }

    public static Integer safeLongToInteger(Long l) {
        if (l == null) {
            return null;
        }

        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Couldn't transform long '" + l + "' to int, too big or too small.");
        }

        return (int) l.longValue();
    }

    public static <T extends Enum> IModel<String> createLocalizedModelForEnum(T value, Component comp) {
        String key = value != null ? value.getClass().getSimpleName() + "." + value.name() : "";
        return new StringResourceModel(key, comp, null);
    }

    public static <T extends Enum> IModel<List<T>> createReadonlyModelFromEnum(final Class<T> type) {
        return new AbstractReadOnlyModel<List<T>>() {

            @Override
            public List<T> getObject() {
                List<T> list = new ArrayList<T>();
                Collections.addAll(list, type.getEnumConstants());

                return list;
            }
        };
    }

    public static DropDownChoicePanel createActivationStatusPanel(String id, final IModel<ActivationStatusType> model,
                                                                  final Component component) {
        return new DropDownChoicePanel(id, model,
                WebMiscUtil.createReadonlyModelFromEnum(ActivationStatusType.class),
                new IChoiceRenderer<ActivationStatusType>() {

                    @Override
                    public Object getDisplayValue(ActivationStatusType object) {
                        return WebMiscUtil.createLocalizedModelForEnum(object, component).getObject();
                    }

                    @Override
                    public String getIdValue(ActivationStatusType object, int index) {
                        return Integer.toString(index);
                    }
                }, true);

    }

    public static String getName(ObjectType object) {
        if (object == null) {
            return null;
        }

        return getName(object.asPrismObject());
    }

    public static String getName(PrismObject object) {
        if (object == null) {
            return null;
        }
        PolyString name = getValue(object, ObjectType.F_NAME, PolyString.class);

        return name != null ? name.getOrig() : null;
    }

    public static String getIdentification(ObjectType object) {
        if (object == null) {
            return null;
        }
        return getName(object.asPrismObject()) + " (" + object.getOid() + ")";
    }

    public static PolyStringType createPolyFromOrigString(String str) {
        if (str == null) {
            return null;
        }

        PolyStringType poly = new PolyStringType();
        poly.setOrig(str);

        return poly;
    }

    public static String getOrigStringFromPoly(PolyString str) {
        return str != null ? str.getOrig() : null;
    }

    public static String getOrigStringFromPoly(PolyStringType str) {
        return str != null ? str.getOrig() : null;
    }

    public static <T> T getValue(PrismContainerValue object, QName propertyName, Class<T> type) {
        if (object == null) {
            return null;
        }

        PrismProperty property = object.findProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return null;
        }

        return (T) property.getRealValue(type);
    }

    public static <T> T getContainerValue(PrismContainerValue object, QName containerName,
                                          Class<T> type) {
        if (object == null) {
            return null;
        }

        PrismContainer container = object.findContainer(containerName);
        if (container == null || container.isEmpty()) {
            return null;
        }

        PrismContainerValue containerValue = container.getValue();

        if (containerValue == null || containerValue.isEmpty()) {
            return null;
        }

        return (T) containerValue.getValue();
    }

    public static <T> T getValue(PrismContainer object, QName propertyName, Class<T> type) {
        if (object == null) {
            return null;
        }

        return getValue(object.getValue(), propertyName, type);
    }

    public static Locale getLocaleFromString(String localeString) {
        if (localeString == null) {
            return null;
        }
        localeString = localeString.trim();
        if (localeString.toLowerCase().equals("default")) {
            return Locale.getDefault();
        }

        // Extract language
        int languageIndex = localeString.indexOf('_');
        String language = null;
        if (languageIndex == -1) {
            // No further "_" so is "{language}" only
            return new Locale(localeString, "");
        } else {
            language = localeString.substring(0, languageIndex);
        }

        // Extract country
        int countryIndex = localeString.indexOf('_', languageIndex + 1);
        String country = null;
        if (countryIndex == -1) {
            // No further "_" so is "{language}_{country}"
            country = localeString.substring(languageIndex + 1);
            return new Locale(language, country);
        } else {
            // Assume all remaining is the variant so is "{language}_{country}_{variant}"
            country = localeString.substring(languageIndex + 1, countryIndex);
            String variant = localeString.substring(countryIndex + 1);
            return new Locale(language, country, variant);
        }
    }

    public static void encryptCredentials(ObjectDelta delta, boolean encrypt, MidPointApplication app) {
        if (delta == null || delta.isEmpty()) {
            return;
        }

        PropertyDelta propertyDelta = delta.findPropertyDelta(new ItemPath(
                SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD,
                PasswordType.F_VALUE));
        if (propertyDelta == null) {
            return;
        }

        Collection<PrismPropertyValue<ProtectedStringType>> values = propertyDelta
                .getValues(ProtectedStringType.class);
        for (PrismPropertyValue<ProtectedStringType> value : values) {
            ProtectedStringType string = value.getValue();
            encryptProtectedString(string, encrypt, app);
        }
    }

    public static void encryptCredentials(PrismObject object, boolean encrypt, MidPointApplication app) {
        PrismContainer password = object.findContainer(new ItemPath(
                SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD));
        if (password == null) {
            return;
        }
        PrismProperty protectedStringProperty = password.findProperty(PasswordType.F_VALUE);
        if (protectedStringProperty == null
                || protectedStringProperty.getRealValue(ProtectedStringType.class) == null) {
            return;
        }

        ProtectedStringType string = (ProtectedStringType) protectedStringProperty
                .getRealValue(ProtectedStringType.class);

        encryptProtectedString(string, encrypt, app);
    }

    public static void encryptProtectedString(ProtectedStringType string, boolean encrypt, MidPointApplication app) {
        if (string == null) {
            return;
        }
        Protector protector = app.getProtector();
        try {
            if (encrypt) {
                if (StringUtils.isEmpty(string.getClearValue())) {
                    return;
                }
                protector.encrypt(string);
            } else {
                if (string.getEncryptedDataType() == null) {
                    return;
                }
                protector.decrypt(string);
            }
        } catch (EncryptionException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't encrypt protected string", ex);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't encrypt/decrypt protected string", e);
        }
    }

    public static <T extends Selectable> List<T> getSelectedData(TablePanel panel) {
        DataTable table = panel.getDataTable();
        BaseSortableDataProvider<T> provider = (BaseSortableDataProvider<T>) table.getDataProvider();

        List<T> selected = new ArrayList<T>();
        for (T bean : provider.getAvailableData()) {
            if (bean.isSelected()) {
                selected.add(bean);
            }
        }

        return selected;
    }

    public static Collection<ObjectDelta<? extends ObjectType>> createDeltaCollection(ObjectDelta<? extends ObjectType>... deltas) {
        Collection<ObjectDelta<? extends ObjectType>> collection = new ArrayList<ObjectDelta<? extends ObjectType>>();
        for (ObjectDelta delta : deltas) {
            collection.add(delta);
        }

        return collection;
    }

    public static boolean showResultInPage(OperationResult result) {
        if (result == null) {
            return false;
        }

        return !result.isSuccess() && !result.isHandledError() && !result.isInProgress();
    }

    public static String formatDate(Date date) {
        return formatDate(null, date);
    }

    public static String formatDate(String format, Date date) {
        if (StringUtils.isEmpty(format)) {
            format = "EEEE, d. MMM yyyy HH:mm:ss";
        }
        Locale locale = Session.get().getLocale();
        if (locale == null) {
            locale = Locale.US;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, locale);
        return dateFormat.format(date);
    }

    public static boolean isActivationEnabled(PrismObject object) {
        Validate.notNull(object);

        PrismContainer activation = object.findContainer(UserType.F_ACTIVATION); //this is equal to account activation...
        if (activation == null) {
            return false;
        }

        ActivationStatusType status = (ActivationStatusType) activation.getPropertyRealValue(ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.class);
        if (status == null) {
            return false;
        }

        //todo imrove with activation dates...
        return ActivationStatusType.ENABLED.equals(status);
    }

    public static boolean isSuccessOrHandledError(OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");

        return result.isSuccess() || result.isHandledError();
    }

    public static String createUserIcon(PrismObject<UserType> object) {
        UserType user = object.asObjectable();
        CredentialsType credentials = user.getCredentials();

        //if user has superuser role assigned, it's superuser
        for (AssignmentType assignment : user.getAssignment()) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef == null) {
                continue;
            }
            if (StringUtils.equals(targetRef.getOid(), SystemObjectsType.ROLE_SUPERUSER.value())) {
                return "silk-user_red";
            }
        }

        return "silk-user";
    }

    public static double getSystemLoad() {
        com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        long prevUpTime = runtimeMXBean.getUptime();
        long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();

        try {
            Thread.sleep(30);
        } catch (Exception ignored) {
        }

        operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;

        double cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));

        return cpuUsage;
    }

    public static double getMaxRam() {
        int MB = 1024 * 1024;

        MemoryMXBean mBean = ManagementFactory.getMemoryMXBean();
        long maxHeap = mBean.getHeapMemoryUsage().getMax();
        long maxNonHeap = mBean.getNonHeapMemoryUsage().getMax();

        return (maxHeap + maxNonHeap) / MB;
    }

    public static double getRamUsage() {
        int MB = 1024 * 1024;

        MemoryMXBean mBean = ManagementFactory.getMemoryMXBean();
        long usedHead = mBean.getHeapMemoryUsage().getUsed();
        long usedNonHeap = mBean.getNonHeapMemoryUsage().getUsed();

        return (usedHead + usedNonHeap) / MB;
    }

    /**
     * Checks table if has any selected rows ({@link Selectable} interface dtos), adds "single"
     * parameter to selected items if it's not null. If table has no selected rows warn message
     * is added to feedback panel, and feedback is refreshed through {@link AjaxRequestTarget}
     *
     * @param target
     * @param single             this parameter is used for row actions when action must be done only on chosen row.
     * @param table
     * @param page
     * @param nothingWarnMessage
     * @param <T>
     * @return
     */
    public static <T extends Selectable> List<T> isAnythingSelected(AjaxRequestTarget target, T single, TablePanel table,
                                                                    PageBase page, String nothingWarnMessage) {
        List<T> selected;
        if (single != null) {
            selected = new ArrayList<T>();
            selected.add(single);
        } else {
            selected = WebMiscUtil.getSelectedData(table);
            if (selected.isEmpty()) {
                page.warn(page.getString(nothingWarnMessage));
                target.add(page.getFeedbackPanel());
            }
        }

        return selected;
    }

    public static void refreshFeedbacks(MarkupContainer component, final AjaxRequestTarget target) {
        component.visitChildren(IFeedback.class, new IVisitor<Component, Void>() {

            @Override
            public void component(final Component component, final IVisit<Void> visit) {
                target.add(component);
            }
        });
    }
}
