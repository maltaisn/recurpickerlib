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

import org.junit.Test;

class RecurrenceBuilderTestJava {

    @Test
    public void should_allow_builder_from_java() {
        // Not really a test, but if it compiles then it's fine.
        Recurrence r1 = new Recurrence.Builder(Recurrence.Period.WEEKLY)
                .setFrequency(3)
                .setDaysOfWeek(Recurrence.FRIDAY, Recurrence.SATURDAY)
                .setEndType(Recurrence.EndType.BY_COUNT)
                .setEndCount(10)
                .build();
        Recurrence r2 = new Recurrence.Builder(Recurrence.Period.MONTHLY)
                .setDayInMonth(-1)
                .setEndDate(TestDateExtensionsKt.dateFor("2020-09-30"))
                .build();
        Recurrence r3 = Recurrence.DOES_NOT_REPEAT;
    }
}
