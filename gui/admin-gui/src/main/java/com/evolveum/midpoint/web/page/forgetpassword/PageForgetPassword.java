package com.evolveum.midpoint.web.page.forgetpassword;


import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
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
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
// import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;.
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;














import org.apache.commons.lang.StringUtils;
import org.apache.wicket.atmosphere.EventBus;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jruby.RubyProcess.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author sabria
 */


@PageDescriptor(url = "/forgetpassword")
public class PageForgetPassword extends PageBase {


	@Autowired
	private transient PrismContext prismContext;
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
				//MidPointPrincipal principal = SecurityUtils.getPrincipalUser();

				//	OperationResult result = new OperationResult(OPERATION_LOAD_USER);
				//		 PrismObject<UserType> principleUser = WebModelUtils.loadObject(UserType.class,
				//	"00000000-0000-0000-0000-000000000002", result, PageForgetPassword.this);
				//			System.out.println(principleUser.getDisplayName());
				try {
					MidPointPrincipal principal=getSecurityEnforcer().getUserProfileService().getPrincipal("administrator");
					Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
					getSecurityEnforcer().setupPreAuthenticatedSecurityContext(authentication);
					

				} catch (ObjectNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


				UserType user= checkUser(email.getModelObject(),username.getModelObject() );


				//Check if the email and the uid exists and matches in the idm
				if(user!=null){
					//If the parameters are ok reset the password
					System.out.println("Reset Password User var.");
			//		PageParameters parameters = new PageParameters();
				//	PageForgetPasswordQuestions pageForgetPasswordQuestions =new PageForgetPasswordQuestions();
			//		pageForgetPasswordQuestions.setUserTypeObject(user);

			//		setResponsePage(pageForgetPasswordQuestions);

					resetPassword(user);


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
		

		Task task = createSimpleTask(OPERATION_RESET_PASSWORD);
		System.out.println("Reset Password1");
		OperationResult result = new OperationResult(OPERATION_RESET_PASSWORD);
		ProtectedStringType password = new ProtectedStringType();
	    Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolve(),
                        SystemConfigurationType.F_DEFAULT_USER_TEMPLATE ,SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF);
	    System.out.println("Reset Password2");
		PrismObject<SystemConfigurationType> systemConfig;
		String newPassword="";
		PageBase page = (PageBase) getPage();

		ModelService model = page.getModelService();
		try {
			System.out.println("getModel");
			systemConfig = model.getObject(SystemConfigurationType.class,
			        SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, task, result);
			newPassword=ValuePolicyGenerator.generate(systemConfig.asObjectable().getGlobalPasswordPolicy().getStringPolicy(), systemConfig.asObjectable().getGlobalPasswordPolicy().getStringPolicy().getLimitations().getMinLength(), result);
			System.out.println("Reset Password3");
		} catch (ObjectNotFoundException e1) {
			// TODO Auto-generated catch block
			System.out.println(e1);
		} catch (SchemaException e1) {
			System.out.println(e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityViolationException e1) {
			System.out.println(e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CommunicationException e1) {
			System.out.println(e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ConfigurationException e1) {
			System.out.println(e1);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		System.out.println("Passs:"+newPassword);
		password.setClearValue(newPassword);

		WebMiscUtil.encryptProtectedString(password, true, getMidpointApplication());
		final ItemPath valuePath = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
				CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
		System.out.println("Reset Password4");
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		PrismObjectDefinition objDef= registry.findObjectDefinitionByCompileTimeClass(UserType.class);

		PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(valuePath, objDef, password);
		Class<? extends ObjectType> type =  UserType.class;

		deltas.add(ObjectDelta.createModifyDelta(user.getOid(), delta, type, getPrismContext()));
		try {
			System.out.println("Reset Password5");
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

		System.out.println("checkkk");
		OperationResult result = new OperationResult(LOAD_USER_EMAIL);	
		String idmEmail=null;
		Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
		options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
				GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));

		Task task = createSimpleTask(OPERATION_LOAD_USER);
		OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);

		LOGGER.info("CheckUser Poly oncesi");
		PolyString userId = new PolyString(username, username);
		PolyString emailAddress = new PolyString(username, username);
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

		EqualFilter filter;
		PageBase page = (PageBase) getPage();

		ModelService model = page.getModelService();
		try {
			filters.add(EqualFilter.createEqual(UserType.F_NAME, UserType.class,getPrismContext(),PolyStringOrigMatchingRule.NAME,username));
			filters.add(EqualFilter.createEqual(UserType.F_EMAIL_ADDRESS, UserType.class,getPrismContext(),PolyStringOrigMatchingRule.NAME,email));

			ObjectQuery query = new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));


			List<PrismObject<UserType>> userList= WebModelUtils.searchObjects(UserType.class, query,
                    result, this);
					
				//	model.searchObjects(UserType.class, query, options, task, subResult);

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


		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		} 










	}


}