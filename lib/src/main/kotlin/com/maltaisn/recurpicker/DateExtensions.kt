/*
 * Copyright 2020 Nicolas Maltais
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

package com.maltaisn.recurpicker

import java.util.Calendar

/**
 * Value returned by [Calendar.get] with [Calendar.WEEK_OF_MONTH] for the last week of the month.
 */
internal const val CALENDAR_LAST_WEEK_IN_MONTH = 5

/**
 * Compare [this] date with another [date], ignoring time of the day.
 * Returns `-1` if [this] is on a day before [date].
 * Returns `0` if [this] is on same day as [date].
 * Returns `1` if [this] is on a day after [date].
 */
internal fun Long.compareDay(date: Long, calendar: Calendar): Int {
    // Special cases since DATE_NONE makes calendar overflow.
    when {
        date == Recurrence.DATE_NONE && this == Recurrence.DATE_NONE -> return 0
        date == Recurrence.DATE_NONE || this == Recurrence.DATE_NONE -> {
            throw IllegalArgumentException("Cannot compare dates.")
        }
    }

    calendar.timeInMillis = this
    val year1 = calendar[Calendar.YEAR]
    val day1 = calendar[Calendar.DAY_OF_YEAR]

    calendar.timeInMillis = date
    val year2 = calendar[Calendar.YEAR]
    val day2 = calendar[Calendar.DAY_OF_YEAR]

    return when {
        year1 > year2 -> 1
        year1 < year2 -> -1
        day1 > day2 -> 1
        day1 < day2 -> -1
        else -> 0
    }
}
