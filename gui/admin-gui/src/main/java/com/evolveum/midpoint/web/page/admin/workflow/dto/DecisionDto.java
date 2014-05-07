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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DecisionType;

import java.util.Date;

/**
 * @author lazyman
 */
public class DecisionDto extends Selectable {

    public static final String F_USER = "user";
    public static final String F_RESULT = "result";
    public static final String F_COMMENT = "comment";
    public static final String F_TIME = "time";

    private String user;
    private String result;
    private String comment;
    private Date time;

    public DecisionDto(DecisionType decision) {
        if (decision.getApprover() != null && decision.getApprover().getName() != null) {
            this.user = decision.getApprover().getName().getOrig();
        } else if (decision.getApproverName() != null) {
            this.user = decision.getApproverName();
        } else {
            this.user = decision.getApproverRef().getOid();
        }
        if (Boolean.TRUE.equals(decision.isApproved())) {
            this.result = "APPROVED";
        } else if (Boolean.FALSE.equals(decision.isApproved())) {
            this.result = "REJECTED";     // todo i18n
        } else {
            this.result = "-";
        }
        this.comment = decision.getComment();
        this.time = XmlTypeConverter.toDate(decision.getDateTime());
    }

    public String getTime() {
        return time.toLocaleString();      // todo formatting
    }

    public String getUser() {
        return user;
    }

    public String getResult() {
        return result;
    }

    public String getComment() {
        return comment;
    }
}
