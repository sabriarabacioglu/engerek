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

package com.evolveum.midpoint.web.page.admin.users.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.prism.HeaderStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class UserAccountDto implements Serializable {

    private ObjectWrapper object;
    private UserDtoStatus status;

    private boolean loadedOK = true;
    private String description;
    private OperationResult result;

    public UserAccountDto(ObjectWrapper object, UserDtoStatus status) {
        setObject(object);
        setStatus(status);
    }

    public UserAccountDto(boolean loaded, String description, OperationResult result){
        setLoadedOK(loaded);
        setDescription(description);
        this.result = result;
    }

    public OperationResult getResult() {
        return result;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isLoadedOK() {
        return loadedOK;
    }

    public void setLoadedOK(boolean loadedOK) {
        this.loadedOK = loadedOK;
    }

    public ObjectWrapper getObject() {
        return object;
    }

    public void setObject(ObjectWrapper object) {
        Validate.notNull(object, "Object wrapper must not be null.");
        this.object = object;
    }

    public UserDtoStatus getStatus() {
        return status;
    }

    public void setStatus(UserDtoStatus status) {
        Validate.notNull(status, "Status must not be null.");
        this.status = status;

        switch (status) {
            case DELETE:
                object.setHeaderStatus(HeaderStatus.DELETED);
                break;
            case UNLINK:
                object.setHeaderStatus(HeaderStatus.UNLINKED);
                break;
            case ADD:
                object.setHeaderStatus(HeaderStatus.ADDED);
                break;
            default:
                object.setHeaderStatus(HeaderStatus.NORMAL);
        }
    }
}
