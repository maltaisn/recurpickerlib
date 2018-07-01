package com.maltaisn.recurpicker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RecurSerializationTest {

    @Test
    public void recur_serialization_1() {
        Recurrence r1 = new Recurrence(System.currentTimeMillis(), Recurrence.DAILY)
                .setFrequency(3).setEndByCount(100);
        byte[] arr = r1.toByteArray();

        Recurrence r2 = new Recurrence(arr, 0);
        assertRecurrenceEquals(r1, r2);
    }

    @Test
    public void recur_serialization_2() {
        Recurrence r1 = new Recurrence(System.currentTimeMillis(), Recurrence.WEEKLY)
                .setFrequency(5)
                .setWeeklySetting(Recurrence.SATURDAY | Recurrence.SUNDAY)
                .setEndByDate(System.currentTimeMillis() + 100000);
        byte[] arr = r1.toByteArray();

        assertEquals(arr.length, Recurrence.BYTE_ARRAY_LENGTH);

        Recurrence r2 = new Recurrence(arr, 0);
        assertRecurrenceEquals(r1, r2);
    }

    @Test
    public void recur_serialization_3() {
        Recurrence r1 = new Recurrence(System.currentTimeMillis(), Recurrence.MONTHLY)
                .setFrequency(2)
                .setMonthlySetting(Recurrence.SAME_DAY_OF_MONTH)
                .setEndByDateOrCount(System.currentTimeMillis() + 100000, 5);
        byte[] arr = r1.toByteArray();

        assertEquals(arr.length, Recurrence.BYTE_ARRAY_LENGTH);

        Recurrence r2 = new Recurrence(arr, 0);
        assertRecurrenceEquals(r1, r2);
    }

    private static void assertRecurrenceEquals(Recurrence r1, Recurrence r2) {
        assertEquals(r1.getPeriod(), r2.getPeriod());
        assertEquals(r1.getStartDate(), r2.getStartDate());
        assertEquals(r1.getFrequency(), r2.getFrequency());
        assertEquals(r1.getEndCount(), r2.getEndCount());
        assertEquals(r1.getDaySetting(), r2.getDaySetting());
        assertEquals(r1.getEndType(), r2.getEndType());
        assertEquals(r1.getEndDate(), r2.getEndDate());
        assertEquals(r1.getEndCount(), r2.getEndCount());
    }

}
