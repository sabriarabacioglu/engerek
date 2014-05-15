package com.evolveum.midpoint.web.page.forgetpassword.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;

public class ForgetPasswordDto implements Serializable {
	
	
	private String searchText;
	private String userName;
	private String email;
	
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	
	
	
	
	
	
	
	
	
	

}
