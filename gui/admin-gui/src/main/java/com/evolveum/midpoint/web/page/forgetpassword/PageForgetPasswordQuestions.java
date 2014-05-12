package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.menu.top.TopMenuBar;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PageDescriptor(url = "/forgetpasswordquestions")
public class PageForgetPasswordQuestions extends PageBase {
	
	private UserType userTypeObject;
	
	private static final Trace LOGGER = TraceManager.getTrace(PageForgetPassword.class);

	private static final String ID_PWDRESETQUESTIONFORM = "pwdResetQuestionForm";
	private static final String ID_LABELQUESTION1 = "labelQuestion1";
	private static final String ID_LABELQUESTION2 = "labelQuestion2";
	private static final String ID_LABELQUESTION3 = "labelQuestion3";

	

	public PageForgetPasswordQuestions() {

		TopMenuBar menuBar = getTopMenuBar();
		menuBar.addOrReplace(new LocalePanel(TopMenuBar.ID_RIGHT_PANEL));
		 
		
	
	        if (userTypeObject==null) {
	        	
	        	//TODO change the error
	            getSession().error(getString("pageForgetPasswordQuestions.message.usernotfound"));
	            throw new RestartResponseException(PageForgetPasswordQuestions.class);
	        }
		
		
		 Form form = new Form(ID_PWDRESETQUESTIONFORM) {

	            @Override
	            protected void onSubmit() {
	            	
	            	
	            
	                 
	               
	              LOGGER.info("Reset Password user info form submitted.");
	          
	                //Check if the email and the uid exists and matches in the idm
	                
	           
	             
	              
	            }
	        };
		
		

	}

	public UserType getUserTypeObject() {
		return userTypeObject;
	}

	public void setUserTypeObject(UserType userTypeObject) {
		this.userTypeObject = userTypeObject;
	}

}
