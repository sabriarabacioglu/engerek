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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.web.page.admin.resources.dto.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.message.OpResult;
import com.evolveum.midpoint.web.component.message.OperationResultPanel;
import com.evolveum.midpoint.web.component.message.Param;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

public class PageResource extends PageAdminResources {

	public static final String PARAM_RESOURCE_ID = "resourceId";
	private static final String DOT_CLASS = PageResource.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

	private static final String TEST_CONNECTION = DOT_CLASS + "testConnection";

	private IModel<ResourceDto> model;

	public PageResource() {
		model = new LoadableModel<ResourceDto>() {

			@Override
			protected ResourceDto load() {
				return loadResource();
			}
		};
		initLayout();
	}

	private ResourceDto loadResource() {
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
		PrismObject<ResourceType> resource = null;

		try {
			Collection<PropertyPath> resolve = MiscUtil.createCollection(new PropertyPath(
					ResourceType.F_CONNECTOR));

			Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);

			StringValue resourceOid = getPageParameters().get(PARAM_RESOURCE_ID);
			resource = getModelService().getObject(ResourceType.class, resourceOid.toString(), resolve, task,
					result);

			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get resource.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (resource == null) {
            getSession().error(getString("pageResource.message.cantResourceDetails"));

            if (!result.isSuccess()) {
                showResultInSession(result);
            }
            throw new RestartResponseException(PageResources.class);
        }
		return new ResourceDto(resource, getMidpointApplication().getPrismContext(), resource.asObjectable().getConnector(),
				initCapabilities(resource.asObjectable()));
	}

