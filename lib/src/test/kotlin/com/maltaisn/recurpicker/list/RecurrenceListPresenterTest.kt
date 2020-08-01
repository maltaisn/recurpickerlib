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

package com.maltaisn.recurpicker.list

import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
internal class RecurrenceListPresenterTest {

    private val view: RecurrenceListContract.View = mock()
    private val itemView: RecurrenceListContract.ItemView = mock()

    private val presenter = RecurrenceListPresenter()
    private val settings = RecurrencePickerSettings {
        presets = listOf(
            Recurrence(Recurrence.Period.NONE),
            Recurrence(Recurrence.Period.DAILY),
            null)
    }

    @Before
    fun setUp() {
        whenever(view.settings).thenReturn(settings)
    }

    @Test
    fun getItemCount_detached() {
        assertEquals(0, presenter.itemCount)
    }

    @Test
    fun getItemCount_noSelection() {
        presenter.attach(view, null)

        assertEquals(settings.presets.size, presenter.itemCount)
    }

    @Test
    fun getItemCount_withSelection() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Recurrence.Period.DAILY) {
            frequency = 3
        })

        presenter.attach(view, null)

        assertEquals(settings.presets.size + 1, presenter.itemCount)
    }

    @Test
    fun bindItems() {
        val selected = Recurrence(Recurrence.Period.DAILY) {
            frequency = 3
        }
        whenever(view.selectedRecurrence).thenReturn(selected)
        whenever(view.startDate).thenReturn(Recurrence.DATE_NONE)

        presenter.attach(view, null)

        presenter.onBindItemView(itemView, 3)
        verify(itemView).bindCustomView()
        verify(itemView, never()).bindRecurrenceView(any(), any(), anyLong(), anyBoolean())

        presenter.onBindItemView(itemView, 0)
        verify(itemView).bindRecurrenceView(settings.formatter, selected, Recurrence.DATE_NONE, true)

        presenter.onBindItemView(itemView, 1)
        verify(itemView).bindRecurrenceView(settings.formatter, settings.presets[0]!!, Recurrence.DATE_NONE, false)

        presenter.onBindItemView(itemView, 2)
        verify(itemView).bindRecurrenceView(settings.formatter, settings.presets[1]!!, Recurrence.DATE_NONE, false)
    }

    @Test
    fun itemClick_recurrence() {
        presenter.attach(view, null)
        presenter.onItemClicked(0)

        verify(view).setRecurrenceResult(settings.presets[0]!!)
        verify(view).exit()
    }

    @Test
    fun itemClick_custom() {
        presenter.attach(view, null)
        presenter.onItemClicked(2)

        verify(view).setCustomResult()
        verify(view).exit()
    }

    @Test
    fun cancel() {
        presenter.attach(view, null)
        presenter.onCancel()

        verify(view).setCancelResult()
        verify(view).exit()
    }
}
