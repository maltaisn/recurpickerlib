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

package com.maltaisn.recurpicker

import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TestDateExtensionsTest {

    @Test
    fun `should compare days`() {
        val cal = Calendar.getInstance()
        assertEquals(dateFor("2018-01-01").compareDay(dateFor("2019-01-01"), cal), -1)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2018-01-01"), cal), 1)
        assertEquals(dateFor("2019-01-01").compareDay(dateFor("2019-01-01"), cal), 0)
        assertEquals(dateFor("2019-01-01T01:00:00.000").compareDay(
            dateFor("2019-01-01T10:00:00.000"), cal), 0)
    }

    @Test
    fun `should consider two NONE dates equal`() {
        val cal = Calendar.getInstance()
        assertEquals(Recurrence.DATE_NONE.compareDay(Recurrence.DATE_NONE, cal), 0)
    }

    @Test
    fun `should fail to compare date with NONE date`() {
        val cal = Calendar.getInstance()
        assertFailsWith<IllegalArgumentException> {
            Recurrence.DATE_NONE.compareDay(dateFor("2019-01-01"), cal)
        }
        assertFailsWith<IllegalArgumentException> {
            dateFor("2019-01-01").compareDay(Recurrence.DATE_NONE, cal)
        }
    }
}
