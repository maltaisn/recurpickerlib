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

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.list.RecurrenceListDialog

class MainActivity : AppCompatActivity(), RecurrenceListDialog.Callback {

    private var selectedRecurrence: Recurrence? = null


    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)

        val settings = RecurrencePickerSettings()

        selectedRecurrence = state?.getParcelable("selectedRecurrence") ?: settings.presets[0]
        val dialog = RecurrenceListDialog.newInstance(settings, selectedRecurrence)

        val btn: Button = findViewById(R.id.btn_show_dialog)
        btn.setOnClickListener {
            dialog.selectedRecurrence = selectedRecurrence
            dialog.show(supportFragmentManager, "recurrence-list-dialog")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedRecurrence", selectedRecurrence)
    }

    override fun onRecurrencePresetSelected(recurrence: Recurrence) {
        Log.i(TAG, "Recurrence list dialog selected: ${recurrence}.")
        selectedRecurrence = recurrence
    }

    override fun onRecurrenceCustomClicked() {
        Log.i(TAG, "Recurrence list dialog custom clicked.")
        selectedRecurrence = null
    }

    override fun onRecurrenceListDialogCancelled() {
        Log.i(TAG, "Recurrence list dialog cancelled.")
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}
