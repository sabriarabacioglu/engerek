/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class TypeRestriction extends Restriction {

    @Override
    public Criterion interpret(ObjectFilter filter) throws QueryException {
        String property = getContext().getAlias(null) + "." + RObject.F_OBJECT_TYPE_CLASS;

        TypeFilter typeFilter = (TypeFilter) filter;
        Set<RObjectType> values = getValues(typeFilter.getType());

        if (values.size() > 1) {
            return Restrictions.in(property, values);
        }

        return Restrictions.eq(property, values.iterator().next());
    }

    private Set<RObjectType> getValues(QName typeQName) {
        Set<RObjectType> set = new HashSet<>();

        RObjectType type = ClassMapper.getHQLTypeForQName(typeQName);
        set.add(type);

        switch (type) {
            case OBJECT:
                set.addAll(Arrays.asList(RObjectType.values()));
                break;
            case FOCUS:
                set.add(RObjectType.USER);
            case ABSTRACT_ROLE:
                set.add(RObjectType.ROLE);
                set.add(RObjectType.ORG);
                break;
            default:
        }

        return set;
    }

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        if (filter instanceof TypeFilter) {
            return true;
        }
        return false;
    }

    @Override
    public Restriction cloneInstance() {
        return new TypeRestriction();
    }
}
