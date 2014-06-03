package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.PageBase;

@PageDescriptor(url = "/resetpasswordsuccess")
public class PageResetPasswordSuccess extends PageBase{
	
	@Override
	protected IModel<String> createPageTitleModel() {
		return new Model<String>("");
	}


}
