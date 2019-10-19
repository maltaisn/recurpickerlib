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
import kotlin.test.assertEquals


internal class RRuleFormatTest {

    private val formatter = RRuleFormatter()

    @Test
    fun doesNotRepeat() {
        val r = Recurrence.DOES_NOT_REPEAT
        testRRule(r, "RRULE:FREQ=NONE")
    }

    @Test
    fun daily_withFrequency() {
        val r = Recurrence(Period.DAILY) {
            frequency = 5
        }
        testRRule(r, "RRULE:FREQ=DAILY;INTERVAL=5")
    }

    @Test
    fun weekly() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.MONDAY, Recurrence.TUESDAY, Recurrence.THURSDAY)
        }
        testRRule(r, "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,TH")
    }

    @Test
    fun weekly_allDays() {
        val r = Recurrence(Period.WEEKLY) {
            frequency = 10
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
        }
        testRRule(r, "RRULE:FREQ=WEEKLY;INTERVAL=10;BYDAY=SU,MO,TU,WE,TH,FR,SA")
    }

    @Test
    fun monthly_same_day() {
        val r = Recurrence(Period.MONTHLY)
        testRRule(r, "RRULE:FREQ=MONTHLY")
    }

    @Test
    fun monthly_same_day_specific() {
        val r = Recurrence(Period.MONTHLY) {
            dayInMonth = 15
        }
        testRRule(r, "RRULE:FREQ=MONTHLY;BYMONTHDAY=15")
    }

    @Test
    fun monthly_same_week() {
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
    fun monthly_same_week_bySetPos() {
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
    fun monthly_last_day() {
        val r = Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        }
        testRRule(r, "RRULE:FREQ=MONTHLY;BYMONTHDAY=-1")
    }

    @Test
    fun yearly() {
        val r = Recurrence(Period.YEARLY)
        testRRule(r, "RRULE:FREQ=YEARLY")
    }

    @Test
    fun daily_endDate() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-01-01")
        }
        testRRule(r, "RRULE:FREQ=DAILY;UNTIL=20200101T000000")
    }

    @Test
    fun daily_endCount() {
        val r = Recurrence(Period.DAILY) {
            endCount = 42
        }
        testRRule(r, "RRULE:FREQ=DAILY;COUNT=42")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_noHeader() {
        formatter.parse("FREQ=DAILY")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_noPeriod() {
        formatter.parse("RRULE:BYDAY=FR,SA")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_endDate_invalidFormat() {
        formatter.parse("RRULE:UNTIL=2020-01-01")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_wrongPeriod() {
        formatter.parse("RRULE:FREQ=HOURLY")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_wrongDayOfWeek_weekly() {
        formatter.parse("RRULE:FREQ=WEEKLY;BYDAY=SUN,MON,TUE")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_wrongDayOfWeek_monthly() {
        formatter.parse("RRULE:FREQ=MONTHLY;BYDAY=-1FRI")
    }


    private fun testRRule(r: Recurrence, rrule: String) {
        assertEquals(rrule, formatter.format(r))
        assertEquals(r, formatter.parse(rrule))
    }

}
