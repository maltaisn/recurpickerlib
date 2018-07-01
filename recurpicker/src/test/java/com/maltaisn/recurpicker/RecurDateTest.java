package com.maltaisn.recurpicker;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class RecurrenceDateTest {

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


    private static long getDate(int year, int month, int day) {
        calendar.set(year, month, day);
        return calendar.getTimeInMillis() / 1000 * 1000;  // Floor to seconds
    }

}
