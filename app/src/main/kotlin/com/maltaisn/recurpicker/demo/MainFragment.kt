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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.list.RecurrenceListDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerFragment


class MainFragment : Fragment(), RecurrenceListDialog.Callback, RecurrencePickerFragment.Callback {

    private var selectedRecurrence: Recurrence? = null
    private var startDate = System.currentTimeMillis()

    private lateinit var pickerFragment: RecurrencePickerFragment


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        val settings = RecurrencePickerSettings()

        selectedRecurrence = state?.getParcelable("selectedRecurrence") ?: settings.presets[0]
        val dialog = RecurrenceListDialog.newInstance(settings)

        pickerFragment = RecurrencePickerFragment.newInstance(settings)
        pickerFragment.setTargetFragment(this, 0)

        val btn: Button = view.findViewById(R.id.btn_show_dialog)
        btn.setOnClickListener {
            dialog.selectedRecurrence = selectedRecurrence
            dialog.startDate = startDate
            dialog.show(childFragmentManager, "recurrence-list-dialog")
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedRecurrence", selectedRecurrence)
    }

    override fun onRecurrencePresetSelected(recurrence: Recurrence) {
        selectedRecurrence = recurrence
    }

    override fun onRecurrenceCustomClicked() {
        pickerFragment.selectedRecurrence = selectedRecurrence
        pickerFragment.startDate = startDate
        requireFragmentManager().beginTransaction()
                .add(R.id.picker_fragment_container, pickerFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit()
    }

    override fun onRecurrenceCreated(recurrence: Recurrence) {
        selectedRecurrence = recurrence
    }

}
