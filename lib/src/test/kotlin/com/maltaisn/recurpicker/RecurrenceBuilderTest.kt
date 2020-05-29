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


internal class RecurrenceBuilderTest {

    @Test
    fun settingEndCount_shouldSetEndType() {
        Recurrence(Period.DAILY) {
            assertEquals(EndType.NEVER, endType)
            endCount = 15
            assertEquals(EndType.BY_COUNT, endType)
        }
    }

    @Test
    fun settingEndDate_shouldSetEndType() {
        Recurrence(Period.WEEKLY) {
            assertEquals(EndType.NEVER, endType)
            endDate = dateFor("2018-01-15")
            assertEquals(EndType.BY_DATE, endType)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongFrequency_negative() {
        Recurrence(Period.DAILY) { frequency = -1 }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_wrongFrequency_zero() {
        Recurrence(Period.DAILY) { frequency = 0 }
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrow_setDaysOfWeek_notWeekly() {
        Recurrence(Period.YEARLY) { setDaysOfWeek(Recurrence.SUNDAY) }
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrow_setDaysOfWeek_invalidFlag() {
        Recurrence(Period.YEARLY) { setDaysOfWeek(Int.MAX_VALUE) }
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrow_setDayOfWeekInMonth_notMonthly() {
        Recurrence(Period.DAILY) { setDayOfWeekInMonth(Recurrence.SATURDAY, 3) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_setDayOfWeekInMonth_wrongDay() {
        Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(0b110101, 3) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_setDayOfWeekInMonth_wrongWeek_zero() {
        Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.WEDNESDAY, 0) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_setDayOfWeekInMonth_wrongWeek_outOfRange() {
        Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.WEDNESDAY, 5) }
    }

    @Test(expected = IllegalStateException::class)
    fun shouldThrow_dayInMonth_notMonthly() {
        Recurrence(Period.WEEKLY) { dayInMonth = 15 }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_dayInMonth_outOfRange() {
        Recurrence(Period.MONTHLY) { dayInMonth = 32 }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrow_dayInMonth_outOfRange2() {
        Recurrence(Period.MONTHLY) { dayInMonth = -32 }
    }

    @Test
    fun convertPeriod_weekly_allDays_freq1_shouldBeDaily() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
        }
        assertEquals(Period.DAILY, r.period)
    }

    @Test
    fun convertPeriod_weekly_allDays_freq2_shouldBeDaily() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
            frequency = 2
        }
        assertEquals(Period.WEEKLY, r.period)
    }

    @Test
    fun convertPeriod_endCountTooLow_shouldBeNone() {
        val r1 = Recurrence(Period.DAILY) { endCount = 0 }
        val r2 = Recurrence(Period.DAILY) { endCount = -1 }
        assertEquals(Period.NONE, r1.period)
        assertEquals(Period.NONE, r2.period)
    }

    @Test
    fun convertEndType_periodIsNone() {
        val r = Recurrence(Period.NONE) { endCount = 12 }
        assertEquals(EndType.NEVER, r.endType)
    }

    @Test
    fun convertEndType_endDateIsNone_shouldBeNever() {
        val r = Recurrence(Period.YEARLY) { endDate = Recurrence.DATE_NONE }
        assertEquals(EndType.NEVER, r.endType)
    }

    @Test
    fun normalization_endDate_endCount_endsNever() {
        val r = Recurrence(Period.YEARLY) {
            endCount = 12
            endDate = dateFor("2019-01-01")
            endType = EndType.NEVER
        }
        assertEquals(0, r.endCount)
        assertEquals(Recurrence.DATE_NONE, r.endDate)
        assertEquals(EndType.NEVER, r.endType)
    }

    @Test
    fun normalization_endCount_endsByDate() {
        val r = Recurrence(Period.YEARLY) {
            endDate = dateFor("2019-01-01")
            endCount = 10
            endType = EndType.BY_DATE
        }
        assertEquals(0, r.endCount)
        assertEquals(dateFor("2019-01-01"), r.endDate)
        assertEquals(EndType.BY_DATE, r.endType)
    }

    @Test
    fun normalization_endDate_endsByCount() {
        val r = Recurrence(Period.YEARLY) {
            endDate = dateFor("2019-01-01")
            endCount = 10
            endType = EndType.BY_COUNT
        }
        assertEquals(10, r.endCount)
        assertEquals(Recurrence.DATE_NONE, r.endDate)
        assertEquals(EndType.BY_COUNT, r.endType)
    }

    @Test
    fun setDaysOfWeek_notAdditive() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.SATURDAY, Recurrence.MONDAY)
            setDaysOfWeek(Recurrence.TUESDAY, Recurrence.WEDNESDAY, Recurrence.FRIDAY)
        }
        assertEquals(1 or Recurrence.TUESDAY or Recurrence.WEDNESDAY or Recurrence.FRIDAY, r.byDay)
    }

    @Test
    fun setDayOfWeekInMonth() {
        val r1 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SATURDAY, -1) }
        val r2 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SATURDAY, 3) }
        assertEquals(0b0000001110000001, r1.byDay)
        assertEquals(0b0000011110000001, r2.byDay)
    }

    @Test
    fun normalization_monthly_byDay() {
        Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.WEDNESDAY, -3)
            assertEquals(0, byMonthDay)
        }
    }

    @Test
    fun normalization_monthly_byMonthlyDay() {
        Recurrence(Period.MONTHLY) {
            dayInMonth = -23
            assertEquals(0, byDay)
        }
    }

    @Test
    fun copyConstructor() {
        val r1 = Recurrence(Period.WEEKLY) {
            frequency = 3
            endType = EndType.BY_COUNT
            endCount = 15
        }
        val r2 = Recurrence(r1)
        assertEquals(r1.period, r2.period)
        assertEquals(r1.frequency, r2.frequency)
        assertEquals(r1.byDay, r2.byDay)
        assertEquals(r1.byMonthDay, r2.byMonthDay)
        assertEquals(r1.endType, r2.endType)
        assertEquals(r1.endCount, r2.endCount)
        assertEquals(r1.endDate, r2.endDate)
    }

    @Test
    fun directBuilderUse() {
        val builder = Builder(Period.DAILY)
        builder.frequency = 3
        builder.endCount = 15
        val r = builder.build()
        assertEquals(3, r.frequency)
        assertEquals(15, r.endCount)
    }

    @Test
    fun everyDayOfWeekConstant() {
        assertEquals(1 or Recurrence.SUNDAY or Recurrence.MONDAY or Recurrence.TUESDAY or Recurrence.WEDNESDAY or
                Recurrence.THURSDAY or Recurrence.FRIDAY or Recurrence.SATURDAY, Recurrence.EVERY_DAY_OF_WEEK)
    }

}
