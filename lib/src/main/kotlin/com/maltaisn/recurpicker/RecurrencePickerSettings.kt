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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.RecurrencePickerSettings.Builder
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import java.text.DateFormat
import java.util.*


/**
 * A class for configuring the behavior of the recurrence list and picker dialogs.
 * The class is immutable, use the [Builder] to create it.
 */
class RecurrencePickerSettings private constructor(
        /**
         * The recurrence formatter used to format the recurrences shown in the recurrence list dialog.
         * The date format is also used to format the end dates in the recurrence picker.
         */
        val formatter: RecurrenceFormatter,

        /**
         * The list of recurrence presets shown in the recurrence list dialog.
         * A `null` recurrence will result in the "Custom..." item being shown.
         */
        val presets: List<Recurrence?>
) : Parcelable {

    class Builder {

        /** @see RecurrencePickerSettings.formatter */
        var formatter: RecurrenceFormatter = RecurrenceFormatter(DateFormat.getDateInstance())

        /** @see RecurrencePickerSettings.presets */
        var presets: List<Recurrence?> = arrayListOf(
                Recurrence(Recurrence.DATE_NONE, Period.NONE) { isDefault = true },
                Recurrence(Recurrence.DATE_NONE, Period.DAILY) { isDefault = true },
                Recurrence(Recurrence.DATE_NONE, Period.WEEKLY) { isDefault = true },
                Recurrence(Recurrence.DATE_NONE, Period.MONTHLY) { isDefault = true },
                Recurrence(Recurrence.DATE_NONE, Period.YEARLY) { isDefault = true },
                null)

        fun build() = RecurrencePickerSettings(formatter, presets)
    }

    // Parcelable stuff
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val bundle = Bundle()
        bundle.putSerializable("dateFormat", formatter.dateFormat)
        bundle.putParcelableArrayList("presets", ArrayList(presets))
        parcel.writeBundle(bundle)
    }

    override fun describeContents() = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<RecurrencePickerSettings> {
            override fun createFromParcel(parcel: Parcel) = RecurrencePickerSettings {
                val bundle = parcel.readBundle(RecurrencePickerSettings::class.java.classLoader)!!
                formatter = RecurrenceFormatter(bundle.getSerializable("dateFormat") as DateFormat)
                presets = bundle.getParcelableArrayList("presets")!!
            }

            override fun newArray(size: Int) = arrayOfNulls<RecurrencePickerSettings>(size)
        }

        /**
         * Utility function to create settings using constructor-like syntax.
         */
        inline operator fun invoke(init: Builder.() -> Unit = {}) = Builder().apply(init).build()
    }

}
