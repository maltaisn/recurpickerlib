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

import android.os.Bundle
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.EndType
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.picker.RecurrencePickerContract.Presenter
import com.maltaisn.recurpicker.picker.RecurrencePickerContract.View
import java.util.*


internal class RecurrencePickerPresenter : Presenter {

    private var view: View? = null

    private val settings: RecurrencePickerSettings
        get() = view!!.settings


    private var period = Period.NONE
    private var frequency = 0
    private var daysOfWeek = 0
    private var monthlySettingIndex = -1
    private var endType = EndType.NEVER
    private var endDate = Recurrence.DATE_NONE
    private var endCount = 0

    private val startDateCal = Calendar.getInstance()


    override fun attach(view: View, state: Bundle?) {
        check(this.view == null) { "Presenter already attached." }
        this.view = view

        check(view.startDate != Recurrence.DATE_NONE) { "Start date must be set on RecurrencePickerFragment." }
        startDateCal.timeInMillis = view.startDate

        if (state == null) {
            // Get the selected recurrence or the default if none set or recurrence is "Does not repeat".
            var r = view.selectedRecurrence
            if (r == null || r.period == Period.NONE) {
                r = settings.defaultPickerRecurrence
            }

            // Intialize fields for the selected recurrence.
            period = r.period
            frequency = r.frequency
            daysOfWeek = r.daysOfWeekOrDefault
            monthlySettingIndex = r.monthlySettingIndexOrDefault
            endType = r.endType
            endDate = r.endDate
            endCount = r.endCountOrDefault
            setDefaultEndDate()

        } else {
            // Read saved state.
            period = state.getSerializable("period") as Period
            frequency = state.getInt("frequency")
            daysOfWeek = state.getInt("daysOfWeek")
            monthlySettingIndex = state.getInt("monthlySettingIndex")
            endType = state.getSerializable("endType") as EndType
            endDate = state.getLong("endDate")
            endCount = state.getInt("endCount")
        }

        // Update view state
        view.apply {
            setFrequencyView(frequency.toString())
            updatePeriodDropdown()

            updatePeriodSettingViews()
            updateCheckedWeekBtns()
            setMonthlySettingItems(isStartDateOnLastDay, startDayOfWeek, startWeekInMonth)
            setSelectedMonthlySettingItem(monthlySettingIndex)

            updateCheckedEndType()
            updateEndDateView()
            updateEndDateLabels()
            setEndCountView(endCount.toString())
            updateEndCountLabels()
        }
    }

    override fun detach() {
        view = null

        period = Period.NONE
        frequency = 0
        daysOfWeek = 0
        monthlySettingIndex = -1
        endType = EndType.NEVER
        endDate = Recurrence.DATE_NONE
        endCount = 0
    }

    override fun saveState(state: Bundle) {
        state.putSerializable("period", period)
        state.putInt("frequency", frequency)
        state.putInt("daysOfWeek", daysOfWeek)
        state.putInt("monthlySettingIndex", monthlySettingIndex)
        state.putSerializable("endType", endType)
        state.putLong("endDate", endDate)
        state.putInt("endCount", endCount)
    }

    override fun onCancel() {
        view?.setCancelResult()
        view?.exit()
    }

    override fun onConfirm() {
        view?.setConfirmResult(buildRecurrence())
        view?.exit()
    }

    override fun onFrequencyChanged(frequencyStr: String) {
        val newFrequency = try {
            frequencyStr.toInt()
        } catch (e: NumberFormatException) {
            1
        }
        if (newFrequency != frequency) {
            frequency = newFrequency
            updatePeriodDropdown()
        }
    }

    override fun onPeriodItemSelected(index: Int) {
        period = when (index) {
            0 -> Period.DAILY
            1 -> Period.WEEKLY
            2 -> Period.MONTHLY
            else -> Period.YEARLY
        }
        setDefaultEndDate(true)
        updatePeriodSettingViews()
        view?.clearFocus()
    }

    override fun onWeekBtnChecked(dayOfWeek: Int, checked: Boolean) {
        daysOfWeek = if (checked) {
            daysOfWeek or (1 shl dayOfWeek)
        } else {
            daysOfWeek and (1 shl dayOfWeek).inv()
        }
        view?.clearFocus()
    }

    override fun onMonthlySettingItemSelected(index: Int) {
        monthlySettingIndex = index
        view?.clearFocus()
    }

    override fun onEndNeverClicked() {
        endType = EndType.NEVER
        updateCheckedEndType()
        view?.clearFocus()
    }

    override fun onEndDateClicked() {
        endType = EndType.BY_DATE
        updateCheckedEndType()
        view?.clearFocus()
    }

    override fun onEndCountClicked() {
        endType = EndType.BY_COUNT
        updateCheckedEndType()
        view?.clearFocus()
    }

    override fun onEndDateInputClicked() {
        val view = view ?: return
        view.showEndDateDialog(endDate, view.startDate)
    }

    override fun onEndDateEntered(date: Long) {
        endDate = date
        updateEndDateView()
    }

