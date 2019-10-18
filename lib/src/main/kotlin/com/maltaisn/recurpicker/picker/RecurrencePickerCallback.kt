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

import com.maltaisn.recurpicker.Recurrence


/**
 * Interface to be implemented by either the parent fragment of this fragment, the target fragment
 * or the parent activity. If none of these implements it, there won't be any callback.
 */
interface RecurrencePickerCallback {

    /**
     * Called if the "Done" button is clicked and a custom [recurrence] is created.
     */
    fun onRecurrenceCreated(recurrence: Recurrence)

    /**
     * Called if the recurrence picker fragment back arrow is clicked,
     * or if the recurrence picker dialog is cancelled by click outside or "Cancel" button.
     */
    fun onRecurrencePickerCancelled() = Unit

}
