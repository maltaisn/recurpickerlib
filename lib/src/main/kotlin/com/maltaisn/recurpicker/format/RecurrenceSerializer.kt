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
import com.maltaisn.recurpicker.Recurrence.EndType
import com.maltaisn.recurpicker.Recurrence.Period
import java.nio.ByteBuffer
import java.util.*


/**
 * Utility class to write a [Recurrence] as a byte array and read it back.
 * Read is backward-compatible with previous versions.
 * Prefer [RRuleFormatter] to this format for serialization since its more flexible.
 */
@Deprecated("Use RRuleFormatter instead.")
class RecurrenceSerializer {

    private val calendar = Calendar.getInstance()

    /**
     * Read a recurrence from a byte [array] and return it.
     * The encoded recurrence object must start at index 0 in the array.
     */
    fun read(array: ByteArray): Recurrence {
        val bb = ByteBuffer.wrap(array)
        return when (bb.int) {
            VERSION_1 -> {
                require(bb.remaining() >= VERSION_1_LENGTH) { "Invalid length." }
                bb.get()  // Discard default flag byte
                val startDate = bb.long

                Recurrence(Period.values()[bb.int + 1]) {
                    frequency = bb.int

                    val daySetting = bb.int
                    if (period == Period.WEEKLY) {
                        setDaysOfWeek(daySetting)
                    } else if (period == Period.MONTHLY) {
                        when (daySetting) {
                            0 -> dayInMonth = 0
                            1 -> {
                                calendar.timeInMillis = startDate
                                var weekInMonth = calendar[Calendar.DAY_OF_WEEK_IN_MONTH]
                                if (weekInMonth == 5) weekInMonth = -1
                                setDayOfWeekInMonth(1 shl calendar[Calendar.DAY_OF_WEEK], weekInMonth)
                            }
                            2 -> dayInMonth = -1
                        }
                    }

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
                require(bb.remaining() >= VERSION_2_LENGTH) { "Invalid length." }
                Recurrence(Period.values()[bb.get().toInt()]) {
                    frequency = bb.int
                    byDay = bb.short.toInt()
                    byMonthDay = bb.get().toInt()

                    val endType = EndType.values()[bb.get().toInt()]
                    endCount = bb.int
                    endDate = bb.long
                    this.endType = endType
                }
            }
            else -> throw IllegalArgumentException("Unknown recurrence schema version.")
        }
    }

    /**
     * Write a [recurrence][r] to a byte array and return it.
     */
    fun write(r: Recurrence): ByteArray = ByteBuffer.allocate(25).apply {
        putInt(VERSION)
        put(r.period.ordinal.toByte())
        putInt(r.frequency)
        putShort(r.byDay.toShort())
        put(r.byMonthDay.toByte())
        put(r.endType.ordinal.toByte())
        putInt(r.endCount)
        putLong(r.endDate)
    }.array()


    companion object {
        /**
         * Version 1, from v1.0.0 to v1.6.0
         * - 0: int, version
         * - 4: byte, default flag (0=false, 1=true)
         * - 5: long, start date
         * - 13: int, period (-1=none, 0=daily, 1=weekly, 2=monthly, 3=yearly)
         * - 17: int, frequency
         * - 21: int, day setting (both weekly and monthly additional options,
         *      if monthly, 0=same_day, 1=same_week, 2=last_day)
         * - 25: int, end type (0=never, 1=date, 2=count)
         * - 29: int, end count
         * - 33: long, end date (0=none)
         * - Length: 41, 37 excluding version
         */
        private const val VERSION_1 = 100
        private const val VERSION_1_LENGTH = 37

        /**
         * Version 2, from v2.0.0
         * - 0: int, version
         * - 4: byte, period (0=none, 1=daily, 2=weekly, 3=monthly, 4=yearly)
         * - 5: int, frequency
         * - 9: short, byDay
         * - 11: byte: byMonthDay
         * - 12: byte, end type (0=never, 1=date, 2=count)
         * - 13: int, end count
         * - 17: long, end date (Long.MIN_VALUE=none)
         * - Length: 25, 21 excluding version
         */
        private const val VERSION_2 = 101
        private const val VERSION_2_LENGTH = 21

        private const val VERSION = VERSION_2
    }

}
