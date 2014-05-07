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
package com.evolveum.midpoint.audit.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class AuditEventRecord implements DebugDumpable {
	
	/**
	 * Timestamp in millis.
	 */
	private Long timestamp;
	
	/**
	 * Unique identification of the event.
	 */
	private String eventIdentifier;
	
	// session ID
	private String sessionIdentifier;
	
	// channel???? (e.g. web gui, web service, ...)
	
	// task ID (not OID!)
	private String taskIdentifier;
	private String taskOID;
	
	// host ID
	private String hostIdentifier;
	
	// initiator (subject, event "owner"): store OID, type(implicit?), name
	private PrismObject<UserType> initiator;

	// (primary) target (object, the thing acted on): store OID, type, name
	// OPTIONAL
	private PrismObject<? extends ObjectType> target;
	
	// user that the target "belongs to"????: store OID, name
	private PrismObject<UserType> targetOwner;
		
	// event type
	private AuditEventType eventType;
	
	// event stage (request, execution)
	private AuditEventStage eventStage;
	
	// delta
	private Collection<ObjectDeltaOperation<? extends ObjectType>> deltas;
	
	// delta order (primary, secondary)
	
	private String channel;
	
	// outcome (success, failure)
	private OperationResultStatus outcome;

	// result (e.g. number of entries, returned object, business result of workflow task or process instance - approved, rejected)
    private String result;

    private String parameter;

    private String message;

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	public AuditEventRecord() {
		this.deltas = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>();
	}
	
	public AuditEventRecord(AuditEventType eventType) {
		this.deltas = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>();
		this.eventType = eventType;
	}

	public AuditEventRecord(AuditEventType eventType, AuditEventStage eventStage) {
		this.deltas = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>();
		this.eventType = eventType;
		this.eventStage = eventStage;
	}

	public Long getTimestamp() {
		return timestamp;
	}
	
	public void clearTimestamp() {
		timestamp = null;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getEventIdentifier() {
		return eventIdentifier;
	}

	public void setEventIdentifier(String eventIdentifier) {
		this.eventIdentifier = eventIdentifier;
	}

	public String getSessionIdentifier() {
		return sessionIdentifier;
	}

	public void setSessionIdentifier(String sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}

	public String getTaskIdentifier() {
		return taskIdentifier;
	}

	public void setTaskIdentifier(String taskIdentifier) {
		this.taskIdentifier = taskIdentifier;
	}

	public String getTaskOID() {
		return taskOID;
	}

	public void setTaskOID(String taskOID) {
		this.taskOID = taskOID;
	}

	public String getHostIdentifier() {
		return hostIdentifier;
	}

	public void setHostIdentifier(String hostIdentifier) {
		this.hostIdentifier = hostIdentifier;
	}

	public PrismObject<UserType> getInitiator() {
		return initiator;
	}

	public void setInitiator(PrismObject<UserType> initiator) {
		this.initiator = initiator;
	}

	public PrismObject<? extends ObjectType> getTarget() {
		return target;
	}

	public void setTarget(PrismObject<? extends ObjectType> target) {
		this.target = target;
	}

	public PrismObject<UserType> getTargetOwner() {
		return targetOwner;
	}

	public void setTargetOwner(PrismObject<UserType> targetOwner) {
		this.targetOwner = targetOwner;
	}

	public AuditEventType getEventType() {
		return eventType;
	}

	public void setEventType(AuditEventType eventType) {
		this.eventType = eventType;
	}

	public AuditEventStage getEventStage() {
		return eventStage;
	}

	public void setEventStage(AuditEventStage eventStage) {
		this.eventStage = eventStage;
	}

	public Collection<ObjectDeltaOperation<? extends ObjectType>> getDeltas() {
		return deltas;
	}
	
	public void addDelta(ObjectDeltaOperation<? extends ObjectType> delta) {
		deltas.add(delta);
	}

	public void addDeltas(Collection<ObjectDeltaOperation<? extends ObjectType>> deltasToAdd) {
		deltas.addAll(deltasToAdd);
	}
	
	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public void clearDeltas() {
		deltas.clear();
	}

	public OperationResultStatus getOutcome() {
		return outcome;
	}

	public void setOutcome(OperationResultStatus outcome) {
		this.outcome = outcome;
	}
	
	public void setResult(String result) {
        this.result = result;
	}

    public String getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public void checkConsistence() {
		if (initiator != null) {
			initiator.checkConsistence();
		}
		if (target != null) {
			target.checkConsistence();
		}
		if (targetOwner != null) {
			targetOwner.checkConsistence();
		}
		if (deltas != null) {
			ObjectDeltaOperation.checkConsistence(deltas);
		}
//        //TODO: should this be here?
//        if (result != null && result.getStatus() != null) {
//            if (result.getStatus() != outcome) {
//                throw new IllegalStateException("Status in result (" + result.getStatus() + ") differs from outcome (" + outcome + ")");
//            }
//        }
	}
	
	public AuditEventRecord clone() {
		AuditEventRecord clone = new AuditEventRecord();
		clone.channel = this.channel;
		clone.deltas = MiscSchemaUtil.cloneObjectDeltaOperationCollection(this.deltas);
		clone.eventIdentifier = this.eventIdentifier;
		clone.eventStage = this.eventStage;
		clone.eventType = this.eventType;
		clone.hostIdentifier = this.hostIdentifier;
		clone.initiator = this.initiator;
		clone.outcome = this.outcome;
		clone.sessionIdentifier = this.sessionIdentifier;
		clone.target = this.target;
		clone.targetOwner = this.targetOwner;
		clone.taskIdentifier = this.taskIdentifier;
		clone.taskOID = this.taskOID;
		clone.timestamp = this.timestamp;
        clone.result = this.result;
        clone.parameter = this.parameter;
        clone.message = this.message;
		return clone;
	}

	@Override
	public String toString() {
		return "AUDIT[" + formatTimestamp(timestamp) + " eid=" + eventIdentifier
				+ " sid=" + sessionIdentifier + ", tid=" + taskIdentifier
				+ " toid=" + taskOID + ", hid=" + hostIdentifier + ", I=" + formatObject(initiator)
				+ ", T=" + formatObject(target) + ", TO=" + formatObject(targetOwner) + ", et=" + eventType
				+ ", es=" + eventStage + ", D=" + deltas + ", ch="+ channel +", o=" + outcome + ", r=" + result + ", p=" + parameter
                + ", m=" + message + "]";
	}

    private String formatResult(OperationResult result) {
        if (result == null || result.getReturns() == null || result.getReturns().isEmpty()) {
            return "nothing";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : result.getReturns().keySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key);
            sb.append("=");
            sb.append(result.getReturns().get(key));
        }
        return sb.toString();
    }

    private static String formatTimestamp(Long timestamp) {
		if (timestamp == null) {
			return "null";
		}
		return TIMESTAMP_FORMAT.format(new java.util.Date(timestamp));
	}
	
	private static String formatObject(PrismObject<? extends ObjectType> object) {
		if (object == null) {
			return "null";
		}
		return object.toString();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("AUDIT");
		sb.append("\n");
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Timestamp", formatTimestamp(timestamp), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Identifier", eventIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Session Identifier", sessionIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Task Identifier", taskIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Task OID", taskOID, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Host Identifier", hostIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Initiator", formatObject(initiator), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Target", formatObject(target), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Target Owner", formatObject(targetOwner), indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Type", eventType, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Stage", eventStage, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Channel", channel, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Outcome", outcome, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Result", result, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Parameter", parameter, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Message", message, indent + 1);
		DebugUtil.debugDumpLabel(sb, "Deltas", indent + 1);
		if (deltas == null || deltas.isEmpty()) {
			sb.append(" none");
		} else {
			sb.append(" ").append(deltas.size()).append(" deltas\n");
			DebugUtil.debugDump(sb, deltas, indent + 2, false);
		}
		return sb.toString();
	}
}
