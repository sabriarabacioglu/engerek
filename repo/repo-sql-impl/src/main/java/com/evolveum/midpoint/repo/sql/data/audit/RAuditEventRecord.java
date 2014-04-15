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

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = RAuditEventRecord.TABLE_NAME)
public class RAuditEventRecord implements Serializable {

    public static final String TABLE_NAME = "m_audit_event";
    public static final String COLUMN_TIMESTAMP = "timestampValue";

    private long id;
    private Timestamp timestamp;
    private String eventIdentifier;
    private String sessionIdentifier;
    private String taskIdentifier;
    private String taskOID;
    private String hostIdentifier;

    //prism object - user
    private String initiatorOid;
    private String initiatorName;
    //prism object
    private String targetOid;
    private String targetName;
    private RObjectType targetType;
    //prism object - user
    private String targetOwnerOid;
    private String targetOwnerName;

    private RAuditEventType eventType;
    private RAuditEventStage eventStage;

    //collection of object deltas
    private Set<RObjectDeltaOperation> deltas;
    private String channel;
    private ROperationResultStatus outcome;
    private String parameter;
    private String message;

    private String result;

    public String getResult() {
        return result;
    }
    @Column(length = 1024)
    public String getMessage() {
        return message;
    }

    public String getParameter() {
        return parameter;
    }

    public String getChannel() {
        return channel;
    }

