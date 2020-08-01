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

import com.maltaisn.recurpicker.Recurrence.EndType
import com.maltaisn.recurpicker.Recurrence.Period
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Utility object to find the dates of the events of a [Recurrence] object.
 * All date parameters are given as millis since UTC epoch time.
 * This class is not thread-safe.
 */
class RecurrenceFinder {

    /**
     * The timezone used for finding recurrence events.
     * Note that changing this doesn't change the fact that all date parameters are in **UTC** millis since epoch time.
     */
    var timeZone: TimeZone = TimeZone.getDefault()
        set(value) {
            field = value
            current.timeZone = timeZone
            temp.timeZone = timeZone
        }

    /**
     * Main calendar used for finding recurrence events.
     * The calendar fields are changed according the the period and frequency and the millis it is on is returned.
     */
    private val current = GregorianCalendar()

    /**
     * Temp calendar
     */
    private val temp = GregorianCalendar()

    /**
     * Get event dates of a [recurrence][r] based on a [previous event date][base] of the
     * same recurrence. If the recurrence has an end by count rule, it's important to also
     * supply the [base count][baseCount] to indicate how many events have happened as of
     * the base date so the algorithm knows when to stop. Starting [from a date][fromDate],
     * a certain [amount] of events are found and returned.
     *
     * While using this method is a bit more complicated than using [find], it will be more
     * performant to find a recurrence event if previous event dates are known. For example,
     * finding the 1000th event when the first 999 events are known.
     *
     * @param r The recurrence.
     * @param startDate The start date of the recurring event, cannot be [Recurrence.DATE_NONE].
     * @param base The base event date. Might get added to the returned list of events
     * depending on the [fromDate] value.
     * @param baseCount The number of events that have happened as of the [base] date.
     * If the base date is the start date, this should be 1 since only the event on
     * the start date has happened. This value is only used if the recurrence has an end by count rule.
     * Any value can safely be used otherwise.
     * @param amount The maximum number of events to find, must be at least 0.
     *  @param fromDate The date from which to start finding recurrence events. Can be set
     * to [Recurrence.DATE_NONE] to find events from the start date of the recurrence.
     * @param includeStart Whether the [startDate] or [fromDate] should be included in the list of events.
     */
    @JvmOverloads
    fun findBasedOn(
        r: Recurrence,
        startDate: Long,
        base: Long,
        baseCount: Int,
        amount: Int,
        fromDate: Long = Recurrence.DATE_NONE,
        includeStart: Boolean = true
    ): MutableList<Long> {
        require(amount >= 0) { "Amount must be 0 or greater" }
        require(startDate != Recurrence.DATE_NONE) { "Start date cannot be none." }

        if (amount == 0) {
            return mutableListOf()
        }

        var from = if (fromDate == Recurrence.DATE_NONE || fromDate < base) {
            // If no "from" date is specified, start at base event date.
            // If "from" date is before the base event date, take a shortcut and start at base too.
            base
        } else {
            fromDate
        }

        if (!includeStart) {
            // Add one day to "from" date to exclude it.
            temp.timeInMillis = from
            temp.add(Calendar.DATE, 1)
            from = temp.timeInMillis
        }

        current.timeInMillis = base

        return when (r.period) {
            Period.NONE -> findEventsForNoneRecurrence(startDate, from)
            Period.DAILY -> findEventsForDailyRecurrence(r, from, baseCount, amount)
            Period.WEEKLY -> findEventsForWeeklyRecurrence(r, from, amount, base, baseCount)
            Period.MONTHLY -> findEventsForMonthlyRecurrence(r, from, amount, baseCount)
            Period.YEARLY -> findEventsForYearlyRecurrence(r, startDate, from, amount, baseCount)
        }
    }

    /**
     * Get an [amount] of event dates of a [recurrence][r] after a [date][fromDate].
     * Note that the recurrence's start date is never included in the returned list.
     *
     * @param r The recurrence.
     * @param startDate The start date of the recurring event, cannot be [Recurrence.DATE_NONE].
     * @param amount The number of events to find, must be at least 1.
     * @param fromDate The date from which to start finding recurrence events. Can be set
     * to [Recurrence.DATE_NONE] to find events from the start date of the recurrence.
     * The date is inclusive meaning an event on this date will be included.
     * @param includeStart Whether the [startDate] or [fromDate] should be included in the list of events.
     */
    @JvmOverloads
    fun find(
        r: Recurrence,
        startDate: Long,
        amount: Int,
        fromDate: Long = Recurrence.DATE_NONE,
        includeStart: Boolean = true
    ): MutableList<Long> {
        return findBasedOn(r, startDate, startDate, 1, amount, fromDate, includeStart)
    }

