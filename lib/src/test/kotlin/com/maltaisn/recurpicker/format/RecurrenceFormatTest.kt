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

import android.content.Context
import android.content.res.Resources
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.MonthlyDay
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.dateFor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals


@RunWith(MockitoJUnitRunner::class)
class RecurrenceFormatTest {

    @Mock
    private lateinit var resources: Resources

    private lateinit var recurFormat: RecurrenceFormatter

    @Before
    fun setUp() {
        val context = mock<Context>()
        whenever(context.resources).thenReturn(resources)

        // Mock all format strings, plurals and arrays used.
        whenever(resources.getStringArray(R.array.rp_days_of_week_abbr)).thenReturn(
                arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
        whenever(resources.getString(R.string.rp_format_none)).thenReturn("Does not repeat")
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_day), any())).thenAnswer { "Every ${it.arguments[1]} days" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_week), any())).thenAnswer { "Every ${it.arguments[1]} weeks" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_month), any())).thenAnswer { "Every ${it.arguments[1]} months" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_year), any())).thenAnswer { "Every ${it.arguments[1]} years" }
        whenever(resources.getString(eq(R.string.rp_format_weekly_option), any())).thenAnswer { "on ${it.arguments[1]}" }
        whenever(resources.getString(R.string.rp_format_weekly_all)).thenReturn("every day of the week")
        whenever(resources.getString(R.string.rp_format_monthly_same_day)).thenReturn("on the same day each month")
        whenever(resources.getString(R.string.rp_format_monthly_last_day)).thenReturn("on the last day of the month")
        whenever(resources.getIntArray(R.array.rp_format_monthly_same_week)).thenReturn(
                intArrayOf(R.string.rp_format_monthly_sun, 0, 0, 0, 0, 0, 0, 0))
        whenever(resources.getString(eq(R.string.rp_format_monthly_sun), any())).thenAnswer { "on every ${it.arguments[1]} Sunday" }
        whenever(resources.getStringArray(R.array.rp_format_monthly_ordinal)).thenReturn(
                arrayOf("first", "second", "third", "fourth", "last"))
        whenever(resources.getString(eq(R.string.rp_format_end_date), any())).thenAnswer { "until ${it.arguments[1]}" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_end_count), any())).thenAnswer { "for ${it.arguments[1]} events" }

        recurFormat = RecurrenceFormatter(context, SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH))
    }

    @Test
    fun format_doesNotRepeat() {
        val r = Recurrence(dateFor("2019-09-24"), Period.NONE)
        assertEquals("Does not repeat", recurFormat.format(r))
    }

    @Test
    fun format_daily() {
        val r = Recurrence(0, Period.DAILY) { isDefault = true }
        assertEquals("Every 1 days", recurFormat.format(r))
    }

    @Test
    fun format_weekly() {
        val r = Recurrence(0, Period.WEEKLY) { isDefault = true }
        assertEquals("Every 1 weeks", recurFormat.format(r))
    }

    @Test
    fun format_monthly() {
        val r = Recurrence(0, Period.MONTHLY) { isDefault = true }
        assertEquals("Every 1 months", recurFormat.format(r))
    }

    @Test
    fun format_yearly() {
        val r = Recurrence(0, Period.YEARLY) { isDefault = true }
        assertEquals("Every 1 years", recurFormat.format(r))
    }

    @Test
    fun format_weekly_days() {
        val r1 = Recurrence(0, Period.WEEKLY) { weeklyDays = Recurrence.FRIDAY or Recurrence.SATURDAY }
        assertEquals("Every 1 weeks on Fri, Sat", recurFormat.format(r1))

        val r2 = Recurrence(0, Period.WEEKLY) { weeklyDays = Recurrence.MONDAY or Recurrence.TUESDAY or Recurrence.WEDNESDAY }
        assertEquals("Every 1 weeks on Mon, Tue, Wed", recurFormat.format(r2))
    }

    @Test
    fun format_weekly_everyDay() {
        val r = Recurrence(0, Period.WEEKLY) {
            frequency = 2
            weeklyDays = Recurrence.EVERY_DAY_OF_WEEK
        }
        assertEquals("Every 2 weeks on every day of the week", recurFormat.format(r))
    }

    @Test
    fun format_monthly_lastDay() {
        val r = Recurrence(dateFor("2018-12-31"), Period.MONTHLY) { monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH }
        assertEquals("Every 1 months (on the last day of the month)", recurFormat.format(r))
    }

    @Test
    fun format_monthly_sameDayOfWeek() {
        val r1 = Recurrence(dateFor("2019-09-08"), Period.MONTHLY) { monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK }
        assertEquals("Every 1 months (on every second Sunday)", recurFormat.format(r1))

        val r2 = Recurrence(dateFor("2019-09-15"), Period.MONTHLY) { monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK }
        assertEquals("Every 1 months (on every third Sunday)", recurFormat.format(r2))

        val r3 = Recurrence(dateFor("2019-09-29"), Period.MONTHLY) { monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK }
        assertEquals("Every 1 months (on every last Sunday)", recurFormat.format(r3))
    }

    @Test
    fun format_monthly_sameDay() {
        val r1 = Recurrence(dateFor("2019-09-08"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_MONTH
            isDefault = true
        }
        assertEquals("Every 1 months", recurFormat.format(r1))

        val r2 = Recurrence(dateFor("2019-09-08"), Period.MONTHLY) {
            frequency = 2
            monthlyDay = MonthlyDay.SAME_DAY_OF_MONTH
        }
        assertEquals("Every 2 months (on the same day each month)", recurFormat.format(r2))
    }

    @Test
    fun format_end_date() {
        val r = Recurrence(dateFor("2018-01-01"), Period.YEARLY) { endDate = dateFor("2020-01-01") }
        assertEquals("Every 1 years; until Jan 1, 2020", recurFormat.format(r))
    }

    @Test
    fun format_end_count() {
        val r = Recurrence(dateFor("2018-01-01"), Period.YEARLY) { endCount = 30 }
        assertEquals("Every 1 years; for 30 events", recurFormat.format(r))
    }

}
