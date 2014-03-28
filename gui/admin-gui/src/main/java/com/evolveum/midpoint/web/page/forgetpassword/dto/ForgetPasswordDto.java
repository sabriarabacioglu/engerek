package com.evolveum.midpoint.web.page.forgetpassword.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;

public class ForgetPasswordDto implements Serializable {
	
	public static final String F_ACCOUNTS ="accounts";
	public static final String F_PASSWORD = "password";
	
	private List<PasswordAccountDto> accounts;
	private String password;
	
	public List<PasswordAccountDto> getAccounts(){
		if(accounts==null){
			accounts = new ArrayList<PasswordAccountDto>();
			
		}
		return accounts;
	}
	
	public String getPassword(){
		return password;
	}
	
	
	public void setPassword(String password){
		this.password= password;
	}

}
