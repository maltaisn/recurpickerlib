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
import java.util.*


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

    @Test(expected = IllegalStateException::class)
    fun attach_shouldThrowNoStartDate() {
        whenever(view.startDate).thenReturn(Recurrence.DATE_NONE)
        presenter.attach(view, null)
    }

    @Test(expected = IllegalStateException::class)
    fun attach_shouldThrowAttachTwice() {
        presenter.attach(view, null)
        presenter.attach(view, null)
    }

    @Test
    fun attach_verifyDefaultIsUsed_noSelection() {
        whenever(view.selectedRecurrence).thenReturn(null)
        presenter.attach(view, null)
        verify(view).setEndDateView("2019-12-31")
        // We'll assume the rest is set correctly too...
    }

    @Test
    fun attach_verifyDefaultIsUsed_periodNone() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence.DOES_NOT_REPEAT)
        presenter.attach(view, null)
        verify(view).setEndDateView("2019-12-31")
        // We'll assume the rest is set correctly too...
    }

    @Test
    fun attach_verifyFrequency() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.DAILY) {
            frequency = 749
        })
        presenter.attach(view, null)
        verify(view).setFrequencyView("749")
    }

    @Test
    fun attach_verifyFrequencyMaxLength() {
        presenter.attach(view, null)
        verify(view).setFrequencyMaxLength(2)
    }

    @Test
    fun attach_verifyPeriodDropdown_daily() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.DAILY))
        presenter.attach(view, null)
        verify(view).setPeriodItems(1)
        verify(view).setSelectedPeriodItem(0)
    }

    @Test
    fun attach_verifyPeriodDropdown_monthly() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) { frequency = 67 })
        presenter.attach(view, null)
        verify(view).setPeriodItems(67)
        verify(view).setSelectedPeriodItem(2)
    }

    @Test
    fun attach_verifyWeekBtnsChecked_defaultDay() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.WEEKLY))
        presenter.attach(view, null)
        verifyWeekBtnsChecked(Calendar.TUESDAY)
    }

    @Test
    fun attach_verifyWeekBtnsChecked_customDays() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.WEEKLY) {
            setDaysOfWeek(Recurrence.SUNDAY, Recurrence.WEDNESDAY, Recurrence.THURSDAY)
        })
        presenter.attach(view, null)
        verifyWeekBtnsChecked(Calendar.SUNDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
    }

    @Test
    fun attach_verifyNoSettingsShown() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.DAILY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
    }

    @Test
    fun attach_verifyWeeklySettingsShown() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.WEEKLY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(true)
        verify(view).setMonthlySettingShown(false)
    }

    @Test
    fun attach_verifyMonthlySettingsShown() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY))
        presenter.attach(view, null)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(true)
    }

    @Test
    fun attach_verifyMonthlyDropdown() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY))
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(false, Calendar.TUESDAY, 3)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun attach_verifyMonthlyDropdown_sameWeek() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SATURDAY, 4)
        })
        whenever(view.startDate).thenReturn(dateFor("2019-10-26"))
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(false, Calendar.SATURDAY, 4)
        verify(view).setSelectedMonthlySettingItem(1)
    }

    @Test
    fun attach_verifyMonthlyDropdown_lastDay() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        })
        whenever(view.startDate).thenReturn(dateFor("2019-12-31"))
        presenter.attach(view, null)
        verify(view).setMonthlySettingItems(true, Calendar.TUESDAY, -1)
        verify(view).setSelectedMonthlySettingItem(2)
    }

    @Test
    fun attach_verifyMonthlyDropdown_sameDayDifferentThanStartDate() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = 31
        })
        presenter.attach(view, null)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun attach_verifyMonthlyDropdown_sameWeekDifferentThanStartDate() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Calendar.WEDNESDAY, 3)
        })
        presenter.attach(view, null)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun attach_verifyMonthlyDropdown_lastDayDifferentThanStartDate() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.MONTHLY) {
            dayInMonth = -1
        })
        presenter.attach(view, null)
        verify(view).setSelectedMonthlySettingItem(0)
    }

    @Test
    fun attach_verifyEndNeverChecked() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY))
        presenter.attach(view, null)
        verify(view).setEndNeverChecked(true)
        verify(view).setEndDateChecked(false)
        verify(view).setEndCountChecked(false)
    }

    @Test
    fun attach_verifyEndDateChecked() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endDate = dateFor("2100-01-01")
        })
        presenter.attach(view, null)
        verify(view).setEndNeverChecked(false)
        verify(view).setEndDateChecked(true)
        verify(view).setEndCountChecked(false)
    }

    @Test
    fun attach_verifyEndCountChecked() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endCount = 13
        })
        presenter.attach(view, null)
        verify(view).setEndNeverChecked(false)
        verify(view).setEndDateChecked(false)
        verify(view).setEndCountChecked(true)
    }

    @Test
    fun attach_verifyEndDateViews() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endDate = dateFor("2100-01-01")
        })
        presenter.attach(view, null)
        verify(view).setEndDateView("2100-01-01")
        verify(view).setEndDateViewEnabled(true)
    }

    @Test
    fun attach_verifyEndCountViews() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Period.YEARLY) {
            endCount = 13
        })
        presenter.attach(view, null)
        verify(view).setEndCountView("13")
        verify(view).setEndCountViewEnabled(true)
    }

    @Test
    fun attach_verifyEndDateLabels_prefix() {
        whenever(view.endDateText).thenReturn(" p ")
        presenter.attach(view, null)
        verify(view).setEndDateLabels("p", "")
    }

    @Test
    fun attach_verifyEndDateLabels_suffix() {
        whenever(view.endDateText).thenReturn(" | s ")
        presenter.attach(view, null)
        verify(view).setEndDateLabels("", "s")
    }

    @Test
    fun attach_verifyEndDateLabels_prefix_suffix() {
        whenever(view.endDateText).thenReturn(" p | s ")
        presenter.attach(view, null)
        verify(view).setEndDateLabels("p", "s")
    }

    @Test
    fun attach_verifyEndCountLabels_prefix() {
        whenever(view.getEndCountTextFor(anyInt())).thenReturn(" p ")
        presenter.attach(view, null)
        verify(view).setEndCountLabels("p", "")
    }

    @Test
    fun attach_verifyEndCountLabels_suffix() {
        whenever(view.getEndCountTextFor(anyInt())).thenReturn(" | s ")
        presenter.attach(view, null)
        verify(view).setEndCountLabels("", "s")
    }

    @Test
    fun attach_verifyEndCountLabels_prefix_suffix() {
        whenever(view.getEndCountTextFor(anyInt())).thenReturn(" p | s ")
        presenter.attach(view, null)
        verify(view).setEndCountLabels("p", "s")
    }

    @Test
    fun attach_verifyEndCountMaxLength() {
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
