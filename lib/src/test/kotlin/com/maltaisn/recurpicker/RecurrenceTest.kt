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

import com.maltaisn.recurpicker.Recurrence.Companion.compareDay
import com.maltaisn.recurpicker.Recurrence.Period
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


internal class RecurrenceTest {

    @Test
    fun equals_hashCode_allFields() {
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
    fun equals_hashCode_allFields2() {
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
    fun equals_hashCode_differentEndDateOnSameDay() {
        val r1 = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01")
        }
        val r2 = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01") + 1000
        }
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun toStringDebug() {
        assertTrue(BuildConfig.DEBUG)
        assertEquals("Recurrence{ Does not repeat }",
                Recurrence(Period.NONE).toString())
        assertEquals("Recurrence{ Every 5 days }",
                Recurrence(Period.DAILY) { frequency = 5 }.toString())
        assertEquals("Recurrence{ Every year; until Dec 30, 2010 }",
                Recurrence(Period.YEARLY) { endDate = dateFor("2010-12-30") }.toString())
        assertEquals("Recurrence{ Every year; for 12 events }",
                Recurrence(Period.YEARLY) { endCount = 12 }.toString())
        assertEquals("Recurrence{ Every week on Sun, Wed }",
                Recurrence(Period.WEEKLY) { setDaysOfWeek(Recurrence.SUNDAY, Recurrence.WEDNESDAY) }.toString())

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
    fun isRecurringOnDaysOfWeek_weekly() {
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
    fun isRecurringOnDaysOfWeek_monthly() {
        val r = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SATURDAY, 1)
        }
        assertTrue(r.isRecurringOnDaysOfWeek(0))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.SATURDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY or Recurrence.SATURDAY))
    }

    @Test
    fun dayOfWeekInMonth_weekInMonth() {
        val r = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SATURDAY, -2)
        }
        assertEquals(Calendar.SATURDAY, r.dayOfWeekInMonth)
        assertEquals(-2, r.weekInMonth)
    }

    @Test
    fun compareDay() {
        val cal = Calendar.getInstance()
        assertEquals(dateFor("2018-01-01").compareDay(dateFor("2019-01-01"), cal), -1)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2018-01-01"), cal), 1)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2019-01-01"), cal), 0)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2019-01-01") + 1000, cal), 0)
    }

    @Test
    fun compareDay_dateNone() {
        val cal = Calendar.getInstance()
        assertEquals(Recurrence.DATE_NONE.compareDay(Recurrence.DATE_NONE, cal), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun compareDay_dateNone_illegal() {
        val cal = Calendar.getInstance()
        assertEquals(Recurrence.DATE_NONE.compareDay(dateFor("2019-01-01"), cal), -1)
        assertEquals(dateFor("2019-01-01").compareDay(Recurrence.DATE_NONE, cal), 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun compareDay_dateNone_illegal2() {
        val cal = Calendar.getInstance()
        assertEquals(dateFor("2019-01-01").compareDay(Recurrence.DATE_NONE, cal), 1)
    }

}
