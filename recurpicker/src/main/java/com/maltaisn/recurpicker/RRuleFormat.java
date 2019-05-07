/*
 * Copyright (c) 2019 Nicolas Maltais
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.maltaisn.recurpicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Class used to convert a recurrence to a RRule.
 * See <a href="https://tools.ietf.org/html/rfc5545">RFC5545 standard</a>.
 */
public class RRuleFormat {

    private static final String[] RRULE_BYDAY_VALUES = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};

    private static final SimpleDateFormat RRULE_DATE_FORMAT =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);

    /**
     * Convert the recurrence to a RFC 5545 string rule
     * If recurrence does not repeat, null will be returned because RRule doesn't support it.
     * @return the RRule string
     */
    public static String format(Recurrence r) {
        if (r.getPeriod() == Recurrence.NONE) {
            return null;
        }

        StringBuilder rule = new StringBuilder();

        // Start date
        rule.append("DTSTART=");
        rule.append(RRULE_DATE_FORMAT.format(r.startDate.getTime()));
        rule.append(';');

        // Period
        rule.append("FREQ=");
        switch (r.getPeriod()) {
            case Recurrence.NONE:
            case Recurrence.DAILY:
                rule.append("DAILY");
                break;
            case Recurrence.WEEKLY:
                rule.append("WEEKLY");
                break;
            case Recurrence.MONTHLY:
                rule.append("MONTHLY");
                break;
            case Recurrence.YEARLY:
                rule.append("YEARLY");
                break;
        }

        // Frequency
        rule.append(";INTERVAL=");
        rule.append(r.getFrequency());
        rule.append(';');

        // Day setting
        switch (r.getPeriod()) {
            case Recurrence.NONE:
            case Recurrence.DAILY:
                break;
            case Recurrence.WEEKLY:
                rule.append("BYDAY=");
                for (int i = 0; i < 7; i++) {
                    if (r.isRepeatedOnDaysOfWeek(1 << (i + 1))) {
                        rule.append(RRULE_BYDAY_VALUES[i]);
                        rule.append(',');
                    }
                }
                rule.deleteCharAt(rule.length() - 1); // Delete extra ","
                rule.append(';');
                break;
            case Recurrence.MONTHLY:
                switch (r.getDaySetting()) {
                    case Recurrence.SAME_DAY_OF_MONTH:
                        rule.append("BYMONTHDAY=");
                        rule.append(r.startDate.get(Calendar.DAY_OF_MONTH));
                        break;
                    case Recurrence.SAME_DAY_OF_WEEK:
                        rule.append("BYSETPOS=");
                        int week = r.startDate.get(Calendar.DAY_OF_WEEK_IN_MONTH);
                        if (week == 5) {
                            rule.append("-1");
                        } else {
                            rule.append(week);
                        }
                        rule.append(";BYDAY=");
                        int dayOfWeek = r.startDate.get(Calendar.DAY_OF_WEEK);
                        rule.append(RRULE_BYDAY_VALUES[dayOfWeek - 1]);
                        break;
                    case Recurrence.LAST_DAY_OF_MONTH:
                        rule.append("BYMONTHDAY=-1");
                        break;
                }
                rule.append(';');
                break;
            case Recurrence.YEARLY:
                rule.append("BYMONTH=");
                rule.append(r.startDate.get(Calendar.MONTH) + 1);
                rule.append(";BYMONTHDAY=");
                rule.append(r.startDate.get(Calendar.DAY_OF_MONTH));
                rule.append(';');
                break;
        }

        // End type
        switch (r.getEndType()) {
            case Recurrence.END_NEVER:
                break;
            case Recurrence.END_BY_DATE:
                rule.append("UNTIL=");
                rule.append(RRULE_DATE_FORMAT.format(r.endDate.getTime()));
                rule.append(';');
                break;
            case Recurrence.END_BY_COUNT:
                rule.append("COUNT=");
                rule.append(r.getEndCount());
                rule.append(';');
                break;
        }

        rule.deleteCharAt(rule.length() - 1); // Delete extra ";"

        return rule.toString();
    }

}
