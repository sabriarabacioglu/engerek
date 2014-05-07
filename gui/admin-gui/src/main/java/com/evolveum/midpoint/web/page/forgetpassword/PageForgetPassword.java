package com.evolveum.midpoint.web.page.forgetpassword;


import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.atmosphere.NotifyMessage;
import com.evolveum.midpoint.web.component.menu.top.TopMenuBar;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.home.PageMyPasswords;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.forgetpassword.dto.ForgetPasswordDto;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
// import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;.
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.ProtectedStringType;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIConversion.User;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.atmosphere.EventBus;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author sabria
 */


@PageDescriptor(url = "/forgetpassword")
public class PageForgetPassword extends PageBase {

	private static final Trace LOGGER = TraceManager.getTrace(PageForgetPassword.class);
	
    private static final String ID_PWDRESETFORM = "pwdresetform";

    private static final String ID_USERNAME = "username";
    private static final String ID_EMAIL = "email";
    private static final String DOT_CLASS = PageForgetPassword.class.getName() + ".";
    private static final String LOAD_USER_EMAIL = DOT_CLASS + "loadUserEmail";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE_PASSWORD = DOT_CLASS + "savePassword";

    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_RESET_PASSWORD = DOT_CLASS + "resetPassword";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    
    
   
    public PageForgetPassword() {
    	
    	

   
        TopMenuBar menuBar = getTopMenuBar();
        menuBar.addOrReplace(new LocalePanel(TopMenuBar.ID_RIGHT_PANEL));
        
        Form form = new Form(ID_PWDRESETFORM) {

            @Override
            protected void onSubmit() {
            	 final RequiredTextField<String> username = (RequiredTextField) get(ID_USERNAME);
                 RequiredTextField<String> email = (RequiredTextField) get(ID_EMAIL);
               
              LOGGER.info("Reset Password user info form submitted.");
          
                //Check if the email and the uid exists and matches in the idm
                
            	UserType user= checkUser(email.getModelObject(),username.getModelObject() );
                
            	if(user!=null){
                	//If the parameters are ok reset the password
                    System.out.println("Reset Password User var.");
                    PageParameters parameters = new PageParameters();
                    parameters.add(PageForgetPasswordQuestions.PARAM_OBJECT_ID, user.getOid());
                    setResponsePage(PageForgetPasswordQuestions.class, parameters);
                   
                   
                	
                	
                }
                else{
                	
                }
              
            }
        };

        form.add(new RequiredTextField(ID_USERNAME, new Model<String>()));
        form.add(new RequiredTextField(ID_EMAIL, new Model<String>()));
       
        add(form);
    }
    
    
    

    @Override
    protected IModel<String> createPageTitleModel() {
        return new Model<String>("");
    }

 
    
    private void resetPassword(UserType user){
    	
    	  
        Task task = createSimpleTask(OPERATION_SAVE_PASSWORD);
    	System.out.println("Reset Password");
    	 OperationResult result = new OperationResult(OPERATION_RESET_PASSWORD);
    	 ProtectedStringType password = new ProtectedStringType();
         password.setClearValue("Qwerty123");
         
         WebMiscUtil.encryptProtectedString(password, true, getMidpointApplication());
         final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
                 CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
     	 System.out.println("Reset Password2");
         SchemaRegistry registry = getPrismContext().getSchemaRegistry();
         Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
         PrismObjectDefinition objDef= registry.findObjectDefinitionByCompileTimeClass(UserType.class);
         
         PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, password);
         Class<? extends ObjectType> type =  UserType.class;
         
         deltas.add(ObjectDelta.createModifyDelta(user.getOid(), delta, type, getPrismContext()));
         try {
        		System.out.println("Reset Password3");
			getModelService().executeChanges(deltas, null, task, result);
		} catch (ObjectAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PolicyViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
    }
    
   
    
    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<UserType> user) {
        return new PasswordAccountDto(user.getOid(), getString("PageForgetPassword.accountMidpoint"),
                getString("PageForgetPassword.resourceMidpoint"), WebMiscUtil.isActivationEnabled(user), true);
    }
    
    private PasswordAccountDto createPasswordAccountDto(PrismObject<ShadowType> account) {
        PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
        String resourceName;
        if (resourceRef == null || resourceRef.getValue() == null || resourceRef.getValue().getObject() == null) {
            resourceName = getString("PageForgetPassword.couldntResolve");
        } else {
            resourceName = WebMiscUtil.getName(resourceRef.getValue().getObject());
        }

        return new PasswordAccountDto(account.getOid(), WebMiscUtil.getName(account),
                resourceName, WebMiscUtil.isActivationEnabled(account));
    }
    
    
    //Checkd if the user exists with the given email and username in the idm 
    public UserType checkUser(String email,String username){
    	OperationResult result = new OperationResult(LOAD_USER_EMAIL);	
    	String idmEmail=null;
    	Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));
        
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);
        
        LOGGER.info("CheckUser Poly oncesi");
        PolyString YOUR_NAME = new PolyString(username, username);
        EqualsFilter filter;

        ModelService model = getModelService();
		try {
		
	      filter = EqualsFilter.createEqual(UserType.F_NAME, UserType.class,getPrismContext(),PolyStringOrigMatchingRule.NAME,YOUR_NAME);
	               
	        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
			
			 
			  List<PrismObject<UserType>> userList= model.searchObjects(UserType.class, query, options, task, subResult);
		        
		        LOGGER.info("CheckUser try ici");
			  if((userList!=null) && (!userList.isEmpty())){
				  System.out.println("User bulundu ya la.");
			        
			        LOGGER.info("User bulundu");
				  UserType user=  userList.get(0).asObjectable();
				  System.out.println("Emaillllllll:::::: "+user.getEmailAddress());
				  if(user.getEmailAddress().equalsIgnoreCase(email)){
		
				  return user;
				  }
				  else return null;
			  }
			  else return null;
			 
			 
		} catch (SchemaException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (SecurityViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
      
        
        
        
        
        
        
     

    	
    }
    
   
}