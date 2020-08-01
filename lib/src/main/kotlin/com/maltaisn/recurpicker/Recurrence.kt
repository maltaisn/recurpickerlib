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

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import com.maltaisn.recurpicker.Recurrence.Companion.DATE_NONE
import com.maltaisn.recurpicker.Recurrence.DaysOfWeek
import com.maltaisn.recurpicker.Recurrence.EndType
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.Recurrence.Period.DAILY
import com.maltaisn.recurpicker.Recurrence.Period.MONTHLY
import com.maltaisn.recurpicker.Recurrence.Period.NONE
import com.maltaisn.recurpicker.Recurrence.Period.WEEKLY
import com.maltaisn.recurpicker.Recurrence.Period.YEARLY
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * An object describing a recurrence rule.
 * All recurrence objects are immutable.
 *
 * @property period The time period over which the recurrence happens.
 *
 * @property frequency The recurrence frequency i.e after how many periods to repeat the event.
 * For example, if period is daily and frequency is 2, events will happen every 2 days.
 * Frequency of [Period.NONE] recurrences is always 1.
 *
 * @property byDay Least-significant bit is `1` if the value is used, `0` otherwise.
 *
 * If period is [WEEKLY], this value is a bit field of the days of the week on which
 * a weekly recurrence occurs. Bit field valid flags are the [DaysOfWeek] constants.
 * If value is `1` (i.e no flags set), events will happen on the same day of the week as start date's.
 *
 * If period is [MONTHLY], the first byte is a bit field with a single [DaysOfWeek] flag set
 * indicating on which day of the week the events happen. The second byte is a number
 * indicating on which week of the month that the events happen, to which 4 is added.
 * Negative values start counting from the end of the month.
 *
 * `week in month = (byDay >>> 8) - 4`: between -4 and 4.
 *
 * @property byMonthDay If period is [MONTHLY], the day of the month on which the events happen.
 * The number must be between -31 and 31. If the value is `0` (the default),
 * the events will happen on the same day of the month as the start date's.
 * The value can also be `0` if [byDay] determines the day of the month instead.
 * Negative numbers start counting from the end of the month.
 *
 * @property endType The rule for the recurrence end, see [EndType].
 *
 * @property endDate The end date if end type is [EndType.BY_DATE]. If not, end date is
 * always [DATE_NONE]. The end date is inclusive so the last event might be on the end date.
 *
 * @property endCount The number of events before the end of the recurrence if end type is
 * [EndType.BY_COUNT]. If not, end count is always `0`. Since the start date is exclusive,
 * the number of events never includes the start date event.
 */
