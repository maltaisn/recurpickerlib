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

@file:Suppress("TooManyFunctions")

package com.maltaisn.recurpicker.picker

import com.maltaisn.recurpicker.BaseContract
import com.maltaisn.recurpicker.Recurrence

internal interface RecurrencePickerContract {

    interface View : BaseContract.View {
        val endDateText: String
        fun getEndCountTextFor(count: Int): String

        fun clearFocus()

        fun setFrequencyView(frequency: String)
        fun setFrequencyMaxLength(length: Int)

        fun setPeriodItems(frequency: Int)
        fun setSelectedPeriodItem(index: Int)

        fun setWeekBtnsShown(shown: Boolean)
        fun setWeekBtnChecked(dayOfWeek: Int, checked: Boolean)

        fun setMonthlySettingShown(shown: Boolean)
        fun setMonthlySettingItems(showLastDay: Boolean, dayOfWeekInMonth: Int, weekInMonth: Int)
        fun setSelectedMonthlySettingItem(index: Int)

        fun setEndNeverChecked(checked: Boolean)

        fun setEndDateChecked(checked: Boolean)
        fun setEndDateView(date: String)
        fun setEndDateViewEnabled(enabled: Boolean)
        fun setEndDateLabels(prefix: String, suffix: String)
        fun showEndDateDialog(date: Long, minDate: Long)

        fun setEndCountChecked(checked: Boolean)
        fun setEndCountView(count: String)
        fun setEndCountViewEnabled(enabled: Boolean)
        fun setEndCountLabels(prefix: String, suffix: String)
        fun setEndCountMaxLength(length: Int)

        fun setCancelResult()
        fun setConfirmResult(recurrence: Recurrence)
    }

    interface Presenter : BaseContract.Presenter<View> {
        fun onConfirm()

        fun onFrequencyChanged(frequencyStr: String)
        fun onPeriodItemSelected(index: Int)
        fun onWeekBtnChecked(dayOfWeek: Int, checked: Boolean)
        fun onMonthlySettingItemSelected(index: Int)

        fun onEndNeverClicked()
        fun onEndDateClicked()
        fun onEndCountClicked()
        fun onEndDateInputClicked()
        fun onEndDateEntered(date: Long)
        fun onEndCountChanged(endCountStr: String)
    }
}
