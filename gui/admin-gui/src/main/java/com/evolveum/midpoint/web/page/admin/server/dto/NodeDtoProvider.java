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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.apache.wicket.Component;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class NodeDtoProvider extends BaseSortableDataProvider<NodeDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(NodeDtoProvider.class);
    private static final String DOT_CLASS = NodeDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_NODES = DOT_CLASS + "listNodes";
    private static final String OPERATION_COUNT_NODES = DOT_CLASS + "countNodes";

    public NodeDtoProvider(Component component) {
        super(component);
    }

    @Override
    public Iterator<? extends NodeDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_NODES);
        Task task = getTaskManager().createTaskInstance(OPERATION_LIST_NODES);
        try {
        	ObjectPaging paging = createPaging(first, count);
        	ObjectQuery query = getQuery();
        	if (query == null) {
        		query = new ObjectQuery();
        	}
        	query.setPaging(paging);

            List<PrismObject<NodeType>> nodes = getModel().searchObjects(NodeType.class, query, null, task, result);

            for (PrismObject<NodeType> node : nodes) {
                getAvailableData().add(new NodeDto(node.asObjectable()));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing nodes", ex);
            result.recordFatalError("Couldn't list nodes.", ex);
        }

        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_NODES);
        Task task = getTaskManager().createTaskInstance(OPERATION_COUNT_NODES);
        try {
            count = getModel().countObjects(NodeType.class, getQuery(), null, task, result);
            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when counting nodes", ex);
            result.recordFatalError("Couldn't count nodes.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return count;
    }
}
