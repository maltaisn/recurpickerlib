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

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.maltaisn.recurpicker.R

/**
 * Custom AutoCompleteTextView adapter to disable filtering since we want it to act like a spinner.
 */
internal class DropdownAdapter(context: Context, items: List<String> = mutableListOf()) :
    ArrayAdapter<String>(context, R.layout.rp_item_dropdown, items) {

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) = null
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
    }
}
