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
import com.maltaisn.recurpicker.format.RecurrenceFormatter


internal interface RecurrenceListContract {

    interface View {
        val settings: RecurrencePickerSettings
        val startDate: Long
        val selectedRecurrence: Recurrence?

        fun exit()

        fun setRecurrenceResult(recurrence: Recurrence)
        fun setCustomResult()
        fun setCancelResult()
    }

    interface ItemView {
        fun bindRecurrenceView(formatter: RecurrenceFormatter, recurrence: Recurrence,
                               startDate: Long, checked: Boolean)
        fun bindCustomView()
    }

    interface Presenter {
        fun attach(view: View, state: Bundle?)
        fun detach()

        fun saveState(state: Bundle)

        fun onCancel()

        val itemCount: Int
        fun onItemClicked(pos: Int)
        fun onBindItemView(itemView: ItemView, pos: Int)
    }

}
