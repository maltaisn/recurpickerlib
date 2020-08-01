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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
internal class RecurrencePickerPresenterTest {

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
        on { startDate } doReturn dateFor("2019-10-31")
        on { endDateText } doReturn "p|s"
        on { getEndCountTextFor(anyInt()) } doReturn "p|s"
    }

    @Before
    fun setUp() {
        presenter.attach(view, null)
        clearInvocations(view)
    }

    @Test
    fun `should set cancel result and exit when cancelled`() {
        presenter.onCancel()
        verify(view).setCancelResult()
        verify(view).exit()
    }

    @Test
    fun `should set confirm result and exit when confirmed`() {
        presenter.onConfirm()
        verify(view).setConfirmResult(settings.defaultPickerRecurrence)
        verify(view).exit()
    }

    @Test
    fun `should update frequency views and create recurrence with right frequency after changing frequency`() {
        presenter.onFrequencyChanged("6")
        verify(view).setPeriodItems(6)
        verify(view).setSelectedPeriodItem(0)
        assertEquals(6, createRecurrence().frequency)
    }

    @Test
    fun `should not update frequency views when setting frequency to current value`() {
        presenter.onFrequencyChanged("3")
        verify(view, never()).setPeriodItems(anyInt())
    }

    @Test
    fun `should update frequency views with frequency 1 when setting invalid frequency literal`() {
        presenter.onFrequencyChanged("foo")
        verify(view).setPeriodItems(1)
        verify(view).setSelectedPeriodItem(0)
    }

    @Test
    fun `should set frequency views and create recurrence with max frequency when using frequency above maximum`() {
        presenter.onFrequencyChanged("12")
        verify(view).setFrequencyView("10")
        assertEquals(10, createRecurrence().frequency)
    }

    @Test
    fun `should set frequency views and create recurrence with frequency 1 when using zero frequency`() {
        presenter.onFrequencyChanged("0")
        verify(view).setFrequencyView("1")
        assertEquals(1, createRecurrence().frequency)
    }

    @Test
    fun `should hide weekly and monthly settings when selecting daily period`() {
        presenter.onPeriodItemSelected(0)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
        assertEquals(Period.DAILY, createRecurrence().period)
    }

    @Test
    fun `should show weekly settings when selecting weekly period`() {
        presenter.onPeriodItemSelected(1)
        verify(view).setWeekBtnsShown(true)
        verify(view).setMonthlySettingShown(false)
        assertEquals(Period.WEEKLY, createRecurrence().period)
    }

    @Test
    fun `should show monthly settings when selecting monthly period`() {
        presenter.onPeriodItemSelected(2)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(true)
        assertEquals(Period.MONTHLY, createRecurrence().period)
    }

    @Test
    fun `should hide weekly and monthly settings when selecting yearly period`() {
        presenter.onPeriodItemSelected(3)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
        assertEquals(Period.YEARLY, createRecurrence().period)
    }

    @Test
    fun `should update end date to 3 days after start date when selecting daily period`() {
        presenter.onPeriodItemSelected(0)
        verify(view).setEndDateView("2019-11-02")
    }

    @Test
    fun `should update end date to 3 weeks after start date when selecting weekly period`() {
        presenter.onPeriodItemSelected(1)
        verify(view).setEndDateView("2019-11-14")
    }

    @Test
    fun `should update end date to 3 months after start date when selecting monthly period`() {
        presenter.onPeriodItemSelected(2)
        verify(view).setEndDateView("2019-12-31")
    }

    @Test
    fun `should update end date to 3 years after start date when selecting yearly period`() {
        presenter.onPeriodItemSelected(3)
        verify(view).setEndDateView("2021-10-31")
    }

    @Test
    fun `should create recurrence with right days of week set when checking day of week buttons`() {
        presenter.onPeriodItemSelected(1) // Set period to weekly
        presenter.onWeekBtnChecked(Calendar.TUESDAY, true)
        presenter.onWeekBtnChecked(Calendar.THURSDAY, false)
        presenter.onWeekBtnChecked(Calendar.FRIDAY, true)

        val r = createRecurrence()
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.TUESDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.THURSDAY))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.FRIDAY))
    }

    @Test
    fun `should create monthly recurrence on same day of month as start date when selecting 'same day of month'`() {
        presenter.onPeriodItemSelected(2) // Set period to monthly
        presenter.onMonthlySettingItemSelected(0)
        assertEquals(0, createRecurrence().byMonthDay)
    }

    @Test
    fun `should create monthly recurrence on same day of week in month as start date when selecting 'same day of week in month'`() {
        presenter.onPeriodItemSelected(2) // Set period to monthly
        presenter.onMonthlySettingItemSelected(1)

        val r = createRecurrence()
        assertEquals(Calendar.THURSDAY, r.dayOfWeekInMonth)
        assertEquals(-1, r.weekInMonth)
    }

    @Test
    fun `should create monthly recurrence on last day of month when selecting 'last day of month'`() {
        presenter.onPeriodItemSelected(2) // Set period to monthly
        presenter.onMonthlySettingItemSelected(2)
        assertEquals(-1, createRecurrence().byMonthDay)
    }

    @Test
    fun `should create recurrence that never ends when selecting 'end never'`() {
        presenter.onEndNeverClicked()
        verify(view).setEndNeverChecked(true)
        assertEquals(Recurrence.EndType.NEVER, createRecurrence().endType)
    }

    @Test
    fun `should create recurrence that ends by date when selecting 'end by date'`() {
        presenter.onEndNeverClicked() // Set to never, because already set to end by date.
        presenter.onEndDateClicked()
        verify(view).setEndDateChecked(true)
        verify(view).setEndDateViewEnabled(true)
        assertEquals(Recurrence.EndType.BY_DATE, createRecurrence().endType)
    }

    @Test
    fun `should create recurrence that ends by count when selecting 'end by count'`() {
        presenter.onEndCountClicked()
        verify(view).setEndCountChecked(true)
        verify(view).setEndCountViewEnabled(true)
        assertEquals(Recurrence.EndType.BY_COUNT, createRecurrence().endType)
    }

    @Test
    fun `should show end date dialog when end date input is clicked`() {
        presenter.onEndDateInputClicked()
        verify(view).showEndDateDialog(settings.defaultPickerRecurrence.endDate, view.startDate)
    }

    @Test
    fun `should update end date views and use entered end date when creating recurrence after changing end date`() {
        val date = dateFor("2033-01-01")
        presenter.onEndDateEntered(date)
        verify(view).setEndDateView("2033-01-01")
        assertEquals(date, createRecurrence().endDate)
    }

    @Test
    fun `should update end count views and use entered end count when creating recurrence after changing end count`() {
        presenter.onEndCountClicked()
        presenter.onEndCountChanged("99")
        verify(view).setEndCountLabels(any(), any())
        assertEquals(99, createRecurrence().endCount)
    }

    @Test
    fun `should not update end count views when setting end count to current value`() {
        presenter.onEndCountClicked()
        presenter.onEndCountChanged("1")
        verify(view, never()).setEndCountLabels(any(), any())
    }

    @Test
    fun `should update end count views with end count 1 when setting invalid end count`() {
        presenter.onEndCountClicked()
        presenter.onEndCountChanged("12") // Change it because invalid count sets to 1 and it's
        // already 1 by default, so it wouldn't be updated otherwise.
        clearInvocations(view)
        presenter.onEndCountChanged("foo")
        verify(view).setEndCountLabels(any(), any())
    }

    @Test
    fun `should update end count views with max end count when setting end count above max`() {
        presenter.onEndCountClicked()
        clearInvocations(view)
        presenter.onEndCountChanged("120")
        verify(view).setEndCountView("100")
        assertEquals(100, createRecurrence().endCount)
    }

    @Test
    fun `should update end count views with end count 1 when setting zero end count`() {
        presenter.onEndCountClicked()
        clearInvocations(view)
        presenter.onEndCountChanged("0")
        verify(view).setEndCountView("1")
        assertEquals(1, createRecurrence().endCount)
    }

    @Test
    fun `should create default weekly recurrence if only day of week checked is the same as start date's`() {
        presenter.onPeriodItemSelected(1)
        assertEquals(0x01, createRecurrence().byDay)
    }

    @Test
    fun `should create weekly recurrence with correct days of week set`() {
        presenter.onPeriodItemSelected(1)
        presenter.onWeekBtnChecked(Calendar.THURSDAY, false)
        presenter.onWeekBtnChecked(Calendar.MONDAY, true)
        assertEquals(Recurrence.MONDAY + 1, createRecurrence().byDay)
    }

    private fun createRecurrence(): Recurrence {
        // Create the recurrence, capture it, and return it.
        presenter.onConfirm()
        argumentCaptor<Recurrence>().apply {
            verify(view).setConfirmResult(capture())
            return allValues.first()
        }
    }
}
