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

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class RecurrencePickerSettingsParcelTest {

    @Test
    fun parcelTest() {
        val settings = RecurrencePickerSettings {
            formatter = RecurrenceFormatter(DateFormat.getDateInstance())
            presets = listOf(Recurrence.DOES_NOT_REPEAT, Recurrence(Period.DAILY), null)
            defaultPickerRecurrence = Recurrence(Period.DAILY)
            maxEndCount = 13
            maxFrequency = 27
        }

        // Write
        val parcel = Parcel.obtain()
        settings.writeToParcel(parcel, settings.describeContents())
        parcel.setDataPosition(0)

        // Read
        val settings2 = RecurrencePickerSettings.CREATOR.createFromParcel(parcel)

        val date = GregorianCalendar(2019, Calendar.MARCH, 9).timeInMillis
        assertEquals(settings.formatter.dateFormat.format(date), settings2.formatter.dateFormat.format(date))
        assertEquals(settings.presets, settings2.presets)
        assertEquals(settings.defaultPickerRecurrence, settings2.defaultPickerRecurrence)
        assertEquals(settings.maxEndCount, settings2.maxEndCount)
        assertEquals(settings.maxFrequency, settings2.maxFrequency)
    }
}
