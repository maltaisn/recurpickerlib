/*
 * Copyright 2020 Nicolas Maltais
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

package com.maltaisn.recurpicker.picker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.maltaisn.recurpicker.getCallback

/**
 * Delegate [RecurrencePickerContract.View] implementation used by the picker dialog and fragment
 * since they share most of their functionality.
 */
internal class RecurrencePickerDelegate :
    RecurrencePickerContract.View, DateDialogFragment.Callback {

    private var _presenter: RecurrencePickerContract.Presenter? = null
    private val presenter get() = _presenter!!

    private var _fragment: Fragment? = null
    private val fragment get() = _fragment!!

    private var _context: Context? = null
    private val context get() = _context!!

    private var _binding: Binding? = null
    private val binding get() = _binding!!

    override lateinit var settings: RecurrencePickerSettings
    override var startDate = Recurrence.DATE_NONE
    override var selectedRecurrence: Recurrence? = null

    fun attach(
        presenter: RecurrencePickerContract.Presenter,
        fragment: Fragment,
        context: Context,
        view: View
    ) {
        _presenter = presenter
        _fragment = fragment
        _context = context
        _binding = Binding(view)
    }

    fun detach() {
        _presenter = null
        _fragment = null
        _context = null
        _binding = null
    }

    fun setupViews() {
        // Frequency
        binding.frequencyInput.addTextChangedListener {
            presenter.onFrequencyChanged(it.toString())
        }
        binding.frequencyInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.frequencyInput.clearFocus()
            }
            false
        }

        // Period
        binding.periodDropdown.setAdapter(binding.periodAdapter)
        binding.periodDropdown.setOnItemClickListener { _, _, position, _ ->
            presenter.onPeriodItemSelected(position)
            binding.periodDropdown.requestLayout() // Force view to wrap width to new text
        }

        setupPeriodRelatedViews()
        setupEndViews()
    }

    private fun setupPeriodRelatedViews() {
        // Days of the week
        for ((i, weekBtn) in binding.weekBtns.withIndex()) {
            weekBtn.addOnCheckedChangeListener { _, isChecked ->
                presenter.onWeekBtnChecked(i + 1, isChecked)
            }
        }

        // Monthly setting
        binding.monthlyDropdown.setAdapter(binding.monthlyAdapter)
        binding.monthlyDropdown.setOnItemClickListener { _, _, position, _ ->
            presenter.onMonthlySettingItemSelected(position)
        }
    }

    private fun setupEndViews() {
        // End by date
        binding.endDateInput.setOnClickListener { presenter.onEndDateInputClicked() }

        // End by count
        binding.endCountInput.addTextChangedListener {
            presenter.onEndCountChanged(it.toString())
        }
        binding.endCountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.endCountInput.clearFocus()
            }
            false
        }
    }

    fun onCancel() {
        presenter.onCancel()
    }

    fun onRestoreInstanceState(state: Bundle) {
        settings = state.getParcelable("settings")!!
        startDate = state.getLong("startDate")
        selectedRecurrence = state.getParcelable("selectedRecurrence")!!
    }

    fun onSaveInstanceState(state: Bundle) {
        state.putParcelable("settings", settings)
        state.putLong("startDate", startDate)
        state.putParcelable("selectedRecurrence", selectedRecurrence)

        presenter.saveState(state)
    }

    override fun onDateDialogConfirmed(date: Long) {
        presenter.onEndDateEntered(date)
    }

    override fun exit() = Unit

    override val endDateText: String
        get() = context.getString(R.string.rp_picker_end_date)

    override fun getEndCountTextFor(count: Int) =
        context.resources.getQuantityString(R.plurals.rp_picker_end_count, count)

    override fun clearFocus() {
        // Clear focus from input fields
        binding.frequencyInput.clearFocus()
        binding.endCountInput.clearFocus()

        // Hide keyboard too
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun setFrequencyView(frequency: String) {
        binding.frequencyInput.let {
            it.setText(frequency)
            it.setSelection(frequency.length)
        }
    }

    override fun setFrequencyMaxLength(length: Int) {
        binding.frequencyInput.setMaxLength(length)
    }

    override fun setPeriodItems(frequency: Int) {
        val res = context.resources
        res.obtainTypedArray(R.array.rp_picker_periods).use { ta ->
            binding.periodAdapter.let { adapter ->
                adapter.clear()
                adapter.addAll(List(ta.length()) {
                    res.getQuantityString(ta.getResourceId(it, 0), frequency)
                })
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun setSelectedPeriodItem(index: Int) {
        binding.periodDropdown.let {
            it.setText(binding.periodAdapter.getItem(index))
            it.requestLayout() // Force view to wrap width to new text
        }
    }

    override fun setWeekBtnsShown(shown: Boolean) {
        binding.weeklyGroup.isVisible = shown
    }

    override fun setWeekBtnChecked(dayOfWeek: Int, checked: Boolean) {
        binding.weekBtns[dayOfWeek - 1].isChecked = checked
    }

    override fun setMonthlySettingShown(shown: Boolean) {
        binding.monthlyGroup.isVisible = shown
    }

    override fun setMonthlySettingItems(showLastDay: Boolean, dayOfWeekInMonth: Int, weekInMonth: Int) {
        val res = context.resources
        binding.monthlyAdapter.let { adapter ->
            adapter.clear()
            adapter.add(res.getString(R.string.rp_format_monthly_same_day)) // on the same day each month
            adapter.add(RecurrenceFormatter.getDayOfWeekInMonthText(
                res, dayOfWeekInMonth, weekInMonth)) // on every <first> <Sunday>
            if (showLastDay) {
                // on the last day of the month
                adapter.add(res.getString(R.string.rp_format_monthly_last_day))
            }
        }
    }

    override fun setSelectedMonthlySettingItem(index: Int) {
        binding.monthlyDropdown.setText(binding.monthlyAdapter.getItem(index))
    }

    override fun setEndNeverChecked(checked: Boolean) = Unit

    override fun setEndDateChecked(checked: Boolean) = Unit

    override fun setEndDateView(date: String) {
        binding.endDateInput.let {
            it.setText(date)
            it.requestLayout() // Force view to wrap width to new text
        }
    }

    override fun setEndDateViewEnabled(enabled: Boolean) {
        binding.endDateInput.isEnabled = enabled
    }

    override fun setEndDateLabels(prefix: String, suffix: String) {
        binding.endDatePrefixLabel?.let {
            it.isVisible = prefix.isNotEmpty()
            it.text = prefix
        }
        binding.endDateSuffixLabel.text = suffix
    }

    override fun showEndDateDialog(date: Long, minDate: Long) {
        DateDialogFragment.newInstance(date, minDate)
            .show(fragment.childFragmentManager, RECCURENCE_END_DATE_DIALOG_TAG)
    }

    override fun setEndCountChecked(checked: Boolean) = Unit

    override fun setEndCountView(count: String) {
        binding.endCountInput.let {
            it.setText(count)
            it.setSelection(count.length)
        }
    }

    override fun setEndCountViewEnabled(enabled: Boolean) {
        binding.endCountInput.isEnabled = enabled
    }

    override fun setEndCountLabels(prefix: String, suffix: String) {
        binding.endCountPrefixLabel?.let {
            it.isVisible = prefix.isNotEmpty()
            it.text = prefix
        }
        binding.endCountSuffixLabel.text = suffix
    }

    override fun setEndCountMaxLength(length: Int) {
        binding.endCountInput.setMaxLength(length)
    }

    override fun setCancelResult() {
        fragment.getCallback<RecurrencePickerCallback>()?.onRecurrencePickerCancelled()
    }

    override fun setConfirmResult(recurrence: Recurrence) {
        fragment.getCallback<RecurrencePickerCallback>()?.onRecurrenceCreated(recurrence)
    }

    private fun TextView.setMaxLength(length: Int) {
        this.filters = arrayOf(InputFilter.LengthFilter(length))
    }

    private class Binding(val root: View) {
        val frequencyInput: EditText = root.findViewById(R.id.rp_picker_freq_input)
        val periodDropdown: AutoCompleteTextView = root.findViewById(R.id.rp_picker_period_dropdown)
        val weeklyGroup: Group = root.findViewById(R.id.rp_picker_weekly_group)
        val weekBtns: List<MaterialButton>
        val monthlyGroup: Group = root.findViewById(R.id.rp_picker_monthly_group)
        val monthlyDropdown: AutoCompleteTextView = root.findViewById(R.id.rp_picker_monthly_dropdown)
        val endDatePrefixLabel: TextView? = root.findViewById(R.id.rp_picker_end_date_prefix_label)
        val endDateSuffixLabel: TextView = root.findViewById(R.id.rp_picker_end_date_suffix_label)
        val endDateInput: EditText = root.findViewById(R.id.rp_picker_end_date_input)
        val endCountPrefixLabel: TextView? = root.findViewById(R.id.rp_picker_end_count_prefix_label)
        val endCountSuffixLabel: TextView = root.findViewById(R.id.rp_picker_end_count_suffix_label)
        val endCountInput: EditText = root.findViewById(R.id.rp_picker_end_count_input)

        val periodAdapter = DropdownAdapter(root.context)
        val monthlyAdapter = DropdownAdapter(root.context)

        init {
            val ta = root.context.resources.obtainTypedArray(R.array.rp_picker_week_btn_ids)
            weekBtns = List(ta.length()) { root.findViewById(ta.getResourceId(it, 0)) }
            ta.recycle()
        }
    }

    companion object {
        private const val RECCURENCE_END_DATE_DIALOG_TAG = "recurrence_end_date_dialog"
    }
}
