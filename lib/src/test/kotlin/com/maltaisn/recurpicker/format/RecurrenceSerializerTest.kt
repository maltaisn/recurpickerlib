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

package com.maltaisn.recurpicker.format

import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.dateFor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
class RecurrenceSerializerTest {

    private val serializer = RecurrenceSerializer()

    @Test
    fun `should read serialized recurrence (version 1)`() {
        // Used to convert byte[] to hex string: https://stackoverflow.com/a/9855338/5288316

        val a1 = hexStringToByteArray("00000064010000016D6BB9C600000000000000000" +
                "10000000000000000000000000000000000000000")
        assertEquals(Recurrence(Period.DAILY), serializer.read(a1))

        val a2 = hexStringToByteArray("0000006400000000E8DF6BDA00000000010000000" +
                "50000009400000000000000000000000000000000")
        assertEquals(Recurrence(Period.WEEKLY) {
            frequency = 5
            setDaysOfWeek(Recurrence.MONDAY, Recurrence.WEDNESDAY, Recurrence.SATURDAY)
        }, serializer.read(a2))

        val a3 = hexStringToByteArray("000000640000000176B72ABC80000000020000000" +
                "2000000020000000100000000000001941B15C880")
        assertEquals(Recurrence(Period.MONTHLY) {
            frequency = 2
            dayInMonth = -1
            endDate = dateFor("2024-12-31")
        }, serializer.read(a3))

        val a4 = hexStringToByteArray("000000640000000176B72ABC80000000030000000" +
                "100000000000000020000000C0000000000000000")
        assertEquals(Recurrence(Period.YEARLY) {
            endCount = 12
        }, serializer.read(a4))

        val a5 = hexStringToByteArray("00000064000000016941EC5080000000020000000" +
                "10000000100000000000000000000000000000000")
        assertEquals(Recurrence(Period.MONTHLY) {
            setDayOfWeekInMonth(Recurrence.SUNDAY, 1)
        }, serializer.read(a5))
    }

    @Test
    fun `should read serialized recurrence (version 2)`() {
        val r1 = Recurrence(Period.DAILY)
        assertEquals(r1, serializer.read(serializer.write(r1)))

        val r2 = Recurrence(Period.WEEKLY) {
            frequency = 5
            setDaysOfWeek(Recurrence.MONDAY, Recurrence.WEDNESDAY, Recurrence.SATURDAY)
        }
        assertEquals(r2, serializer.read(serializer.write(r2)))

        val r3 = Recurrence(Period.MONTHLY) {
            frequency = 2
            dayInMonth = -1
            endDate = dateFor("2024-12-31")
        }
        assertEquals(r3, serializer.read(serializer.write(r3)))

        val r4 = Recurrence(Period.YEARLY) {
            endCount = 12
        }
        assertEquals(r4, serializer.read(serializer.write(r4)))
    }

    @Test
    fun `should fail to read serialized recurrence (unknown version)`() {
        assertFailsWith<java.lang.IllegalArgumentException> {
            serializer.read(hexStringToByteArray("00000000000000016941EC5080000000020000000" +
                    "10000000100000000000000000000000000000000"))
        }
    }

    @Test
    fun `should fail to read serialized recurrence of bad length (version 1)`() {
        assertFailsWith<java.lang.IllegalArgumentException> {
            serializer.read(hexStringToByteArray("00000064000000016941"))
        }
    }

    @Test
    fun `should fail to read serialized recurrence of bad length (version 2)`() {
        assertFailsWith<java.lang.IllegalArgumentException> {
            serializer.read(hexStringToByteArray("00000065000000016941"))
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
