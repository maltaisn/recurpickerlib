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
import com.maltaisn.recurpicker.Recurrence.MonthlyDay
import com.maltaisn.recurpicker.Recurrence.Period
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import kotlin.test.assertEquals


@RunWith(AndroidJUnit4::class)
internal class RecurrenceParcelTest {

    @Test
    fun parcelTest() {
        testParcel(Recurrence(dateFor("2018-01-01"), Period.DAILY) {
            frequency = 5
            endDate = dateFor("2020-01-01")
        })
        testParcel(Recurrence(dateFor("2018-01-01"), Period.MONTHLY) {
            monthlyDay = MonthlyDay.SAME_DAY_OF_WEEK
            endCount = 30
        })
        testParcel(Recurrence(dateFor("2020-01-01"), Period.YEARLY) { isDefault = true })
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

    private fun dateFor(date: String) = SimpleDateFormat("yyyy-MM-dd").parse(date)!!.time

}
