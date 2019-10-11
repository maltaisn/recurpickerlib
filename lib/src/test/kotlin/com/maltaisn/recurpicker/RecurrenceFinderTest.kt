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

import com.maltaisn.recurpicker.Recurrence.MonthlyDay
import com.maltaisn.recurpicker.Recurrence.Period
import org.junit.Test
import kotlin.test.assertEquals


internal class RecurrenceFinderTest {

    private val finder = RecurrenceFinder()

    @Test(expected = IllegalArgumentException::class)
    fun wrongAmount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY)
        finder.find(r, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noStartDate() {
        val r = Recurrence(Recurrence.DATE_NONE, Period.DAILY)
        finder.find(r, 0)
    }

    @Test
    fun none() {
        val r = Recurrence(dateFor("2019-01-01"), Period.NONE)
        assertEquals(listOf(
                dateFor("2019-01-01")
        ), finder.find(r, 10))
    }

    @Test
    fun none_fromDate() {
        val r1 = Recurrence(dateFor("2019-01-01"), Period.NONE)
        assertEquals(listOf(
                dateFor("2019-01-01")
        ), finder.find(r1, 10, dateFor("2019-01-01")))

        val r2 = Recurrence(dateFor("2019-01-01"), Period.NONE)
        assertEquals(emptyList<Long>(), finder.find(r2, 10, dateFor("2019-01-02")))
    }

    @Test
    fun daily() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY)
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-02"),
                dateFor("2019-01-03"),
                dateFor("2019-01-04"),
                dateFor("2019-01-05")
        ), finder.find(r, 5))
    }

    @Test
    fun daily_frequency3() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            frequency = 3
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-04"),
                dateFor("2019-01-07"),
                dateFor("2019-01-10"),
                dateFor("2019-01-13")
        ), finder.find(r, 5))
    }

    @Test
    fun daily_endDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endDate = dateFor("2019-01-03")
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-02"),
                dateFor("2019-01-03")
        ), finder.find(r, 1000))
    }

    @Test
    fun daily_endCount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-02"),
                dateFor("2019-01-03"),
                dateFor("2019-01-04"),
                dateFor("2019-01-05")
        ), finder.find(r, 1000))
    }

    @Test
    fun daily_basedOn() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-01-03"),
                dateFor("2019-01-04"),
                dateFor("2019-01-05")
        ), finder.findBasedOn(r, dateFor("2019-01-03"), 3, 1000))
    }

    @Test
    fun daily_basedOn_fromDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endCount = 6
        }
        assertEquals(listOf(
                dateFor("2019-01-05"),
                dateFor("2019-01-06")
        ), finder.findBasedOn(r, dateFor("2019-01-03"), 3, 1000, dateFor("2019-01-05")))
    }

    @Test
    fun weekly_tue() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY)
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-08"),
                dateFor("2019-01-15"),
                dateFor("2019-01-22"),
                dateFor("2019-01-29")
        ), finder.find(r, 5))
    }

    @Test
    fun weekly_sun_mon_wed() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            setWeekDays(Recurrence.SUNDAY, Recurrence.MONDAY, Recurrence.WEDNESDAY)
        }
        assertEquals(listOf(
                dateFor("2019-01-02"),
                dateFor("2019-01-06"),
                dateFor("2019-01-07"),
                dateFor("2019-01-09"),
                dateFor("2019-01-13")
        ), finder.find(r, 5))
    }

    @Test
    fun weekly_all_days() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            frequency = 2
            weeklyDays = Recurrence.EVERY_DAY_OF_WEEK
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
        ), finder.find(r, 8))
    }

    @Test
    fun weekly_frequency3() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            frequency = 3
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-22"),
                dateFor("2019-02-12"),
                dateFor("2019-03-05"),
                dateFor("2019-03-26")
        ), finder.find(r, 5))
    }

    @Test
    fun weekly_endDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            endDate = dateFor("2019-01-15")
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-08"),
                dateFor("2019-01-15")
        ), finder.find(r, 1000))
    }

    @Test
    fun weekly_endCount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            endCount = 4
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-01-08"),
                dateFor("2019-01-15"),
                dateFor("2019-01-22")
        ), finder.find(r, 1000))
    }

    @Test
    fun weekly_basedOn() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-01-15"),
                dateFor("2019-01-22"),
                dateFor("2019-01-29")
        ), finder.findBasedOn(r, dateFor("2019-01-15"), 3, 1000))
    }

    @Test
    fun weekly_basedOn_fromDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.WEEKLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-01-22"),
                dateFor("2019-01-29")
        ), finder.findBasedOn(r, dateFor("2019-01-15"), 3, 1000, dateFor("2019-01-22")))
    }

    @Test
    fun monthly_sameDay() {
        val r = Recurrence(dateFor("2019-01-01"), Period.MONTHLY)
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-02-01"),
                dateFor("2019-03-01"),
                dateFor("2019-04-01"),
                dateFor("2019-05-01")
        ), finder.find(r, 5))
    }

    @Test
    fun monthly_sameDay_31() {
        val r = Recurrence(dateFor("2019-01-31"), Period.MONTHLY) {
            frequency = 3
        }
        assertEquals(listOf(
                dateFor("2019-01-31"),
                dateFor("2019-07-31"),
                dateFor("2019-10-31"),
                dateFor("2020-01-31")
        ), finder.find(r, 4))
    }

    @Test
    fun monthly_sameWeek_third() {
        val r = Recurrence(dateFor("2019-01-15"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
        }
        assertEquals(listOf(
                dateFor("2019-01-15"),
                dateFor("2019-02-19"),
                dateFor("2019-03-19"),
                dateFor("2019-04-16"),
                dateFor("2019-05-21")
        ), finder.find(r, 5))
    }

    @Test
    fun monthly_lastDay() {
        val r = Recurrence(dateFor("2019-01-31"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH
        }
        assertEquals(listOf(
                dateFor("2019-01-31"),
                dateFor("2019-02-28"),
                dateFor("2019-03-31"),
                dateFor("2019-04-30"),
                dateFor("2019-05-31")
        ), finder.find(r, 5))
    }

    @Test
    fun monthly_sameWeek_last() {
        val r = Recurrence(dateFor("2019-01-31"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
        }
        assertEquals(listOf(
                dateFor("2019-01-31"),
                dateFor("2019-02-28"),
                dateFor("2019-03-28"),
                dateFor("2019-04-25"),
                dateFor("2019-05-30")
        ), finder.find(r, 5))
    }

    @Test
    fun monthly_frequency3() {
        val r = Recurrence(dateFor("2019-01-01"), Period.MONTHLY) {
            frequency = 3
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-04-01"),
                dateFor("2019-07-01"),
                dateFor("2019-10-01"),
                dateFor("2020-01-01")
        ), finder.find(r, 5))
    }

    @Test
    fun monthly_endDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.MONTHLY) {
            endDate = dateFor("2019-03-01")
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-02-01"),
                dateFor("2019-03-01")
        ), finder.find(r, 1000))
    }

    @Test
    fun monthly_endCount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.MONTHLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2019-02-01"),
                dateFor("2019-03-01"),
                dateFor("2019-04-01"),
                dateFor("2019-05-01")
        ), finder.find(r, 1000))
    }

    @Test
    fun monthly_basedOn() {
        val r = Recurrence(dateFor("2019-01-01"), Period.MONTHLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-03-01"),
                dateFor("2019-04-01"),
                dateFor("2019-05-01")
        ), finder.findBasedOn(r, dateFor("2019-03-01"), 3, 1000))
    }

    @Test
    fun monthly_basedOn_fromDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.MONTHLY) {
            endCount = 6
        }
        assertEquals(listOf(
                dateFor("2019-05-01"),
                dateFor("2019-06-01")
        ), finder.findBasedOn(r, dateFor("2019-03-01"), 3, 1000, dateFor("2019-05-01")))
    }

    @Test
    fun yearly() {
        val r = Recurrence(dateFor("2019-03-23"), Period.YEARLY)
        assertEquals(listOf(
                dateFor("2019-03-23"),
                dateFor("2020-03-23"),
                dateFor("2021-03-23"),
                dateFor("2022-03-23"),
                dateFor("2023-03-23")
        ), finder.find(r, 5))
    }

    @Test
    fun yearly_frequency3() {
        val r = Recurrence(dateFor("2019-01-01"), Period.YEARLY) {
            frequency = 3
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2022-01-01"),
                dateFor("2025-01-01"),
                dateFor("2028-01-01"),
                dateFor("2031-01-01")
        ), finder.find(r, 5))
    }

    @Test
    fun yearly_endDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.YEARLY) {
            endDate = dateFor("2021-01-01")
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2020-01-01"),
                dateFor("2021-01-01")
        ), finder.find(r, 1000))
    }

    @Test
    fun yearly_endCount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.YEARLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2019-01-01"),
                dateFor("2020-01-01"),
                dateFor("2021-01-01"),
                dateFor("2022-01-01"),
                dateFor("2023-01-01")
        ), finder.find(r, 1000))
    }

    @Test
    fun yearly_basedOn() {
        val r = Recurrence(dateFor("2019-01-01"), Period.YEARLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2021-01-01"),
                dateFor("2022-01-01"),
                dateFor("2023-01-01")
        ), finder.findBasedOn(r, dateFor("2021-01-01"), 3, 1000))
    }

    @Test
    fun yearly_basedOn_fromDate() {
        val r = Recurrence(dateFor("2019-01-01"), Period.YEARLY) {
            endCount = 5
        }
        assertEquals(listOf(
                dateFor("2022-01-01"),
                dateFor("2023-01-01")
        ), finder.findBasedOn(r, dateFor("2021-01-01"), 3, 1000, dateFor("2022-01-01")))
    }

    @Test
    fun yearly_feb29() {
        val r = Recurrence(dateFor("2096-02-29"), Period.YEARLY)
        assertEquals(listOf(
                dateFor("2096-02-29"),
                dateFor("2104-02-29"),
                dateFor("2108-02-29"),
                dateFor("2112-02-29")
        ), finder.find(r, 4))
    }

    @Test
    fun findBetween() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY)
        assertEquals(listOf(
                dateFor("2019-01-27"),
                dateFor("2019-01-28"),
                dateFor("2019-01-29")
        ), finder.findBetween(r, dateFor("2019-01-27"), dateFor("2019-01-30")))
    }

    @Test
    fun findBetween_endCount() {
        val r = Recurrence(dateFor("2019-01-01"), Period.DAILY) {
            endCount = 7
        }
        assertEquals(listOf(
                dateFor("2019-01-04"),
                dateFor("2019-01-05"),
                dateFor("2019-01-06"),
                dateFor("2019-01-07")
        ), finder.findBetween(r, dateFor("2019-01-04"), dateFor("2019-01-10")))
    }

}
