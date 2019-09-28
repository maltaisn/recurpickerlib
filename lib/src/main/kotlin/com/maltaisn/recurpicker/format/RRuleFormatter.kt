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
import java.text.SimpleDateFormat
import java.util.*


/**
 * Utility class to write a [Recurrence] as a RRule and read it back.
 * See [RFC2445][https://tools.ietf.org/html/rfc2445#section-4.8.5.4] and
 * [RFC5545][https://tools.ietf.org/html/rfc5545#section-3.3.10].
 *
 * Note that this doesn't really follow the specifications since rules specify
 * a `DTSTART` attribute which should be part of the event not the recurrence rule.
 * There's also a `DEFAULT` attribute that obviously doesn't exist in the spec.
 * The `NONE` value for the `FREQ` attribute is not supported too.
 *
 * Start dates and end dates are formatted to a local time string.
 */
class RRuleFormatter {

    private val calendar = Calendar.getInstance()

    /**
     * Parse a RFC 5545 string recurrence rule and return a recurrence.
     * Note that this method is designed to parse only the output of [format].
     *
     * Other recurrence rules may not parse correctly since [Recurrence] only
     * supports a thin subset of the actual specification. For example if recurring
     * yearly but not on the same day as start date, this information is lost when parsing,
     * since yearly recurrence can only happen on the same day as start date.
     */
    fun parse(rrule: String): Recurrence {
        require(rrule.startsWith("RRULE:")) { "Recurrence rule string is invalid." }

        val attributes = rrule.substring(6).split(';').associate {
            val pos = it.indexOf('=')
            it.substring(0, pos) to it.substring(pos + 1)
        }

        val startDateStr = requireNotNull(attributes["DTSTART"]) { "Recurrence rule must specify start date." }
        val startDate = DATE_FORMAT.parse(startDateStr)!!.time

        val periodStr = requireNotNull(attributes["FREQ"]) { "Recurrence rule must specify period." }
        val period = when (periodStr) {
            "NONE" -> Period.NONE
            "DAILY" -> Period.DAILY
            "WEEKLY" -> Period.WEEKLY
            "MONTHLY" -> Period.MONTHLY
            "YEARLY" -> Period.YEARLY
            else -> error("Unsupported recurrence period.")
        }

        return Recurrence(startDate, period) {
            frequency = attributes["INTERVAL"]?.toInt() ?: 1

            if (period == Period.WEEKLY) {
                // Weekly days
                weeklyDays = 0
                val daysStr = requireNotNull(attributes["BYDAY"]) { "Weekly recurrence must specify days." }.split(',')
                for (dayStr in daysStr) {
                    weeklyDays = weeklyDays or (1 shl (BYDAY_VALUES.indexOf(dayStr) + 1))
                }

            } else if (period == Period.MONTHLY) {
                // Monthly day
                val monthDayStr = attributes["BYMONTHDAY"]?.toInt()
                monthlyDay = when (monthDayStr) {
                    null -> MonthlyDay.SAME_DAY_OF_WEEK
                    -1 -> MonthlyDay.LAST_DAY_OF_MONTH
                    else -> MonthlyDay.SAME_DAY_OF_MONTH
                }
            }

            // End type
            val endDateStr = attributes["UNTIL"]
            if (endDateStr != null) {
                endDate = DATE_FORMAT.parse(endDateStr)!!.time
            } else {
                val endCountStr = attributes["COUNT"]
                if (endCountStr != null) {
                    endCount = endCountStr.toInt()
                }
            }

            isDefault = attributes["DEFAULT"]?.equals("1") ?: false
        }
    }

    /**
     * Format a [recurrence][r] to a string recurrence rule and return it.
     */
    fun format(r: Recurrence): String {
        val sb = StringBuilder()
        sb.append("RRULE:")

        // Start date
        sb.append("DTSTART=")
        sb.append(DATE_FORMAT.format(r.startDate))
        sb.append(';')

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

        // Day setting
        when (r.period) {
            Period.NONE, Period.DAILY -> {
            }
            Period.WEEKLY -> {
                sb.append("BYDAY=")
                for (i in 0..6) {
                    if (r.isRecurringOnDaysOfWeek(1 shl i + 1)) {
                        sb.append(BYDAY_VALUES[i])
                        sb.append(',')
                    }
                }
                sb.deleteCharAt(sb.length - 1) // Delete extra ","
                sb.append(';')
            }
            Period.MONTHLY -> {
                calendar.timeInMillis = r.startDate
                when (r.monthlyDay) {
                    MonthlyDay.SAME_DAY_OF_MONTH -> {
                        sb.append("BYMONTHDAY=")
                        sb.append(calendar[Calendar.DAY_OF_MONTH])
                    }
                    MonthlyDay.SAME_DAY_OF_WEEK -> {
                        val week = calendar[Calendar.DAY_OF_WEEK_IN_MONTH]
                        sb.append("BYDAY=")
                        if (week == 5) {
                            sb.append("-1")
                        } else {
                            sb.append(week)
                        }
                        val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
                        sb.append(BYDAY_VALUES[dayOfWeek - 1])
                    }
                    MonthlyDay.LAST_DAY_OF_MONTH -> {
                        sb.append("BYMONTHDAY=-1")
                    }
                }
                sb.append(';')
            }
            Period.YEARLY -> {
                calendar.timeInMillis = r.startDate
                sb.append("BYMONTH=")
                sb.append(calendar[Calendar.MONTH] + 1)
                sb.append(";BYMONTHDAY=")
                sb.append(calendar[Calendar.DAY_OF_MONTH])
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

        // Default flag
        if (r.isDefault) {
            sb.append("DEFAULT=1;")
        }

        sb.deleteCharAt(sb.length - 1) // Delete extra ";"
        return sb.toString()
    }

    companion object {
        private val BYDAY_VALUES = arrayOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH)
    }

}
