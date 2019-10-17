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
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val mainFragment by lazy { supportFragmentManager.findFragmentById(R.id.main_fragment)!! }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        if (mainFragment.childFragmentManager.backStackEntryCount > 0) {
            // Pop backstack of child fragment first (which contains the recurrence picker fragment).
            mainFragment.childFragmentManager.popBackStack()
        } else {
            // Pop main back stack.
            super.onBackPressed()
        }
    }

}
