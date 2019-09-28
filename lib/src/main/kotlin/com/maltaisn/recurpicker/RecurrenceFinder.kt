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

import com.maltaisn.recurpicker.Recurrence.*
import com.maltaisn.recurpicker.Recurrence.Companion.compareDay
import java.util.*


/**
 * Utility object to find the dates of the events of a [Recurrence] object.
 * All date parameters are given as millis since epoch time.
 */
class RecurrenceFinder {

    private val date = GregorianCalendar()
    private val temp = GregorianCalendar()

    /**
     * Get event dates of a [recurrence][r] based on a [previous event date][base] of the
     * same recurrence. If the recurrence has an end by count rule, it's important to also
     * supply the [base count][baseCount] to indicate how many events have happened as of
     * the base date so the algorithm knows when to stop. Starting [from a date][fromDate],
     * a certain [amount] of events are found and returned.
     *
     * While using this method is a bit more complicated than using [find], it can be faster
     * to find a recurrence event if previous event dates are known. For example, finding the
     * 1000th event when the first 999 events are known.
     *
     * @param r The recurrence.
     * @param base The base event date. May included in the returned list of events
     * depending on the [fromDate] value.
     * @param baseCount The number of events that have happened as of the [base] date. This
     * is only used if the recurrence has an end by count rule. Any value can safely be used otherwise.
     * For example, if the base date is the start date, this should be 1 since only the event on
     * the start date has happened.
     * @param amount The number of events to find, must be at least 1.
     * @param fromDate The date from which to start finding recurrence events. Can be set
     * to [Recurrence.DATE_NONE] to find events from the start date of the recurrence.
     * Time of the day isn't taken into account, only the date.
     */
    @JvmOverloads
    fun findBasedOn(r: Recurrence, base: Long, baseCount: Int,
                    amount: Int, fromDate: Long = Recurrence.DATE_NONE): MutableList<Long> {
        require(amount >= 1) { "Amount must be 1 or greater" }

        val list = mutableListOf<Long>()

        val from = if (fromDate == Recurrence.DATE_NONE) r.startDate else fromDate
        date.timeInMillis = base

        var count = baseCount - 1
        var fromDateReached = false

        when (r.period) {
            Period.NONE -> {
                if (r.startDate.compareDay(from, temp) != -1) {
                    list += r.startDate
                }
            }
            Period.DAILY -> {
                while (true) {
                    if (list.size == amount || r.endType == EndType.BY_COUNT && count >= r.endCount ||
                            r.endType == EndType.BY_DATE && date.timeInMillis.compareDay(r.endDate, temp) == 1) {
                        // Amount, end count or end date has been reached.
                        return list
                    }

                    if (!fromDateReached && date.timeInMillis.compareDay(from, temp) != -1) {
                        // From date has been reached.
                        fromDateReached = true
                    }
                    if (fromDateReached && list.size < amount) {
                        list += date.timeInMillis
                    }
                    count++

                    // Add days to the current date.
                    date.add(Calendar.DATE, r.frequency)
                }
            }
            Period.WEEKLY -> {
                val startDay = date[Calendar.DAY_OF_WEEK]
                var isFirstWeek = true

                // Set date to beginning of the week
                date[Calendar.DAY_OF_WEEK] = Calendar.SUNDAY

                while (true) {
                    for (day in 1..7) {
                        if (list.size == amount || r.endType == EndType.BY_COUNT && count >= r.endCount
                                || r.endType == EndType.BY_DATE && date.timeInMillis.compareDay(r.endDate, temp) == 1) {
                            // Amount, end count or end date has been reached.
                            return list
                        }

                        if ((!isFirstWeek || day >= startDay)
                                && r.isRecurringOnDaysOfWeek(1 shl day)) {
                            // On first week, don't count events on days before start day.
                            if (!fromDateReached && date.timeInMillis.compareDay(from, temp) != -1) {
                                // From date has been reached.
                                fromDateReached = true
                            }
                            if (fromDateReached && list.size < amount) {
                                list += date.timeInMillis
                            }
                            count++
                        }

                        // Increment the date by one day.
                        date.add(Calendar.DATE, 1)
                    }

                    // Skip a number of weeks depending on frequency.
                    date.add(Calendar.DATE, 7 * (r.frequency - 1))

                    isFirstWeek = false
                }
            }
            Period.MONTHLY -> {
                // Save info on which day in the month the events happen.
                val rday = date[Calendar.DAY_OF_WEEK]
                val rmonthDay = date[Calendar.DAY_OF_MONTH]
                val rweek = date[Calendar.DAY_OF_WEEK_IN_MONTH]

                loop@ while (true) {
                    if (list.size == amount || r.endType == EndType.BY_COUNT && count >= r.endCount
                            || r.endType == EndType.BY_DATE && date.timeInMillis.compareDay(r.endDate, temp) == 1) {
                        // Amount, end count or end date has been reached.
                        return list
                    }

                    when (r.monthlyDay) {
                        MonthlyDay.LAST_DAY_OF_MONTH -> {
                            // Set date to the last day of the month.
                            date[Calendar.DAY_OF_MONTH] = date.getActualMaximum(Calendar.DAY_OF_MONTH)
                        }
                        MonthlyDay.SAME_DAY_OF_MONTH -> {
                            // Set date to the same day as start date.
                            if (rmonthDay > date.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                                // The month doesn't have this day, eg: February doesn't have a 31.
                                date.add(Calendar.MONTH, r.frequency)
                                continue@loop
                            } else {
                                date[Calendar.DAY_OF_MONTH] = rmonthDay
                            }
                        }
                        MonthlyDay.SAME_DAY_OF_WEEK -> {
                            // Set to the middle of the month to make sure month doesn't change
                            // when changing day of week and week of month below.
                            date.set(Calendar.DAY_OF_MONTH, 15)

                            if (rweek == 5) {
                                // Last week of the month
                                date[Calendar.DAY_OF_WEEK] = rday
                                date[Calendar.DAY_OF_WEEK_IN_MONTH] = -1
                            } else {
                                date[Calendar.DAY_OF_WEEK] = rday
                                date[Calendar.DAY_OF_WEEK_IN_MONTH] = rweek
                            }
                        }
                    }

                    if (!fromDateReached && date.timeInMillis.compareDay(from, temp) != -1) {
                        // From date has been reached.
                        fromDateReached = true
                    }
                    if (fromDateReached && list.size < amount) {
                        list += date.timeInMillis
                    }
                    count++

                    // Add months to the current date.
                    date.add(Calendar.MONTH, r.frequency)
                }
            }
            Period.YEARLY -> {
                // Check if start date is on Feb 29.
                date.timeInMillis = r.startDate
                val isStartFeb29 = date.isLeapYear(date[Calendar.YEAR]) &&
                        date[Calendar.DAY_OF_YEAR] == 60
                date.timeInMillis = base

                while (true) {
                    if (list.size == amount || r.endType == EndType.BY_COUNT && count >= r.endCount ||
                            r.endType == EndType.BY_DATE && date.timeInMillis.compareDay(r.endDate, temp) == 1) {
                        // Amount, end count or end date has been reached.
                        return list
                    }

                    if (!fromDateReached && date.timeInMillis.compareDay(from, temp) != -1) {
                        // From date has been reached.
                        fromDateReached = true
                    }
                    if (fromDateReached && list.size < amount) {
                        list += date.timeInMillis
                    }
                    count++

                    // Add years to the current date.
                    while (true) {
                        date.add(Calendar.YEAR, r.frequency)
                        if (isStartFeb29) {
                            // Adding years to a calendar on Feb 29 sets it to Feb 28, change that.
                            if (date.isLeapYear(date[Calendar.YEAR])) {
                                date[Calendar.DAY_OF_YEAR] = 60
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
            }
        }

        return list
    }

    /**
     * Get an [amount] of event dates of a [recurrence][r] after a [date][fromDate].
     * Note that the recurrence's start date is never included in the returned list.
     *
     * @param r The recurrence.
     * @param amount The number of events to find, must be at least 1.
     * @param fromDate The date from which to start finding recurrence events. Can be set
     * to [Recurrence.DATE_NONE] to find events from the start date of the recurrence.
     */
    @JvmOverloads
    fun find(r: Recurrence, amount: Int, fromDate: Long = Recurrence.DATE_NONE): MutableList<Long> {
        return findBasedOn(r, r.startDate, 1, amount, fromDate)
    }

    /**
     * Get event dates of a [recurrence][r] between a [start date][startDate] (inclusive)
     * and an [end date][endDate] (exclusive).
     */
    fun findBetween(r: Recurrence, startDate: Long, endDate: Long): MutableList<Long> {
        val list = mutableListOf<Long>()
        var lastDate = r.startDate
        var count = 1
        while (true) {
            // Find the next event based on the last.
            val events = findBasedOn(r, lastDate, count, 2)

            if (events.size == 1) {
                // No more events.
                return list
            } else {
                // Add event if after start date.
                lastDate = events.last()
                if (lastDate >= endDate) {
                    return list
                } else if (lastDate >= startDate) {
                    list += lastDate
                }
                count++
            }
        }
    }

}
