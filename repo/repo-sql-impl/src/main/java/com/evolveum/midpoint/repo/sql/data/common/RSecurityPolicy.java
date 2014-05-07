package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.util.Collection;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@ForeignKey(name = "fk_security_policy")
public class RSecurityPolicy extends RObject<SecurityPolicyType> {

    private RPolyString name;

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RSecurityPolicy that = (RSecurityPolicy) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(SecurityPolicyType jaxb, RSecurityPolicy repo, PrismContext prismContext)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
    }

    @Override
    public SecurityPolicyType toJAXB(PrismContext prismContext,
                                     Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        SecurityPolicyType object = new SecurityPolicyType();
        RUtil.revive(object, prismContext);
        RSecurityPolicy.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}

