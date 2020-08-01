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

package com.maltaisn.recurpicker.picker

import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.dateFor
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
internal class RecurrencePickerPresenterAttachTest {

    private val presenter = RecurrencePickerPresenter()
    private val settings = RecurrencePickerSettings {
        formatter = RecurrenceFormatter(SimpleDateFormat("yyyy-MM-dd"))
        defaultPickerRecurrence = Recurrence(Period.DAILY) {
            frequency = 3
            endDate = dateFor("2019-12-31")
            maxFrequency = 10
            maxEndCount = 100
        }
    }

    private val view: RecurrencePickerContract.View = mock {
        on { settings } doReturn settings
        on { startDate } doReturn dateFor("2019-10-15")
        on { endDateText } doReturn "p|s"
        on { getEndCountTextFor(anyInt()) } doReturn "p|s"
    }

    @Test
    fun `should throw if no start date is set`() {
        whenever(view.startDate).thenReturn(Recurrence.DATE_NONE)
        assertFailsWith<IllegalStateException> {
            presenter.attach(view, null)
        }
    }

    @Test
    fun `should throw if attached twice`() {
        presenter.attach(view, null)
        assertFailsWith<IllegalStateException> {
            presenter.attach(view, null)
        }
    }

    @Test
    fun `should use default values when no selection is passed`() {
        whenever(view.selectedRecurrence).thenReturn(null)
        presenter.attach(view, null)
        verify(view).setEndDateView("2019-12-31")
        // We'll assume the rest is set correctly too...
    }