    override fun onEndCountChanged(endCountStr: String) {
        val newEndCount = try {
            endCountStr.toInt()
        } catch (e: NumberFormatException) {
            1
        }
        if (newEndCount != endCount) {
            endCount = newEndCount
            updateEndCountLabels()
        }
    }

    private fun updatePeriodDropdown() {
        view?.setPeriodItems(frequency)
        view?.setSelectedPeriodItem(period.ordinal - 1)
    }

    private fun updatePeriodSettingViews() {
        view?.setWeekBtnsShown(period == Period.WEEKLY)
        view?.setMonthlySettingShown(period == Period.MONTHLY)
    }

    private fun updateCheckedWeekBtns() {
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            view?.setWeekBtnChecked(day, daysOfWeek and (1 shl day) != 0)
        }
    }

    private fun updateEndDateView() {
        view?.setEndDateView(settings.formatter.dateFormat.format(endDate))
    }

    private fun updateEndDateLabels() {
        val view = view ?: return
        val labelParts = view.endDateText.split('|').map { it.trim() }
        view.setEndDateLabels(labelParts.first(), labelParts.getOrNull(1) ?: "")
    }

    private fun updateEndCountLabels() {
        val view = view ?: return
        val labelParts = view.getEndCountTextFor(endCount).split('|').map { it.trim() }
        view.setEndCountLabels(labelParts[0], labelParts.getOrNull(1) ?: "")
    }

    private fun updateCheckedEndType() {
        val isByDate = endType == EndType.BY_DATE
        val isByCount = endType == EndType.BY_COUNT
        view?.setEndNeverChecked(endType == EndType.NEVER)
        view?.setEndDateChecked(isByDate)
        view?.setEndDateViewEnabled(isByDate)
        view?.setEndCountChecked(isByCount)
        view?.setEndCountViewEnabled(isByCount)
    }

    private fun setDefaultEndDate(force: Boolean = false) {
        if (force || endDate == Recurrence.DATE_NONE) {
            // Default two 2 periods after start date.
            val cal = Calendar.getInstance()
            cal.timeInMillis = view!!.startDate
            cal.add(when (period) {
                Period.DAILY -> Calendar.DATE
                Period.WEEKLY -> Calendar.WEEK_OF_YEAR
                Period.MONTHLY -> Calendar.MONTH
                else -> Calendar.YEAR
            }, 2)
            endDate = cal.timeInMillis
            updateEndDateView()
        }
    }

    private val isStartDateOnLastDay: Boolean
        get() = startDateCal[Calendar.DAY_OF_MONTH] ==
                startDateCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    private val startWeekInMonth: Int
        get() {
            val weekInMonth = startDateCal[Calendar.DAY_OF_WEEK_IN_MONTH]
            return if (weekInMonth == 5) -1 else weekInMonth
        }

    private val startDayOfWeek: Int
        get() = startDateCal[Calendar.DAY_OF_WEEK]

    /**
     * Returns the recurrence days of the week bit field.
     * If not weekly or no day is set, default to the same day of the week as start date's.
     */
    private val Recurrence.daysOfWeekOrDefault
        get() = if (period == Period.WEEKLY && this.byDay != 1) {
            this.byDay
        } else {
            (1 shl startDateCal[Calendar.DAY_OF_WEEK])
        }

    /**
     * Returns the monthly setting index for a recurrence. If not monthly, default
     */
    private val Recurrence.monthlySettingIndexOrDefault: Int
        get() = when {
            this.period != Period.MONTHLY -> 0

            // On the last day of the month. Start date must be on the last day of its month.
            this.byMonthDay == -1 && isStartDateOnLastDay -> 2

            // On the same day of the same week each month. Start date must be on this same day and week.
            this.byDay != 0 && this.weekInMonth == startWeekInMonth && this.dayOfWeekInMonth == startDayOfWeek -> 1

            // On the same day each month. Start date must be on the same day as set by recurrence.
            // Recurrence may have set day to 0, meaning the day should always be the same as start date's.
            // As a result of the rules above, some recurrences aren't supported by the picker, so use this as fallback.
            else -> 0
        }

    /**
     * Returns the recurrence's end count. If not set, default to 1.
     */
    private val Recurrence.endCountOrDefault: Int
        get() = if (this.endCount == 0) 1 else this.endCount


    /**
     * Build the recurrence described by the user.
     */
    private fun buildRecurrence() = Recurrence(period) {
        frequency = this@RecurrencePickerPresenter.frequency
        endDate = this@RecurrencePickerPresenter.endDate
        endCount = this@RecurrencePickerPresenter.endCount
        endType = this@RecurrencePickerPresenter.endType

        if (period == Period.WEEKLY) {
            setDaysOfWeek(daysOfWeek)
        } else if (period == Period.MONTHLY) {
            when (monthlySettingIndex) {
                1 -> setDayOfWeekInMonth(1 shl startDayOfWeek, startWeekInMonth)
                2 -> dayInMonth = -1
                else -> dayInMonth = 0
            }
        }
    }

}
