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

package com.maltaisn.recurpicker.list

import android.os.Bundle
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.list.RecurrenceListContract.*


internal open class RecurrenceListPresenter : Presenter {

    private var view: View? = null

    private val settings: RecurrencePickerSettings
        get() = view!!.settings

    private val recurrences = mutableListOf<Recurrence?>()
    private var checkedPos = -1


    override fun attach(view: View, state: Bundle?) {
        this.view = view

        if (state == null) {
            val selected = view.selectedRecurrence
            if (selected != null) {
                // Check if selected recurrence matches any of the presets.
                checkedPos = -1
                for ((i, preset) in settings.presets.withIndex()) {
                    if (selected == preset) {
                        checkedPos = i
                        break
                    }
                }

                // No match found, add recurrence as top item.
                if (checkedPos == -1) {
                    recurrences += selected
                    checkedPos = 0
                }
            }

            // Add presets to the list.
            recurrences += settings.presets

        } else {
            checkedPos = state.getInt("checkedPos")
            recurrences += state.getParcelableArrayList<Recurrence?>("recurrences")!!
        }
    }

    override fun detach() {
        view = null

        recurrences.clear()
        checkedPos = -1
    }

    override fun saveState(state: Bundle) {
        state.putInt("checkedPos", checkedPos)
        state.putParcelableArrayList("recurrences", ArrayList(recurrences))
    }

    override fun onCancel() {
        view?.setCancelResult()
        view?.exit()
    }

    override val itemCount: Int
        get() = recurrences.size

    override fun onItemClicked(pos: Int) {
        val recurrence = recurrences[pos]
        if (recurrence == null) {
            view?.setCustomResult()
        } else {
            view?.setRecurrenceResult(recurrence)
        }
        view?.exit()
    }

    override fun onBindItemView(itemView: ItemView, pos: Int) {
        val recurrence = recurrences[pos]
        if (recurrence == null) {
            itemView.bindCustomView()
        } else {
            itemView.bindRecurrenceView(settings.formatter, recurrence, pos == checkedPos)
        }
    }

}
