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

package com.maltaisn.recurpicker.demo

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.RecurrenceFinder
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.demo.databinding.FragmentMainBinding
import com.maltaisn.recurpicker.demo.databinding.ItemEventBinding
import com.maltaisn.recurpicker.list.RecurrenceListCallback
import com.maltaisn.recurpicker.list.RecurrenceListDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerCallback
import com.maltaisn.recurpicker.picker.RecurrencePickerDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerFragment
import java.text.DateFormat

/**
 * The main fragment.
 *
 * Child fragments use [Fragment.getParentFragment] to callback to this fragment.
 * This way, callbacks are retained across configuration changes without memory leaks.
 */
class MainFragment : Fragment(), DateDialogFragment.Callback,
    RecurrenceListCallback, RecurrencePickerCallback {

    // Recurrence list and picker fragments
    private val listDialog by lazy { RecurrenceListDialog.newInstance(settings) }
    private val pickerFragment by lazy { RecurrencePickerFragment.newInstance(settings) }
    private val pickerDialog by lazy { RecurrencePickerDialog.newInstance(settings) }

    // View bindings
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!.contentLayout
    private val selectedBinding get() = binding.selectedCard
    private val optionsBinding get() = binding.optionsCard

    private var eventsAdapter: EventsAdapter? = null

    // Recurrence presets used in the recurrence list dialog.
    private val recurrencePresets = mutableListOf(
        Recurrence.DOES_NOT_REPEAT,
        Recurrence(Period.DAILY),
        Recurrence(Period.WEEKLY),
        Recurrence(Period.MONTHLY),
        Recurrence(Period.YEARLY),
        null)

    // Settings used by both recurrence fragments.
    private val settings = RecurrencePickerSettings {
        // Set the presets list.
        presets = recurrencePresets
    }

    // Used to find the list of recurrence events
    private val recurrenceFinder = RecurrenceFinder()

    // Date format used to display recurrence events date
    private val eventDateFormat = DateFormat.getDateInstance(DateFormat.FULL)

    // The start date set by user in millis.
    private var startDate = System.currentTimeMillis()

    // The currently selected recurrence.
    private var selectedRecurrence = Recurrence.DOES_NOT_REPEAT

    // The list of recurrence events found.
    private var recurrenceEvents = mutableListOf<Long>()

    // Whether the list of events is complete or more events could be found.
    private var isDoneFinding = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        _binding = FragmentMainBinding.bind(view) // Note: inflate doesn't work here?
        return view
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Fixes bug with MaterialCardView and Canvas.clipPath() on API < 18.
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        // Set up options views
        val startDateInput = optionsBinding.startDateInput
        val startDateDialog by lazy { DateDialogFragment() }
        startDateInput.setOnClickListener {
            startDateDialog.date = startDate
            startDateDialog.show(childFragmentManager, "start_date_dialog")
        }

        val enableListCheck = optionsBinding.enableListCheck
        val enablePickerCheck = optionsBinding.enablePickerCheck
        enableListCheck.setOnCheckedChangeListener { _, isChecked ->
            enablePickerCheck.isChecked = enablePickerCheck.isChecked || !isChecked
        }
        enablePickerCheck.setOnCheckedChangeListener { _, isChecked ->
            enableListCheck.isChecked = enableListCheck.isChecked || !isChecked
            optionsBinding.usePickerDialogCheck.isEnabled = isChecked
        }

        optionsBinding.enableDarkThemeCheck.setOnCheckedChangeListener { _, isChecked ->
            // Enable or disable dark theme without restarting the app.
            AppCompatDelegate.setDefaultNightMode(if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            })
        }

        // Set up selected recurrence views
        selectedBinding.selectedForeground.setOnClickListener { chooseRecurrence() }

        // Set up events list views
        val eventsRcv = binding.eventsView.eventsRcv

        val layoutManager = LinearLayoutManager(requireContext())
        eventsRcv.layoutManager = layoutManager

        eventsAdapter = EventsAdapter()
        eventsRcv.adapter = eventsAdapter
        eventsRcv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isDoneFinding && layoutManager.findLastCompletelyVisibleItemPosition() >
                    recurrenceEvents.size - LOAD_MORE_TRIGGER
                ) {
                    // When almost scrolled to the end of the list, find more events.
                    findMoreRecurrenceEvents()
                }
            }
        })

        if (state != null) {
            // Restore saved state
            startDate = state.getLong("startDate")
            selectedRecurrence = state.getParcelable("selectedRecurrence")!!
            recurrenceEvents = state.getLongArray("recurrenceEvents")!!.toMutableList()
            isDoneFinding = state.getBoolean("isDoneFinding")
        } else {
            // Find initial events
            findMoreRecurrenceEvents()
        }

        // Set initial views state
        updateStartDateInput()
        updateSelectedLabel()
        updateRecurrenceCount()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)

        // Save state
        state.putLong("startDate", startDate)
        state.putParcelable("selectedRecurrence", selectedRecurrence)
        state.putLongArray("recurrenceEvents", recurrenceEvents.toLongArray())
        state.putBoolean("isDoneFinding", isDoneFinding)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        eventsAdapter = null
    }

    override fun onDateDialogConfirmed(date: Long) {
        // Start date has been selected. Update it and its view.
        startDate = date
        updateStartDateInput()

        // Reset the events list since changing start date changes event dates.
        resetRecurrenceEvents()
    }

    override fun onRecurrenceCustomClicked() {
        // A "Custom..." was clicked in list dialog.
        // Show picker fragment to customize the recurrence.
        showPickerFragment()
    }

    override fun onRecurrencePresetSelected(recurrence: Recurrence) {
        // A recurrence preset was selected in the list dialog.
        // Update the selected recurrence and reset events.
        selectedRecurrence = recurrence
        updateSelectedLabel()
        resetRecurrenceEvents()
    }

    override fun onRecurrenceCreated(recurrence: Recurrence) {
        // A recurrence was created in the picker fragment.
        // Update the selected recurrence and reset events.
        selectedRecurrence = recurrence
        updateSelectedLabel()
        resetRecurrenceEvents()
    }

    private fun chooseRecurrence() {
        // Show the correct fragment depending on which is enabled.
        if (optionsBinding.enableListCheck.isChecked) {
            // Add or remove the `null` item which shows a "Custom..." item in the list dialog.
            val enablePicker = optionsBinding.enablePickerCheck.isChecked
            if (enablePicker && null !in recurrencePresets) {
                recurrencePresets.add(null)
            } else if (!enablePicker && null in recurrencePresets) {
                recurrencePresets.remove(null)
            }
            showListDialog()
        } else {
            showPickerFragment()
        }
    }

    private fun showListDialog() {
        // Setup and show the recurrence list dialog.
        listDialog.selectedRecurrence = selectedRecurrence
        listDialog.startDate = startDate
        listDialog.show(childFragmentManager, "recurrence_list_dialog")
    }

    private fun showPickerFragment() {
        // Setup and show the recurrence picker.
        if (optionsBinding.usePickerDialogCheck.isChecked) {
            // Use the dialog picker.
            pickerDialog.selectedRecurrence = selectedRecurrence
            pickerDialog.startDate = startDate
            pickerDialog.show(childFragmentManager, "recurrence_picker_dialog")
        } else {
            // Use the fragment picker.
            pickerFragment.selectedRecurrence = selectedRecurrence
            pickerFragment.startDate = startDate
            childFragmentManager.beginTransaction()
                .add(R.id.picker_fragment_container, pickerFragment, "recurrence_picker_fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun resetRecurrenceEvents() {
        // Clear the list of found events and notify the adapter
        val sizeBefore = recurrenceEvents.size
        recurrenceEvents.clear()
        eventsAdapter?.notifyItemRangeRemoved(0, sizeBefore)

        // Find initial events
        isDoneFinding = false
        findMoreRecurrenceEvents()
    }

    private fun findMoreRecurrenceEvents() {
        if (isDoneFinding) return

        val sizeBefore = recurrenceEvents.size

        // Find and add new events
        recurrenceEvents.addAll(if (sizeBefore == 0) {
            // No events have been found yet, so find an initial batch of events.
            recurrenceFinder.find(selectedRecurrence, startDate, LOAD_BATCH_SIZE)
        } else {
            // Some events have been found already. Take the last event date and use
            // it as the base date to find more recurrence events.
            // Since the last event is already in the list, we don't want to include it.
            val baseDate = recurrenceEvents.last()
            recurrenceFinder.findBasedOn(selectedRecurrence, startDate,
                baseDate, sizeBefore, LOAD_BATCH_SIZE, includeStart = false)
        })

        // If less items were found than the number queried, all events have been found.
        isDoneFinding = (recurrenceEvents.size - sizeBefore) < LOAD_BATCH_SIZE

        // Notify the adapter and update the count.
        eventsAdapter?.notifyItemRangeInserted(sizeBefore, recurrenceEvents.size - sizeBefore)
        updateRecurrenceCount()
    }

    private fun updateRecurrenceCount() {
        // Update the text view showing the number of events found.
        // Append a "+" suffix when not all events have been found.
        val n = recurrenceEvents.size
        val suffix = if (isDoneFinding) "" else "+"
        binding.eventsView.eventsSubtitle.text = requireContext().getString(R.string.subtitle_events, n, suffix)
    }

    private fun updateSelectedLabel() {
        // Use the RecurrenceFormatter specified in the settings to format the selected recurrence and display it.
        selectedBinding.selectedLabel.text = settings.formatter
            .format(requireContext(), selectedRecurrence, startDate)
    }

    private fun updateStartDateInput() {
        // Use the date format specified in the settings to format the start date and display it.
        optionsBinding.startDateInput.setText(settings.formatter.dateFormat.format(startDate))
    }

    // View holder used to display a recurrence event item in the recycler view.
    private class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(number: String, date: String) {
            binding.eventNumberLabel.text = number
            binding.eventDateLabel.text = date
        }
    }

    private inner class EventsAdapter : RecyclerView.Adapter<EventViewHolder>() {
        override fun getItemCount() = recurrenceEvents.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            EventViewHolder(ItemEventBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            // Bind view holder with the event number and date.
            holder.bind(requireContext().getString(R.string.event_number, position + 1),
                eventDateFormat.format(recurrenceEvents[position]))
        }
    }

    companion object {
        /** The number of items loaded at once when user scrolls to the end of the list. */
        private const val LOAD_BATCH_SIZE = 20

        /** The number of items left to show until the end of the list at which more events are found. */
        private const val LOAD_MORE_TRIGGER = 4
    }
}
