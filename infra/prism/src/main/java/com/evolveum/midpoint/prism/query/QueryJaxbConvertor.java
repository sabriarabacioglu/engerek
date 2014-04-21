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

package com.evolveum.midpoint.prism.query;

import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.query_2.SearchFilterType;

/**
 * This is mostly legacy converter between JAXB/DOM representation of queries and filter and the native prism
 * representation. It is used only by the code that has to deal with JAXB/DOM (such as JAX-WS web service).
 * 
 * @author Katka Valalikova
 * @author Radovan Semancik
 *
 */
public class QueryJaxbConvertor {

    public static <O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, QueryType queryType, PrismContext prismContext)
            throws SchemaException {
        if (queryType == null) {
            return null;
        }
        return createObjectQueryInternal(clazz, queryType.getFilter(), queryType.getPaging(), prismContext);
    }

    public static <O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, SearchFilterType filterType, PrismContext prismContext)
            throws SchemaException {
        return createObjectQueryInternal(clazz, filterType, null, prismContext);
    }

    public static <O extends Objectable> ObjectFilter createObjectFilter(Class<O> clazz, SearchFilterType filterType, PrismContext prismContext)
            throws SchemaException {
        ObjectQuery query = createObjectQueryInternal(clazz, filterType, null, prismContext);
        if (query == null) {
            return null;
        } else {
            return query.getFilter();
        }
    }

    public static <O extends Objectable> ObjectQuery createObjectQueryInternal(Class<O> clazz, SearchFilterType filterType, PagingType pagingType, PrismContext prismContext)
			throws SchemaException {

        boolean filterNotEmpty = filterType != null && filterType.containsFilterClause();
		if (!filterNotEmpty && pagingType == null) {
			return null;
		}
		
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);

		if (objDef == null) {
			throw new SchemaException("cannot find obj definition for class "+clazz);
		}

		try {
			ObjectQuery query = new ObjectQuery();
			
			if (filterNotEmpty) {
				MapXNode rootFilter = filterType.getFilterClauseXNode(prismContext);
				ObjectFilter filter = QueryConvertor.parseFilter(rootFilter, objDef);
				query.setFilter(filter);
			}

			if (pagingType != null) {
				ObjectPaging paging = PagingConvertor.createObjectPaging(pagingType);
				query.setPaging(paging);
			}
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}
	
    public static QueryType createQueryType(ObjectQuery query, PrismContext prismContext) throws SchemaException{

		ObjectFilter filter = query.getFilter();
		QueryType queryType = new QueryType();
		if (filter != null){
			SearchFilterType filterType = new SearchFilterType();
            MapXNode filterXNode = QueryConvertor.serializeFilter(filter, prismContext);
			filterType.setFilterClauseXNode(filterXNode);
			queryType.setFilter(filterType);
		}
				
		queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
		return queryType;

	}

}