    /**
     * Get event dates of a [recurrence][r] between a [start date][start] (inclusive)
     * and an [end date][end] (exclusive).
     * @param startDate The start date of the recurring event, cannot be [Recurrence.DATE_NONE].
     */
    fun findBetween(r: Recurrence, startDate: Long, start: Long, end: Long): MutableList<Long> {
        val list = mutableListOf<Long>()
        var lastDate = startDate
        var count = 1
        while (true) {
            // Find the next event based on the last.
            val events = findBasedOn(r, startDate, lastDate, count, 2)

            if (events.size == 1) {
                // No more events.
                return list
            } else {
                // Add event if after start date.
                lastDate = events.last()
                if (lastDate >= end) {
                    return list
                } else if (lastDate >= start) {
                    list += lastDate
                }
                count++
            }
        }
    }

    private fun findEventsForNoneRecurrence(startDate: Long, fromDate: Long): MutableList<Long> {
        val events = mutableListOf<Long>()
        if (startDate.compareDay(fromDate) >= 0) {
            // Does not repeat, so only add the start event if after the from date.
            events += startDate
        }
        return events
    }

    private fun findEventsForDailyRecurrence(
        r: Recurrence,
        fromDate: Long,
        baseCount: Int,
        amount: Int
    ) = findEventsForRecurrence(r, fromDate, amount, baseCount,
        goToNextPeriod = {
            current.add(Calendar.DATE, r.frequency)
        })

    private fun findEventsForWeeklyRecurrence(
        r: Recurrence,
        fromDate: Long,
        amount: Int,
        base: Long,
        baseCount: Int
    ): MutableList<Long> {
        val startDay = current[Calendar.DAY_OF_WEEK]
        var isFirstWeek = true

        // Set date to beginning of the week
        current[Calendar.DAY_OF_WEEK] = Calendar.SUNDAY
        if (current.timeInMillis > base) {
            // The above statement will give a date that is the start of the current week in most devices like Google
            // Pixel. But in devices from OEMs like Samsung it will return the start date for the next week i.e. a future
            // date. This causes the return list of dates returned to miss dates in the first week when selecting a
            // WEEKLY frequency that NEVER ends. So if a future date is returned then move back by 7 days to solve this.
            current.add(Calendar.DATE, -DAYS_IN_WEEK)
        }

        var dayOfWeek = Calendar.SUNDAY
        return findEventsForRecurrence(r, fromDate, amount, baseCount,
            adjustDate = {
                val isAfterStartDate = !isFirstWeek || dayOfWeek >= startDay
                val isRecurringOnDayDefault = r.byDay == 1 && dayOfWeek == startDay // See Recurrence.byDay docs.
                val isRecurringOnDayCustom = r.byDay != 1 && r.isRecurringOnDaysOfWeek(1 shl dayOfWeek)
                isAfterStartDate && (isRecurringOnDayDefault || isRecurringOnDayCustom)
            },
            goToNextPeriod = {
                // Go to next day.
                current.add(Calendar.DATE, 1)
                dayOfWeek++

                if (dayOfWeek > Calendar.SATURDAY) {
                    // This week is done, go to the next week in which there might be an event (depends on frequency).
                    // 1 week is subtracted from the frequency since date was just advanced day by day for one week.
                    current.add(Calendar.DATE, DAYS_IN_WEEK * (r.frequency - 1))
                    dayOfWeek = Calendar.SUNDAY
                    isFirstWeek = false
                }
            })
    }

