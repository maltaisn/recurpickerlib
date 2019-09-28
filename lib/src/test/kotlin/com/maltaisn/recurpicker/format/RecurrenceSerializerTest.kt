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
import com.maltaisn.recurpicker.Recurrence.MonthlyDay
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.dateFor
import org.junit.Test
import kotlin.test.assertEquals


@Suppress("DEPRECATION")
class RecurrenceSerializerTest {

    private val serializer = RecurrenceSerializer()

    @Test
    fun read_version1() {
        val a1 = hexStringToByteArray("00000064010000016D6BB9C60000000000000000010000000000000000000000000000000000000000")
        assertEquals(Recurrence(dateFor("2019-09-26"), Period.DAILY) {
            isDefault = true
        }, serializer.read(a1))

        val a2 = hexStringToByteArray("0000006400000000E8DF6BDA0000000001000000050000009400000000000000000000000000000000")
        assertEquals(Recurrence(dateFor("2001-09-11"), Period.WEEKLY) {
            frequency = 5
            setWeekDays(Recurrence.MONDAY, Recurrence.WEDNESDAY, Recurrence.SATURDAY)
        }, serializer.read(a2))

        val a3 = hexStringToByteArray("000000640000000176B72ABC800000000200000002000000020000000100000000000001941B15C880")
        assertEquals(Recurrence(dateFor("2020-12-31"), Period.MONTHLY) {
            frequency = 2
            monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH
            endDate = dateFor("2024-12-31")
        }, serializer.read(a3))

        val a4 = hexStringToByteArray("000000640000000176B72ABC80000000030000000100000000000000020000000C0000000000000000")
        assertEquals(Recurrence(dateFor("2020-12-31"), Period.YEARLY) {
            endCount = 12
        }, serializer.read(a4))
    }

    @Test
    fun read_write_version2() {
        val r1 = Recurrence(dateFor("2019-09-26"), Period.DAILY) { isDefault = true }
        assertEquals(r1, serializer.read(serializer.write(r1)))

        val r2 = Recurrence(dateFor("2001-09-11"), Period.WEEKLY) {
            frequency = 5
            setWeekDays(Recurrence.MONDAY, Recurrence.WEDNESDAY, Recurrence.SATURDAY)
        }
        assertEquals(r2, serializer.read(serializer.write(r2)))

        val r3 = Recurrence(dateFor("2020-12-31"), Period.MONTHLY) {
            frequency = 2
            monthlyDay = MonthlyDay.LAST_DAY_OF_MONTH
            endDate = dateFor("2024-12-31")
        }
        assertEquals(r3, serializer.read(serializer.write(r3)))

        val r4 = Recurrence(dateFor("2020-12-31"), Period.YEARLY) {
            endCount = 12
        }
        assertEquals(r4, serializer.read(serializer.write(r4)))
    }


    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

}