class Recurrence private constructor(
    val period: Period,
    val frequency: Int,
    val byDay: Int,
    val byMonthDay: Int,
    val endType: EndType,
    val endDate: Long,
    val endCount: Int,
    private val calendar: Calendar
) : Parcelable {

    /**
     * If period is [MONTHLY], get the week of the month on which the events happen.
     * Returns `0` if undefined or events happen on a particular day of the month instead.
     */
    val weekInMonth: Int
        get() {
            check(period == MONTHLY) { "Week in month is a monthly recurrence property." }
            return (byDay ushr Byte.SIZE_BITS) - MAX_WEEKS_IN_MONTH
        }

    /**
     * If period is [MONTHLY], get the day of the week on which the events happen.
     * Returns `0` if undefined or events happen on a particular day of the month instead.
     * Returns a `Calendar.SUNDAY-SATURDAY` constant otherwise.
     */
    val dayOfWeekInMonth: Int
        get() {
            check(period == MONTHLY) { "Day of week in month is a monthly recurrence property." }
            for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                if (isRecurringOnDaysOfWeek(1 shl day)) {
                    return day
                }
            }
            return 0
        }

    /**
     * If period is [WEEKLY], checks if events happen on certain [days] of the week.
     * Returns true only if recurrence happens on all of these days.
     * To get the number of days set use `Integer.bitCount(recurrence.byDay)  - 1`.
     * If period is [MONTHLY], checks if events happen on a certain day of the week specified
     * by a single flag set in [days].
     * @param days A bit field of [DaysOfWeek] values.
     */
    fun isRecurringOnDaysOfWeek(@DaysOfWeek days: Int): Boolean = ((byDay and days) == days)

    /**
     * Note: since two recurrences with dates at different times of the day will produce the same
     * events, they are considered equal, even though the dates may not have the same value.
     * Same applies for [hashCode].
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Recurrence) return false
        return period == other.period &&
                frequency == other.frequency &&
                byDay == other.byDay &&
                byMonthDay == other.byMonthDay &&
                endType == other.endType &&
                endCount == other.endCount &&
                getDaysInDate(endDate, calendar) == getDaysInDate(other.endDate, calendar)
    }

    override fun hashCode(): Int = arrayOf(period, frequency, byDay, byMonthDay,
        endType, getDaysInDate(endDate, calendar), endCount).contentHashCode()

    /**
     * Return a human readable string representation of the recurrence.
     * This is only for debug purposes and will not even work on release builds.
     * [RecurrenceFormatter] should be used instead.
     */
    override fun toString() = if (BuildConfig.DEBUG) {
        val sb = StringBuilder()
        sb.append("Recurrence{ ")

        when (period) {
            NONE -> sb.append("Does not repeat")
            DAILY -> appendPeriodDetails(sb, "day")
            WEEKLY -> appendWeeklyDetails(sb)
            MONTHLY -> appendMonthlyDetails(sb)
            YEARLY -> appendPeriodDetails(sb, "year")
        }

        appendEndTypeDetails(sb)

        sb.append(" }")
        sb.toString()
    } else {
        super.toString()
    }

    private fun appendPeriodDetails(sb: StringBuilder, name: String) {
        sb.append("Every ")
        sb.append(toStringPlural(name, frequency, false))
    }

    private fun appendWeeklyDetails(sb: StringBuilder) {
        appendPeriodDetails(sb, "week")

        // Append a list of days of week
        sb.append(" on ")
        if (byDay == EVERY_DAY_OF_WEEK) {
            // on every day of the week
            sb.append("every day of the week")
        } else {
            // on [Sun, Mon, Wed, ...]
            val dfs = DateFormatSymbols.getInstance(Locale.ENGLISH)
            for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                if (isRecurringOnDaysOfWeek(1 shl day)) {
                    sb.append(dfs.shortWeekdays[day])
                    sb.append(", ")
                }
            }
            sb.delete(sb.length - 2, sb.length) // Remove extra separator
        }
    }

    private fun appendMonthlyDetails(sb: StringBuilder) {
        appendPeriodDetails(sb, "month")

        // Append additional monthly setting
        sb.append(" (on ")
        sb.append(when {
            byDay != 0 -> {
                val dfs = DateFormatSymbols.getInstance(Locale.ENGLISH)
                val ordinals = arrayOf("first", "second", "third", "fourth")
                dfs.weekdays[dayOfWeekInMonth] + " of the " + when {
                    weekInMonth == -1 -> "last"
                    weekInMonth < 0 -> ordinals[abs(weekInMonth) - 1] + " to last"
                    else -> ordinals[weekInMonth - 1]
                } + " week"
            }
            byMonthDay == 0 -> {
                "the same day each month"
            }
            byMonthDay == -1 -> {
                "the last day of the month"
            }
            byMonthDay < 0 -> {
                "${-byMonthDay} days before the end of the month"
            }
            else -> {
                "the $byMonthDay of each month"
            }
        })
        sb.append(')')
    }

    private fun appendEndTypeDetails(sb: StringBuilder) {
        if (endType != EndType.NEVER) {
            sb.append("; ")
            if (endType == EndType.BY_DATE) {
                val df = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
                sb.append("until ")
                sb.append(df.format(endDate))
            } else {
                sb.append("for ")
                sb.append(toStringPlural("event", endCount, true))
            }
        }
    }

    private fun toStringPlural(text: String, quantity: Int, alwaysIncludeQuantity: Boolean): String {
        return if (quantity <= 1) {
            (if (alwaysIncludeQuantity) "$quantity " else "") + text
        } else {
            quantity.toString() + " " + text + "s"
        }
    }

    /**
     * Builder for recurrence.
     */
    class Builder {

        private val calendar = Calendar.getInstance()

        /** @see Recurrence.frequency */
        var period: Period = NONE
            private set

        /**
         * Frequency must be at least 1.
         * @see Recurrence.frequency
         */
        @set:JvmSynthetic
        var frequency: Int = 1
            set(value) {
                require(value >= 1) { "Frequency must be 1 or greater." }
                field = value
            }

        /**
         * Don't set this directly! Use [setDaysOfWeek] and [setDayOfWeekInMonth].
         * @see Recurrence.byDay
         */
        internal var byDay: Int = 0

        /**
         * Don't set this directly! Use [dayInMonth].
         * @see Recurrence.byMonthDay
         */
        internal var byMonthDay: Int = 0

        /**
         * This field doesn't have to be set directly most of the time,
         * since setting the end date or end count will set it automatically.
         * @see Recurrence.endType
         */
        @Suppress("RedundantSetter")
        @set:JvmSynthetic
        var endType: EndType = EndType.NEVER
            set(value) {
                field = value // Default setter needed to avoid collision when using JvmSynthetic?
            }

        /**
         * Setting this value will changing end type to by date.
         * If end date is [DATE_NONE], recurrence end type will be set to never.
         * @see Recurrence.endDate
         */
        @set:JvmSynthetic
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
        @set:JvmSynthetic
        var endCount: Int = 0
            set(value) {
                endType = EndType.BY_COUNT
                field = value
            }

        /**
         * Create a builder initialized for a recurrence with a [period].
         */
        constructor(period: Period) {
            this.period = period
            if (period == WEEKLY) {
                byDay = 1
            }
        }

        /**
         * Create a builder initialized with the values of another [recurrence].
         */
        constructor(recurrence: Recurrence) : this(recurrence.period) {
            frequency = recurrence.frequency
            byDay = recurrence.byDay
            byMonthDay = recurrence.byMonthDay
            endDate = recurrence.endDate
            endCount = recurrence.endCount
            endType = recurrence.endType
        }

        fun setFrequency(frequency: Int) = apply { this.frequency = frequency }
        fun setEndType(endType: EndType) = apply { this.endType = endType }
        fun setEndDate(endDate: Long) = apply { this.endDate = endDate }
        fun setEndCount(endCount: Int) = apply { this.endCount = endCount }

        /**
         * If period is [WEEKLY], set [byDay] field to a list of [days] of the week.
         */
        fun setDaysOfWeek(@DaysOfWeek vararg days: Int) = apply {
            check(period == WEEKLY) { "Period must be weekly to set the list of days of the week." }
            byDay = 0x01
            @SuppressLint("WrongConstant")
            for (day in days) {
                require(day in 0..EVERY_DAY_OF_WEEK) { "Day of the week flag is invalid." }
                byDay = byDay or day
            }
        }

        /**
         * If period is [MONTHLY], set the day in the month on which events happen.
         * @see Recurrence.byMonthDay
         */
        @set:JvmSynthetic
        var dayInMonth: Int
            get() = byMonthDay
            set(value) {
                check(period == MONTHLY) { "Period must be monthly to set the day in month." }
                require(value.absoluteValue <= MAX_DAYS_IN_MONTH) { "Day in month must be between -31 and 31." }
                byDay = 0
                byMonthDay = value
            }

        fun setDayInMonth(dayInMonth: Int) = apply { this.dayInMonth = dayInMonth }

        /**
         * If period is [MONTHLY], set [byDay] setting so that the recurrence
         * happens on a [day] of a week in the month. If both parameters are `0`,
         * the week number and day of the week will be the same as start date's.
         * @param day Day of the week, use [DaysOfWeek] constants.
         * @param week On which week of the month the events will happen, starting from `1`.
         * From -4 to 4, negative values count the week number from the end of the month.
         */
        fun setDayOfWeekInMonth(day: Int, week: Int) = apply {
            check(period == MONTHLY) { "Period must be monthly to set the day of week in month." }
            require(Integer.bitCount(day) == 1 && day in SUNDAY..SATURDAY) { "Day of the week flag is invalid." }
            require(week.absoluteValue <= MAX_WEEKS_IN_MONTH && week != 0) { "Week of the month is invalid." }
            byDay = 0x01 or day or ((week + MAX_WEEKS_IN_MONTH) shl Byte.SIZE_BITS)
            byMonthDay = 0
        }

        /**
         * Build the recurrence object described by the builder.
         * This validates and normalizes all field values.
         */
        fun build(): Recurrence {
            if (period == WEEKLY && byDay == EVERY_DAY_OF_WEEK && frequency == 1) {
                // Recurring every week on every day of the week so make it daily.
                period = DAILY
                byDay = 0
            }

            if (period == NONE || endType == EndType.BY_DATE && endDate == DATE_NONE) {
                // Does not repeat or end by date set but no end date set.
                endType = EndType.NEVER
            } else if (endType == EndType.BY_COUNT && endCount < 1) {
                // Recurrence ends after less than one event so make it non-recurring.
                period = NONE
                endType = EndType.NEVER
            }

            if (period == NONE) {
                // All recurrence with NONE period are the same.
                return DOES_NOT_REPEAT
            }

            // Set unused fields to default values so equals work correctly.
            val endDate = if (endType == EndType.BY_DATE) endDate else DATE_NONE
            val endCount = if (endType == EndType.BY_COUNT) endCount else 0

            return Recurrence(period, frequency, byDay, byMonthDay, endType, endDate, endCount, calendar)
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
     * Int def for the [byDay] bit field property.
     */
    @IntDef(flag = true, value = [SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY])
    @Retention(AnnotationRetention.SOURCE)
    annotation class DaysOfWeek

    enum class EndType {
        /** Recurrence will never end. */
        NEVER,

        /** Recurrence will end on a date. */
        BY_DATE,

        /** Recurrence will end after a number of events. */
        BY_COUNT
    }

    companion object {
        // Bit flags for days of the week bit field
        const val SUNDAY = 1 shl Calendar.SUNDAY
        const val MONDAY = 1 shl Calendar.MONDAY
        const val TUESDAY = 1 shl Calendar.TUESDAY
        const val WEDNESDAY = 1 shl Calendar.WEDNESDAY
        const val THURSDAY = 1 shl Calendar.THURSDAY
        const val FRIDAY = 1 shl Calendar.FRIDAY
        const val SATURDAY = 1 shl Calendar.SATURDAY

        const val EVERY_DAY_OF_WEEK = 0b11111111 // LSB is 1 to follow byDay layout so it can be used for comparison.

        /** Date value used for no end date. */
        const val DATE_NONE = Long.MIN_VALUE

        /**
         * A recurrence that doesn't repeat. When finding events for this recurrence,
         * only the start date event will be returned.
         */
        @JvmField
        val DOES_NOT_REPEAT = Recurrence(NONE, 1, 0, 0,
            EndType.NEVER, DATE_NONE, 0, Calendar.getInstance())

        private const val MAX_DAYS_IN_YEAR = 366
        private const val MAX_DAYS_IN_MONTH = 31
        private const val MAX_WEEKS_IN_MONTH = 4

        /**
         * Inline factory function to create a [Recurrence] without directly using the builder.
         */
        inline operator fun invoke(period: Period, build: Builder.() -> Unit = {}): Recurrence {
            val builder = Builder(period)
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

        private fun getDaysInDate(date: Long, calendar: Calendar): Int {
            if (date == DATE_NONE) return 0
            calendar.timeInMillis = date
            return calendar[Calendar.YEAR] * MAX_DAYS_IN_YEAR + calendar[Calendar.DAY_OF_YEAR]
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<Recurrence> {
            override fun createFromParcel(parcel: Parcel) = Recurrence(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Recurrence>(size)
        }
    }

    // Parcelable stuff
    private constructor(parcel: Parcel) : this(
        parcel.readSerializable() as Period,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readSerializable() as EndType,
        parcel.readLong(),
        parcel.readInt(),
        Calendar.getInstance())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeSerializable(period)
            writeInt(frequency)
            writeInt(byDay)
            writeInt(byMonthDay)
            writeSerializable(endType)
            writeLong(endDate)
            writeInt(endCount)
        }
    }

    override fun describeContents() = 0
}
