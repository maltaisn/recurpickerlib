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

import static org.junit.Assert.assertEquals;

public class RecurSettingsTest {

    @Test
    public void recur_weekly_all_days_freq1() {
        Recurrence r = new Recurrence(System.currentTimeMillis(), Recurrence.WEEKLY)
                .setFrequency(1)
                .setWeeklySetting(Recurrence.EVERY_DAY_OF_WEEK);
        assertEquals(r.getPeriod(), Recurrence.DAILY);
        assertEquals(r.getDaySetting(), 0);
    }

    @Test
    public void recur_weekly_all_days_freq2() {
        Recurrence r = new Recurrence(System.currentTimeMillis(), Recurrence.WEEKLY)
                .setFrequency(2)
                .setWeeklySetting(Recurrence.EVERY_DAY_OF_WEEK);
        assertEquals(r.getPeriod(), Recurrence.WEEKLY);
        assertEquals(r.getDaySetting(), Recurrence.EVERY_DAY_OF_WEEK);
    }

    @Test
    public void recur_monthly_last_day_change_start() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 31).getTimeInMillis();
        Recurrence r = new Recurrence(startDate, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.LAST_DAY_OF_MONTH);

        assertEquals(r.getDaySetting(), Recurrence.LAST_DAY_OF_MONTH);

        long newDate = new GregorianCalendar(2018, Calendar.JANUARY, 1).getTimeInMillis();
        r.setStartDate(newDate);

        assertEquals(r.getDaySetting(), Recurrence.SAME_DAY_OF_MONTH);
    }

    @Test
    public void recur_monthly_last_day_wrong_start() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 1).getTimeInMillis();
        Recurrence r = new Recurrence(startDate, Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.LAST_DAY_OF_MONTH);

        assertEquals(r.getDaySetting(), Recurrence.SAME_DAY_OF_MONTH);

        long newDate = new GregorianCalendar(2018, Calendar.JANUARY, 31).getTimeInMillis();
        r.setStartDate(newDate).setMonthlySetting(Recurrence.LAST_DAY_OF_MONTH);

        assertEquals(r.getDaySetting(), Recurrence.LAST_DAY_OF_MONTH);
    }

    @Test
    public void recur_end_date_on_start_date() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 1).getTimeInMillis();
        Recurrence r = new Recurrence(startDate, Recurrence.DAILY)
                .setEndByDate(startDate);

        assertEquals(r.getPeriod(), Recurrence.NONE);
    }

    @Test
    public void recur_start_date_on_end_date() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 1).getTimeInMillis();
        long endDate = new GregorianCalendar(2018, Calendar.JANUARY, 2).getTimeInMillis();
        Recurrence r = new Recurrence(startDate, Recurrence.DAILY)
                .setEndByDate(endDate).setStartDate(endDate);

        assertEquals(r.getPeriod(), Recurrence.NONE);
    }

    @Test
    public void recur_end_by_count_0() {
        long startDate = new GregorianCalendar(2018, Calendar.JANUARY, 1).getTimeInMillis();
        Recurrence r = new Recurrence(startDate, Recurrence.DAILY)
                .setEndByCount(0);

        assertEquals(r.getPeriod(), Recurrence.NONE);
    }

}
