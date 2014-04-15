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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RExclusion;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RRoleApproverRef;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualCollection;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExclusionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@QueryEntity(collections = {
        @VirtualCollection(jaxbName = @JaxbName(localPart = "inducement"), jaxbType = Set.class,
                jpaName = "assignments", jpaType = Set.class, additionalParams = {
                @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                        value = "ABSTRACT_ROLE")}, collectionType = RAssignment.class)})

@Entity
@ForeignKey(name = "fk_abstract_role")
@org.hibernate.annotations.Table(appliesTo = "m_abstract_role",
        indexes = {@Index(name = "iRequestable", columnNames = "requestable")})
public abstract class RAbstractRole<T extends AbstractRoleType> extends RFocus<T> {

    private Set<RExclusion> exclusion;
    private Boolean requestable;
    private Set<RObjectReference> approverRef;
    private String approvalProcess;

    public Boolean getRequestable() {
        return requestable;
    }

    @Column(nullable = true)
    public String getApprovalProcess() {
        return approvalProcess;
    }

    @Transient
    public Set<RAssignment> getInducement() {
        return getAssignments(RAssignmentOwner.ABSTRACT_ROLE);
    }

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RExclusion> getExclusion() {
        if (exclusion == null) {
            exclusion = new HashSet<RExclusion>();
        }
        return exclusion;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RRoleApproverRef.DISCRIMINATOR)
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getApproverRef() {
        if (approverRef == null) {
            approverRef = new HashSet<RObjectReference>();
        }
        return approverRef;
    }

    public void setApproverRef(Set<RObjectReference> approverRef) {
        this.approverRef = approverRef;
    }

    public void setExclusion(Set<RExclusion> exclusion) {
        this.exclusion = exclusion;
    }

    public void setApprovalProcess(String approvalProcess) {
        this.approvalProcess = approvalProcess;
    }

    public void setRequestable(Boolean requestable) {
        this.requestable = requestable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RAbstractRole that = (RAbstractRole) o;

        if (exclusion != null ? !exclusion.equals(that.exclusion) : that.exclusion != null)
            return false;
        if (approverRef != null ? !approverRef.equals(that.approverRef) : that.approverRef != null)
            return false;
        if (approvalProcess != null ? !approvalProcess.equals(that.approvalProcess) : that.approvalProcess != null)
            return false;
        if (requestable != null ? !requestable.equals(that.requestable) : that.requestable != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (approvalProcess != null ? approvalProcess.hashCode() : 0);
        result = 31 * result + (requestable != null ? requestable.hashCode() : 0);
        return result;
    }

    public static <T extends AbstractRoleType> void copyFromJAXB(AbstractRoleType jaxb, RAbstractRole<T> repo,
                                                                 PrismContext prismContext) throws DtoTranslationException {
        RFocus.copyFromJAXB(jaxb, repo, prismContext);
        repo.setRequestable(jaxb.isRequestable());

        for (AssignmentType inducement : jaxb.getInducement()) {
            RAssignment rInducement = new RAssignment(repo, RAssignmentOwner.ABSTRACT_ROLE);
            RAssignment.copyFromJAXB(inducement, rInducement, jaxb, prismContext);

            repo.getAssignments().add(rInducement);
        }

        for (ExclusionType exclusion : jaxb.getExclusion()) {
            RExclusion rExclusion = new RExclusion(repo);
            RExclusion.copyFromJAXB(exclusion, rExclusion, jaxb, prismContext);

            repo.getExclusion().add(rExclusion);
        }

        for (ObjectReferenceType approverRef : jaxb.getApproverRef()) {
            RObjectReference ref = RUtil.jaxbRefToRepo(approverRef, prismContext, repo, RReferenceOwner.ROLE_APPROVER);
            if (ref != null) {
                repo.getApproverRef().add(ref);
            }
        }
        
        PrismObjectDefinition<AbstractRoleType> roleDefinition = jaxb.asPrismObject().getDefinition();

        repo.setApprovalProcess(jaxb.getApprovalProcess());
    }
}
