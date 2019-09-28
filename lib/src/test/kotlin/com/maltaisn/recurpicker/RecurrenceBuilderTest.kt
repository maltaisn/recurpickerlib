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

import com.maltaisn.recurpicker.Recurrence.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


internal class RecurrenceBuilderTest {

    @Test
    fun settingEndCount_shouldSetEndType() {
        Recurrence(0, Period.DAILY) {
            assertEquals(EndType.NEVER, endType)
            endCount = 15
            assertEquals(EndType.BY_COUNT, endType)
        }
    }

    @Test
    fun settingEndDate_shouldSetEndType() {
        Recurrence(dateFor("2018-01-01"), Period.WEEKLY) {
            assertEquals(EndType.NEVER, endType)
            endDate = dateFor("2018-01-15")
            assertEquals(EndType.BY_DATE, endType)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongFrequency_negative() {
        Recurrence(0, Period.DAILY) { frequency = -1 }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongFrequency_zero() {
        Recurrence(0, Period.DAILY) { frequency = 0 }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongStartDate_monthlyLastDayOfMonth() {
        Recurrence(dateFor("2019-12-01"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongWeeklySetting() {
        Recurrence(0, Period.WEEKLY) {
            weeklyDays = -1
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongWeeklySetting2() {
        Recurrence(0, Period.WEEKLY) {
            weeklyDays = 255
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_noStartDate() {
        Recurrence(0, Period.DAILY) {
            startDate = Recurrence.DATE_NONE
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_endDateBeforeStartDate() {
        Recurrence(dateFor("2019-12-01"), Period.DAILY) {
            endDate = dateFor("2018-11-01")
        }
    }

    @Test
    fun shouldThrow_endDateBeforeStartDate_onSameDay() {
        Recurrence(dateFor("2019-12-01") + 1000, Period.DAILY) {
            endDate = dateFor("2019-12-01")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongIsDefault() {
        Recurrence(0, Period.DAILY) {
            frequency = 2
            isDefault = true
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongIsDefault2() {
        Recurrence(dateFor("2019-09-23"), Period.WEEKLY) {
            setWeekDays(Recurrence.SUNDAY)
            isDefault = true
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongIsDefault3() {
        Recurrence(0, Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
            isDefault = true
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongIsDefault4() {
        Recurrence(0, Period.WEEKLY) {
            endCount = 2
            isDefault = true
        }
    }

    @Test
    fun convertPeriod_weekly_noDays_shouldBeNone() {
        val r = Recurrence(0, Period.WEEKLY) {
            weeklyDays = 0
        }
        assertEquals(Period.NONE, r.period)
    }

    @Test
    fun convertPeriod_weekly_allDays_shouldBeDaily() {
        val r = Recurrence(0, Period.WEEKLY) {
            weeklyDays = Recurrence.EVERY_DAY_OF_WEEK
        }
        assertEquals(Period.DAILY, r.period)
    }

    @Test
    fun convertPeriod_endDateOnStartDate_shouldBeNone() {
        val r = Recurrence(dateFor("2019-01-01") + 1000, Period.DAILY) {
            endDate = dateFor("2019-01-01")
        }
        assertEquals(Period.NONE, r.period)
    }

    @Test
    fun convertPeriod_endCountTooLow_shouldBeNone() {
        val r1 = Recurrence(0, Period.DAILY) { endCount = 0 }
        val r2 = Recurrence(0, Period.DAILY) { endCount = -1 }
        assertEquals(Period.NONE, r1.period)
        assertEquals(Period.NONE, r2.period)
    }

    @Test
    fun convertEndType_endDateIsNone_shouldBeNever() {
        val r = Recurrence(0, Period.YEARLY) { endDate = Recurrence.DATE_NONE }
        assertEquals(EndType.NEVER, r.endType)
    }

    @Test
    fun normalization_weeklyDays() {
        val r = Recurrence(0, Period.YEARLY) { weeklyDays = 10 }
        assertEquals(0, r.weeklyDays)
    }

    @Test
    fun normalization_monthlyDay() {
        val r = Recurrence(0, Period.YEARLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
        }
        assertEquals(MonthlyDay.SAME_DAY_OF_MONTH, r.monthlyDay)
    }

    @Test
    fun normalization_endDate_endCount_endsNever() {
        val r = Recurrence(0, Period.YEARLY) {
            endCount = 12
            endDate = 1200321
            endType = EndType.NEVER
        }
        assertEquals(0, r.endCount)
        assertEquals(Recurrence.DATE_NONE, r.endDate)
    }

    @Test
    fun normalization_endCount_endsByDate() {
        val r = Recurrence(dateFor("2018-01-01"), Period.YEARLY) {
            endDate = dateFor("2019-01-01")
            endCount = 10
            endType = EndType.BY_DATE
        }
        assertEquals(0, r.endCount)
        assertEquals(dateFor("2019-01-01"), r.endDate)
    }

    @Test
    fun normalization_endDate_endsByCount() {
        val r = Recurrence(dateFor("2018-01-01"), Period.YEARLY) {
            endDate = dateFor("2019-01-01")
            endCount = 10
            endType = EndType.BY_COUNT
        }
        assertEquals(10, r.endCount)
        assertEquals(Recurrence.DATE_NONE, r.endDate)
    }

    @Test
    fun isDefault_shouldNotBeSet() {
        val r = Recurrence(dateFor("2018-01-01"), Period.YEARLY)
        assertFalse(r.isDefault)
    }

    @Test
    fun isDefault_shouldBeSet() {
        val r = Recurrence(dateFor("2018-01-01"), Period.YEARLY) { isDefault = true }
        assertTrue(r.isDefault)
    }

    @Test
    fun isDefault_shouldBeSet2() {
        val r = Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            isDefault = true
            frequency = 1
            setWeekDays(Recurrence.TUESDAY)
        }
        assertTrue(r.isDefault)
    }

    @Test
    fun weeklyDays_defaultShouldBeSet() {
        Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            assertEquals(Recurrence.TUESDAY, weeklyDays)
        }
    }

    @Test
    fun addWeekDays() {
        Recurrence(0, Period.WEEKLY) {
            setWeekDays(Recurrence.SATURDAY, Recurrence.MONDAY)
            assertEquals(Recurrence.SATURDAY or Recurrence.MONDAY, weeklyDays)

            setWeekDays(Recurrence.SATURDAY, Recurrence.MONDAY, Recurrence.FRIDAY)
            assertEquals(Recurrence.SATURDAY or Recurrence.MONDAY or Recurrence.FRIDAY, weeklyDays)
        }
    }

    @Test
    fun copyConstructor() {
        val r1 = Recurrence(dateFor("2019-09-24"), Period.WEEKLY) {
            frequency = 3
            endType = EndType.BY_COUNT
            endCount = 15
        }
        val r2 = Recurrence(r1)

        assertEquals(r1.startDate, r2.startDate)
        assertEquals(r1.period, r2.period)
        assertEquals(r1.frequency, r2.frequency)
        assertEquals(r1.weeklyDays, r2.weeklyDays)
        assertEquals(r1.monthlyDay, r2.monthlyDay)
        assertEquals(r1.endType, r2.endType)
        assertEquals(r1.endCount, r2.endCount)
        assertEquals(r1.endDate, r2.endDate)
        assertEquals(r1.isDefault, r2.isDefault)
    }

    @Test
    fun directBuilderUse() {
        val builder = Builder(dateFor("2019-01-01"), Period.DAILY)
        builder.frequency = 3
        builder.endCount = 15
        val r = builder.build()
        assertEquals(3, r.frequency)
        assertEquals(15, r.endCount)
    }

    @Test
    fun everyDayOfWeekConstant() {
        assertEquals(Recurrence.SUNDAY or Recurrence.MONDAY or Recurrence.TUESDAY or Recurrence.WEDNESDAY or
                Recurrence.THURSDAY or Recurrence.FRIDAY or Recurrence.SATURDAY, Recurrence.EVERY_DAY_OF_WEEK)
    }

}
