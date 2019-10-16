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

import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*


// Dialog used to choose the start date.
internal class DateDialogFragment : DialogFragment() {

    var date: Long = System.currentTimeMillis()

    override fun onCreateDialog(state: Bundle?): Dialog {
        if (state != null) {
            date = state.getLong("date")
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date

        val context = requireContext()

        val datePicker = DatePicker(context)
        datePicker.init(calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DATE]) { _, year, month, day ->
            calendar.set(year, month, day)
            date = calendar.timeInMillis
        }

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
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        state.putLong("date", date)
    }

    private val callback: Callback?
        get() = (parentFragment as? Callback)
                ?: (targetFragment as? Callback)
                ?: (activity as? Callback)


    interface Callback {
        fun onDateDialogConfirmed(date: Long)
    }

}
