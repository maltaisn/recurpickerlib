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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


internal class RecurrenceTest {

    @Test
    fun equals_hashCode_allFields() {
        val r1 = Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            frequency = 17
            weeklyDays = Recurrence.MONDAY or Recurrence.TUESDAY
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
    fun isRecurringOnDaysOfWeek() {
        val r = Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            weeklyDays = Recurrence.MONDAY or Recurrence.TUESDAY or Recurrence.FRIDAY
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
        assertEquals(dateFor("2018-01-01").compareDay(dateFor("2019-01-01")), -1)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2018-01-01")), 1)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2019-01-01")), 0)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2019-01-01") + 1000), 0)
    }

}
