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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskPersistenceStatus;
import com.evolveum.midpoint.task.api.TaskRecurrence;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskWaitingReason;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UriStack;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class SimpleTaskAdapter implements Task {

    @Override
    public void addDependent(String taskIdentifier) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isAsynchronous() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public TaskExecutionStatus getExecutionStatus() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeWaiting() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeRunnable() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setInitialExecutionStatus(TaskExecutionStatus value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public TaskPersistenceStatus getPersistenceStatus() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isTransient() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isPersistent() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public TaskRecurrence getRecurrenceStatus() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isSingle() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isCycle() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public ScheduleType getSchedule() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public TaskBinding getBinding() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isTightlyBound() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isLooselyBound() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setBinding(TaskBinding value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setBindingImmediate(TaskBinding value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getHandlerUri() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setHandlerUri(String value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setHandlerUriImmediate(String value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public UriStack getOtherHandlersUriStack() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getTaskIdentifier() {
        return null;
    }

    @Override
    public PrismObject<UserType> getOwner() {
        return null;
    }

    @Override
    public void setOwner(PrismObject<UserType> owner) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getChannel() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setChannel(String channelUri) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public PrismObject<UserType> getRequestee() {
        return null;
    }

    @Override
    public void setRequesteeTransient(PrismObject<UserType> user) {

    }

    @Override
    public String getOid() {
        return null;
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public ObjectReferenceType getObjectRef() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setObjectRef(ObjectReferenceType objectRef) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setObjectRef(String oid, QName type) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setObjectTransient(PrismObject object) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getObjectOid() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public OperationResult getResult() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setResult(OperationResult result) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setResultImmediate(OperationResult result, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Long getLastRunStartTimestamp() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Long getLastRunFinishTimestamp() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Long getNextRunStartTime(OperationResult parentResult) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public PolyStringType getName() {
        return null;
    }

    @Override
    public void setName(PolyStringType value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setName(String value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setNameImmediate(PolyStringType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public <C extends Containerable> PrismContainer<C> getExtension() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public <T> PrismProperty<T> getExtensionProperty(QName propertyName) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public <T extends PrismValue> Item<T> getExtensionItem(QName propertyName) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public <C extends Containerable> void setExtensionContainer(PrismContainer<C> item) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setExtensionReference(PrismReference reference) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setExtensionProperty(PrismProperty<?> property) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setExtensionPropertyImmediate(PrismProperty<?> property, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void addExtensionProperty(PrismProperty<?> property) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public <T> void setExtensionPropertyValue(QName propertyName, T value) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public <T extends Containerable> void setExtensionContainerValue(QName containerName, T value)
            throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setExtensionItem(Item item) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void modifyExtension(ItemDelta itemDelta) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public long getProgress() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setProgress(long value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setProgressImmediate(long progress, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public PrismObject<TaskType> getTaskPrismObject() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void refresh(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String debugDump() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String debugDump(int indent) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean canRun() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void savePendingModifications(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getCategory() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeRecurringSimple(int interval) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeRecurringCron(String cronLikeSpecification) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeSingle() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getNode() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public OperationResultStatusType getResultStatus() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public ThreadStopActionType getThreadStopAction() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isResilient() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setCategory(String category) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setDescriptionImmediate(String value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setDescription(String value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void deleteExtensionProperty(PrismProperty<?> property) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setThreadStopAction(ThreadStopActionType value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeRecurring(ScheduleType schedule) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeSingle(ScheduleType schedule) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Task createSubtask() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Deprecated
    @Override
    public TaskRunResult waitForSubtasks(Integer interval, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Deprecated
    @Override
    public TaskRunResult waitForSubtasks(Integer interval, Collection<ItemDelta<?>> extensionDeltas,
                                         OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public String getParent() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding,
                               Collection<ItemDelta<?>> extensionDeltas) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void pushHandlerUri(String uri, ScheduleType schedule, TaskBinding binding, ItemDelta<?> delta) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void finishHandler(OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public List<Task> listSubtasks(OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public List<Task> listPrerequisiteTasks(OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void startWaitingForTasksImmediate(OperationResult result) throws SchemaException, ObjectNotFoundException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public List<String> getDependents() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void deleteDependent(String value) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public List<Task> listDependents(OperationResult result) throws SchemaException, ObjectNotFoundException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Task getParentTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public TaskWaitingReason getWaitingReason() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void makeWaiting(TaskWaitingReason reason) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void pushWaitForTasksHandlerUri() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Long getCompletionTimestamp() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void setObjectRefImmediate(ObjectReferenceType value, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public PrismReference getExtensionReference(QName propertyName) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public void addExtensionReference(PrismReference reference) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public List<Task> listSubtasksDeeply(OperationResult result) throws SchemaException {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Collection<ItemDelta<?>> getPendingModifications() {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public Long getExpectedTotal() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setExpectedTotal(Long value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setExpectedTotalImmediate(Long value, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
