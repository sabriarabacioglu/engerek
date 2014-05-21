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

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AsyncUpdatePanel;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

/**
 * @author lazyman
 */
public abstract class AsyncDashboardPanel<V, T> extends AsyncUpdatePanel<V, CallableResult<T>> {

    private static final String ID_DASHBOARD_PARENT = "dashboardParent";
    private static final String ID_DASHBOARD_TITLE = "dashboardTitle";
    private static final String ID_TITLE = "title";
    private static final String ID_PRELOADER_CONTAINER = "preloaderContainer";
    private static final String ID_PRELOADER = "preloader";
    private static final String ID_DASHBOARD_CONTENT = "dashboardContent";
    private static final String ID_CONTENT = "content";
    private static final String ID_ICON = "icon";

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, DashboardColor color) {
        this(id, title, icon, new Model(), color);
    }

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, IModel<V> callableParameterModel,
                               DashboardColor color) {
        this(id, title, icon, callableParameterModel, Duration.seconds(DEFAULT_TIMER_DURATION), color);
    }

    public AsyncDashboardPanel(String id, IModel<String> title, String icon, IModel<V> callableParameterModel,
                               Duration durationSecs, DashboardColor color) {
        super(id, callableParameterModel, durationSecs);

        WebMarkupContainer dashboardTitle = (WebMarkupContainer) get(
                createComponentPath(ID_DASHBOARD_PARENT, ID_DASHBOARD_TITLE));

        Label label = (Label) dashboardTitle.get(ID_TITLE);
        label.setDefaultModel(title);

        if (color == null) {
            color = DashboardColor.GRAY;
        }
        Component dashboardParent = get(ID_DASHBOARD_PARENT);
        dashboardParent.add(new AttributeAppender("class", " " + color.getCssClass()));

        WebMarkupContainer iconI = new WebMarkupContainer(ID_ICON);
        iconI.add(AttributeModifier.replace("class", icon));
        dashboardTitle.add(iconI);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer dashboardParent = new WebMarkupContainer(ID_DASHBOARD_PARENT);
        add(dashboardParent);

        WebMarkupContainer dashboardTitle = new WebMarkupContainer(ID_DASHBOARD_TITLE);
        dashboardParent.add(dashboardTitle);
        Label title = new Label(ID_TITLE);
        title.setRenderBodyOnly(true);
        dashboardTitle.add(title);

        WebMarkupContainer dashboardContent = new WebMarkupContainer(ID_DASHBOARD_CONTENT);
        dashboardContent.add(AttributeModifier.append("style", getDashboardBodyCss()));
        dashboardParent.add(dashboardContent);

        dashboardContent.add(new Label(ID_CONTENT));

        WebMarkupContainer preloaderContainer = new WebMarkupContainer(ID_PRELOADER_CONTAINER);
        preloaderContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isLoadingVisible();
            }
        });
        dashboardContent.add(preloaderContainer);

        preloaderContainer.add(getLoadingComponent(ID_PRELOADER));
    }

    public String getDashboardBodyCss() {
        return "padding: 0px;";
    }

    @Override
    protected void onPostSuccess(AjaxRequestTarget target) {
        WebMarkupContainer dashboardContent = getDashboardContent();
        dashboardContent.replace(getMainComponent(ID_CONTENT));

        PageBase page = (PageBase) getPage();
        showResultIfError(page);

        target.add(this, page.getFeedbackPanel());
    }

    private WebMarkupContainer getDashboardContent() {
        return (WebMarkupContainer) get(createComponentPath(ID_DASHBOARD_PARENT, ID_DASHBOARD_CONTENT));
    }

    @Override
    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
        String message = "Error occurred while fetching data: " + ex.getMessage();
        Label errorLabel = new Label(ID_CONTENT, message);
        WebMarkupContainer dashboardContent = getDashboardContent();
        dashboardContent.replace(errorLabel);

        PageBase page = (PageBase) getPage();
        showResultIfError(page);

        target.add(this, page.getFeedbackPanel());
    }

    private void showResultIfError(PageBase page) {
        CallableResult<T> result = (CallableResult<T>) getModel().getObject();
        if (result != null && result.getResult() != null) {
            OperationResult opResult = result.getResult();
            if (!WebMiscUtil.isSuccessOrHandledError(opResult)) {
                page.showResult(opResult);
            }
        }
    }
}
