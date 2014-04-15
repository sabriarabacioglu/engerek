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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.id.RAExtPolyStringId;
import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@IdClass(RAExtPolyStringId.class)
@Table(name = "m_assignment_ext_poly")
@org.hibernate.annotations.Table(appliesTo = "m_assignment_ext_poly",
        indexes = {@Index(name = "iAExtensionPolyString", columnNames = {"extensionType", "eName", "orig"})})
public class RAExtPolyString implements RAExtValue {

    //owner entity
    private RAssignmentExtension anyContainer;
    private String ownerOid;
    private Short ownerId;

    private RAssignmentExtensionType extensionType;

    private boolean dynamic;
    private String name;
    private String type;
    private RValueType valueType;

    //orig value
    private String value;
    private String norm;

    public RAExtPolyString() {
        this(null);
    }

    public RAExtPolyString(PolyString polyString) {
        if (polyString != null) {
            value = polyString.getOrig();
            norm = polyString.getNorm();
        }
    }

    @ForeignKey(name = "fk_assignment_ext_poly")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "anyContainer_owner_id", referencedColumnName = "owner_type")
    })
    public RAssignmentExtension getAnyContainer() {
        return anyContainer;
    }

    @Id
    @Column(name = "anyContainer_owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && anyContainer != null) {
            ownerOid = anyContainer.getOwnerOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "anyContainer_owner_id")
    public Short getOwnerId() {
        if (ownerId == null && anyContainer != null) {
            ownerId = anyContainer.getOwnerId();
        }
        return ownerId;
    }

    @Id
    @Enumerated(EnumType.ORDINAL)
    public RAssignmentExtensionType getExtensionType() {
        return extensionType;
    }

    @Id
    @Column(name = "eName", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getName() {
        return name;
    }

    @Column(name = "eType", length = RUtil.COLUMN_LENGTH_QNAME)
    public String getType() {
        return type;
    }

    @Enumerated(EnumType.ORDINAL)
    public RValueType getValueType() {
        return valueType;
    }

    /**
     * @return true if this property has dynamic definition
     */
    @Column(name = "dynamicDef")
    public boolean isDynamic() {
        return dynamic;
    }

    @Column(name = "orig")
    public String getValue() {
        return value;
    }

    public String getNorm() {
        return norm;
    }

    public void setNorm(String norm) {
        this.norm = norm;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValueType(RValueType valueType) {
        this.valueType = valueType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public void setAnyContainer(RAssignmentExtension anyContainer) {
        this.anyContainer = anyContainer;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setOwnerId(Short ownerId) {
        this.ownerId = ownerId;
    }

    public void setExtensionType(RAssignmentExtensionType extensionType) {
        this.extensionType = extensionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAExtPolyString that = (RAExtPolyString) o;

        if (dynamic != that.dynamic) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (valueType != that.valueType) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (norm != null ? !norm.equals(that.norm) : that.norm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (dynamic ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
