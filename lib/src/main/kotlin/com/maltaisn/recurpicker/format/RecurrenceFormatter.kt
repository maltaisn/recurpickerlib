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
import com.maltaisn.recurpicker.Recurrence.*
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
     * @param r recurrence to format
     */
    fun format(r: Recurrence, context: Context): String {
        val res = context.resources

        // Generate first part of the text: does not repeat, every [freq] day/week/month/year.
        val recurSb = StringBuilder()
        recurSb.append(when (r.period) {
            Period.NONE -> res.getString(R.string.rp_format_none)
            Period.DAILY -> res.getQuantityString(R.plurals.rp_format_day, r.frequency)
            Period.WEEKLY -> res.getQuantityString(R.plurals.rp_format_week, r.frequency)
            Period.MONTHLY -> res.getQuantityString(R.plurals.rp_format_month, r.frequency)
            Period.YEARLY -> res.getQuantityString(R.plurals.rp_format_year, r.frequency)
        }.replace("|", ""))

        // Day setting
        if (!r.isDefault) {
            if (r.period == Period.WEEKLY) {
                // Make a list of days of week
                val weekOptionStr = StringBuilder()
                if (r.weeklyDays == Recurrence.EVERY_DAY_OF_WEEK) {
                    // on every day of the week
                    weekOptionStr.append(res.getString(R.string.rp_format_weekly_all))

                } else {
                    // on [Sun, Mon, Wed, ...]
                    val daysAbbr = res.getStringArray(R.array.rp_days_of_week_abbr)
                    for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                        if (r.isRecurringOnDaysOfWeek(1 shl day)) {
                            weekOptionStr.append(daysAbbr[day - 1])
                            weekOptionStr.append(", ")
                        }
                    }
                    weekOptionStr.delete(weekOptionStr.length - 2, weekOptionStr.length)  // Remove extra separator
                }
                recurSb.append(' ')
                recurSb.append(res.getString(R.string.rp_format_weekly_option, weekOptionStr.toString()))

            } else if (r.period == Period.MONTHLY) {
                recurSb.append(" (")
                when (r.monthlyDay) {
                    MonthlyDay.SAME_DAY_OF_MONTH -> {
                        // on the same day of each month
                        recurSb.append(res.getString(R.string.rp_format_monthly_same_day))
                    }
                    MonthlyDay.SAME_DAY_OF_WEEK -> {
                        // on every [nth] [Sunday]
                        recurSb.append(getSameDayOfSameWeekString(r.startDate, res))
                    }
                    MonthlyDay.LAST_DAY_OF_MONTH -> {
                        // on the last day of each month
                        recurSb.append(res.getString(R.string.rp_format_monthly_last_day))
                    }
                }
                recurSb.append(")")
            }
        }

        // End type
        if (r.endType != EndType.NEVER) {
            recurSb.append("; ")
            if (r.endType == EndType.BY_DATE) {
                // until [date]
                recurSb.append(res.getString(R.string.rp_format_end_date,
                        dateFormat.format(Date(r.endDate))))
            } else {
                // for [endCount] event(s)
                recurSb.append(res.getQuantityString(R.plurals.rp_format_end_count, r.endCount))
            }
        }

        return recurSb.toString()
    }

    /**
     * Get the text for a date to display on a monthly recurrence repeated on the
     * same day of week of same week, eg: "on third Sunday" or "on last Friday"
     * @param date date to get it for
     */
    internal fun getSameDayOfSameWeekString(date: Long, res: Resources): String {
        calendar.timeInMillis = date

        // Since strings are in a string array, can't be formatted directly
        // so do it in the same its done in android.content.res.Resources
        val daysStr = res.getIntArray(R.array.rp_format_monthly_same_week)
        val numbersStr = res.getStringArray(R.array.rp_format_monthly_ordinal)
        val weekNbStr = numbersStr[calendar[Calendar.DAY_OF_WEEK_IN_MONTH] - 1]
        return res.getString(daysStr[calendar[Calendar.DAY_OF_WEEK] - 1], weekNbStr)
    }

}
