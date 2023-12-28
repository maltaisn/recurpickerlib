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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    fun `should return zero item count when detached`() {
        assertEquals(0, presenter.itemCount)
    }

    @Test
    fun `should return item count equal to number of presets`() {
        presenter.attach(view, null)

        assertEquals(settings.presets.size, presenter.itemCount)
    }

    @Test
    fun `should return item count equal to number of presets plus one when there's a selection`() {
        whenever(view.selectedRecurrence).thenReturn(Recurrence(Recurrence.Period.DAILY) {
            frequency = 3
        })

        presenter.attach(view, null)

        assertEquals(settings.presets.size + 1, presenter.itemCount)
    }

    @Test
    fun `should bind items`() {
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
    fun `should set result when item is clicked`() {
        presenter.attach(view, null)
        presenter.onItemClicked(0)

        verify(view).setRecurrenceResult(settings.presets[0]!!)
        verify(view).exit()
    }

    @Test
    fun `should set custom result when customize item is clicked`() {
        presenter.attach(view, null)
        presenter.onItemClicked(2)

        verify(view).setCustomResult()
        verify(view).exit()
    }

    @Test
    fun `should set cancel result when cancelled`() {
        presenter.attach(view, null)
        presenter.onCancel()

        verify(view).setCancelResult()
        verify(view).exit()
    }
}