	@Override
	protected IModel<String> createPageTitleModel() {
		return new LoadableModel<String>(false) {

			@Override
			protected String load() {
				String name = model.getObject().getName();
				return new StringResourceModel("page.title", PageResource.this, null, null, name).getString();
			}
		};
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		SortableDataProvider<ResourceObjectTypeDto> provider = new ListDataProvider<ResourceObjectTypeDto>(this,
				new PropertyModel<List<ResourceObjectTypeDto>>(model, "objectTypes"));
		provider.setSort("displayName", SortOrder.ASCENDING);
		TablePanel objectTypes = new TablePanel<ResourceObjectTypeDto>("objectTypesTable", provider,
				initObjectTypesColumns());
		objectTypes.setShowPaging(true);
		objectTypes.setOutputMarkupId(true);
		mainForm.add(objectTypes);

		initResourceColumns(mainForm);
		initConnectorDetails(mainForm);
		createCapabilitiesList(mainForm);

		AjaxLink<String> link = new AjaxLink<String>("seeDebug",
				createStringResource("pageResource.seeDebug")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				PageParameters parameters = new PageParameters();
				parameters.add(PageDebugView.PARAM_OBJECT_ID, model.getObject().getOid());
				setResponsePage(PageDebugView.class, parameters);
			}
		};
		mainForm.add(link);
		initButtons(mainForm);
	}

	private void initResourceColumns(Form mainForm) {
		mainForm.add(new Label("resourceOid", new PropertyModel<Object>(model, "oid")));
		mainForm.add(new Label("resourceName", new PropertyModel<Object>(model, "name")));
		mainForm.add(new Label("resourceType", new PropertyModel<Object>(model, "type")));
		mainForm.add(new Label("resourceVersion", new PropertyModel<Object>(model, "version")));
		mainForm.add(new Label("resourceProgress", new PropertyModel<Object>(model, "progress")));
	}

    private IModel<String> createTestConnectionStateTooltip(final String expression) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PropertyModel<ResourceStatus> pModel = new PropertyModel<ResourceStatus>(model, expression);
                ResourceStatus status = pModel.getObject();
                if (status == null) {
                    return "";
                }

                return PageResource.this.getString(ResourceStatus.class.getSimpleName() + "." + status.name());
            }
        };
    }

	private void initConnectorDetails(Form mainForm) {
		WebMarkupContainer container = new WebMarkupContainer("connectors");
		container.setOutputMarkupId(true);

        Image image = new Image("overallStatus", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                return new PackageResourceReference(PageResource.class, model.getObject().getState()
                        .getOverall().getIcon());
            }
        });
        image.add(new AttributeModifier("title", createTestConnectionStateTooltip("state.overall")));
		container.add(image);

        image = new Image("confValidation", new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                return new PackageResourceReference(PageResource.class, model.getObject().getState()
                        .getConfValidation().getIcon());
            }
        });
        image.add(new AttributeModifier("title", createTestConnectionStateTooltip("state.confValidation")));
		container.add(image);

        image = new Image("conInitialization", new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {

                return new PackageResourceReference(PageResource.class, model.getObject().getState()
                        .getConInitialization().getIcon());
            }
        });
        image.add(new AttributeModifier("title", createTestConnectionStateTooltip("state.conInitialization")));
		container.add(image);

        image = new Image("conConnection", new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                return new PackageResourceReference(PageResource.class, model.getObject().getState()
                        .getConConnection().getIcon());
            }
        });
        image.add(new AttributeModifier("title", createTestConnectionStateTooltip("state.conConnection")));
		container.add(image);

		/*container.add(new Image("conSanity", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return new PackageResourceReference(PageResource.class, model.getObject().getState()
						.getConSanity().getIcon());
			}
		}));*/

        image = new Image("conSchema", new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                return new PackageResourceReference(PageResource.class, model.getObject().getState()
                        .getConSchema().getIcon());
            }
        });
		container.add(image);
        image.add(new AttributeModifier("title", createTestConnectionStateTooltip("state.conSchema")));
		mainForm.add(container);
	}

	private List<String> initCapabilities(ResourceType resource) {
		OperationResult result = new OperationResult("Load resource capabilities");
		List<String> capabilitiesName = new ArrayList<String>();
		try {
			List<Object> capabilitiesList = ResourceTypeUtil.listEffectiveCapabilities(resource);

			if (capabilitiesList != null && !capabilitiesList.isEmpty()) {
				for (int i = 0; i < capabilitiesList.size(); i++) {
					capabilitiesName.add(ResourceTypeUtil.getCapabilityDisplayName(capabilitiesList.get(i)));
				}
			}
		} catch (Exception ex) {
			result.recordFatalError("Couldn't load resource capabilities for resource'"
					+ new PropertyModel<Object>(model, "name") + ".", ex);

		}
		return capabilitiesName;
	}

	private List<IColumn<ResourceObjectTypeDto>> initObjectTypesColumns() {
		List<IColumn<ResourceObjectTypeDto>> columns = new ArrayList<IColumn<ResourceObjectTypeDto>>();

		columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.displayName"),
				"displayName", "displayName"));
		columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.nativeObjectClass"),
				"nativeObjectClass"));
		columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.help"), "help"));
		columns.add(new PropertyColumn(createStringResource("pageResource.objectTypes.type"), "type"));

		return columns;
	}

	private void createCapabilitiesList(Form mainForm) {
		ListView<String> listCapabilities = new ListView<String>("listCapabilities", createCapabilitiesModel(model)) {

			@Override
			protected void populateItem(ListItem<String> item) {
				 item.add(new Label("capabilities", item.getModel()));

			}
		};
		mainForm.add(listCapabilities);
	}

	private IModel<List<String>> createCapabilitiesModel(final IModel<ResourceDto> model) {
		return new LoadableModel<List<String>>(false) {

			@Override
			protected List<String> load() {
				ResourceDto resource = model.getObject();
				return resource.getCapabilities();
			}
		};
	}

	private void initButtons(Form mainForm){
		AjaxLinkButton back = new AjaxLinkButton("back", createStringResource("pageResource.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageResources.class);
			}
		};
		mainForm.add(back);

		/*AjaxLinkButton save = new AjaxLinkButton("save", createStringResource("pageResource.button.save")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				savePerform(target);
			}
		};
		mainForm.add(save);*/

		AjaxLinkButton test = new AjaxLinkButton("test", createStringResource("pageResource.button.test")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				testConnectionPerformed(target);
				//add(PageResource.this);
			}
		};
		mainForm.add(test);
	}

	private void savePerform(AjaxRequestTarget target){
		//TODO: implement
	}

	private void testConnectionPerformed(AjaxRequestTarget target){
        OperationResult result = null;
    	ResourceDto dto = model.getObject();
    	if (StringUtils.isEmpty(dto.getOid())) {
    		result.recordFatalError("Resource oid not defined in request");
		}

    	try {
    		result = getModelService().testResource(dto.getOid(), createSimpleTask(TEST_CONNECTION));
    		ResourceController.updateResourceState(dto.getState(), result);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Fail to test resource connection", ex);
		}

    	if(result == null) {
    		result = new OperationResult(TEST_CONNECTION);
    	}

    	WebMarkupContainer connectors = (WebMarkupContainer)get("mainForm:connectors");
    	target.add(connectors);

    	if(!result.isSuccess()){
    		showResult(result);
    		target.add(getFeedbackPanel());
    	}
	}
}