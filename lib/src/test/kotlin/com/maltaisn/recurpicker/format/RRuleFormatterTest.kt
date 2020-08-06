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

package com.maltaisn.recurpicker.format

import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.dateFor
import org.junit.Test
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class RRuleFormatterTest {

    private val formatter = RRuleFormatter()

    @Test
    fun `should test 'does not repeat' recurrence`() {
        val r = Recurrence.DOES_NOT_REPEAT
        testRRule(r, "RRULE:FREQ=NONE")
    }

    @Test
    fun `should test daily recurrence with frequency 5`() {
        val r = Recurrence(Period.DAILY) {
            frequency = 5
        }
        testRRule(r, "RRULE:FREQ=DAILY;INTERVAL=5")
    }

    @Test
    fun `should test weekly recurrence with days set`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.MONDAY, Recurrence.TUESDAY, Recurrence.THURSDAY)
        }
        testRRule(r, "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,TH")
    }

    @Test
    fun `should test weekly recurrence set on all days with frequency 2`() {
        val r = Recurrence(Period.WEEKLY) {
            frequency = 10
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
        }
        testRRule(r, "RRULE:FREQ=WEEKLY;INTERVAL=10;BYDAY=SU,MO,TU,WE,TH,FR,SA")
    }

    @Test
    fun `should test default monthly recurrence`() {
        val r = Recurrence(Period.MONTHLY)
        testRRule(r, "RRULE:FREQ=MONTHLY")
    }

    @Test
    fun `should test monthly recurrence set on a specific day`() {
        val r = Recurrence(Period.MONTHLY) {
            dayInMonth = 15
        }
        testRRule(r, "RRULE:FREQ=MONTHLY;BYMONTHDAY=15")
    }

    @Test
    fun `should test monthly recurrence set on a specific day of a week in month`() {
        val r1 = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.FRIDAY, 4)
        }
        testRRule(r1, "RRULE:FREQ=MONTHLY;BYDAY=4FR")

        val r2 = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.MONDAY, -1)
        }
        testRRule(r2, "RRULE:FREQ=MONTHLY;BYDAY=-1MO")
    }

    @Test
    fun `should parse monthly recurrence using BYSETPOS to describe monthly recurrence on same week`() {
        val r1 = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.FRIDAY, 4)
        }
        assertEquals(r1, formatter.parse("RRULE:FREQ=MONTHLY;BYDAY=FR;BYSETPOS=4"))

        val r2 = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.MONDAY, -1)
        }
        assertEquals(r2, formatter.parse("RRULE:FREQ=MONTHLY;BYDAY=MO;BYSETPOS=-1"))
    }

    @Test
    fun `should test monthly recurrence on last day of month`() {
        val r = Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        }
        testRRule(r, "RRULE:FREQ=MONTHLY;BYMONTHDAY=-1")
    }

    @Test
    fun `should test yearly recurrence`() {
        val r = Recurrence(Period.YEARLY)
        testRRule(r, "RRULE:FREQ=YEARLY")
    }

    @Test
    fun `should test daily recurrence with end date, default timezone`() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01T00:00:00.000")
        }
        formatter.timeZone = TimeZone.getDefault()
        testRRule(r, "RRULE:FREQ=DAILY;UNTIL=20200101")
    }

    @Test
    fun `should test daily recurrence with end date, GMT timezone`() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01T00:00:00.000Z")
        }
        formatter.timeZone = TimeZone.getTimeZone("GMT")
        testRRule(r, "RRULE:FREQ=DAILY;UNTIL=20200101")
    }

    @Test
    fun `should test daily recurrence with end count`() {
        val r = Recurrence(Period.DAILY) {
            endCount = 42
        }
        testRRule(r, "RRULE:FREQ=DAILY;COUNT=42")
    }

    @Test
    fun `should parse end date (date only)`() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01")
        }
        assertEquals(r, formatter.parse("RRULE:FREQ=DAILY;UNTIL=20200101"))
    }

    @Test
    fun `should parse end date (date time)`() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01T10:11:12.000")
        }
        assertEquals(r, formatter.parse("RRULE:FREQ=DAILY;UNTIL=20200101T101112"))
    }

    @Test
    fun `should parse end date (UTC date time)`() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01T10:11:12.000Z")
        }
        assertEquals(r, formatter.parse("RRULE:FREQ=DAILY;UNTIL=20200101T101112Z"))
    }

    @Test
    fun `should fail to parse rrule with missing signature`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("FREQ=DAILY")
        }
    }

    @Test
    fun `should fail to parse rrule with no FREQ attribute set`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("RRULE:BYDAY=FR,SA")
        }
    }

    @Test
    fun `should fail to parse rrule with invalid end date format`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("RRULE:UNTIL=2020-01-01")
        }
    }

    @Test
    fun `should fail to parse rrule with unsupported FREQ attribute value`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("RRULE:FREQ=HOURLY")
        }
    }

    @Test
    fun `should fail to parse rrule with invalid day of the week literals`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("RRULE:FREQ=WEEKLY;BYDAY=SUN,MON,TUE")
        }
    }

    @Test
    fun `should fail to parse rrule with invalid BYDAY value`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("RRULE:FREQ=MONTHLY;BYDAY=-1FRI")
        }
    }

    @Test
    fun `should fail to parse rrule with invalid number format`() {
        assertFailsWith<RRuleParseException> {
            formatter.parse("RRULE:FREQ=DAILY;INTERVAL=foo")
        }
    }

    private fun testRRule(r: Recurrence, rrule: String) {
        assertEquals(rrule, formatter.format(r))
        assertEquals(r, formatter.parse(rrule))
    }
}
