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
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.dateFor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class RecurrenceFormatterTest {

    @Mock
    private lateinit var context: Context

    private lateinit var recurFormat: RecurrenceFormatter

    @Before
    fun setUp() {
        val resources: Resources = mock()
        whenever(context.resources).thenReturn(resources)

        // Mock all format strings, plurals and arrays used.
        // Note that plurals are ignored so test assert for values like "Every 1 days".
        whenever(resources.getStringArray(R.array.rp_days_of_week_abbr3)).thenReturn(
            arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
        whenever(resources.getString(R.string.rp_format_none)).thenReturn("Does not repeat")
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_day), anyInt(),
            anyVararg())).thenAnswer { "Every ${it.arguments[2]} days" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_week), anyInt(),
            anyVararg())).thenAnswer { "Every ${it.arguments[2]} weeks" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_month), anyInt(),
            anyVararg())).thenAnswer { "Every ${it.arguments[2]} months" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_year), anyInt(),
            anyVararg())).thenAnswer { "Every ${it.arguments[2]} years" }
        whenever(resources.getString(eq(R.string.rp_format_weekly_option),
            any())).thenAnswer { "on ${it.arguments[1]}" }
        whenever(resources.getString(R.string.rp_format_weekly_all)).thenReturn("every day of the week")
        whenever(resources.getString(R.string.rp_format_monthly_same_day)).thenReturn("on the same day each month")
        whenever(resources.getString(R.string.rp_format_monthly_last_day)).thenReturn("on the last day of the month")
        whenever(resources.getStringArray(R.array.rp_format_monthly_same_week)).thenReturn(
            arrayOf("on every %s Sunday", "", "", "", "", "", ""))
        whenever(resources.getStringArray(R.array.rp_format_monthly_ordinal)).thenReturn(
            arrayOf("first", "second", "third", "fourth", "last"))
        whenever(resources.getString(eq(R.string.rp_format_end_date), any())).thenAnswer { "until ${it.arguments[1]}" }
        whenever(resources.getQuantityString(eq(R.plurals.rp_format_end_count), anyInt(),
            anyVararg())).thenAnswer { "for ${it.arguments[2]} events" }

        recurFormat = RecurrenceFormatter(SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH))
    }

    @Test
    fun `should format 'does not repeat' recurrence`() {
        val r = Recurrence.DOES_NOT_REPEAT
        assertEquals("Does not repeat", recurFormat.format(context, r))
    }

    @Test
    fun `should format daily recurrence`() {
        val r = Recurrence(Period.DAILY)
        assertEquals("Every 1 days", recurFormat.format(context, r))
    }

    @Test
    fun `should format default weekly recurrence`() {
        val r = Recurrence(Period.WEEKLY)
        assertEquals("Every 1 weeks", recurFormat.format(context, r))
    }

    @Test
    fun `should format weekly recurrence with specific days set`() {
        val r = Recurrence(Period.WEEKLY) { setDaysOfWeek(Recurrence.SUNDAY, Recurrence.SATURDAY) }
        assertEquals("Every 1 weeks on Sun, Sat", recurFormat.format(context, r))
    }

    @Test
    fun `should format weekly recurrence with specific days set and recurring on start date`() {
        val r = Recurrence(Period.WEEKLY) { setDaysOfWeek(Recurrence.SUNDAY, Recurrence.SATURDAY) }
        assertEquals("Every 1 weeks on Sun, Sat", recurFormat.format(context, r, dateFor("2019-10-12")))
    }

    @Test
    fun `should format weekly recurrence with single day same as start date's`() {
        val r = Recurrence(Period.WEEKLY) { setDaysOfWeek(Recurrence.SATURDAY) }
        assertEquals("Every 1 weeks", recurFormat.format(context, r, dateFor("2019-10-12")))
    }

    @Test
    fun `should format weekly recurrence with single day not same as start date's`() {
        val r = Recurrence(Period.WEEKLY) { setDaysOfWeek(Recurrence.SATURDAY) }
        assertEquals("Every 1 weeks on Sat", recurFormat.format(context, r, dateFor("2019-10-13")))
    }

    @Test
    fun `should format weekly recurrence with all days of the week set`() {
        val r = Recurrence(Period.WEEKLY) {
            frequency = 2
            setDaysOfWeek(Recurrence.EVERY_DAY_OF_WEEK)
        }
        assertEquals("Every 2 weeks on every day of the week", recurFormat.format(context, r))
    }

    @Test
    fun `should format monthly recurrence`() {
        val r = Recurrence(Period.MONTHLY)
        assertEquals("Every 1 months", recurFormat.format(context, r))
    }

    @Test
    fun `should format monthly recurrence with specific day of month (with start date)`() {
        val r = Recurrence(Period.MONTHLY) { dayInMonth = 12 }
        assertEquals("Every 1 months (on the same day each month)",
            recurFormat.format(context, r, dateFor("2019-10-12")))
    }

    @Test
    fun `should fail to format monthly recurrence with specific day of month (without start date)`() {
        val r = Recurrence(Period.MONTHLY) { dayInMonth = 12 }
        assertFailsWith<IllegalArgumentException> {
            recurFormat.format(context, r)
        }
    }

    @Test
    fun `should format monthly recurrence on the last day of month`() {
        val r = Recurrence(Period.MONTHLY) { dayInMonth = -1 }
        assertEquals("Every 1 months (on the last day of the month)", recurFormat.format(context, r))
    }

    @Test
    fun `should format monthly recurrence on a day in a week of month`() {
        val r1 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SUNDAY, 2) }
        assertEquals("Every 1 months (on every second Sunday)", recurFormat.format(context, r1))

        val r2 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SUNDAY, 3) }
        assertEquals("Every 1 months (on every third Sunday)", recurFormat.format(context, r2))

        val r3 = Recurrence(Period.MONTHLY) { setDayOfWeekInMonth(Recurrence.SUNDAY, -1) }
        assertEquals("Every 1 months (on every last Sunday)", recurFormat.format(context, r3))
    }

    @Test
    fun `should format yearly recurrence`() {
        val r = Recurrence(Period.YEARLY)
        assertEquals("Every 1 years", recurFormat.format(context, r))
    }

    @Test
    fun `should format recurrence with end date`() {
        val r = Recurrence(Period.YEARLY) { endDate = dateFor("2020-01-01") }
        assertEquals("Every 1 years; until Jan 1, 2020", recurFormat.format(context, r))
    }

    @Test
    fun `should format recurrence with end count`() {
        val r = Recurrence(Period.YEARLY) { endCount = 30 }
        assertEquals("Every 1 years; for 30 events", recurFormat.format(context, r))
    }
}
