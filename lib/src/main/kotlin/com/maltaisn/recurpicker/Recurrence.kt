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


import androidx.annotation.IntDef
import com.maltaisn.recurpicker.Recurrence.*
import com.maltaisn.recurpicker.Recurrence.Companion.DATE_NONE
import com.maltaisn.recurpicker.Recurrence.Period.*
import java.util.*

/**
 * An object describing a recurrence rule.
 * All recurrence objects are immutable.
 *
 * @property startDate The start date of the recurrence in millis time since epoch.
 * The start date is exclusive meaning the first event will never happen on that day.
 *
 * @property period The time period over which the recurrence happens.
 *
 * @property frequency The recurrence frequency i.e after how many periods to repeat the event.
 * For example, if period is daily and frequency is 2, events will happen every 2 days.
 * Frequency of [Period.NONE] recurrences is always 1.
 *
 * @property weeklyDays A bit field of the days of the week on which a weekly recurrence occurs.
 * Bit field valid flags are the [WeeklyDays] constants.
 * Only used if the [period] set is [Period.WEEKLY], otherwise value is `0`.
 *
 * @property monthlyDay The setting which sets on which day of the month the events happen.
 * See [MonthlyDay] for more information on each setting. For recurrence not recurring monthly,
 * this setting will always have the [MonthlyDay.SAME_DAY_OF_MONTH] value.
 *
 * @property endType The rule for the recurrence end, see [EndType].
 *
 * @property endDate The end date if end type is [EndType.BY_DATE]. If not, end date is
 * always [DATE_NONE]. The end date is inclusive so the last event might be on the end date.
 *
 * @property endCount The number of events before the end of the recurrence if end type is
 * [EndType.BY_COUNT]. If not, end count is always `0`. Since the start date is exclusive,
 * the number of events never includes the start date event.
 *
 * @property isDefault Whether recurrence is "default". A default recurrence will not show the
 * day of the week or the monthly setting when formatted to text. For example, a default weekly
 * recurrence starting on Tue Sep 24, 2019 will be formatted to "Repeat weekly" instead of "Repeat
 * weekly on Tuesday" since the day of the week is implied to be the same as the starting date's.
 * The value of this field has no effect when finding recurrence events.
 */
