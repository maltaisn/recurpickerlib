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
import com.maltaisn.recurpicker.Recurrence.MonthlyDay
import com.maltaisn.recurpicker.Recurrence.Period
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


internal class RecurrenceTest {

    @Test
    fun equals_hashCode_allFields() {
        val r1 = Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            frequency = 17
            setWeekDays(Recurrence.MONDAY, Recurrence.TUESDAY)
            endCount = 23
        }
        val r2 = Recurrence(r1)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun equals_hashCode_allFields2() {
        val r1 = Recurrence(dateFor("2019-09-30"), Period.MONTHLY) {
            frequency = 17
            monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH
            endDate = dateFor("2020-01-01")
        }
        val r2 = Recurrence(r1)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }


    @Test
    fun equals_hashCode_sameDay_differentStartDate() {
        val r1 = Recurrence(dateFor("2019-01-01"), Period.DAILY)
        val r2 = Recurrence(dateFor("2019-01-01") + 1000, Period.DAILY)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun equals_hashCode_sameDay_differentEndDate() {
        val r1 = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endDate = dateFor("2020-01-01")
        }
        val r2 = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endDate = dateFor("2020-01-01") + 1000
        }
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun toStringDebug() {
        assertTrue(BuildConfig.DEBUG)
        assertEquals("Recurrence{ From Sep 24, 2019, does not repeat }",
                Recurrence(dateFor("2019-09-24"), Period.NONE).toString())
        assertEquals("Recurrence{ From Dec 30, 2001, on every 5 days }",
                Recurrence(dateFor("2001-12-30"), Period.DAILY) { frequency = 5 }.toString())
        assertEquals("Recurrence{ From Dec 30, 2001, on every year; until Dec 30, 2010 }",
                Recurrence(dateFor("2001-12-30"), Period.YEARLY) { endDate = dateFor("2010-12-30") }.toString())
        assertEquals("Recurrence{ From Dec 30, 2001, on every year; for 12 events }",
                Recurrence(dateFor("2001-12-30"), Period.YEARLY) { endCount = 12 }.toString())
        assertEquals("Recurrence{ From Dec 30, 2001, on every week on Sun, Wed }",
                Recurrence(dateFor("2001-12-30"), Period.WEEKLY) { setWeekDays(Recurrence.SUNDAY, Recurrence.WEDNESDAY) }.toString())
        assertEquals("Recurrence{ From Sep 24, 2019, on every month (on every fourth Tuesday) }",
                Recurrence(dateFor("2019-09-24"), Period.MONTHLY) { monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK }.toString())
    }

    @Test
    fun isRecurringOnDaysOfWeek() {
        val r = Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            setWeekDays(Recurrence.MONDAY, Recurrence.TUESDAY, Recurrence.FRIDAY)
        }
        assertTrue(r.isRecurringOnDaysOfWeek(0))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY or Recurrence.FRIDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.SATURDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.MONDAY or Recurrence.SATURDAY))
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
