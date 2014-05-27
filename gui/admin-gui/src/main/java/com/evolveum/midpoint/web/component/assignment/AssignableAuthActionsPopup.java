package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AssignableAuthActionsPopup extends SimplePanel {
	private static final String ID_ASSIGNABLE_ACTIONS_FORM = "addActionsForm";
	private static final String ID_ACTIONS_TABLE = "actionsTable";
	private static final String ID_ADD_ACTION = "addAction";	

    private IModel<List<AutzActionsTableDto>> authorizations_list;

	AssignableAuthActionsPopup(String id) {
		super(id, null);  //second parameter is model
        	
		authorizations_list = new LoadableModel<List<AutzActionsTableDto>>(false){
        	@Override
        	protected List<AutzActionsTableDto> load() {
        		PageBase page = getPageBase();
        		   
        		//actions from the DescriptorLoader
        		Map<String, DisplayableValue<String>[]> authDesc = DescriptorLoader.getActions();
        		Collection<DisplayableValue<String>[]> authColl = authDesc.values();
        		List<AutzActionsTableDto> authlist = new ArrayList();
        		for (DisplayableValue<String>[] ac : authColl){
        			for (int i=0; i<ac.length;i++ ){
        				//System.out.println(ac[i].getLabel() +" "+ ac[i].getDescription() +" " + ac[i].getValue());
        			}
        			  //authlist.add(new AutzActionsTableDto(ac.getLabel(),ac.getDescription(),ac.getValue()));
        		}
        		
        		//Actions from the ModelInteractionService 		
        		ModelInteractionService service = page.getModelInteractionService();
        		Collection<DisplayableValue<String>> collection = (Collection<DisplayableValue<String>>) service.getActionUrls();
        		List<AutzActionsTableDto> listOfActions= new ArrayList();
        		for (DisplayableValue<String> tr : collection){
        			listOfActions.add(new AutzActionsTableDto(tr.getLabel(),tr.getDescription(),tr.getValue()));
        		}  
        		
        		return listOfActions;
        		//return authlist;
        	}
        	
        };

		initPopPanelLayout();
	}

	
	private void initPopPanelLayout() {
		Form assignableActionForm = new Form(ID_ASSIGNABLE_ACTIONS_FORM);
		add(assignableActionForm);

		TablePanel table = createTable();
		assignableActionForm.add(table);

		AjaxButton addButton = new AjaxButton(ID_ADD_ACTION,
				createStringResource("assignableAuthActionsPopup.button.add")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				 addPerformed(target, getSelectedAuthorizations());
			}

		};
		assignableActionForm.add(addButton);

	}



	private TablePanel createTable() {
		List<IColumn> columns = createMultiSelectColumns();
		ListDataProvider provider = new ListDataProvider(this, authorizations_list);
		
		// ObjectDataProvider provider = new ObjectDataProvider(getPageBase(),
		// type);
		// provider.setQuery(getProviderQuery());
		TablePanel table = new TablePanel(ID_ACTIONS_TABLE, provider, columns);
		table.setOutputMarkupId(true);

		return table;
	}
	
	
	private List<AutzActionsTableDto> getSelectedAuthorizations(){
        List<AutzActionsTableDto> selected = new ArrayList<AutzActionsTableDto>();       
        List<AutzActionsTableDto> all = authorizations_list.getObject();  

        for(AutzActionsTableDto dto: all){
            if(dto.isSelected()){
                selected.add(dto);
            }
        }

        return selected;
    }
	
	 private List<IColumn> createMultiSelectColumns() {
	        List<IColumn> columns = new ArrayList<IColumn>();

	        IColumn column = new CheckBoxHeaderColumn();
	        columns.add(column);

	        columns.add(new PropertyColumn<AutzActionsTableDto,String>(createStringResource("assignableAuthActionsPopup.label"), "authName"));
	        
	        columns.add(new PropertyColumn<AutzActionsTableDto,String>(createStringResource("assignableAuthActionsPopup.description"), "autzDesc"));
	        
	        columns.add(new PropertyColumn<AutzActionsTableDto,String>(createStringResource("assignableAuthActionsPopup.actionURI"), "authURI"));

	        return columns;
	    }
	
	protected void addPerformed(AjaxRequestTarget target, List<AutzActionsTableDto> selected) {

    }

}
