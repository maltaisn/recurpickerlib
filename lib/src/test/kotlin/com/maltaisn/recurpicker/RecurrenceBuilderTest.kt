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

import com.maltaisn.recurpicker.Recurrence.Builder
import com.maltaisn.recurpicker.Recurrence.EndType
import com.maltaisn.recurpicker.Recurrence.Period
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

internal class RecurrenceBuilderTest {

    @Test
    fun `should set end type when setting end count`() {
        Recurrence(Period.DAILY) {
            assertEquals(EndType.NEVER, endType)
            endCount = 15
            assertEquals(EndType.BY_COUNT, endType)
        }
    }

    @Test
    fun `should set end type when setting end date`() {
        Recurrence(Period.WEEKLY) {
            assertEquals(EndType.NEVER, endType)
            endDate = dateFor("2018-01-15")
            assertEquals(EndType.BY_DATE, endType)
        }
    }

    @Test
    fun `should fail to set negative frequency`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.DAILY) { frequency = -1 }
        }
    }

    @Test
    fun `should fail to set zero frequency`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.DAILY) { frequency = 0 }
        }
    }

    @Test
    fun `should fail to set days of the week for non-weekly period`() {
        assertFailsWith<IllegalStateException> {
            Recurrence(Period.YEARLY) { setDaysOfWeek(Recurrence.SUNDAY) }
        }
    }

    @Test
    fun `should fail to set invalid days of the week`() {
        assertFailsWith<IllegalStateException> {
            Recurrence(Period.YEARLY) { setDaysOfWeek(Int.MAX_VALUE) }
        }
    }

    @Test
    fun `should fail to set day of week in month for non-monthly period`() {
        assertFailsWith<IllegalStateException> {
            Recurrence(Period.DAILY) { setDayOfWeekInMonth(Recurrence.SATURDAY, 3) }
        }
    }

    @Test
    fun `should fail to set multiple day of week in month`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(0b110101, 3) }
        }
    }

    @Test
    fun `should fail to set invalid day of week in month`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(3242342, 3) }
        }
    }

    @Test
    fun `should fail to set week in month to zero`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.WEDNESDAY, 0) }
        }
    }

    @Test
    fun `should fail to set week in month out of range`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.WEDNESDAY, 5) }
        }
    }

    @Test
    fun `should fail to set day in month for non-monthly period`() {
        assertFailsWith<IllegalStateException> {
            Recurrence(Period.WEEKLY) { dayInMonth = 15 }
        }
    }

    @Test
    fun `should fail to set out of range day in month (positive)`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.MONTHLY) { dayInMonth = 32 }
        }
    }

    @Test
    fun `should fail to set out of range day in month (negative)`() {
        assertFailsWith<IllegalArgumentException> {
            Recurrence(Period.MONTHLY) { dayInMonth = -32 }
        }
    }

    @Test
    fun `should convert weekly on every day of the week to daily (if frequency is 1)`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
        }
        assertEquals(Period.DAILY, r.period)
    }

    @Test
    fun `should not convert weekly on every day of the week to daily (if frequency is over 1)`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
            frequency = 2
        }
        assertEquals(Period.WEEKLY, r.period)
    }

    @Test
    fun `should make 'does not repeat' recurrence when end count is less than 1`() {
        val r1 = Recurrence(Period.DAILY) { endCount = 0 }
        val r2 = Recurrence(Period.DAILY) { endCount = -1 }
        assertEquals(Period.NONE, r1.period)
        assertEquals(Period.NONE, r2.period)
    }

    @Test
    fun `should build same recurrence if period is none`() {
        val r1 = Recurrence(Period.MONTHLY) {
            dayInMonth = 15
            endCount = 0
        }
        val r2 = Recurrence.DOES_NOT_REPEAT
        assertSame(r1, r2)
    }

    @Test
    fun `should not set end count on recurrence with none period`() {
        val r = Recurrence(Period.NONE) { endCount = 12 }
        assertEquals(EndType.NEVER, r.endType)
    }

    @Test
    fun `should not set end date on recurrence with none period`() {
        val r = Recurrence(Period.NONE) { endDate = dateFor("2019-01-01") }
        assertEquals(Recurrence.DATE_NONE, r.endDate)
        assertEquals(EndType.NEVER, r.endType)
    }

    @Test
    fun `should not set end count or end date if changing end type to never after setting them`() {
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
    fun `should not set end count if changing end type to date after setting it`() {
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
    fun `should not set end date if changing end type to count after setting it`() {
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
    fun `should overwrite previously set days of week when using setDaysOfWeek`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.SATURDAY, Recurrence.MONDAY)
            setDaysOfWeek(Recurrence.TUESDAY, Recurrence.WEDNESDAY, Recurrence.FRIDAY)
        }
        assertEquals(1 or Recurrence.TUESDAY or Recurrence.WEDNESDAY or Recurrence.FRIDAY, r.byDay)
    }

    @Test
    fun `should set day of week in month`() {
        val r1 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SATURDAY, -1) }
        val r2 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SATURDAY, 3) }
        assertEquals(0b0000001110000001, r1.byDay)
        assertEquals(0b0000011110000001, r2.byDay)
    }

    @Test
    fun `should not set byMonthDay field when setting day of week in month for monthly period`() {
        Recurrence(Period.MONTHLY) {
            dayInMonth = 12
            setDayOfWeekInMonth(Recurrence.WEDNESDAY, -3)
            assertEquals(0, byMonthDay)
        }
    }

    @Test
    fun `should not set byDay field when setting day in month for monthly period`() {
        Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.WEDNESDAY, -3)
            dayInMonth = -23
            assertEquals(0, byDay)
        }
    }

    @Test
    fun `should copy all recurrence fields when using builder copy constructor`() {
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
    fun `should allow direct builder use without inline DSL`() {
        val builder = Builder(Period.DAILY)
        builder.frequency = 3
        builder.endCount = 15
        val r = builder.build()
        assertEquals(3, r.frequency)
        assertEquals(15, r.endCount)
    }

    @Test
    fun `every day of week constant should include all days`() {
        assertEquals(1 or Recurrence.SUNDAY or Recurrence.MONDAY or Recurrence.TUESDAY or Recurrence.WEDNESDAY or
                Recurrence.THURSDAY or Recurrence.FRIDAY or Recurrence.SATURDAY, Recurrence.EVERY_DAY_OF_WEEK)
    }
}
