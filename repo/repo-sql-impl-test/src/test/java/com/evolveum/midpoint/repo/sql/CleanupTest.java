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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventRecord;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.SimpleTaskAdapter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CleanupTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(CleanupTest.class);

    private Calendar create_2013_07_12_12_00_Calendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.MAY);
        calendar.set(Calendar.DAY_OF_MONTH, 7);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Duration createDuration(Calendar when, long now) throws Exception {
        long seconds = (now - when.getTimeInMillis()) / 1000;
        return DatatypeFactory.newInstance().newDuration("PT" + seconds + "S").negate();
    }

    private CleanupPolicyType createPolicy(Calendar when, long now) throws Exception {
        CleanupPolicyType policy = new CleanupPolicyType();

        Duration duration = createDuration(when, now);
        policy.setMaxAge(duration);

        return policy;
    }

    @Test
    public void testAuditCleanup() throws Exception {
        //GIVEN
        Calendar calendar = create_2013_07_12_12_00_Calendar();
        for (int i = 0; i < 3; i++) {
            long timestamp = calendar.getTimeInMillis();
            AuditEventRecord record = new AuditEventRecord();
            record.addDelta(createObjectDeltaOperation(i));
            record.setTimestamp(timestamp);
            LOGGER.info("Adding audit record with timestamp {}", new Object[]{new Date(timestamp)});

            auditService.audit(record, new SimpleTaskAdapter());
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        Session session = getFactory().openSession();
        try {
            session.beginTransaction();

            Query query = session.createQuery("select count(*) from " + RAuditEventRecord.class.getSimpleName());
            Long count = (Long) query.uniqueResult();

            AssertJUnit.assertEquals(3L, (long) count);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        //WHEN
        calendar = create_2013_07_12_12_00_Calendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.add(Calendar.MINUTE, 1);

        final long NOW = System.currentTimeMillis();
        CleanupPolicyType policy = createPolicy(calendar, NOW);

        OperationResult result = new OperationResult("Cleanup audit");
        auditService.cleanupAudit(policy, result);
        result.recomputeStatus();

        //THEN
        AssertJUnit.assertTrue(result.isSuccess());

        session = getFactory().openSession();
        try {
            session.beginTransaction();

            Query query = session.createQuery("from " + RAuditEventRecord.class.getSimpleName());
            List<RAuditEventRecord> records = query.list();

            AssertJUnit.assertEquals(1, records.size());
            RAuditEventRecord record = records.get(0);

            Date finished = new Date(record.getTimestamp().getTime());

            Date mark = new Date(NOW);
            Duration duration = policy.getMaxAge();
            duration.addTo(mark);

            AssertJUnit.assertTrue("finished: " + finished + ", mark: " + mark, finished.after(mark));

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    private ObjectDeltaOperation createObjectDeltaOperation(int i) throws Exception {
        ObjectDeltaOperation delta = new ObjectDeltaOperation();
        delta.setExecutionResult(new OperationResult("asdf"));
        UserType user = new UserType();
        prismContext.adopt(user);
        PolyStringType name = new PolyStringType();
        name.setOrig("a" + i);
        name.setNorm("a" + i);
        user.setName(name);

        delta.setObjectDelta(ObjectDelta.createAddDelta(user.asPrismObject()));

        return delta;
    }
}
