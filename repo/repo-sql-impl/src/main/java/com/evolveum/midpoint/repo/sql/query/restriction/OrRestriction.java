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

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class OrRestriction extends NaryLogicalRestriction<OrFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        return (filter instanceof OrFilter);
    }

    @Override
    public Criterion interpret(OrFilter filter)
            throws QueryException {

        validateFilter(filter);

        Disjunction disjunction = Restrictions.disjunction();
        updateJunction(filter.getConditions(), disjunction);

        return disjunction;
    }

    @Override
    public OrRestriction cloneInstance() {
        return new OrRestriction();
    }
}
