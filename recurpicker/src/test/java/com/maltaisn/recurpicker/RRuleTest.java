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

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static junit.framework.Assert.assertEquals;

public class RRuleTest {

    @Test
    public void rrule_daily() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 15, 13, 25, 36).getTimeInMillis();

        Recurrence r1 = new Recurrence(startDate, Recurrence.DAILY).setFrequency(1);
        assertEquals("DTSTART=20180115T132536;FREQ=DAILY;INTERVAL=1", RRuleFormat.format(r1));

        Recurrence r2 = new Recurrence(startDate, Recurrence.DAILY).setFrequency(3);
        assertEquals("DTSTART=20180115T132536;FREQ=DAILY;INTERVAL=3", RRuleFormat.format(r2));

        Recurrence r3 = new Recurrence(startDate, Recurrence.DAILY).setFrequency(5);
        assertEquals("DTSTART=20180115T132536;FREQ=DAILY;INTERVAL=5", RRuleFormat.format(r3));
    }

    @Test
    public void rrule_weekly() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis();

        Recurrence r1 = new Recurrence(startDate, Recurrence.WEEKLY)
                .setWeeklySetting(Recurrence.SUNDAY | Recurrence.MONDAY | Recurrence.FRIDAY);
        assertEquals("DTSTART=20180101T000000;FREQ=WEEKLY;INTERVAL=1;BYDAY=SU,MO,FR", RRuleFormat.format(r1));

        Recurrence r2 = new Recurrence(startDate, Recurrence.WEEKLY)
                .setWeeklySetting(Recurrence.SATURDAY | Recurrence.WEDNESDAY | Recurrence.TUESDAY);
        assertEquals("DTSTART=20180101T000000;FREQ=WEEKLY;INTERVAL=1;BYDAY=TU,WE,SA", RRuleFormat.format(r2));
    }

    @Test
    public void rrule_monthly() {
        long date1 = new GregorianCalendar(2018, Calendar.JANUARY, 18, 0, 0, 0).getTimeInMillis();
        long date2 = new GregorianCalendar(2018, Calendar.MARCH, 31, 0, 0, 0).getTimeInMillis();

        // Same day of month
        Recurrence r1 = new Recurrence(date1, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_MONTH);
        assertEquals("DTSTART=20180118T000000;FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=18", RRuleFormat.format(r1));

        Recurrence r2 = new Recurrence(date2, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_MONTH);
        assertEquals("DTSTART=20180331T000000;FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=31", RRuleFormat.format(r2));

        // Same day of week
        Recurrence r3 = new Recurrence(date1, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_WEEK);
        assertEquals("DTSTART=20180118T000000;FREQ=MONTHLY;INTERVAL=1;BYSETPOS=3;BYDAY=TH", RRuleFormat.format(r3));

        Recurrence r4 = new Recurrence(date2, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_WEEK);
        assertEquals("DTSTART=20180331T000000;FREQ=MONTHLY;INTERVAL=1;BYSETPOS=-1;BYDAY=SA", RRuleFormat.format(r4));

        // Last day of month
        Recurrence r5 = new Recurrence(date2, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.LAST_DAY_OF_MONTH);
        assertEquals("DTSTART=20180331T000000;FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=-1", RRuleFormat.format(r5));
    }

    @Test
    public void rrule_yearly() {
        Recurrence r1 = new Recurrence(new GregorianCalendar(2018, Calendar.JANUARY,
                1, 0, 0, 0).getTimeInMillis(), Recurrence.YEARLY);
        assertEquals("DTSTART=20180101T000000;FREQ=YEARLY;INTERVAL=1;BYMONTH=1;BYMONTHDAY=1", RRuleFormat.format(r1));

        Recurrence r2 = new Recurrence(new GregorianCalendar(2018, Calendar.DECEMBER,
                29, 23, 59, 59).getTimeInMillis(), Recurrence.YEARLY);
        assertEquals("DTSTART=20181229T235959;FREQ=YEARLY;INTERVAL=1;BYMONTH=12;BYMONTHDAY=29", RRuleFormat.format(r2));
    }

    @Test
    public void rrule_end_count() {
        long startDate = new GregorianCalendar(2018, Calendar.DECEMBER, 29, 23, 59, 59).getTimeInMillis();

        Recurrence r1 = new Recurrence(startDate, Recurrence.DAILY).setEndByCount(10);
        assertEquals("DTSTART=20181229T235959;FREQ=DAILY;INTERVAL=1;COUNT=10", RRuleFormat.format(r1));

        Recurrence r2 = new Recurrence(startDate, Recurrence.DAILY).setEndByCount(1);
        assertEquals("DTSTART=20181229T235959;FREQ=DAILY;INTERVAL=1;COUNT=1", RRuleFormat.format(r2));
    }

    @Test
    public void rrule_end_date() {
        long startDate = new GregorianCalendar(2018, Calendar.DECEMBER, 29, 23, 59, 59).getTimeInMillis();

        Recurrence r1 = new Recurrence(startDate, Recurrence.DAILY).setEndByDate(new GregorianCalendar(
                2019, Calendar.MARCH, 31, 0, 0, 0).getTimeInMillis());
        assertEquals("DTSTART=20181229T235959;FREQ=DAILY;INTERVAL=1;UNTIL=20190331T000000", RRuleFormat.format(r1));

        Recurrence r2 = new Recurrence(startDate, Recurrence.DAILY).setEndByDate(new GregorianCalendar(
                2020, Calendar.DECEMBER, 29, 23, 59, 59).getTimeInMillis());
        assertEquals("DTSTART=20181229T235959;FREQ=DAILY;INTERVAL=1;UNTIL=20201229T235959", RRuleFormat.format(r2));
    }

}
