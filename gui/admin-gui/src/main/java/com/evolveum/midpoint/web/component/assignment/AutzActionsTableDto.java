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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *  @author arda
 * */
public class AutzActionsTableDto<T extends ObjectType> extends Selectable {

	private String authName;
	private String autzDesc;
	private String authURI;

public String getAuthURI() {
		return authURI;
	}

	public void setAuthURI(String authURI) {
		this.authURI = authURI;
	}

public AutzActionsTableDto(String authName, String autzDesc){
	this.authName= authName;
	this.autzDesc=autzDesc;
	}

public AutzActionsTableDto(String authName, String autzDesc, String authURI){
	this.authName= authName;
	this.autzDesc=autzDesc;
	this.authURI=authURI;
	}

public String getAuthName() {
	return authName;
}

public void setAuthName(String authName) {
	this.authName = authName;
}

public String getAutzDesc() {
	return autzDesc;
}

public void setAutzDesc(String autzDesc) {
	this.autzDesc = autzDesc;
}

@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((authName == null) ? 0 : authName.hashCode());
	result = prime * result + ((authURI == null) ? 0 : authURI.hashCode());
	result = prime * result + ((autzDesc == null) ? 0 : autzDesc.hashCode());
	return result;
}

@Override
public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	AutzActionsTableDto other = (AutzActionsTableDto) obj;
	if (authName == null) {
		if (other.authName != null)
			return false;
	} else if (!authName.equals(other.authName))
		return false;
	if (authURI == null) {
		if (other.authURI != null)
			return false;
	} else if (!authURI.equals(other.authURI))
		return false;
	if (autzDesc == null) {
		if (other.autzDesc != null)
			return false;
	} else if (!autzDesc.equals(other.autzDesc))
		return false;
	return true;
}


}