    private fun findEventsForMonthlyRecurrence(
        r: Recurrence,
        fromDate: Long,
        amount: Int,
        baseCount: Int
    ): MutableList<Long> {
        // If events happen on the same day of each month but byMonthDay is 0,
        // use the start date's day for the day of the month.
        val monthDay = if (r.byMonthDay != 0) r.byMonthDay else current[Calendar.DAY_OF_MONTH]

        return findEventsForRecurrence(r, fromDate, amount, baseCount,
            adjustDate = {
                if (r.byDay != 0) {
                    // Set to the middle of the month to make sure month doesn't change
                    // when changing day of week and week of month below.
                    current.set(Calendar.DAY_OF_MONTH, MONTH_MIDDLE_DAY)
                    current[Calendar.DAY_OF_WEEK] = r.dayOfWeekInMonth
                    current[Calendar.DAY_OF_WEEK_IN_MONTH] = r.weekInMonth
                    true
                } else {
                    // Set date to the same day of each month.
                    val maxDays = current.getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (abs(monthDay) <= maxDays) {
                        current[Calendar.DAY_OF_MONTH] = if (monthDay > 0) {
                            monthDay
                        } else {
                            // Negative value meaning a number of days from the end of the month.
                            current.getActualMaximum(Calendar.DAY_OF_MONTH) + monthDay + 1
                        }
                        true
                    } else {
                        // If recurring on a specific day of the month but the current month doesn't have this day,
                        // (e.g.: February doesn't have a 31), then skip the month altogether.
                        false
                    }
                }
            },
            goToNextPeriod = {
                current.add(Calendar.MONTH, r.frequency)
            })
    }

    private fun findEventsForYearlyRecurrence(
        r: Recurrence,
        startDate: Long,
        fromDate: Long,
        amount: Int,
        baseCount: Int
    ): MutableList<Long> {
        // Check if start date is on Feb 29.
        temp.timeInMillis = startDate
        val isStartDateOnFeb29 = temp.isLeapYear(temp[Calendar.YEAR]) && temp[Calendar.DAY_OF_YEAR] == FEB_29

        return findEventsForRecurrence(r, fromDate, amount, baseCount,
            adjustDate = {
                if (isStartDateOnFeb29) {
                    if (current.isLeapYear(current[Calendar.YEAR])) {
                        // Adding years to a calendar on Feb 29 sets it to Feb 28. Change it back.
                        current[Calendar.DAY_OF_YEAR] = FEB_29
                        true
                    } else {
                        // No Feb 29 this year, skip.
                        false
                    }
                } else {
                    true
                }
            },
            goToNextPeriod = {
                current.add(Calendar.YEAR, r.frequency)
            })
    }

    /**
     * Base logic to find and return events for a recurrence [r]. Implements checks for end constraints,
     * max amount constraint and "from" date constraint. Remaining logic is to be implemented at call site.
     *
     * This function works with "periods", the shortest unit of time which might contain 0 or 1 event. For every period,
     * [adjustDate] is called to set the [current] calendar on the day in the period in which there *could* be an event.
     * For example, for a monthly recurrence on the last day of the month, [adjustDate] would set the day to the last
     * day of the month, the "period" here being a month. If there's no event within the current period, [adjustDate]
     * should return `false`. If there's an event, it is added to the list if matching constraints: end count, end date,
     * max [amount], and [fromDate]. Then, the current date is advanced to the next period with [goToNextPeriod].
     */
    private inline fun findEventsForRecurrence(
        r: Recurrence,
        fromDate: Long,
        amount: Int,
        baseCount: Int,
        adjustDate: () -> Boolean = { true },
        goToNextPeriod: () -> Unit = {}
    ): MutableList<Long> {
        val events = mutableListOf<Long>()
        var count = baseCount - 1
        var fromDateReached = false

        while (
            events.size < amount && // Amount must be lower than max amount needed.
            !r.isEndCountReached(count) && // If ending by count, count must be lower than end count.
            !r.isEndDateExceeded(current.timeInMillis) // If ending by date, date must be before end date.
        ) {
            if (adjustDate()) {
                if (!fromDateReached && current.timeInMillis.compareDay(fromDate) >= 0) {
                    // From date has been reached.
                    fromDateReached = true
                }
                if (fromDateReached) {
                    events += current.timeInMillis
                }
                count++
            }
            goToNextPeriod()
        }
        return events
    }

    private fun Recurrence.isEndCountReached(count: Int) =
        this.endType == EndType.BY_COUNT && count >= this.endCount

    private fun Recurrence.isEndDateExceeded(date: Long) =
        this.endType == EndType.BY_DATE && date.compareDay(this.endDate) > 0

    private fun Long.compareDay(date: Long) = this.compareDay(date, temp)

    companion object {
        private const val FEB_29 = 60

        private const val DAYS_IN_WEEK = 7
        private const val MONTH_MIDDLE_DAY = 15
    }
}
