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
import com.maltaisn.recurpicker.Recurrence.MonthlyDay
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.dateFor
import org.junit.Test
import kotlin.test.assertEquals


internal class RRuleFormatTest {

    private val formatter = RRuleFormatter()

    @Test
    fun doesNotRepeat() {
        val r = Recurrence(dateFor("2019-01-01"), Period.NONE)
        testRRule(r, "RRULE:DTSTART=20190101T000000;FREQ=NONE")
    }

    @Test
    fun daily_withFrequency() {
        val r = Recurrence(dateFor("2018-01-09"), Period.DAILY) {
            frequency = 5
        }
        testRRule(r, "RRULE:DTSTART=20180109T000000;FREQ=DAILY;INTERVAL=5")
    }

    @Test
    fun daily_default() {
        val r = Recurrence(dateFor("2018-01-09"), Period.DAILY) {
            isDefault = true
        }
        testRRule(r, "RRULE:DTSTART=20180109T000000;FREQ=DAILY;DEFAULT=1")
    }

    @Test
    fun weekly() {
        val r = Recurrence(dateFor("2019-09-27"), Period.WEEKLY) {
            setWeekDays(Recurrence.MONDAY, Recurrence.TUESDAY, Recurrence.THURSDAY)
        }
        testRRule(r, "RRULE:DTSTART=20190927T000000;FREQ=WEEKLY;BYDAY=MO,TU,TH")
    }

    @Test
    fun weekly_allDays() {
        val r = Recurrence(dateFor("2019-09-27"), Period.WEEKLY) {
            frequency = 10
            weeklyDays = Recurrence.EVERY_DAY_OF_WEEK
        }
        testRRule(r, "RRULE:DTSTART=20190927T000000;FREQ=WEEKLY;INTERVAL=10;BYDAY=SU,MO,TU,WE,TH,FR,SA")
    }

    @Test
    fun monthly_same_day() {
        val r = Recurrence(dateFor("2019-09-27"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_MONTH
        }
        testRRule(r, "RRULE:DTSTART=20190927T000000;FREQ=MONTHLY;BYMONTHDAY=27")
    }

    @Test
    fun monthly_same_week() {
        val r1 = Recurrence(dateFor("2019-09-27"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
        }
        testRRule(r1, "RRULE:DTSTART=20190927T000000;FREQ=MONTHLY;BYDAY=4FR")

        val r2 = Recurrence(dateFor("2019-09-30"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
        }
        testRRule(r2, "RRULE:DTSTART=20190930T000000;FREQ=MONTHLY;BYDAY=-1MO")
    }

    @Test
    fun monthly_last_day() {
        val r = Recurrence(dateFor("2019-09-30"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH
        }
        testRRule(r, "RRULE:DTSTART=20190930T000000;FREQ=MONTHLY;BYMONTHDAY=-1")
    }

    @Test
    fun yearly() {
        val r = Recurrence(dateFor("2000-03-15"), Period.YEARLY)
        testRRule(r, "RRULE:DTSTART=20000315T000000;FREQ=YEARLY;BYMONTH=3;BYMONTHDAY=15")
    }

    @Test
    fun daily_endDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endDate = dateFor("2020-01-01")
        }
        testRRule(r, "RRULE:DTSTART=20190101T000000;FREQ=DAILY;UNTIL=20200101T000000")
    }

    @Test
    fun daily_endCount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endCount = 42
        }
        testRRule(r, "RRULE:DTSTART=20190101T000000;FREQ=DAILY;COUNT=42")
    }


    private fun testRRule(r: Recurrence, rrule: String) {
        assertEquals(rrule, formatter.format(r))
        assertEquals(r, formatter.parse(rrule))
    }

}
