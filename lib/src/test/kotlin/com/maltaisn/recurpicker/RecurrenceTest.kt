/*
 * Copyright 2019 Nicolas Maltais
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

package com.maltaisn.recurpicker

import com.maltaisn.recurpicker.Recurrence.Period
import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RecurrenceTest {

    @Test
    fun `should return true for equals and same hash code`() {
        val r1 = Recurrence(Period.WEEKLY) {
            frequency = 17
            setDaysOfWeek(Recurrence.MONDAY, Recurrence.TUESDAY)
            endCount = 23
        }
        val r2 = Recurrence(r1)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `should return true for equals and same hash code 2`() {
        val r1 = Recurrence(Period.MONTHLY) {
            frequency = 17
            dayInMonth = -1
            endDate = dateFor("2020-01-01")
        }
        val r2 = Recurrence(r1)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `should return true for equals and same hash code (end date on same day but different time)`() {
        val r1 = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01T10:00:00.000")
        }
        val r2 = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01T23:00:00.000")
        }
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `should create string representation`() {
        assertEquals("Recurrence{ Does not repeat }",
            Recurrence.DOES_NOT_REPEAT.toString())
        assertEquals("Recurrence{ Every 5 days }",
            Recurrence(Period.DAILY) { frequency = 5 }.toString())
        assertEquals("Recurrence{ Every year; until Dec 30, 2010 }",
            Recurrence(Period.YEARLY) { endDate = dateFor("2010-12-30") }.toString())
        assertEquals("Recurrence{ Every year; for 12 events }",
            Recurrence(Period.YEARLY) { endCount = 12 }.toString())
        assertEquals("Recurrence{ Every week on the same day as start date }",
            Recurrence(Period.WEEKLY).toString())
        assertEquals("Recurrence{ Every week on Sun, Wed }",
            Recurrence(Period.WEEKLY) { setDaysOfWeek(Recurrence.SUNDAY, Recurrence.WEDNESDAY) }.toString())
        assertEquals("Recurrence{ Every 2 weeks on every day of the week }",
            Recurrence(Period.WEEKLY) {
                frequency = 2
                setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
            }.toString())

        assertEquals("Recurrence{ Every month (on Monday of the third week) }",
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.MONDAY, 3) }.toString())
        assertEquals("Recurrence{ Every month (on Saturday of the last week) }",
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SATURDAY, -1) }.toString())
        assertEquals("Recurrence{ Every month (on Wednesday of the fourth to last week) }",
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.WEDNESDAY, -4) }.toString())
        assertEquals("Recurrence{ Every month (on the same day each month) }",
            Recurrence(Period.MONTHLY).toString())
        assertEquals("Recurrence{ Every month (on the last day of the month) }",
            Recurrence(Period.MONTHLY) { dayInMonth = -1 }.toString())
        assertEquals("Recurrence{ Every month (on 16 days before the end of the month) }",
            Recurrence(Period.MONTHLY) { dayInMonth = -16 }.toString())
        assertEquals("Recurrence{ Every month (on the 25 of each month) }",
            Recurrence(Period.MONTHLY) { dayInMonth = 25 }.toString())
    }

    @Test
    fun `should check if weekly recurrence is recurring on days of week`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.MONDAY, Recurrence.TUESDAY, Recurrence.FRIDAY)
        }
        assertTrue(r.isRecurringOnDaysOfWeek(0))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY or Recurrence.FRIDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.SATURDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY or Recurrence.SATURDAY))
    }

    @Test
    fun `should check if monthly recurrence is recurring on day of week`() {
        val r = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SATURDAY, 1)
        }
        assertTrue(r.isRecurringOnDaysOfWeek(0))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.SATURDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY or Recurrence.SATURDAY))
    }

    @Test
    fun `should return day of week in month and week in month for monthly recurrence`() {
        val r = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SATURDAY, -2)
        }
        assertEquals(Calendar.SATURDAY, r.dayOfWeekInMonth)
        assertEquals(-2, r.weekInMonth)
    }

    @Test
    fun `should have frequency set to 1 for 'does not repeat' recurrence`() {
        assertEquals(1, Recurrence.DOES_NOT_REPEAT.frequency)
    }
}
