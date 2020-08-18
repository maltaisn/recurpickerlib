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

package com.maltaisn.recurpicker

import android.os.Bundle
import com.maltaisn.recurpicker.list.RecurrenceListDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerFragment

internal interface BaseContract {

    interface View {
        /**
         * The settings defining the behavior and content of the recurrence picker and list fragments .
         */
        var settings: RecurrencePickerSettings

        /**
         * The start date of the event for which a recurrence is created or edited.
         * - For the [RecurrenceListDialog], this is not necessary and can be set to [Recurrence.DATE_NONE].
         * It will however provide more consise recurrence formatting to text.
         * - For the [RecurrencePickerFragment], this is a required parameter.
         */
        var startDate: Long

        /**
         * The previously selected recurrence that will be shown initially. Can be set to `null`
         * if no recurrence was selected previously. In the recurrence picker, the
         * [RecurrencePickerSettings.defaultPickerRecurrence] will be used if `null` is set.
         */
        var selectedRecurrence: Recurrence?

        fun exit()
    }

    interface Presenter<V : View> {
        fun attach(view: V, state: Bundle?)
        fun detach()

        fun saveState(state: Bundle)

        fun onCancel()
    }
}
