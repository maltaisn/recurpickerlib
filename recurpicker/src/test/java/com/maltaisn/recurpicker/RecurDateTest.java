/*
 * Copyright (c) Nicolas Maltais 2018
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class RecurDateTest {

    private static Calendar calendar = Calendar.getInstance();

    @Test
    public void recur_date_daily() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 2),
                getDate(2018, Calendar.JANUARY, 3),
                getDate(2018, Calendar.JANUARY, 4),
                getDate(2018, Calendar.JANUARY, 5),
                getDate(2018, Calendar.JANUARY, 6)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.DAILY);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_daily_freq3() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 30),
                getDate(2018, Calendar.FEBRUARY, 2),
                getDate(2018, Calendar.FEBRUARY, 5),
                getDate(2018, Calendar.FEBRUARY, 8)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 27), Recurrence.DAILY)
                .setFrequency(3);

        List<Long> actual1 = r.findRecurrences(-1, 4);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_daily_end_date() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 2),
                getDate(2018, Calendar.JANUARY, 3),
                getDate(2018, Calendar.JANUARY, 4)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.DAILY)
                .setEndByDate(expected.get(2));

        List<Long> actual1 = r.findRecurrences(-1, 100);
        assertEquals(expected, actual1);

        // If test is passed, same applies to other periods because same mechanism is used to detect end date
    }

    @Test
    public void recur_date_daily_end_count() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 2),
                getDate(2018, Calendar.JANUARY, 3),
                getDate(2018, Calendar.JANUARY, 4)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.DAILY)
                .setEndByCount(3);

        List<Long> actual1 = r.findRecurrences(-1, 100);
        assertEquals(expected, actual1);

        // If test is passed, same applies to other periods because same mechanism is used to detect end count
    }

    @Test
    public void recur_date_weekly() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 8),
                getDate(2018, Calendar.JANUARY, 15),
                getDate(2018, Calendar.JANUARY, 22),
                getDate(2018, Calendar.JANUARY, 29),
                getDate(2018, Calendar.FEBRUARY, 5),
                getDate(2018, Calendar.FEBRUARY, 12)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.WEEKLY);

        List<Long> actual1 = r.findRecurrences(-1, 6);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_weekly_freq3() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 22),
                getDate(2018, Calendar.FEBRUARY, 12),
                getDate(2018, Calendar.MARCH, 5),
                getDate(2018, Calendar.MARCH, 26),
                getDate(2018, Calendar.APRIL, 16)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.WEEKLY)
                .setFrequency(3);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_weekly_diff_day() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 7),
                getDate(2018, Calendar.JANUARY, 14),
                getDate(2018, Calendar.JANUARY, 21),
                getDate(2018, Calendar.JANUARY, 28)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.WEEKLY)
                .setWeeklySetting(Recurrence.SUNDAY);

        List<Long> actual1 = r.findRecurrences(-1, 4);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_weekly_many_days() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.JANUARY, 3),
                getDate(2018, Calendar.JANUARY, 5),
                getDate(2018, Calendar.JANUARY, 7),
                getDate(2018, Calendar.JANUARY, 10),
                getDate(2018, Calendar.JANUARY, 12)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.WEEKLY)
                .setWeeklySetting(Recurrence.SUNDAY | Recurrence.FRIDAY | Recurrence.WEDNESDAY);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_monthly() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.FEBRUARY, 1),
                getDate(2018, Calendar.MARCH, 1),
                getDate(2018, Calendar.APRIL, 1),
                getDate(2018, Calendar.MAY, 1),
                getDate(2018, Calendar.JUNE, 1)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.MONTHLY);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_monthly_last() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.FEBRUARY, 28),
                getDate(2018, Calendar.MARCH, 31),
                getDate(2018, Calendar.APRIL, 30),
                getDate(2018, Calendar.MAY, 31),
                getDate(2018, Calendar.JUNE, 30)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 31), Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.LAST_DAY_OF_MONTH);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_monthly_same_day_of_week_last() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.FEBRUARY, 28),
                getDate(2018, Calendar.MARCH, 28),
                getDate(2018, Calendar.APRIL, 25),
                getDate(2018, Calendar.MAY, 30),
                getDate(2018, Calendar.JUNE, 27)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 31), Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_WEEK);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_monthly_same_day_of_week_second() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.FEBRUARY, 14),
                getDate(2018, Calendar.MARCH, 14),
                getDate(2018, Calendar.APRIL, 11),
                getDate(2018, Calendar.MAY, 9),
                getDate(2018, Calendar.JUNE, 13)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 10), Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_WEEK);

        List<Long> actual1 = r.findRecurrences(-1, 5);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_monthly_same_day_31() {
        List<Long> expected = Arrays.asList(
                getDate(2018, Calendar.MARCH, 31),
                getDate(2018, Calendar.MAY, 31),
                getDate(2018, Calendar.JULY, 31),
                getDate(2018, Calendar.AUGUST, 31)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 31), Recurrence.MONTHLY)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_MONTH);

        List<Long> actual1 = r.findRecurrences(-1, 4);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_yearly() {
        List<Long> expected = Arrays.asList(
                getDate(2019, Calendar.JANUARY, 1),
                getDate(2020, Calendar.JANUARY, 1),
                getDate(2021, Calendar.JANUARY, 1),
                getDate(2022, Calendar.JANUARY, 1)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.YEARLY);

        List<Long> actual1 = r.findRecurrences(-1, 4);
        assertEquals(expected, actual1);
    }

    @Test
    public void recur_date_yearly_freq3() {
        List<Long> expected = Arrays.asList(
                getDate(2021, Calendar.JANUARY, 1),
                getDate(2024, Calendar.JANUARY, 1),
                getDate(2027, Calendar.JANUARY, 1),
                getDate(2030, Calendar.JANUARY, 1)
        );

        Recurrence r = new Recurrence(getDate(2018, Calendar.JANUARY, 1), Recurrence.YEARLY)
                .setFrequency(3);

        List<Long> actual1 = r.findRecurrences(-1, 4);
        assertEquals(expected, actual1);
    }

    private static long getDate(int year, int month, int day) {
        calendar.set(year, month, day);
        return calendar.getTimeInMillis() / 1000 * 1000;  // Floor to seconds
    }

}