    @ForeignKey(name = "fk_audit_delta")
    @OneToMany(mappedBy = "record", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectDeltaOperation> getDeltas() {
        if (deltas == null) {
            deltas = new HashSet<RObjectDeltaOperation>();
        }
        return deltas;
    }

    public String getEventIdentifier() {
        return eventIdentifier;
    }

    @Enumerated(EnumType.ORDINAL)
    public RAuditEventStage getEventStage() {
        return eventStage;
    }

    @Enumerated(EnumType.ORDINAL)
    public RAuditEventType getEventType() {
        return eventType;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getInitiatorOid() {
        return initiatorOid;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getOutcome() {
        return outcome;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    public String getTargetName() {
        return targetName;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getTargetOid() {
        return targetOid;
    }

    @Enumerated(EnumType.ORDINAL)
    public RObjectType getTargetType() {
        return targetType;
    }

    public String getTargetOwnerName() {
        return targetOwnerName;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getTargetOwnerOid() {
        return targetOwnerOid;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    public String getTaskOID() {
        return taskOID;
    }

    @Column(name = COLUMN_TIMESTAMP)
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setDeltas(Set<RObjectDeltaOperation> deltas) {
        this.deltas = deltas;
    }

    public void setEventIdentifier(String eventIdentifier) {
        this.eventIdentifier = eventIdentifier;
    }

    public void setEventStage(RAuditEventStage eventStage) {
        this.eventStage = eventStage;
    }

    public void setEventType(RAuditEventType eventType) {
        this.eventType = eventType;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public void setInitiatorOid(String initiatorOid) {
        this.initiatorOid = initiatorOid;
    }

    public void setOutcome(ROperationResultStatus outcome) {
        this.outcome = outcome;
    }

    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setTargetType(RObjectType targetType) {
        this.targetType = targetType;
    }

    public void setTargetOwnerName(String targetOwnerName) {
        this.targetOwnerName = targetOwnerName;
    }

    public void setTargetOwnerOid(String targetOwnerOid) {
        this.targetOwnerOid = targetOwnerOid;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public void setTaskOID(String taskOID) {
        this.taskOID = taskOID;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAuditEventRecord that = (RAuditEventRecord) o;

        if (channel != null ? !channel.equals(that.channel) : that.channel != null) return false;
        if (deltas != null ? !deltas.equals(that.deltas) : that.deltas != null) return false;
        if (eventIdentifier != null ? !eventIdentifier.equals(that.eventIdentifier) : that.eventIdentifier != null)
            return false;
        if (eventStage != that.eventStage) return false;
        if (eventType != that.eventType) return false;
        if (hostIdentifier != null ? !hostIdentifier.equals(that.hostIdentifier) : that.hostIdentifier != null)
            return false;
        if (initiatorOid != null ? !initiatorOid.equals(that.initiatorOid) : that.initiatorOid != null) return false;
        if (initiatorName != null ? !initiatorName.equals(that.initiatorName) : that.initiatorName != null)
            return false;
        if (outcome != that.outcome) return false;
        if (sessionIdentifier != null ? !sessionIdentifier.equals(that.sessionIdentifier) : that.sessionIdentifier != null)
            return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null) return false;
        if (targetName != null ? !targetName.equals(that.targetName) : that.targetName != null) return false;
        if (targetType != null ? !targetType.equals(that.targetType) : that.targetType != null) return false;
        if (targetOwnerOid != null ? !targetOwnerOid.equals(that.targetOwnerOid) : that.targetOwnerOid != null)
            return false;
        if (targetOwnerName != null ? !targetOwnerName.equals(that.targetOwnerName) : that.targetOwnerName != null)
            return false;
        if (taskIdentifier != null ? !taskIdentifier.equals(that.taskIdentifier) : that.taskIdentifier != null)
            return false;
        if (taskOID != null ? !taskOID.equals(that.taskOID) : that.taskOID != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (parameter != null ? !parameter.equals(that.parameter) : that.parameter != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (eventIdentifier != null ? eventIdentifier.hashCode() : 0);
        result = 31 * result + (sessionIdentifier != null ? sessionIdentifier.hashCode() : 0);
        result = 31 * result + (taskIdentifier != null ? taskIdentifier.hashCode() : 0);
        result = 31 * result + (taskOID != null ? taskOID.hashCode() : 0);
        result = 31 * result + (hostIdentifier != null ? hostIdentifier.hashCode() : 0);
        result = 31 * result + (initiatorName != null ? initiatorName.hashCode() : 0);
        result = 31 * result + (initiatorOid != null ? initiatorOid.hashCode() : 0);
        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        result = 31 * result + (targetType != null ? targetType.hashCode() : 0);
        result = 31 * result + (targetOwnerOid != null ? targetOwnerOid.hashCode() : 0);
        result = 31 * result + (targetOwnerName != null ? targetOwnerName.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (eventStage != null ? eventStage.hashCode() : 0);
        result = 31 * result + (deltas != null ? deltas.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (outcome != null ? outcome.hashCode() : 0);
        result = 31 * result + (parameter != null ? parameter.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (this.result != null ? this.result.hashCode() : 0);
        return result;
    }

    public static RAuditEventRecord toRepo(AuditEventRecord record, PrismContext prismContext)
            throws DtoTranslationException {

        Validate.notNull(record, "Audit event record must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        RAuditEventRecord repo = new RAuditEventRecord();
        repo.setChannel(record.getChannel());
        if (record.getTimestamp() != null) {
            repo.setTimestamp(new Timestamp(record.getTimestamp()));
        }
        repo.setEventStage(RAuditEventStage.toRepo(record.getEventStage()));
        repo.setEventType(RAuditEventType.toRepo(record.getEventType()));
        repo.setSessionIdentifier(record.getSessionIdentifier());
        repo.setEventIdentifier(record.getEventIdentifier());
        repo.setHostIdentifier(record.getHostIdentifier());
        repo.setParameter(record.getParameter());
        repo.setMessage(trimMessage(record.getMessage()));
        if (record.getOutcome() != null) {
            repo.setOutcome(RUtil.getRepoEnumValue(record.getOutcome().createStatusType(), ROperationResultStatus.class));
        }
        repo.setTaskIdentifier(record.getTaskIdentifier());
        repo.setTaskOID(record.getTaskOID());
        repo.setResult(record.getResult());

        try {
            if (record.getTarget() != null) {
                PrismObject target = record.getTarget();
                repo.setTargetName(getOrigName(target));
                repo.setTargetOid(target.getOid());

                QName type = ObjectTypes.getObjectType(target.getCompileTimeClass()).getTypeQName();
                repo.setTargetType(ClassMapper.getHQLTypeForQName(type));
            }
            if (record.getTargetOwner() != null) {
                PrismObject targetOwner = record.getTargetOwner();
                repo.setTargetOwnerName(getOrigName(targetOwner));
                repo.setTargetOwnerOid(targetOwner.getOid());
            }
            if (record.getInitiator() != null) {
                PrismObject<UserType> initiator = record.getInitiator();
                repo.setInitiatorName(getOrigName(initiator));
                repo.setInitiatorOid(initiator.getOid());
            }

            for (ObjectDeltaOperation<?> delta : record.getDeltas()) {
                if (delta == null) {
                    continue;
                }
                RObjectDeltaOperation rDelta = RObjectDeltaOperation.toRepo(repo, delta, prismContext);
                rDelta.setRecord(repo);
                repo.getDeltas().add(rDelta);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return repo;
    }

	private static String trimMessage(String message) {
		if (message == null || message.length() <= AuditService.MAX_MESSAGE_SIZE) {
			return message;
		}
		return message.substring(0, AuditService.MAX_MESSAGE_SIZE - 4) + "...";
	}

	private static String getOrigName(PrismObject object) {
        PolyString name = (PolyString) object.getPropertyRealValue(ObjectType.F_NAME, PolyString.class);
        return name != null ? name.getOrig() : null;
    }
}
