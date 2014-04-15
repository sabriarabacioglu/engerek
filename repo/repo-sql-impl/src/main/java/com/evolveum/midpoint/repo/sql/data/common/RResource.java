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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.ROperationalState;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RResourceAdministrativeState;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RResourceApproverRef;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Where;

import javax.persistence.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_resource")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
public class RResource extends RObject<ResourceType> {

    private static final Trace LOGGER = TraceManager.getTrace(RResource.class);
    private RPolyString name;
    private REmbeddedReference connectorRef;
    private ROperationalState operationalState;
    //resource business configuration, embedded component can't be used, because then it couldn't use
    //non embedded approverRef relationship
    private RResourceAdministrativeState administrativeState;
    private Set<RObjectReference> approverRef;
    //end of resource business configuration

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RResourceAdministrativeState getAdministrativeState() {
        return administrativeState;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + RResourceApproverRef.DISCRIMINATOR)
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference> getApproverRef() {
        if (approverRef == null) {
            approverRef = new HashSet<RObjectReference>();
        }
        return approverRef;
    }

    @Embedded
    public REmbeddedReference getConnectorRef() {
        return connectorRef;
    }

    @Embedded
    public ROperationalState getOperationalState() {
        return operationalState;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setAdministrativeState(RResourceAdministrativeState administrativeState) {
        this.administrativeState = administrativeState;
    }

    public void setApproverRef(Set<RObjectReference> approverRef) {
        this.approverRef = approverRef;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setOperationalState(ROperationalState operationalState) {
        this.operationalState = operationalState;
    }

    public void setConnectorRef(REmbeddedReference connectorRef) {
        this.connectorRef = connectorRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RResource rResource = (RResource) o;

        if (name != null ? !name.equals(rResource.name) : rResource.name != null)
            return false;
        if (connectorRef != null ? !connectorRef.equals(rResource.connectorRef) : rResource.connectorRef != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(ResourceType jaxb, RResource repo, PrismContext prismContext)
            throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setConnectorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getConnectorRef(), prismContext));

        if (jaxb.getConnector() != null) {
            LOGGER.warn("Connector from resource type won't be saved. It should be translated to connector reference.");
        }
        PrismObject<ResourceType> resource = jaxb.asPrismObject();
        PrismContainerDefinition parentDefinition = resource.getDefinition();

        try {
            if (jaxb.getBusiness() != null) {
                ResourceBusinessConfigurationType business = jaxb.getBusiness();
                repo.getApproverRef().addAll(RUtil.safeListReferenceToSet(business.getApproverRef(),
                        prismContext, repo, RReferenceOwner.RESOURCE_BUSINESS_CONFIGURATON_APPROVER));
                repo.setAdministrativeState(RUtil.getRepoEnumValue(business.getAdministrativeState(),
                        RResourceAdministrativeState.class));
            }
            if (jaxb.getOperationalState() != null) {
                ROperationalState repoOpState = new ROperationalState();
                ROperationalState.copyFromJAXB(jaxb.getOperationalState(), repoOpState, prismContext);
                repo.setOperationalState(repoOpState);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ResourceType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        ResourceType object = new ResourceType();
        RUtil.revive(object, prismContext);
        RResource.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
