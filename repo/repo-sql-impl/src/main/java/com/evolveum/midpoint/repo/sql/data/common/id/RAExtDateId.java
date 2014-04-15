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

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author lazyman
 */
public class RAExtDateId implements Serializable {

    private String ownerOid;
    private Short ownerId;
    private Timestamp value;
    private String name;

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Short getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Short ownerId) {
        this.ownerId = ownerId;
    }

    public Timestamp getValue() {
        return value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAExtDateId raDateId = (RAExtDateId) o;

        if (name != null ? !name.equals(raDateId.name) : raDateId.name != null) return false;
        if (ownerId != null ? !ownerId.equals(raDateId.ownerId) : raDateId.ownerId != null) return false;
        if (ownerOid != null ? !ownerOid.equals(raDateId.ownerOid) : raDateId.ownerOid != null) return false;
        if (value != null ? !value.equals(raDateId.value) : raDateId.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RADateId{" +
                "ownerOid='" + ownerOid + '\'' +
                ", ownerId=" + ownerId +
                ", value=" + value +
                ", name='" + name + '\'' +
                '}';
    }
}
