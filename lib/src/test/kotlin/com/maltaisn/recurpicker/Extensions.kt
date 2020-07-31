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

package com.maltaisn.recurpicker

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.TimeZone

val patterns = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to TimeZone.getTimeZone("GMT"),
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to TimeZone.getTimeZone("GMT"),
    "yyyy-MM-dd'T'HH:mm:ss.SSS" to TimeZone.getDefault(),
    "yyyy-MM-dd" to TimeZone.getDefault()
)

/**
 * Get UTC millis since epoch time for date patterns:
 * - `2020-01-05`: in UTC time zone, time is set to 00:00:00.000.
 * - `2020-01-05T09:12:11.000`: in local time zone.
 * - `2020-01-05T09:12:11.000Z`: in GMT time zone.
 * - `2020-01-05T09:12:11.000-07:00`: in specified time zone.
 * Throws an error if date can't be parsed according to any of these patterns.
 */
internal fun dateFor(date: String): Long {
    val dateFormat = SimpleDateFormat()
    for ((pattern, timeZone) in patterns) {
        if (timeZone != null) {
            dateFormat.timeZone = timeZone
        }
        dateFormat.applyPattern(pattern)
        return try {
            dateFormat.parse(date)?.time ?: continue
        } catch (e: ParseException) {
            continue
        }
    }
    throw IllegalArgumentException("Invalid date literal")
}
