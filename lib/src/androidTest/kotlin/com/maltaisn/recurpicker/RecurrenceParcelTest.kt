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
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertEquals


@RunWith(AndroidJUnit4::class)
internal class RecurrenceParcelTest {

    @Test
    fun parcelTest() {
        testParcel(Recurrence(Period.DAILY) {
            frequency = 5
            endDate = GregorianCalendar(2019, Calendar.JANUARY, 7).timeInMillis
        })
        testParcel(Recurrence(Period.MONTHLY) {
            byMonthDay = 12
            endCount = 5
        })
        testParcel(Recurrence(Period.YEARLY) {
            frequency = 10
        })
    }

    private fun testParcel(r: Recurrence) {
        // Write
        val parcel = Parcel.obtain()
        r.writeToParcel(parcel, r.describeContents())
        parcel.setDataPosition(0)

        // Read
        val r2 = Recurrence.CREATOR.createFromParcel(parcel)
        assertEquals(r, r2)
    }


}