    @Test
    fun `should use default values when selection is set to 'does not repeat'`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence.DOES_NOT_REPEAT)
        presenter.attach(view, null)
        verify(view).setEndDateView("2019-12-31")
        // We'll assume the rest is set correctly too...
    }

    @Test
    fun `should set frequency correctly`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.DAILY) {
            frequency = 749
        })
        presenter.attach(view, null)
        verify(view).setFrequencyView("749")
    }

    @Test
    fun `should set frequency max length to the one set in settings`() {
        presenter.attach(view, null)
        verify(view).setFrequencyMaxLength(2)
    }

    @Test
    fun `should set the period items and selected item for daily frequency 1`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.DAILY))
        presenter.attach(view, null)
        verify(view).setPeriodItems(1)
        verify(view).setSelectedPeriodItem(0)
    }

    @Test
    fun `should set the period items and selected item for monthly frequency 67`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) { frequency = 67 })
        presenter.attach(view, null)
        verify(view).setPeriodItems(67)
        verify(view).setSelectedPeriodItem(2)
    }

    @Test
    fun `should check day of week button to default day for default weekly recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.WEEKLY))
        presenter.attach(view, null)
        verifyWeekBtnsChecked(Calendar.TUESDAY)
    }

    @Test
    fun `should check correct day of week buttons for weekly recurrence with multiple set days`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.SUNDAY, Recurrence.WEDNESDAY, Recurrence.THURSDAY)
        })
        presenter.attach(view, null)
        verifyWeekBtnsChecked(Calendar.SUNDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
    }

    @Test
    fun `should hide weekly and monthly settings for daily recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.DAILY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
    }

    @Test
    fun `show only show weekly settings for weekly recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.WEEKLY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(true)
        verify(view).setMonthlySettingShown(false)
    }

    @Test
    fun `should only show monthly settings for monthly recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(true)
    }

    @Test
    fun `should hide weekly and monthly settings for yearly recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
    }

    @Test
    fun `should set correct monthly settings for 'same day of month' recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY))
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(false, Calendar.TUESDAY, 3)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun `should set correct monthly settings for 'same day of week in month' recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SATURDAY, 4)
        })
        whenever(view.startDate).thenReturn(dateFor("2019-10-26"))
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(false, Calendar.SATURDAY, 4)
        verify(view).setSelectedMonthlySettingItem(1)
    }

    @Test
    fun `should set correct monthly settings for 'last day of month' recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        })
        whenever(view.startDate).thenReturn(dateFor("2019-12-31"))
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(true, Calendar.TUESDAY, -1)
        verify(view).setSelectedMonthlySettingItem(2)
    }

    @Test
    fun `should fallback to 'same day of month' when setting 'nth day from last day of month' recurrence`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = -15
        })
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(false, Calendar.TUESDAY, 3)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun `should fallback to 'same day of month' when setting day in month to a different day than start date's`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = 31
        })
        presenter.attach(view, null)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun `should fallback to 'same day of month' when setting day of week in month month to a different day than start date's`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Calendar.WEDNESDAY, 3)
        })
        presenter.attach(view, null)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun `should fallback to 'same day of month' when setting day in month to a different day than start date's (last day)`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        })
        presenter.attach(view, null)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun `should check end never if recurrence never ends`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY))
        presenter.attach(view, null)
        verify(view).setEndNeverChecked(true)
        verify(view).setEndDateChecked(false)
        verify(view).setEndCountChecked(false)
    }

    @Test
    fun `should check end by date if recurrence ends by date`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endDate = dateFor("2100-01-01")
        })
        presenter.attach(view, null)
        verify(view).setEndNeverChecked(false)
        verify(view).setEndDateChecked(true)
        verify(view).setEndCountChecked(false)
    }

    @Test
    fun `should check end by count if recurrence ends by count`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endCount = 13
        })
        presenter.attach(view, null)
        verify(view).setEndNeverChecked(false)
        verify(view).setEndDateChecked(false)
        verify(view).setEndCountChecked(true)
    }

    @Test
    fun `should setup end date views if ending by date`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endDate = dateFor("2100-01-01")
        })
        presenter.attach(view, null)
        verify(view).setEndDateView("2100-01-01")
        verify(view).setEndDateViewEnabled(true)
    }

    @Test
    fun `should setup end count views if ending by count`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endCount = 13
        })
        presenter.attach(view, null)
        verify(view).setEndCountView("13")
        verify(view).setEndCountViewEnabled(true)
    }

    @Test
    fun `should set end date labels prefix`() {
        whenever(view.endDateText).thenReturn(" p ")
        presenter.attach(view, null)
        verify(view).setEndDateLabels("p", "")
    }

    @Test
    fun `should set end date labels suffix`() {
        whenever(view.endDateText).thenReturn(" | s ")
        presenter.attach(view, null)
        verify(view).setEndDateLabels("", "s")
    }

    @Test
    fun `should set end date labels prefix and suffix`() {
        whenever(view.endDateText).thenReturn(" p | s ")
        presenter.attach(view, null)
        verify(view).setEndDateLabels("p", "s")
    }

    @Test
    fun `should set end count labels prefix`() {
        whenever(view.getEndCountTextFor(anyInt())).thenReturn(" p ")
        presenter.attach(view, null)
        verify(view).setEndCountLabels("p", "")
    }

    @Test
    fun `should set end count labels suffix`() {
        whenever(view.getEndCountTextFor(anyInt())).thenReturn(" | s ")
        presenter.attach(view, null)
        verify(view).setEndCountLabels("", "s")
    }

    @Test
    fun `should set end count labels prefix and suffix`() {
        whenever(view.getEndCountTextFor(anyInt())).thenReturn(" p | s ")
        presenter.attach(view, null)
        verify(view).setEndCountLabels("p", "s")
    }

    @Test
    fun `should set end count max length to the one set in settings`() {
        presenter.attach(view, null)
        verify(view).setEndCountMaxLength(3)
    }

    private fun verifyWeekBtnsChecked(vararg days: Int) {
        var daysBf = 0
        for (day in days) {
            daysBf = daysBf or (1 shl day)
        }
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            verify(view).setWeekBtnChecked(day, (daysBf and (1 shl day)) != 0)
        }
    }
}
