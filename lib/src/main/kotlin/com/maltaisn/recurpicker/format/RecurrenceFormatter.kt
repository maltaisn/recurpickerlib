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

import android.content.Context
import android.content.res.Resources
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.EndType
import com.maltaisn.recurpicker.Recurrence.Period
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * Formatter class for converting [Recurrence] objects to a human-readable string represention.
 * This is basically a localized version of [Recurrence.toString].
 * This class is not thread-safe.
 *
 * @property dateFormat Date format used to format end date.
 */
public class RecurrenceFormatter(public val dateFormat: DateFormat) {

    private val calendar = Calendar.getInstance()

    /**
     * Format a recurrence to its localized string represention.
     * @param context Context needed to access string resources, can be any context.
     * @param r The recurrence to format
     * @param startDate The start date of the event that uses the recurrence. This is optional
     * and can be set to [Recurrence.DATE_NONE] to be unspecified. Specifying the start date will
     * allow to avoid repeating information that will be shared by every event of the recurrence.
     */
    @JvmOverloads
    public fun format(context: Context, r: Recurrence, startDate: Long = Recurrence.DATE_NONE): String {
        val res = context.resources

        if (startDate != Recurrence.DATE_NONE) {
            calendar.timeInMillis = startDate
        }

        val sb = StringBuilder()
        appendPeriodDetails(sb, r, res)

        // Day setting
        if (r.period == Period.WEEKLY) {
            appendWeeklyRecurrenceDetails(sb, r, startDate, res)
        } else if (r.period == Period.MONTHLY) {
            appendMonthlyRecurrenceDetails(sb, r, startDate, res)
        }

        // End type
        appendEndTypeDetails(sb, r, res)

        return sb.toString()
    }

    private fun appendPeriodDetails(sb: java.lang.StringBuilder, r: Recurrence, res: Resources) {
        // does not repeat, every [freq] day/week/month/year.
        sb.append(if (r.period == Period.NONE) {
            res.getString(R.string.rp_format_none)
        } else {
            res.getQuantityString(when (r.period) {
                Period.NONE -> 0
                Period.DAILY -> R.plurals.rp_format_day
                Period.WEEKLY -> R.plurals.rp_format_week
                Period.MONTHLY -> R.plurals.rp_format_month
                Period.YEARLY -> R.plurals.rp_format_year
            }, r.frequency, r.frequency)
        })
    }

    private fun appendWeeklyRecurrenceDetails(sb: StringBuilder, r: Recurrence, startDate: Long, res: Resources) {
        val weekOptionStr = StringBuilder()
        if (r.byDay.countOneBits() > 2) {
            // Multiple days set, they are always appended.
            if (r.byDay == Recurrence.EVERY_DAY_OF_WEEK) {
                // on every day of the week
                weekOptionStr.append(res.getString(R.string.rp_format_weekly_all))
            } else {
                // on [Sun, Mon, Wed, ...]
                appendDaysOfWeekList(weekOptionStr, r, res)
            }
        } else {
            // No day set or single day set, may or may not append it. If events happen on the default day
            // or on the same day of the week as start date's, don't specify the day.
            val isRecurringOnDefaultDay = r.byDay == 1
            val isRecurringOnStartDay = startDate != Recurrence.DATE_NONE &&
                    r.isRecurringOnDaysOfWeek(1 shl calendar[Calendar.DAY_OF_WEEK])
            if (!isRecurringOnDefaultDay && !isRecurringOnStartDay) {
                // Append single day.
                appendDaysOfWeekList(weekOptionStr, r, res)
            }
        }
        if (weekOptionStr.isNotEmpty()) {
            sb.append(' ')
            sb.append(res.getString(R.string.rp_format_weekly_option, weekOptionStr.toString()))
        }
    }

    private fun appendDaysOfWeekList(sb: StringBuilder, r: Recurrence, res: Resources) {
        // Make a list of days of week
        val daysAbbr = res.getStringArray(R.array.rp_days_of_week_abbr3)
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            if (r.isRecurringOnDaysOfWeek(1 shl day)) {
                sb.append(daysAbbr[day - 1])
                sb.append(", ")
            }
        }
        sb.delete(sb.length - 2, sb.length) // Remove extra separator
    }

    private fun appendMonthlyRecurrenceDetails(sb: StringBuilder, r: Recurrence, startDate: Long, res: Resources) {
        if (r.byDay != 0 || r.byMonthDay != 0) {
            // On the same day of the month as start date's, don't specify it.
            sb.append(" (")
            if (startDate != Recurrence.DATE_NONE && r.byMonthDay == calendar[Calendar.DAY_OF_MONTH]) {
                // on the same day of each month
                sb.append(res.getString(R.string.rp_format_monthly_same_day))
            } else if (r.byMonthDay == -1) {
                // on the last day of each month
                sb.append(res.getString(R.string.rp_format_monthly_last_day))
            } else if (r.byDay != 0) {
                // on every [nth] [day of the week]
                sb.append(getDayOfWeekInMonthText(res, r.dayOfWeekInMonth, r.weekInMonth))
            } else {
                throw IllegalArgumentException("Unsupported value of Recurrence.byMonthDay")
            }
            sb.append(')')
        }
    }

    private fun appendEndTypeDetails(sb: StringBuilder, r: Recurrence, res: Resources) {
        if (r.endType != EndType.NEVER) {
            sb.append("; ")
            if (r.endType == EndType.BY_DATE) {
                // until [date]
                sb.append(res.getString(R.string.rp_format_end_date,
                    dateFormat.format(Date(r.endDate))))
            } else {
                // for [endCount] event(s)
                sb.append(res.getQuantityString(R.plurals.rp_format_end_count, r.endCount, r.endCount))
            }
        }
    }

    public companion object {
        /**
         * Returns a localized string in the form "on every first Sunday" for monthly recurrences
         * happening on the same day of the same week each month.
         */
        internal fun getDayOfWeekInMonthText(res: Resources, dayOfWeekInMonth: Int, weekInMonth: Int): String {
            val daysStr = res.getStringArray(R.array.rp_format_monthly_same_week)
            val numbersStr = res.getStringArray(R.array.rp_format_monthly_ordinal)
            val weekNbStr = if (weekInMonth == -1) numbersStr.last() else numbersStr[weekInMonth - 1]
            return String.format(daysStr[dayOfWeekInMonth - 1], weekNbStr)
        }
    }
}
