/*
 * Copyright 2020 Nicolas Maltais
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

package com.maltaisn.recurpicker;


import com.maltaisn.recurpicker.format.RecurrenceFormatter;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Arrays;

public class RecurrencePickerSettingsTestJava {

    @Test
    public void should_allow_builder_from_java() {
        // Not really a test, but if it compiles then it's fine.
        RecurrencePickerSettings settings = new RecurrencePickerSettings.Builder()
                .setMaxEndCount(999)
                .setMaxFrequency(99)
                .setFormatter(new RecurrenceFormatter(DateFormat.getDateInstance()))
                .setPresets(Arrays.asList(Recurrence.DOES_NOT_REPEAT, null))
                .setDefaultPickerRecurrence(new Recurrence.Builder(Recurrence.Period.DAILY).build())
                .build();
    }

}
