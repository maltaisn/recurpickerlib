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

@file:Suppress("MagicNumber")

package com.maltaisn.recurpicker

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.core.os.BundleCompat
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.RecurrencePickerSettings.Builder
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A class for configuring the behavior of the recurrence list and picker dialogs.
 * The class is immutable, use the [Builder] to create it.
 */
public class RecurrencePickerSettings private constructor(
    /**
     * The recurrence formatter used to format the recurrences shown in the recurrence list dialog.
     * The date format is also used to format the end dates in the recurrence picker.
     */
    public val formatter: RecurrenceFormatter,

    /**
     * List of recurrence presets displayed in the recurrence list dialog.
     * A `null` recurrence will be replaced by a "Custom..." item to customize recurrence.
     * Hence, to disable the custom recurrence picker, set no `null` items.
     * The default is a list of recurrences equivalent to "Does not repeat", "Daily",
     * "Weekly", "Monthly", "Yearly", null ("Custom...").
     */
    public val presets: List<Recurrence?>,

    /**
     * The recurrence shown by default in the recurrence picker.
     * This is shown when no recurrence or a "Does not repeat" recurrence is selected.
     */
    public val defaultPickerRecurrence: Recurrence,

    /** The maximum frequency that can be entered in the picker. Must be at least 1. */
    public val maxFrequency: Int,

    /** The maximum end count that can be entered in the picker. Must be at least 1. */
    public val maxEndCount: Int
) : Parcelable {

    public class Builder {

        /** @see RecurrencePickerSettings.formatter */
        @set:JvmSynthetic
        public var formatter: RecurrenceFormatter = RecurrenceFormatter(DateFormat.getDateInstance())

        /** @see RecurrencePickerSettings.presets */
        @set:JvmSynthetic
        public var presets: List<Recurrence?> = mutableListOf(
            Recurrence.DOES_NOT_REPEAT,
            Recurrence(Period.DAILY),
            Recurrence(Period.WEEKLY),
            Recurrence(Period.MONTHLY),
            Recurrence(Period.YEARLY),
            null)

        /** @see RecurrencePickerSettings.defaultPickerRecurrence */
        @set:JvmSynthetic
        public var defaultPickerRecurrence: Recurrence = Recurrence(Period.DAILY)

        /** @see RecurrencePickerSettings.maxFrequency */
        @set:JvmSynthetic
        public var maxFrequency: Int = 99
            set(value) {
                require(value >= 1) { "Max frequency must be at least 1." }
                field = value
            }

        /** @see RecurrencePickerSettings.maxEndCount */
        @set:JvmSynthetic
        public var maxEndCount: Int = 999
            set(value) {
                require(value >= 1) { "Max end count must be at least 1." }
                field = value
            }

        public fun setFormatter(formatter: RecurrenceFormatter): Builder = apply { this.formatter = formatter }
        public fun setPresets(presets: List<Recurrence?>): Builder = apply { this.presets = presets }
        public fun setDefaultPickerRecurrence(recurrence: Recurrence): Builder =
            apply { defaultPickerRecurrence = recurrence }

        public fun setMaxFrequency(frequency: Int): Builder = apply { maxFrequency = frequency }
        public fun setMaxEndCount(count: Int): Builder = apply { maxEndCount = count }

        public fun build(): RecurrencePickerSettings = RecurrencePickerSettings(formatter, presets,
            defaultPickerRecurrence, maxFrequency, maxEndCount)
    }

    // Parcelable stuff
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val bundle = Bundle()
        bundle.putRecurrenceFormatter(formatter)
        bundle.putParcelableArrayList("presets", ArrayList(presets))
        bundle.putParcelable("defaultPickerRecurrence", defaultPickerRecurrence)
        bundle.putInt("maxFrequency", maxFrequency)
        bundle.putInt("maxEndCount", maxEndCount)
        parcel.writeBundle(bundle)
    }

    override fun describeContents(): Int = 0

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<RecurrencePickerSettings> =
            object : Parcelable.Creator<RecurrencePickerSettings> {
                override fun createFromParcel(parcel: Parcel) = RecurrencePickerSettings {
                    val bundle = parcel.readBundle(RecurrencePickerSettings::class.java.classLoader)!!
                    formatter = bundle.getRecurrenceFormatter()
                    presets = BundleCompat.getParcelableArrayList(bundle, "presets", Recurrence::class.java)!!
                    defaultPickerRecurrence = BundleCompat.getParcelable(
                        bundle, "defaultPickerRecurrence", Recurrence::class.java)!!
                    maxFrequency = bundle.getInt("maxFrequency")
                    maxEndCount = bundle.getInt("maxEndCount")
                }

                override fun newArray(size: Int) = arrayOfNulls<RecurrencePickerSettings>(size)
            }

        /**
         * Utility function to create settings using constructor-like syntax.
         */
        public inline operator fun invoke(init: Builder.() -> Unit = {}): RecurrencePickerSettings =
            Builder().apply(init).build()

        private fun Bundle.getRecurrenceFormatter(): RecurrenceFormatter {
            val dateFormat = try {
                this.getSerializableCompat<DateFormat>("dateFormat")!!
            } catch (e: Exception) {
                // Very rarely and on API >= 28, Bundle will fail to get serialized DateFormat.
                // This issue is related to: https://stackoverflow.com/a/54155356/5288316.
                // Luckily, DateFormat is most often a SimpleDateFormat, which can be saved
                // using a string. Note that the naming of the key is important here. I had
                // initially named the key "dateFormatPattern", which led getString to fail
                // like getSerializable did!
                if (this.containsKey("dfPattern")) {
                    SimpleDateFormat(this.getString("dfPattern", ""), Locale.getDefault())
                } else {
                    // Ok, date format was lost for good. Use default.
                    Log.e(TAG, "RecurrencePickerSettings formatter's date format lost during unparcelization.")
                    DateFormat.getDateInstance()
                }
            }
            return RecurrenceFormatter(dateFormat)
        }

        private fun Bundle.putRecurrenceFormatter(formatter: RecurrenceFormatter) {
            val dateFormat = formatter.dateFormat
            this.putSerializable("dateFormat", dateFormat)
            if (dateFormat is SimpleDateFormat) {
                this.putString("dfPattern", dateFormat.toPattern())
            }
        }

        private val TAG = RecurrencePickerSettings::class.java.simpleName
    }
}
