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

import com.maltaisn.recurpicker.Recurrence

/**
 * Call back for the [RecurrenceListDialog].
 * Interface to be implemented by either the parent fragment of the dialog, the target fragment
 * or the parent activity. If none of these implements it, there won't be any callback.
 */
interface RecurrenceListCallback {
    /**
     * Called if a [recurrence] preset is selected in the list.
     */
    fun onRecurrencePresetSelected(recurrence: Recurrence)

    /**
     * Called if the "Custom..." item is selected in the list.
     * This is a good place to show the recurrence picker.
     */
    fun onRecurrenceCustomClicked()

    /**
     * Called if the recurrence list dialog is cancelled, either by
     * a click outside or by a back press.
     */
    fun onRecurrenceListDialogCancelled() = Unit
}
