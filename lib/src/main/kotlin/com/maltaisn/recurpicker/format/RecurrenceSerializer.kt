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
import com.maltaisn.recurpicker.Recurrence.*
import java.nio.ByteBuffer


/**
 * Utility object to write a [Recurrence] as a byte array and read it back.
 * Read is backward-compatible with previous versions.
 */
@Deprecated("Use RRule serialization instead.")
object RecurrenceSerializer {

    /**
     * Read a recurrence from a byte [array] and return it.
     * The encoded recurrence object must start at index 0 in the array.
     */
    @JvmStatic
    fun read(array: ByteArray): Recurrence {
        val bb = ByteBuffer.wrap(array)
        return when (bb.int) {
            VERSION_1 -> {
                val isDefault = bb.get() == 1.toByte()
                Recurrence(bb.long, Period.values()[bb.int + 1]) {
                    this.isDefault = isDefault
                    frequency = bb.int

                    val daySetting = bb.int
                    weeklyDays = daySetting
                    monthlyDay = MonthlyDay.values().getOrNull(daySetting)
                            ?: MonthlyDay.SAME_DAY_OF_MONTH

                    val endType = EndType.values()[bb.int]
                    endCount = bb.int
                    endDate = bb.long
                    if (endDate == 0L) {
                        endDate = Recurrence.DATE_NONE
                    }
                    this.endType = endType
                }
            }
            VERSION_2 -> {
                Recurrence(bb.long, Period.values()[bb.get().toInt()]) {
                    frequency = bb.int
                    weeklyDays = bb.int
                    monthlyDay = MonthlyDay.values()[bb.get().toInt()]

                    val endType = EndType.values()[bb.get().toInt()]
                    endCount = bb.int
                    endDate = bb.long
                    this.endType = endType

                    isDefault = bb.get() == 1.toByte()
                }
            }
            else -> error("Unknown recurrence schema version.")
        }
    }

    /**
     * Write a [recurrence][r] to a byte array and return it.
     */
    @JvmStatic
    fun write(r: Recurrence): ByteArray = ByteBuffer.allocate(36).apply {
        putInt(VERSION)
        putLong(r.startDate)
        put(r.period.ordinal.toByte())
        putInt(r.frequency)
        putInt(r.weeklyDays)
        put(r.monthlyDay.ordinal.toByte())
        put(r.endType.ordinal.toByte())
        putInt(r.endCount)
        putLong(r.endDate)
        put(if (r.isDefault) 1.toByte() else 0)
    }.array()


    /**
     * Version 1, from v1.0.0 to v1.6.0
     * - 0: int, version
     * - 4: byte, default flag (0=false, 1=true)
     * - 5: long, start date
     * - 13: int, period (-1=none, 0=daily, 1=weekly, 2=monthly, 3=yearly)
     * - 17: int, frequency
     * - 21: int, day setting (both weekly and monthly additional options)
     * - 25: int, end type (0=never, 1=date, 2=count)
     * - 29: int, end count
     * - 33: long, end date (0=none)
     * - Length: 41
     */
    private const val VERSION_1 = 100

    /**
     * Version 1, from v2.0.0
     * - 0: int, version
     * - 4: long, start date
     * - 12: byte, period (0=none, 1=daily, 2=weekly, 3=monthly, 4=yearly)
     * - 13: int, frequency
     * - 17: int, weekly setting
     * - 21: byte: monthly setting (0=same day, 1=same week, 2=last)
     * - 22: byte, end type (0=never, 1=date, 2=count)
     * - 23: int, end count
     * - 27: long, end date (Long.MIN_VALUE=none)
     * - 35: byte, default flag (0=false, 1=true)
     * - Length: 36
     */
    private const val VERSION_2 = 101

    private const val VERSION = VERSION_2

}
