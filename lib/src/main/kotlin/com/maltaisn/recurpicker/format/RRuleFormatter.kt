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
import java.text.SimpleDateFormat
import java.util.*


/**
 * Utility class to write a [Recurrence] as a RRule and read it back.
 * See [RFC2445][https://tools.ietf.org/html/rfc2445#section-4.8.5.4] and
 * [RFC5545][https://tools.ietf.org/html/rfc5545#section-3.3.10].
 * Start dates and end dates are formatted to a local time string.
 *
 * This class is thread-safe.
 */
class RRuleFormatter {

    /**
     * Parse a RFC 5545 string recurrence rule and return a recurrence.
     * Note that this method is designed to parse only the output of [format].
     *
     * Other recurrence rules may not parse correctly since [Recurrence] only
     * supports a thin subset of the actual specification. For example if recurring
     * yearly but not on the same day as start date, this information is lost when parsing,
     * since yearly recurrence can only happen on the same day as start date.
     *
     * @throws IllegalArgumentException If recurrence rule cannot be parsed.
     */
    fun parse(rrule: String): Recurrence {
        require(rrule.startsWith("RRULE:")) { "Recurrence rule string is invalid." }

        val attributes = rrule.substring(6).split(';').associate {
            val pos = it.indexOf('=')
            it.substring(0, pos) to it.substring(pos + 1)
        }

        val periodStr = requireNotNull(attributes["FREQ"]) { "Recurrence rule must specify period." }
        val period = when (periodStr) {
            "NONE" -> Period.NONE
            "DAILY" -> Period.DAILY
            "WEEKLY" -> Period.WEEKLY
            "MONTHLY" -> Period.MONTHLY
            "YEARLY" -> Period.YEARLY
            else -> throw IllegalArgumentException("Unsupported recurrence period.")  // Secondly, minutely, hourly
        }

        return try {
            Recurrence(period) {
                frequency = attributes["INTERVAL"]?.toInt() ?: 1

                if (period == Period.WEEKLY) {
                    // Days of the week
                    var days = 0
                    val daysAttr = attributes["BYDAY"]
                    if (daysAttr != null) {
                        for (dayStr in daysAttr.split(',')) {
                            val index = BYDAY_VALUES.indexOf(dayStr)
                            require(index >= 0) { "Invalid day of week literal." }
                            days = days or (1 shl (index + 1))
                        }
                        setDaysOfWeek(days)
                    }

                } else if (period == Period.MONTHLY) {
                    // Monthly settings
                    val byDay = attributes["BYDAY"]
                    if (byDay != null) {
                        val day = BYDAY_VALUES.indexOf(byDay.takeLast(2))
                        require(day >= 0) { "Invalid day of week literal." }
                        val week = attributes["BYSETPOS"]?.toInt() ?: byDay.dropLast(2).toInt()
                        setDayOfWeekInMonth(1 shl (day + 1), week)
                    } else {
                        dayInMonth = attributes["BYMONTHDAY"]?.toInt() ?: 0
                    }
                }

                // End type
                val endDateStr = attributes["UNTIL"]
                if (endDateStr != null) {
                    endDate = requireNotNull(DATE_FORMAT.parse(endDateStr)) {
                        "Invalid end date format '${endDateStr}'."
                    }.time
                } else {
                    val endCountStr = attributes["COUNT"]
                    if (endCountStr != null) {
                        endCount = endCountStr.toInt()
                    }
                }
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Bad number format in recurrence rule.")
        }
    }

    /**
     * Format a [recurrence][r] to a string recurrence rule and return it.
     */
    fun format(r: Recurrence): String {
        val sb = StringBuilder()
        sb.append("RRULE:")

        // Period
        sb.append("FREQ=")
        sb.append(when (r.period) {
            Period.NONE -> "NONE"
            Period.DAILY -> "DAILY"
            Period.WEEKLY -> "WEEKLY"
            Period.MONTHLY -> "MONTHLY"
            Period.YEARLY -> "YEARLY"
        })
        sb.append(';')

        // Frequency
        if (r.frequency != 1) {
            sb.append("INTERVAL=")
            sb.append(r.frequency)
            sb.append(';')
        }

        // Additional settings
        if (r.period == Period.WEEKLY) {
            sb.append("BYDAY=")
            for (i in 0..6) {
                if (r.isRecurringOnDaysOfWeek(1 shl i + 1)) {
                    sb.append(BYDAY_VALUES[i])
                    sb.append(',')
                }
            }
            sb.deleteCharAt(sb.length - 1) // Delete extra ","
            sb.append(';')

        } else if (r.period == Period.MONTHLY) {
            if (r.byDay != 0) {
                sb.append("BYDAY=")
                sb.append(r.weekInMonth)
                sb.append(BYDAY_VALUES[r.dayOfWeekInMonth - 1])
                sb.append(';')
            } else if (r.byMonthDay != 0) {
                sb.append("BYMONTHDAY=")
                sb.append(r.byMonthDay)
                sb.append(';')
            }
        }

        // End type
        when (r.endType) {
            EndType.NEVER -> Unit
            EndType.BY_DATE -> {
                sb.append("UNTIL=")
                sb.append(DATE_FORMAT.format(r.endDate))
                sb.append(';')
            }
            EndType.BY_COUNT -> {
                sb.append("COUNT=")
                sb.append(r.endCount)
                sb.append(';')
            }
        }

        sb.deleteCharAt(sb.length - 1) // Delete extra ";"
        return sb.toString()
    }

    companion object {
        private val BYDAY_VALUES = arrayOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH)
    }

}
