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
import java.util.Calendar
import java.util.Locale

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
        require(rrule.startsWith(RRULE_SIGNATURE)) { "Recurrence rule string is invalid." }

        val attrs = rrule.substring(RRULE_SIGNATURE.length).split(';').associate {
            val pos = it.indexOf('=')
            it.substring(0, pos) to it.substring(pos + 1)
        }

        val period = parsePeriod(attrs)

        return try {
            Recurrence(period) {
                frequency = attrs["INTERVAL"]?.toInt() ?: 1
                if (period == Period.WEEKLY) {
                    parseWeeklyDetails(attrs)
                } else if (period == Period.MONTHLY) {
                    parseMonthlyDetails(attrs)
                }
                parseEndTypeDetails(attrs)
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Bad number format in recurrence rule.", e)
        }
    }

    private fun parsePeriod(attrs: Map<String, String>): Period {
        val periodStr = requireNotNull(attrs["FREQ"]) { "Recurrence rule must specify period." }
        return when (periodStr) {
            "NONE" -> Period.NONE
            "DAILY" -> Period.DAILY
            "WEEKLY" -> Period.WEEKLY
            "MONTHLY" -> Period.MONTHLY
            "YEARLY" -> Period.YEARLY
            else -> throw IllegalArgumentException("Unsupported recurrence period.") // Secondly, minutely, hourly
        }
    }

    private fun Recurrence.Builder.parseWeeklyDetails(attrs: Map<String, String>) {
        // Days of the week
        var days = 0
        val daysAttr = attrs["BYDAY"]
        if (daysAttr != null) {
            for (dayStr in daysAttr.split(',')) {
                val index = BYDAY_VALUES.indexOf(dayStr)
                require(index >= 0) { "Invalid day of week literal." }
                days = days or (1 shl (index + 1))
            }
            setDaysOfWeek(days)
        }
    }

    private fun Recurrence.Builder.parseMonthlyDetails(attrs: Map<String, String>) {
        // Monthly settings
        val byDay = attrs["BYDAY"]
        if (byDay != null) {
            val day = BYDAY_VALUES.indexOf(byDay.takeLast(2))
            require(day >= 0) { "Invalid day of week literal." }
            val week = attrs["BYSETPOS"]?.toInt() ?: byDay.dropLast(2).toInt()
            setDayOfWeekInMonth(1 shl (day + 1), week)
        } else {
            dayInMonth = attrs["BYMONTHDAY"]?.toInt() ?: 0
        }
    }

    private fun Recurrence.Builder.parseEndTypeDetails(attrs: Map<String, String>) {
        val endDateStr = attrs["UNTIL"]
        if (endDateStr != null) {
            endDate = requireNotNull(DATE_FORMAT.parse(endDateStr)) {
                "Invalid end date format '$endDateStr'."
            }.time
        } else {
            val endCountStr = attrs["COUNT"]
            if (endCountStr != null) {
                endCount = endCountStr.toInt()
            }
        }
    }

    /**
     * Format a [recurrence][r] to a string recurrence rule and return it.
     */
    fun format(r: Recurrence): String {
        val sb = StringBuilder()
        sb.append(RRULE_SIGNATURE)

        appendPeriodDetails(sb, r)
        appendFrequencyDetails(sb, r)

        if (r.period == Period.WEEKLY) {
            appendWeeklyDetails(sb, r)
        } else if (r.period == Period.MONTHLY) {
            appendMonthlyDetails(sb, r)
        }

        appendEndTypeDetails(sb, r)

        sb.deleteCharAt(sb.length - 1) // Delete extra ";"
        return sb.toString()
    }

    private fun appendPeriodDetails(sb: StringBuilder, r: Recurrence) {
        sb.append("FREQ=")
        sb.append(when (r.period) {
            Period.NONE -> "NONE"
            Period.DAILY -> "DAILY"
            Period.WEEKLY -> "WEEKLY"
            Period.MONTHLY -> "MONTHLY"
            Period.YEARLY -> "YEARLY"
        })
        sb.append(';')
    }

    private fun appendFrequencyDetails(sb: StringBuilder, r: Recurrence) {
        if (r.frequency != 1) {
            sb.append("INTERVAL=")
            sb.append(r.frequency)
            sb.append(';')
        }
    }

    private fun appendWeeklyDetails(sb: StringBuilder, r: Recurrence) {
        sb.append("BYDAY=")
        for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
            if (r.isRecurringOnDaysOfWeek(1 shl i)) {
                sb.append(BYDAY_VALUES[i - 1])
                sb.append(',')
            }
        }
        sb.deleteCharAt(sb.length - 1) // Delete extra ","
        sb.append(';')
    }

    private fun appendMonthlyDetails(sb: StringBuilder, r: Recurrence) {
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

    private fun appendEndTypeDetails(sb: StringBuilder, r: Recurrence) {
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
    }

    companion object {
        private val BYDAY_VALUES = arrayOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH)

        private const val RRULE_SIGNATURE = "RRULE:"
    }
}
