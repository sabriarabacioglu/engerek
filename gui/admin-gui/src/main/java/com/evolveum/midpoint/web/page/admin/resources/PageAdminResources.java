/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources;


import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.PageDisabledVisibleBehaviour;
import com.evolveum.midpoint.web.component.util.PageVisibleDisabledBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Marker page class for {@link com.evolveum.midpoint.web.component.menu.top.TopMenu}
 *
 * @author lazyman
 */
public class PageAdminResources extends PageAdmin {

    public static final String PARAM_RESOURCE_ID = "resourceOid";

    private static final String DOT_CLASS = PageAdminResources.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem("pageAdminResources.listResources", PageResources.class));
        items.add(new BottomMenuItem("pageAdminResources.detailsResource", PageResource.class,
                new PageVisibleDisabledBehaviour(this, PageResource.class)));

//        items.add(new BottomMenuItem("pageAdminResources.newResource", PageResourceEdit.class,
//                new PageDisabledVisibleBehaviour(this, PageResourceEdit.class) {
//
//                    @Override
//                    public boolean isVisible() {
//                        return !isEditingResource();
//                    }
//                }));
//        items.add(new BottomMenuItem("pageAdminResources.editResource", PageResourceEdit.class,
//                new VisibleEnableBehaviour() {
//
//            @Override
//            public boolean isVisible() {
//                return isEditingResource();
//            }
//
//            @Override
//            public boolean isEnabled() {
//                return false;
//            }
//        }));

        items.add(new BottomMenuItem("pageAdminResources.importResource", PageResourceImport.class,
                new PageVisibleDisabledBehaviour(this, PageResourceImport.class)));
        items.add(new BottomMenuItem("pageAdminResources.contentAccounts", PageContentAccounts.class,
                new PageVisibleDisabledBehaviour(this, PageContentAccounts.class)));
        items.add(new BottomMenuItem("pageAdminResources.accountDetails", PageAccount.class,
                new PageVisibleDisabledBehaviour(this, PageAccount.class)));

        return items;
    }

    protected boolean isEditingResource() {
        StringValue resourceOid = getPageParameters().get(PageResourceEdit.PARAM_RESOURCE_ID);
        return resourceOid != null && StringUtils.isNotEmpty(resourceOid.toString());
    }

    protected PrismObject<ResourceType> loadResource(Collection<ObjectOperationOptions> options) {
        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
        PrismObject<ResourceType> resource = null;

        try {
            Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
            StringValue resourceOid = getPageParameters().get(PARAM_RESOURCE_ID);
            resource = getModelService().getObject(ResourceType.class, resourceOid.toString(), options, task, result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get resource.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        if (resource == null) {
            getSession().error(getString("pageAdminResources.message.cantLoadResource"));

            if (!result.isSuccess()) {
                showResultInSession(result);
            }
            throw new RestartResponseException(PageResources.class);
        }

        return resource;
    }
}
