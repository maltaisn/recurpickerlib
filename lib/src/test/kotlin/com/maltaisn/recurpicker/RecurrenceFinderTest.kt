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

@file:Suppress("MaxLineLength")

package com.maltaisn.recurpicker

import com.maltaisn.recurpicker.Recurrence.Period
import org.junit.Test
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class RecurrenceFinderTest {

    // To validate events with RRule: http://jakubroztocil.github.io/rrule/

    private val finder = RecurrenceFinder()

    @Test
    fun `should fail to find events with negative max amount`() {
        val r = Recurrence(Period.DAILY)
        assertFailsWith<IllegalArgumentException> {
            finder.find(r, dateFor("2019-01-01"), -1)
        }
    }

    @Test
    fun `should fail to find events given no start date`() {
        val r = Recurrence(Period.DAILY)
        assertFailsWith<IllegalArgumentException> {
            finder.find(r, Recurrence.DATE_NONE, 1)
        }
    }

    @Test
    fun `should return empty list with amount set to zero`() {
        val r = Recurrence(Period.DAILY)
        assertEquals(emptyList<Long>(), finder.find(r, dateFor("2019-01-01"), 0))
    }

    @Test
    fun `should only find start date for 'does not repeat' recurrence`() {
        val r = Recurrence.DOES_NOT_REPEAT
        assertEquals(listOf(
            dateFor("2019-01-01")
        ), finder.find(r, dateFor("2019-01-01"), 10))
    }

    @Test
    fun `should only find start date for 'does not repeat' recurrence given a from date before start`() {
        val r1 = Recurrence.DOES_NOT_REPEAT
        assertEquals(listOf(
            dateFor("2019-01-01")
        ), finder.find(r1, dateFor("2019-01-01"), 10, dateFor("2019-01-01")))
    }

    @Test
    fun `should find no events for 'does not repeat' recurrence given a from date after start`() {
        val r2 = Recurrence.DOES_NOT_REPEAT
        assertEquals(emptyList<Long>(), finder.find(r2,
            dateFor("2019-01-01"), 10, dateFor("2019-01-02")))
    }

    @Test
    fun `should find no events for 'does not repeat' recurrence if start is excluded`() {
        val r = Recurrence.DOES_NOT_REPEAT
        assertEquals(emptyList<Long>(), finder.find(r,
            dateFor("2019-01-01"), 10, dateFor("2019-01-01"), includeStart = false))
    }

    @Test
    fun `should find events for daily recurrence`() {
        val r = Recurrence(Period.DAILY)
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-02"),
            dateFor("2019-01-03"),
            dateFor("2019-01-04"),
            dateFor("2019-01-05")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for daily recurrence from a specific date`() {
        val r = Recurrence(Period.DAILY)
        assertEquals(listOf(
            dateFor("2019-01-03"),
            dateFor("2019-01-04"),
            dateFor("2019-01-05")
        ), finder.find(r, dateFor("2019-01-01"), 3, dateFor("2019-01-03")))
    }

    @Test
    fun `should find events for daily recurrence with frequency 3`() {
        val r = Recurrence(Period.DAILY) {
            frequency = 3
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-04"),
            dateFor("2019-01-07"),
            dateFor("2019-01-10"),
            dateFor("2019-01-13")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for daily recurrence stoppping at end date`() {
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2019-01-03")
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-02"),
            dateFor("2019-01-03")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for daily recurrence stopping at end count`() {
        val r = Recurrence(Period.DAILY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-02"),
            dateFor("2019-01-03"),
            dateFor("2019-01-04"),
            dateFor("2019-01-05")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for daily recurrence based on previous event (with end count)`() {
        val r = Recurrence(Period.DAILY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-01-03"),
            dateFor("2019-01-04"),
            dateFor("2019-01-05")
        ), finder.findBasedOn(r, dateFor("2019-01-01"), dateFor("2019-01-03"), 3, 1000))
    }

    @Test
    fun `should find events for daily recurrence based on previous event (with end count and from date)`() {
        val r = Recurrence(Period.DAILY) {
            endCount = 6
        }
        assertEquals(listOf(
            dateFor("2019-01-05"),
            dateFor("2019-01-06")
        ), finder.findBasedOn(r, dateFor("2019-01-01"), dateFor("2019-01-03"), 3, 1000, dateFor("2019-01-05")))
    }

    @Test
    fun `should find events for daily recurrence excluding start date`() {
        val r = Recurrence(Period.DAILY)
        assertEquals(listOf(
            dateFor("2019-01-02"),
            dateFor("2019-01-03")
        ), finder.find(r, dateFor("2019-01-01"), 2, includeStart = false))
    }

    @Test
    fun `should find events for weekly recurrence (on the same day of the week as start date)`() {
        val r = Recurrence(Period.WEEKLY)
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-08"),
            dateFor("2019-01-15"),
            dateFor("2019-01-22"),
            dateFor("2019-01-29")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for weekly recurrence (on set days of the week)`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.SUNDAY, Recurrence.MONDAY, Recurrence.WEDNESDAY)
        }
        assertEquals(listOf(
            dateFor("2019-01-02"),
            dateFor("2019-01-06"),
            dateFor("2019-01-07"),
            dateFor("2019-01-09"),
            dateFor("2019-01-13")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for weekly recurrence from a specific date`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.SUNDAY)
        }
        assertEquals(listOf(
            dateFor("2019-01-06"),
            dateFor("2019-01-13"),
            dateFor("2019-01-20")
        ), finder.find(r, dateFor("2019-01-01"), 3, dateFor("2019-01-05")))
    }

    @Test
    fun `should find events on every day of the week for weekly recurrence (frequency 2)`() {
        val r = Recurrence(Period.WEEKLY) {
            frequency = 2
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-02"),
            dateFor("2019-01-03"),
            dateFor("2019-01-04"),
            dateFor("2019-01-05"),
            dateFor("2019-01-13"),
            dateFor("2019-01-14"),
            dateFor("2019-01-15")
        ), finder.find(r, dateFor("2019-01-01"), 8))
    }

    @Test
    fun `should find events for weekly recurrence with frequency 3`() {
        val r = Recurrence(Period.WEEKLY) {
            frequency = 3
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-22"),
            dateFor("2019-02-12"),
            dateFor("2019-03-05"),
            dateFor("2019-03-26")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for weekly recurrence stopping at end date`() {
        val r = Recurrence(Period.WEEKLY) {
            endDate = dateFor("2019-01-15")
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-08"),
            dateFor("2019-01-15")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for weekly recurrence stopping at end count`() {
        val r = Recurrence(Period.WEEKLY) {
            endCount = 4
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-01-08"),
            dateFor("2019-01-15"),
            dateFor("2019-01-22")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for weekly recurrence based on previous event (with end count)`() {
        val r = Recurrence(Period.WEEKLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-01-15"),
            dateFor("2019-01-22"),
            dateFor("2019-01-29")
        ), finder.findBasedOn(r, dateFor("2019-01-01"), dateFor("2019-01-15"), 3, 1000))
    }

    @Test
    fun `should find events for weekly recurrence based on previous event (with end count and from date)`() {
        val r = Recurrence(Period.WEEKLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-01-22"),
            dateFor("2019-01-29")
        ), finder.findBasedOn(r, dateFor("2019-01-01"), dateFor("2019-01-15"),
            3, 1000, dateFor("2019-01-22")))
    }

    @Test
    fun `should find events for weekly recurrence excluding start date`() {
        val r = Recurrence(Period.WEEKLY)
        assertEquals(listOf(
            dateFor("2019-01-08"),
            dateFor("2019-01-15")
        ), finder.find(r, dateFor("2019-01-01"), 2, includeStart = false))
    }

    @Test
    fun `should find events for weekly recurrence excluding start (with start date not being an event)`() {
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.WEDNESDAY, Recurrence.THURSDAY)
        }
        assertEquals(listOf(
            dateFor("2019-01-02"),
            dateFor("2019-01-03")
        ), finder.find(r, dateFor("2019-01-01"), 2, includeStart = false))
    }

    @Test
    fun `should find events for monthly recurrence`() {
        val r = Recurrence(Period.MONTHLY)
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-02-01"),
            dateFor("2019-03-01"),
            dateFor("2019-04-01"),
            dateFor("2019-05-01")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for monthly recurrence from a specific date`() {
        val r = Recurrence(Period.MONTHLY)
        assertEquals(listOf(
            dateFor("2020-06-01"),
            dateFor("2020-07-01"),
            dateFor("2020-08-01")
        ), finder.find(r, dateFor("2019-01-01"), 3, dateFor("2020-05-05")))
    }

    @Test
    fun `should find events for monthly recurrence (on the 31st day)`() {
        val r = Recurrence(Period.MONTHLY) {
            frequency = 3
        }
        assertEquals(listOf(
            dateFor("2019-01-31"),
            dateFor("2019-07-31"),
            dateFor("2019-10-31"),
            dateFor("2020-01-31")
        ), finder.find(r, dateFor("2019-01-31"), 4))
    }

    @Test
    fun `should find events for monthly recurrence (on the third tuesday)`() {
        val r = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.TUESDAY, 3)
        }
        assertEquals(listOf(
            dateFor("2019-01-15"),
            dateFor("2019-02-19"),
            dateFor("2019-03-19"),
            dateFor("2019-04-16"),
            dateFor("2019-05-21")
        ), finder.find(r, dateFor("2019-01-15"), 5))
    }

    @Test
    fun `should find events for monthly recurrence (on the last day)`() {
        val r = Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        }
        assertEquals(listOf(
            dateFor("2019-01-31"),
            dateFor("2019-02-28"),
            dateFor("2019-03-31"),
            dateFor("2019-04-30"),
            dateFor("2019-05-31")
        ), finder.find(r, dateFor("2019-01-15"), 5))
    }

    @Test
    fun `should find events for monthly recurrence (on the 15th to last day)`() {
        val r = Recurrence(Period.MONTHLY) {
            dayInMonth = -15
        }
        assertEquals(listOf(
            dateFor("2019-01-17"),
            dateFor("2019-02-14"),
            dateFor("2019-03-17"),
            dateFor("2019-04-16"),
            dateFor("2019-05-17")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for monthly recurrence (on the last thursday)`() {
        val r = Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.THURSDAY, -1)
        }
        assertEquals(listOf(
            dateFor("2019-01-31"),
            dateFor("2019-02-28"),
            dateFor("2019-03-28"),
            dateFor("2019-04-25"),
            dateFor("2019-05-30")
        ), finder.find(r, dateFor("2019-01-31"), 5))
    }

    @Test
    fun `should find events for monthly recurrence with frequency 3`() {
        val r = Recurrence(Period.MONTHLY) {
            frequency = 3
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-04-01"),
            dateFor("2019-07-01"),
            dateFor("2019-10-01"),
            dateFor("2020-01-01")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for monthly recurrence stopping at end date`() {
        val r = Recurrence(Period.MONTHLY) {
            endDate = dateFor("2019-03-01")
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-02-01"),
            dateFor("2019-03-01")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for monthly recurrence stopping at end count`() {
        val r = Recurrence(Period.MONTHLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2019-02-01"),
            dateFor("2019-03-01"),
            dateFor("2019-04-01"),
            dateFor("2019-05-01")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for monthly recurrence based on previous event (with end count)`() {
        val r = Recurrence(Period.MONTHLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-03-01"),
            dateFor("2019-04-01"),
            dateFor("2019-05-01")
        ), finder.findBasedOn(r, dateFor("2019-01-01"),
            dateFor("2019-03-01"), 3, 1000))
    }

    @Test
    fun `should find events for monthly recurrence based on previous event (with end count and from date)`() {
        val r = Recurrence(Period.MONTHLY) {
            endCount = 6
        }
        assertEquals(listOf(
            dateFor("2019-05-01"),
            dateFor("2019-06-01")
        ), finder.findBasedOn(r, dateFor("2019-01-01"),
            dateFor("2019-03-01"), 3, 1000, dateFor("2019-05-01")))
    }

    @Test
    fun `should find events for monthly recurrence on last day, starting from over one month after 1st event, but before 2nd`() {
        val r = Recurrence(Period.MONTHLY) {
            endCount = 6
            byMonthDay = -1
        }
        assertEquals(listOf(
            dateFor("2019-03-31"),
            dateFor("2019-04-30"),
            dateFor("2019-05-31")
        ), finder.find(r, dateFor("2019-02-28"), 3, dateFor("2019-03-30")))
    }

    @Test
    fun `should find events for monthly recurrence excluding start`() {
        val r = Recurrence(Period.MONTHLY)
        assertEquals(listOf(
            dateFor("2019-02-01"),
            dateFor("2019-03-01")
        ), finder.find(r, dateFor("2019-01-01"), 2, includeStart = false))
    }

    @Test
    fun `should find events for yearly recurrence`() {
        val r = Recurrence(Period.YEARLY)
        assertEquals(listOf(
            dateFor("2019-03-23"),
            dateFor("2020-03-23"),
            dateFor("2021-03-23"),
            dateFor("2022-03-23"),
            dateFor("2023-03-23")
        ), finder.find(r, dateFor("2019-03-23"), 5))
    }

    @Test
    fun `should find events for yearly recurrence with frequency 3`() {
        val r = Recurrence(Period.YEARLY) {
            frequency = 3
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2022-01-01"),
            dateFor("2025-01-01"),
            dateFor("2028-01-01"),
            dateFor("2031-01-01")
        ), finder.find(r, dateFor("2019-01-01"), 5))
    }

    @Test
    fun `should find events for yearly recurrence stopping at end date`() {
        val r = Recurrence(Period.YEARLY) {
            endDate = dateFor("2021-01-01")
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2020-01-01"),
            dateFor("2021-01-01")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for yearly recurrence stopping at end count`() {
        val r = Recurrence(Period.YEARLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2019-01-01"),
            dateFor("2020-01-01"),
            dateFor("2021-01-01"),
            dateFor("2022-01-01"),
            dateFor("2023-01-01")
        ), finder.find(r, dateFor("2019-01-01"), 1000))
    }

    @Test
    fun `should find events for yearly recurrence based on previous event (with end count)`() {
        val r = Recurrence(Period.YEARLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2021-01-01"),
            dateFor("2022-01-01"),
            dateFor("2023-01-01")
        ), finder.findBasedOn(r, dateFor("2019-01-01"),
            dateFor("2021-01-01"), 3, 1000))
    }

    @Test
    fun `should find events for yearly recurrence based on previous event (with end count and from date)`() {
        val r = Recurrence(Period.YEARLY) {
            endCount = 5
        }
        assertEquals(listOf(
            dateFor("2022-01-01"),
            dateFor("2023-01-01")
        ), finder.findBasedOn(r, dateFor("2019-01-01"),
            dateFor("2021-01-01"), 3, 1000, dateFor("2022-01-01")))
    }

    @Test
    fun `should find events for yearly recurrence excluding start`() {
        val r = Recurrence(Period.YEARLY)
        assertEquals(listOf(
            dateFor("2020-01-01"),
            dateFor("2021-01-01")
        ), finder.find(r, dateFor("2019-01-01"), 2, includeStart = false))
    }

    @Test
    fun `should find events for yearly recurrence excluding start and based on previous event`() {
        val r = Recurrence(Period.YEARLY)
        assertEquals(listOf(
            dateFor("2022-01-01"),
            dateFor("2023-01-01")
        ), finder.findBasedOn(r, dateFor("2019-01-01"), dateFor("2021-01-01"),
            2, 2, includeStart = false))
    }

    @Test
    fun `should find events for yearly recurrence on feb 29`() {
        val r = Recurrence(Period.YEARLY)
        assertEquals(listOf(
            dateFor("2096-02-29"),
            dateFor("2104-02-29"),
            dateFor("2108-02-29"),
            dateFor("2112-02-29")
        ), finder.find(r, dateFor("2096-02-29"), 4))
    }

    @Test
    fun `should find events between two dates (daily recurrence)`() {
        val r = Recurrence(Period.DAILY)
        assertEquals(listOf(
            dateFor("2019-01-27"),
            dateFor("2019-01-28"),
            dateFor("2019-01-29")
        ), finder.findBetween(r, dateFor("2019-01-01"),
            dateFor("2019-01-27"), dateFor("2019-01-30")))
    }

    @Test
    fun `should find events between two dates (recurrence with end count)`() {
        val r = Recurrence(Period.DAILY) {
            endCount = 7
        }
        assertEquals(listOf(
            dateFor("2019-01-04"),
            dateFor("2019-01-05"),
            dateFor("2019-01-06"),
            dateFor("2019-01-07")
        ), finder.findBetween(r, dateFor("2019-01-01"),
            dateFor("2019-01-04"), dateFor("2019-01-10")))
    }

    @Test
    fun `should find events between two dates (no events)`() {
        val r = Recurrence(Period.MONTHLY)
        assertEquals(emptyList<Long>(), finder.findBetween(r, dateFor("2019-01-01"),
            dateFor("2019-01-04"), dateFor("2019-01-10")))
    }

    @Test
    fun `should find events for recurrence using non-default timezone`() {
        // This test should fail if not setting the timezone.
        finder.timeZone = TimeZone.getTimeZone("GMT+07:00")
        val r = Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.THURSDAY)
        }
        assertEquals(listOf(
            dateFor("2020-07-30T00:00:00.000+07:00"),
            dateFor("2020-08-06T00:00:00.000+07:00")
        ), finder.find(r, dateFor("2020-07-29T00:00:00.000+07:00"), 2))
    }

    @Test
    fun `should find events for recurrence until date with timezone`() {
        finder.timeZone = TimeZone.getTimeZone("GMT-04:00")
        val r = Recurrence(Period.DAILY) {
            endDate = dateFor("2020-08-08T00:00:00.000+04:00")
        }
        assertEquals(listOf(
            dateFor("2020-08-06T00:00:00.000+04:00"),
            dateFor("2020-08-07T00:00:00.000+04:00"),
            dateFor("2020-08-08T00:00:00.000+04:00")
        ), finder.find(r, dateFor("2020-08-06T00:00:00.000+04:00"), 1000))
    }

    @Test
    fun `should find events for recurrence, keeping original time of the day`() {
        val r = Recurrence(Period.DAILY)
        assertEquals(listOf(
            dateFor("2020-07-29T07:34:12.001"),
            dateFor("2020-07-30T07:34:12.001"),
            dateFor("2020-07-31T07:34:12.001")
        ), finder.find(r, dateFor("2020-07-29T07:34:12.001"), 3))
    }
}
