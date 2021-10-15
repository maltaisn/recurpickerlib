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

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.use
import androidx.fragment.app.Fragment

/**
 * Returns a callback of type [T] for a fragment.
 */
internal inline fun <reified T> Fragment.getCallback(): T? =
    (parentFragment as? T)
        // Target fragment is deprecated but keep trying to get it for compatibility.
        ?: (@Suppress("DEPRECATION") targetFragment as? T)
        ?: (activity as? T)

/**
 * Get the themed context wrapper for the recurrence picker fragments.
 */
internal fun Fragment.getPickerContextWrapper(): Context {
    val context = requireContext()
    val style = context.obtainStyledAttributes(intArrayOf(R.attr.recurrencePickerStyle)).use {
        it.getResourceId(0, R.style.RecurrencePickerStyle)
    }
    return ContextThemeWrapper(context, style)
}
