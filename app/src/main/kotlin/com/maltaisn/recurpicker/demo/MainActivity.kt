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
import com.maltaisn.recurpicker.demo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG) == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, MainFragment(), MAIN_FRAGMENT_TAG)
                    .commit()
        }
    }

    companion object {
        private const val MAIN_FRAGMENT_TAG = "main_fragment"
    }

}
