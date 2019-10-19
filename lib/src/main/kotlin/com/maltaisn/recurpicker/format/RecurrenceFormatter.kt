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
import java.util.*


/**
 * Formatter class for converting [Recurrence] objects to a human-readable string represention.
 * This is basically a localized version of [Recurrence.toString].
 *
 * @param context Context used to get string values.
 * @property dateFormat Date format used to format end date.
 */
class RecurrenceFormatter(val dateFormat: DateFormat) {

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
    fun format(context: Context, r: Recurrence, startDate: Long = Recurrence.DATE_NONE): String {
        val res = context.resources

        if (startDate != Recurrence.DATE_NONE) {
            calendar.timeInMillis = startDate
        }

        // Generate first part of the text: does not repeat, every [freq] day/week/month/year.
        val sb = StringBuilder()
        sb.append(when (r.period) {
            Period.NONE -> res.getString(R.string.rp_format_none)
            Period.DAILY -> res.getQuantityString(R.plurals.rp_format_day, r.frequency, r.frequency)
            Period.WEEKLY -> res.getQuantityString(R.plurals.rp_format_week, r.frequency, r.frequency)
            Period.MONTHLY -> res.getQuantityString(R.plurals.rp_format_month, r.frequency, r.frequency)
            Period.YEARLY -> res.getQuantityString(R.plurals.rp_format_year, r.frequency, r.frequency)
        })

        // Day setting
        if (r.period == Period.WEEKLY) {
            // Make a list of days of week
            val weekOptionStr = StringBuilder()
            if (r.byDay != 1 && (startDate == Recurrence.DATE_NONE
                            || r.byDay != (1 or (1 shl calendar[Calendar.DAY_OF_WEEK])))) {
                // If events happen on the same day of the week as start date's, don't specify the day.
                if (r.byDay == Recurrence.EVERY_DAY_OF_WEEK) {
                    // on every day of the week
                    weekOptionStr.append(res.getString(R.string.rp_format_weekly_all))

                } else {
                    // on [Sun, Mon, Wed, ...]
                    val daysAbbr = res.getStringArray(R.array.rp_days_of_week_abbr3)
                    for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                        if (r.isRecurringOnDaysOfWeek(1 shl day)) {
                            weekOptionStr.append(daysAbbr[day - 1])
                            weekOptionStr.append(", ")
                        }
                    }
                    weekOptionStr.delete(weekOptionStr.length - 2, weekOptionStr.length)  // Remove extra separator
                }
                sb.append(' ')
                sb.append(res.getString(R.string.rp_format_weekly_option, weekOptionStr.toString()))
            }

        } else if (r.period == Period.MONTHLY) {
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

        // End type
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

        return sb.toString()
    }

    companion object {
        /**
         * Returns a localized string in the form "on every first Sunday" for monthly recurrences
         * happening on the same day of the same week each month.
         */
        internal fun getDayOfWeekInMonthText(res: Resources, dayOfWeekInMonth: Int, weekInMonth: Int): String {
            val daysStr = res.getStringArray(R.array.rp_format_monthly_same_week)
            val numbersStr = res.getStringArray(R.array.rp_format_monthly_ordinal)
            val weekNbStr = if (weekInMonth == -1) numbersStr[4] else numbersStr[weekInMonth - 1]
            return String.format(daysStr[dayOfWeekInMonth - 1], weekNbStr)
        }
    }

}
