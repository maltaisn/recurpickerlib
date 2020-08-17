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

package com.maltaisn.recurpicker.picker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.maltaisn.recurpicker.getCallback
import com.maltaisn.recurpicker.picker.RecurrencePickerContract.Presenter

/**
 * Dialog fragment used to create a custom recurrence with nearly all available options.
 * Provides all the options available in [RecurrencePickerFragment], only in a more compact way.
 * Note: due to the MVP architecture, some interface methods are public but shouldn't be used.
 */
class RecurrencePickerDialog : DialogFragment(),
    RecurrencePickerContract.View, DateDialogFragment.Callback {

    private var presenter: Presenter? = null

    private val dateDialog by lazy { DateDialogFragment() }

    private lateinit var frequencyInput: EditText

    private lateinit var periodDropdown: AutoCompleteTextView
    private lateinit var periodAdapter: ArrayAdapter<String>

    private lateinit var weeklyGroup: Group
    private lateinit var weekBtns: List<MaterialButton>

    private lateinit var monthlyGroup: Group
    private lateinit var monthlyDropdown: AutoCompleteTextView
    private lateinit var monthlyAdapter: ArrayAdapter<String>

    private lateinit var endDropdown: AutoCompleteTextView
    private lateinit var endAdapter: ArrayAdapter<String>
    private lateinit var endAdapterList: MutableList<String>

    private lateinit var endDateGroup: Group
    private lateinit var endDateSuffixLabel: TextView
    private lateinit var endDateInput: EditText

    private lateinit var endCountGroup: Group
    private lateinit var endCountSuffixLabel: TextView
    private lateinit var endCountInput: EditText

    /**
     * The settings defining the recurrence picker dialog behavior and content.
     */
    override lateinit var settings: RecurrencePickerSettings
        private set

    /**
     * The start date of the event for which a recurrence is created.
     * This is a required parameter and cannot be set to [Recurrence.DATE_NONE].
     * The date is used to know on which day of the week and on which week of the month
     * the events will happen. It's also the minimum date for the end date.
     */
    override var startDate = Recurrence.DATE_NONE

    /**
     * The previously selected recurrence that will be shown initially. Can be set to `null`
     * if no recurrence was selected previously. In this case, a simple daily recurrence will
     * be shown.
     */
    override var selectedRecurrence: Recurrence? = null

    /**
     * Whether to show the dialog title or not.
     */
    var showTitle = false

    @SuppressLint("InflateParams")
    override fun onCreateDialog(state: Bundle?): Dialog {
        if (state != null) {
            settings = state.getParcelable("settings")!!
            startDate = state.getLong("startDate")
            selectedRecurrence = state.getParcelable("selectedRecurrence")!!
            showTitle = state.getBoolean("showTitle")
        }

        // Wrap recurrence picker theme to context
        val context = requireContext()
        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.recurrencePickerStyle))
        val style = ta.getResourceId(0, R.style.RecurrencePickerStyle)
        ta.recycle()
        val contextWrapper = ContextThemeWrapper(context, style)
        val localInflater = LayoutInflater.from(contextWrapper)

        val view = localInflater.inflate(R.layout.rp_dialog_picker, null, false)
        setupViews(contextWrapper, view)

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(contextWrapper)
            .setView(view)
            .setTitle(if (showTitle) getString(R.string.rp_picker_title) else null)
            .setPositiveButton(R.string.rp_picker_done) { _, _ -> presenter?.onConfirm() }
            .setNegativeButton(R.string.rp_picker_cancel) { _, _ -> presenter?.onCancel() }
            .create()
        dialog.setOnShowListener {
            // Get dialog's width and padding
            val fgPadding = Rect()
            val window = dialog.window!!
            window.decorView.background.getPadding(fgPadding)
            val padding = fgPadding.left + fgPadding.right
            var width = context.resources.displayMetrics.widthPixels - padding

            // Set dialog's dimensions, with maximum width.
            val dialogMaxWidth = contextWrapper.obtainStyledAttributes(R.styleable.RecurrencePicker).use {
                it.getDimensionPixelSize(R.styleable.RecurrencePicker_rpPickerDialogMaxWidth, -1)
            }
            if (width > dialogMaxWidth) {
                width = dialogMaxWidth
            }
            window.setLayout(width + padding, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.layoutParams = FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Attach the presenter
        presenter = RecurrencePickerPresenter()
        presenter?.attach(this, state)

        return dialog
    }

    private fun setupViews(context: Context, view: View) {
        // Frequency
        frequencyInput = view.findViewById(R.id.rp_picker_freq_input)
        frequencyInput.addTextChangedListener {
            presenter?.onFrequencyChanged(it.toString())
        }
        frequencyInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) frequencyInput.clearFocus()
            false
        }

        // Period
        periodDropdown = view.findViewById(R.id.rp_picker_period_dropdown)
        periodAdapter = DropdownAdapter(context)
        periodDropdown.setAdapter(periodAdapter)
        periodDropdown.setOnItemClickListener { _, _, position, _ ->
            presenter?.onPeriodItemSelected(position)
            periodDropdown.requestLayout() // Force view to wrap width to new text
        }

        setupPeriodRelatedViews(context, view)
        setupEndViews(context, view)
    }

    private fun setupPeriodRelatedViews(context: Context, view: View) {
        // Days of the week
        weeklyGroup = view.findViewById(R.id.rp_picker_weekly_group)
        val weekBtnTa = resources.obtainTypedArray(R.array.rp_picker_week_btn_ids)
        weekBtns = List(weekBtnTa.length()) {
            val btn: MaterialButton = view.findViewById(weekBtnTa.getResourceId(it, 0))
            btn.addOnCheckedChangeListener { _, isChecked ->
                presenter?.onWeekBtnChecked(it + 1, isChecked)
            }
            btn
        }
        weekBtnTa.recycle()

        // Monthly setting
        monthlyGroup = view.findViewById(R.id.rp_picker_monthly_group)
        monthlyDropdown = view.findViewById(R.id.rp_picker_monthly_dropdown)
        monthlyAdapter = DropdownAdapter(context)
        monthlyDropdown.setAdapter(monthlyAdapter)
        monthlyDropdown.setOnItemClickListener { _, _, position, _ ->
            presenter?.onMonthlySettingItemSelected(position)
        }
    }

    private fun setupEndViews(context: Context, view: View) {
        // End dropdown
        endDropdown = view.findViewById(R.id.rp_picker_end_dropdown)
        endAdapterList = mutableListOf(
            getString(R.string.rp_picker_end_never),
            getString(R.string.rp_picker_end_date_prefix_fallback),
            getString(R.string.rp_picker_end_count_prefix_fallback))
        endAdapter = DropdownAdapter(context, endAdapterList)
        endDropdown.setAdapter(endAdapter)
        endDropdown.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> presenter?.onEndNeverClicked()
                1 -> presenter?.onEndDateClicked()
                2 -> presenter?.onEndCountClicked()
            }
        }

        // End by date
        endDateGroup = view.findViewById(R.id.rp_picker_end_date_group)
        endDateInput = view.findViewById(R.id.rp_picker_end_date_input)
        endDateSuffixLabel = view.findViewById(R.id.rp_picker_end_date_suffix_label)
        endDateInput.setOnClickListener { presenter?.onEndDateInputClicked() }

        // End by date
        endCountGroup = view.findViewById(R.id.rp_picker_end_count_group)
        endCountInput = view.findViewById(R.id.rp_picker_end_count_input)
        endCountSuffixLabel = view.findViewById(R.id.rp_picker_end_count_suffix_label)
        endCountInput.addTextChangedListener {
            presenter?.onEndCountChanged(it.toString())
        }
        endCountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) endCountInput.clearFocus()
            false
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        presenter?.onCancel()
    }

    override fun clearFocus() {
        // Clear focus from input fields
        frequencyInput.clearFocus()
        endCountInput.clearFocus()

        // Hide keyboard too
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(frequencyInput.windowToken, 0)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)

        state.putParcelable("settings", settings)
        state.putLong("startDate", startDate)
        state.putParcelable("selectedRecurrence", selectedRecurrence)
        state.putBoolean("showTitle", showTitle)

        presenter?.saveState(state)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Detach the presenter
        presenter?.detach()
        presenter = null
    }

    override fun onDateDialogConfirmed(date: Long) {
        presenter?.onEndDateEntered(date)
    }

    override val endDateText: String
        get() = requireContext().getString(R.string.rp_picker_end_date)

    override fun getEndCountTextFor(count: Int) =
        resources.getQuantityString(R.plurals.rp_picker_end_count, count)

    override fun exit() {
        dismiss()
    }

    override fun setFrequencyView(frequency: String) {
        frequencyInput.setText(frequency)
        frequencyInput.setSelection(frequency.length)
    }

    override fun setFrequencyMaxLength(length: Int) {
        frequencyInput.filters = arrayOf(InputFilter.LengthFilter(length))
    }

    override fun setPeriodItems(frequency: Int) {
        periodAdapter.clear()

        val res = resources
        val periodsTa = res.obtainTypedArray(R.array.rp_picker_periods)
        periodAdapter.addAll(List(periodsTa.length()) {
            res.getQuantityString(periodsTa.getResourceId(it, 0), frequency)
        })
        periodAdapter.notifyDataSetChanged()
        periodsTa.recycle()
    }

    override fun setSelectedPeriodItem(index: Int) {
        periodDropdown.setText(periodAdapter.getItem(index))
        periodDropdown.requestLayout() // Force view to wrap width to new text
    }

    override fun setWeekBtnsShown(shown: Boolean) {
        weeklyGroup.isVisible = shown
    }

    override fun setWeekBtnChecked(dayOfWeek: Int, checked: Boolean) {
        weekBtns[dayOfWeek - 1].isChecked = checked
    }

    override fun setMonthlySettingShown(shown: Boolean) {
        monthlyGroup.isVisible = shown
    }

    override fun setMonthlySettingItems(showLastDay: Boolean, dayOfWeekInMonth: Int, weekInMonth: Int) {
        monthlyAdapter.clear()

        val res = resources
        monthlyAdapter.add(res.getString(R.string.rp_format_monthly_same_day)) // on the same day each month
        monthlyAdapter.add(RecurrenceFormatter.getDayOfWeekInMonthText(
            res, dayOfWeekInMonth, weekInMonth)) // on every <first> <Sunday>
        if (showLastDay) {
            // on the last day of the month
            monthlyAdapter.add(res.getString(R.string.rp_format_monthly_last_day))
        }
    }

    override fun setSelectedMonthlySettingItem(index: Int) {
        monthlyDropdown.setText(monthlyAdapter.getItem(index))
    }

    override fun setEndNeverChecked(checked: Boolean) {
        setEndDropdownItemChecked(0, checked)
    }

    override fun setEndDateChecked(checked: Boolean) {
        setEndDropdownItemChecked(1, checked)
    }

    override fun setEndDateView(date: String) {
        endDateInput.setText(date)
        endDateInput.requestLayout() // Force view to wrap width to new text
    }

    override fun setEndDateViewEnabled(enabled: Boolean) {
        // Not important
    }

    override fun setEndDateLabels(prefix: String, suffix: String) {
        if (prefix.isNotEmpty()) {
            endAdapterList[1] = prefix
            endAdapter.notifyDataSetChanged()
        }
        endDateSuffixLabel.text = suffix
    }

    override fun showEndDateDialog(date: Long, minDate: Long) {
        dateDialog.date = date
        dateDialog.minDate = minDate
        dateDialog.maxDate = Recurrence.DATE_NONE
        dateDialog.show(childFragmentManager, "recurrence_end_date_dialog")
    }

    override fun setEndCountChecked(checked: Boolean) {
        setEndDropdownItemChecked(2, checked)
    }

    override fun setEndCountView(count: String) {
        endCountInput.setText(count)
        endCountInput.setSelection(count.length)
    }

    override fun setEndCountViewEnabled(enabled: Boolean) {
        // Not important
    }

    override fun setEndCountLabels(prefix: String, suffix: String) {
        if (prefix.isNotEmpty()) {
            endAdapterList[2] = prefix
            endAdapter.notifyDataSetChanged()
        }
        endCountSuffixLabel.text = suffix
    }

    override fun setEndCountMaxLength(length: Int) {
        endCountInput.filters = arrayOf(InputFilter.LengthFilter(length))
    }

    override fun setCancelResult() {
        getCallback<RecurrencePickerCallback>()?.onRecurrencePickerCancelled()
    }

    override fun setConfirmResult(recurrence: Recurrence) {
        getCallback<RecurrencePickerCallback>()?.onRecurrenceCreated(recurrence)
    }

    private fun setEndDropdownItemChecked(index: Int, checked: Boolean) {
        if (checked) {
            endDropdown.setText(endAdapterList[index])
            endDropdown.requestLayout() // Force view to wrap width to new text
            endDateGroup.isVisible = (index == 1)
            endCountGroup.isVisible = (index == 2)
        }
    }

    companion object {
        /**
         * Create a new instance of the dialog with [settings].
         * More settings can be set with the returned dialog instance later.
         */
        @JvmStatic
        fun newInstance(settings: RecurrencePickerSettings): RecurrencePickerDialog {
            val dialog = RecurrencePickerDialog()
            dialog.settings = settings
            return dialog
        }
    }
}
