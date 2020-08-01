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

package com.maltaisn.recurpicker.demo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/**
 * Dialog used to choose the start date.
 * This is mostly the same as [com.maltaisn.recurpicker.picker.DateDialogFragment].
 */
internal class DateDialogFragment : DialogFragment() {

    var date: Long = System.currentTimeMillis()

    private var _datePicker: DatePicker? = null
    private val datePicker get() = _datePicker!!

    private val calendar = Calendar.getInstance()

    @SuppressLint("InflateParams")
    override fun onCreateDialog(state: Bundle?): Dialog {
        calendar.timeInMillis = state?.getLong("date") ?: date
        calendar.setToStartOfDay()

        val context = requireContext()

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_date, null, false)
        _datePicker = view.findViewById(R.id.date_picker)
        datePicker.init(calendar[Calendar.YEAR], calendar[Calendar.MONTH],
            calendar[Calendar.DATE], null)

        try {
            // Fixes date picker showing year 1964 or 2100 initially when in spinner mode (API < 21).
            // See https://stackoverflow.com/a/19125686
            @Suppress("DEPRECATION")
            val cal = datePicker.calendarView
            if (cal != null) {
                // Add approximatively 1 day (arbitrary).
                @Suppress("MagicNumber")
                cal.setDate(date + 100000000, false, true)
                // Note that min and max date were set to a time at the start of the day earlier because
                // date picker considers time of the day when checking if time millis is within bounds.
                cal.setDate(date, false, true)
            }
        } catch (e: UnsupportedOperationException) {
            // API >= 21, nothing to fix.
        }

        return MaterialAlertDialogBuilder(context)
            .setView(datePicker)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                calendar.setToStartOfDay()
                callback?.onDateDialogConfirmed(calendar.timeInMillis)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> onCancel(dialog) }
            .create()
    }

    private fun Calendar.setToStartOfDay() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _datePicker = null
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)

        calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
        state.putLong("date", calendar.timeInMillis)
    }

    private val callback: Callback?
        get() = (parentFragment as? Callback)
            ?: (targetFragment as? Callback)
            ?: (activity as? Callback)

    interface Callback {
        fun onDateDialogConfirmed(date: Long)
    }
}