class Recurrence private constructor(
        val startDate: Long,
        val period: Period,
        val frequency: Int,
        @WeeklyDays val weeklyDays: Int,
        val monthlyDay: MonthlyDay,
        val endType: EndType,
        val endDate: Long,
        val endCount: Int,
        val isDefault: Boolean) {

    /**
     * If repeating weekly, check if recurrence happens on certain [days] of the week.
     * Returns true only if recurrence happens on all of these days.
     * @param days A bit field of [WeeklyDays] values, just like [weeklyDays].
     */
    fun isRecurringOnDaysOfWeek(@WeeklyDays days: Int): Boolean = ((weeklyDays and days) == days)


    /**
     * Note: since two recurrences with dates at different times of the day will produce the same
     * events, they are considered equal, even though the dates may not have the same value.
     * Same applies for [hashCode].
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Recurrence) return false
        return isDefault == other.isDefault
                && period == other.period
                && frequency == other.frequency
                && weeklyDays == other.weeklyDays
                && monthlyDay == other.monthlyDay
                && endType == other.endType
                && endCount == other.endCount
                && getDaysInDate(startDate) == getDaysInDate(other.startDate)
                && getDaysInDate(endDate) == getDaysInDate(other.endDate)
    }

    override fun hashCode(): Int = Objects.hash(getDaysInDate(startDate), period, frequency,
            weeklyDays, monthlyDay, endType, getDaysInDate(endDate), endCount, isDefault)


    class Builder {

        /** @see Recurrence.startDate */
        var startDate: Long = DATE_NONE

        /** @see Recurrence.frequency */
        var period: Period = NONE

        /**
         * Frequency must be at least 1.
         * @see Recurrence.frequency
         */
        var frequency: Int = 1

        /**
         * If setting to `0`, the period will be set to [Period.NONE].
         * If setting to every day of the week and frequency is `1`, the period will be set to [Period.DAILY].
         * @see Recurrence.weeklyDays
         */
        @WeeklyDays
        var weeklyDays: Int = 0

        /** @see Recurrence.monthlyDay */
        var monthlyDay: MonthlyDay = MonthlyDay.SAME_DAY_OF_MONTH

        /**
         * This field doesn't have to be set directly most of the time,
         * since setting the end date or end count will set it automatically.
         * @see Recurrence.endType
         */
        var endType: EndType = EndType.NEVER

        /**
         * Setting this value will changing end type to by date. Must be on after [startDate].
         * If end date is [DATE_NONE], recurrence end type will be set to never.
         * If end date is on the same day as start date, period will be set to [Period.NONE].
         * @see Recurrence.endDate
         */
        var endDate: Long = DATE_NONE
            set(value) {
                endType = if (value == DATE_NONE) EndType.NEVER else EndType.BY_DATE
                field = value
            }

        /**
         * Setting this value will changing end type to by count.
         * Must be at least 1. If less than 1, the period will be set to [Period.NONE].
         * @see Recurrence.endCount
         */
        var endCount: Int = 0
            set(value) {
                endType = EndType.BY_COUNT
                field = value
            }

        /**
         * If setting to `true`, the recurrence must meet these criteria to change:
         * - Does not repeat or repeats with a frequency of 1
         * - Never ends.
         * - If repeating weekly, must only repeat on the same day of the week as the start date's.
         * - If repeating monthly, must repeat on the same day of each month.
         * Failing to meet these criteria when setting to `true` will result in an exception.
         */
        var isDefault: Boolean = false


        /**
         * Create a builder initialized for a recurrence starting on [startDate] and with a [period].
         * If recurrence is weekly, [weeklyDays] is set to the same day of the week as [startDate].
         */
        constructor(startDate: Long, period: Period) {
            this.startDate = startDate
            this.period = period

            if (period == WEEKLY && startDate != DATE_NONE) {
                calendar.timeInMillis = startDate
                weeklyDays = 1 shl calendar[Calendar.DAY_OF_WEEK]
            }
        }

        /**
         * Create a builder initialized with the values of another [recurrence].
         */
        constructor(recurrence: Recurrence) {
            startDate = recurrence.startDate
            period = recurrence.period
            frequency = recurrence.frequency
            weeklyDays = recurrence.weeklyDays
            monthlyDay = recurrence.monthlyDay
            endDate = recurrence.endDate
            endCount = recurrence.endCount
            endType = recurrence.endType
            isDefault = recurrence.isDefault
        }

        /**
         * Build the recurrence object described by the builder.
         * This validates and normalizes all field values.
         */
        fun build(): Recurrence {
            require(frequency >= 1) { "Frequency must be 1 or greater." }
            require(startDate != DATE_NONE) { "Start date cannot be DATE_NONE." }

            if (period == WEEKLY) {
                require(weeklyDays in 0..EVERY_DAY_OF_WEEK) { "Day of the week bit field has invalid value." }
                if (weeklyDays == 0) {
                    // Recurring on no days of the week so make it non-recurring.
                    period = NONE
                } else if (weeklyDays == EVERY_DAY_OF_WEEK && frequency == 1) {
                    // Recurring every week on every day of the week so make it daily.
                    period = DAILY
                }
            }

            if (endType == EndType.BY_DATE) {
                // Check if end date is not before start date.
                val comparison = endDate.compareDay(startDate)
                require(comparison != -1) { "End date must be after start date" }
                if (comparison == 0) {
                    period = NONE
                    endType = EndType.NEVER
                }

            } else if (endType == EndType.BY_COUNT && endCount < 1) {
                // Recurrence ends after less than one event so make it non-recurring.
                period = NONE
                endType = EndType.NEVER
            }

            if (period == MONTHLY && monthlyDay == MonthlyDay.LAST_DAY_OF_MONTH) {
                // If monthly recurrence occurs on the last day, check if start date is on the last day of the month.
                calendar.timeInMillis = startDate
                require(calendar[Calendar.DAY_OF_MONTH] == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    "Monthly recurrence cannot occur on the last day of the month if start date isn't on a last day."
                }
            }

            // Normalize unused fields by setting them to default values so equals work correctly.
            var endDate = endDate
            var endCount = endCount
            if (period == NONE || endType != EndType.BY_DATE) endDate = DATE_NONE
            if (period == NONE || endType != EndType.BY_COUNT) endCount = 0
            if (period != WEEKLY) weeklyDays = 0
            if (period != MONTHLY) monthlyDay = MonthlyDay.SAME_DAY_OF_MONTH

            if (isDefault) {
                // Check if recurrence meets the default flag criteria
                isDefault = (frequency == 1 && endType == EndType.NEVER && when (period) {
                    NONE, DAILY, YEARLY -> true
                    WEEKLY -> {
                        calendar.timeInMillis = startDate
                        weeklyDays == 1 shl calendar[Calendar.DAY_OF_WEEK]
                    }
                    MONTHLY -> monthlyDay == MonthlyDay.SAME_DAY_OF_MONTH
                })
                require(isDefault) { "Recurrence does not meet criteria for setting isDefault to true." }
            }

            return Recurrence(startDate, period, frequency, weeklyDays,
                    monthlyDay, endType, endDate, endCount, isDefault)
        }
    }


    enum class Period {
        /**
         * Period for a recurrence that does not repeat.
         * Finding events of this recurrence will return none.
         */
        NONE,

        /**
         * Period for a recurrence that occurs every X day(s).
         */
        DAILY,

        /**
         * Period for a recurrence that occurs every X week(s),
         * on one or several days of the week.
         */
        WEEKLY,

        /**
         * Period for a recurrence that occurs every X month(s), on a particular day of the month.
         */
        MONTHLY,

        /**
         * Period for a recurrence that occurs every X year(s).
         */
        YEARLY
    }

    /**
     * Int def for the [weeklyDays] bit field property.
     */
    @IntDef(flag = true, value = [SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY])
    @Retention(AnnotationRetention.SOURCE)
    annotation class WeeklyDays


    enum class MonthlyDay {
        /**
         * Events will happen on the same day of each month. The day of the month is the same as
         * the start date's. If a month doesn't have that day, no event will be created for that
         * month. (For example if start date is January 31, there won't be an event in February,
         * but there will be one in March).
         */
        SAME_DAY_OF_MONTH,

        /**
         * Events will happen on the same day of the same week of each month. For example, a
         * recurrence might happen on the third Wednesday of every month. The day of the week
         * and the week number is the same as the start date's. Every month has at least four
         * weeks, and if the events happens in the fifth week, it will be formatted as the "last"
         * week of the month.
         */
        SAME_DAY_OF_WEEK,

        /**
         * Events will happen on the last day of each month.
         * The start date must be on the last day of its month.
         */
        LAST_DAY_OF_MONTH
    }


    enum class EndType {
        /** Recurrence will never end. */
        NEVER,

        /** Recurrence will end on a date. */
        BY_DATE,

        /** Recurrence will end after a number of events. */
        BY_COUNT
    }


    companion object {
        // Bit flags for weekly days bit field
        const val SUNDAY = 1 shl Calendar.SUNDAY
        const val MONDAY = 1 shl Calendar.MONDAY
        const val TUESDAY = 1 shl Calendar.TUESDAY
        const val WEDNESDAY = 1 shl Calendar.WEDNESDAY
        const val THURSDAY = 1 shl Calendar.THURSDAY
        const val FRIDAY = 1 shl Calendar.FRIDAY
        const val SATURDAY = 1 shl Calendar.SATURDAY

        const val EVERY_DAY_OF_WEEK = 0b01111111

        /** Date value used for no end date. */
        const val DATE_NONE = Long.MIN_VALUE

        /** Calendar used for checks when creating a recurrence. */
        private val calendar = Calendar.getInstance()


        /**
         * Inline factory function to create a [Recurrence] without directly using the builder.
         */
        inline operator fun invoke(startDate: Long, period: Period, build: Builder.() -> Unit = {}): Recurrence {
            val builder = Builder(startDate, period)
            build(builder)
            return builder.build()
        }

        /**
         * Inline factory function to create a modified copy of a [Recurrence] without directly using the builder.
         */
        inline operator fun invoke(recurrence: Recurrence, build: Builder.() -> Unit = {}): Recurrence {
            val builder = Builder(recurrence)
            build(builder)
            return builder.build()
        }

        /**
         * Compare [this] date with another [date], ignoring time of the day.
         * Returns `-1` if [this] is on a day before [date].
         * Returns `0` if [this] is on same day as [date].
         * Returns `1` if [this] is on a day after [date].
         */
        internal fun Long.compareDay(date: Long): Int {
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

        private fun getDaysInDate(date: Long): Int {
            calendar.timeInMillis = date
            return calendar[Calendar.YEAR] * 366 + calendar[Calendar.DAY_OF_YEAR]
        }
    }

}
