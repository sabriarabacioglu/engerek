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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.id.RAssignmentExtensionId;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@IdClass(RAssignmentExtensionId.class)
@Table(name = "m_assignment_extension")
public class RAssignmentExtension implements Serializable {

    private RAssignment owner;
    private String ownerOid;
    private Short ownerId;

    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short polysCount;

    private Set<RAExtString> strings;
    private Set<RAExtLong> longs;
    private Set<RAExtDate> dates;
    private Set<RAExtReference> references;
    private Set<RAExtPolyString> polys;

    @ForeignKey(name = "none")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    public RAssignment getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOwnerOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "owner_id", length = RUtil.COLUMN_LENGTH_OID)
    public Short getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @OneToMany(mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAExtLong> getLongs() {
        if (longs == null) {
            longs = new HashSet<>();
        }
        return longs;
    }

    @OneToMany(mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAExtString> getStrings() {
        if (strings == null) {
            strings = new HashSet<>();
        }
        return strings;
    }

    @OneToMany(mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAExtDate> getDates() {
        if (dates == null) {
            dates = new HashSet<>();
        }
        return dates;
    }

    @OneToMany(mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAExtReference> getReferences() {
        if (references == null) {
            references = new HashSet<>();
        }
        return references;
    }

    @OneToMany(mappedBy = RAExtValue.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAExtPolyString> getPolys() {
        if (polys == null) {
            polys = new HashSet<>();
        }
        return polys;
    }

    public Short getStringsCount() {
        return stringsCount;
    }

    public Short getLongsCount() {
        return longsCount;
    }

    public Short getDatesCount() {
        return datesCount;
    }

    public Short getReferencesCount() {
        return referencesCount;
    }

    public Short getPolysCount() {
        return polysCount;
    }

    public void setStringsCount(Short stringsCount) {
        this.stringsCount = stringsCount;
    }

    public void setLongsCount(Short longsCount) {
        this.longsCount = longsCount;
    }

    public void setDatesCount(Short datesCount) {
        this.datesCount = datesCount;
    }

    public void setReferencesCount(Short referencesCount) {
        this.referencesCount = referencesCount;
    }

    public void setPolysCount(Short polysCount) {
        this.polysCount = polysCount;
    }

    public void setPolys(Set<RAExtPolyString> polys) {
        this.polys = polys;
    }

    public void setReferences(Set<RAExtReference> references) {
        this.references = references;
    }

    public void setDates(Set<RAExtDate> dates) {
        this.dates = dates;
    }

    public void setLongs(Set<RAExtLong> longs) {
        this.longs = longs;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setStrings(Set<RAExtString> strings) {
        this.strings = strings;
    }

    public void setOwner(RAssignment owner) {
        this.owner = owner;
    }

    public void setOwnerId(Short ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAssignmentExtension that = (RAssignmentExtension) o;

        if (dates != null ? !dates.equals(that.dates) : that.dates != null) return false;
        if (datesCount != null ? !datesCount.equals(that.datesCount) : that.datesCount != null) return false;
        if (longs != null ? !longs.equals(that.longs) : that.longs != null) return false;
        if (longsCount != null ? !longsCount.equals(that.longsCount) : that.longsCount != null) return false;
        if (polys != null ? !polys.equals(that.polys) : that.polys != null) return false;
        if (polysCount != null ? !polysCount.equals(that.polysCount) : that.polysCount != null) return false;
        if (references != null ? !references.equals(that.references) : that.references != null) return false;
        if (referencesCount != null ? !referencesCount.equals(that.referencesCount) : that.referencesCount != null)
            return false;
        if (strings != null ? !strings.equals(that.strings) : that.strings != null) return false;
        if (stringsCount != null ? !stringsCount.equals(that.stringsCount) : that.stringsCount != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stringsCount != null ? stringsCount.hashCode() : 0;
        result = 31 * result + (longsCount != null ? longsCount.hashCode() : 0);
        result = 31 * result + (datesCount != null ? datesCount.hashCode() : 0);
        result = 31 * result + (referencesCount != null ? referencesCount.hashCode() : 0);
        result = 31 * result + (polysCount != null ? polysCount.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(ShadowAttributesType jaxb, RAssignmentExtension repo,
                                    PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyFromJAXB(jaxb.asPrismContainerValue(), repo, prismContext);
    }

    public static void copyFromJAXB(ExtensionType jaxb, RAssignmentExtension repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyFromJAXB(jaxb.asPrismContainerValue(), repo, prismContext);
    }

    private static void copyFromJAXB(PrismContainerValue containerValue, RAssignmentExtension repo,
                                     PrismContext prismContext) throws
            DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(prismContext);

        Set<RAnyValue> values = new HashSet<RAnyValue>();
        try {
            List<Item<?>> items = containerValue.getItems();
            for (Item item : items) {
                values.addAll(converter.convertToRValue(item, true));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        for (RAnyValue value : values) {
            ((RAExtValue) value).setAnyContainer(repo);

            if (value instanceof RAExtDate) {
                repo.getDates().add((RAExtDate) value);
            } else if (value instanceof RAExtLong) {
                repo.getLongs().add((RAExtLong) value);
            } else if (value instanceof RAExtReference) {
                repo.getReferences().add((RAExtReference) value);
            } else if (value instanceof RAExtString) {
                repo.getStrings().add((RAExtString) value);
            } else if (value instanceof RAExtPolyString) {
                repo.getPolys().add((RAExtPolyString) value);
            }
        }

        repo.setStringsCount((short) repo.getStrings().size());
        repo.setDatesCount((short) repo.getDates().size());
        repo.setPolysCount((short) repo.getPolys().size());
        repo.setReferencesCount((short) repo.getReferences().size());
        repo.setLongsCount((short) repo.getLongs().size());
    }
}
