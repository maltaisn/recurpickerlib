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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maltaisn.recurpicker.Recurrence
import java.util.*


internal class DateDialogFragment : DialogFragment() {

    /**
     * The date to be shown initially by the dialog.
     */
    var date: Long = System.currentTimeMillis()

    /**
     * The minimum date allowed by the dialog, or [Recurrence.DATE_NONE] for none.
     */
    var minDate: Long = Recurrence.DATE_NONE

    /**
     * The maximum date allowed by the dialog, or [Recurrence.DATE_NONE] for none.
     */
    var maxDate: Long = Recurrence.DATE_NONE


    override fun onCreateDialog(state: Bundle?): Dialog {
        if (state != null) {
            date = state.getLong("date")
            minDate = state.getLong("minDate")
            maxDate = state.getLong("maxDate")
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date

        val context = requireContext()

        // TODO use MaterialDatePicker when API is ready and documented. It's a mess right now.
        // Since this is not a material component, colorAccent should be defined as either
        // colorPrimary or colorSecondary in styles.xml. (see demo app)
        val datePicker = DatePicker(context)
        datePicker.init(calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DATE]) { _, year, month, day ->
            calendar.set(year, month, day)
            date = calendar.timeInMillis
        }
        if (minDate != Recurrence.DATE_NONE) datePicker.minDate = minDate
        if (maxDate != Recurrence.DATE_NONE) datePicker.maxDate = maxDate

        return MaterialAlertDialogBuilder(context)
                .setView(datePicker)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    calendar.timeInMillis = date
                    calendar[Calendar.HOUR_OF_DAY] = 0
                    calendar[Calendar.MINUTE] = 0
                    calendar[Calendar.SECOND] = 0
                    calendar[Calendar.MILLISECOND] = 0
                    callback?.onDateDialogConfirmed(date)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> onCancel(dialog) }
                .create()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        state.putLong("date", date)
        state.putLong("minDate", minDate)
        state.putLong("maxDate", maxDate)
    }

    override fun onCancel(dialog: DialogInterface) {
        callback?.onDateDialogCancelled()
    }

    private val callback: Callback?
        get() = (parentFragment as? Callback)
                ?: (targetFragment as? Callback)
                ?: (activity as? Callback)

    interface Callback {
        fun onDateDialogConfirmed(date: Long)
        fun onDateDialogCancelled() = Unit
    }

}
