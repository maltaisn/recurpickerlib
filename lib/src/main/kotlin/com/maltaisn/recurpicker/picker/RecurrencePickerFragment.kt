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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.maltaisn.recurpicker.picker.RecurrencePickerContract.Presenter


class RecurrencePickerFragment : Fragment(),
        RecurrencePickerContract.View, DateDialogFragment.Callback {

    private var presenter: Presenter? = null

    private val dateDialog by lazy { DateDialogFragment() }

    private lateinit var toolbar: Toolbar
    private lateinit var frequencyInput: EditText

    private lateinit var periodDropdown: AutoCompleteTextView
    private lateinit var periodAdapter: ArrayAdapter<String>

    private lateinit var weeklyGroup: Group
    private lateinit var weekBtns: List<ToggleButton>

    private lateinit var monthlyGroup: Group
    private lateinit var monthlyDropdown: AutoCompleteTextView
    private lateinit var monthlyAdapter: ArrayAdapter<String>

    private lateinit var endNeverView: View
    private lateinit var endNeverRadio: RadioButton

    private lateinit var endDateView: View
    private lateinit var endDateRadio: RadioButton
    private lateinit var endDatePrefixLabel: TextView
    private lateinit var endDateSuffixLabel: TextView
    private lateinit var endDateInput: EditText

    private lateinit var endCountView: View
    private lateinit var endCountRadio: RadioButton
    private lateinit var endCountPrefixLabel: TextView
    private lateinit var endCountSuffixLabel: TextView
    private lateinit var endCountInput: EditText

    /**
     * The settings defining the recurrence picker fragment behavior and content.
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


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, state: Bundle?): View? {
        if (state != null) {
            settings = state.getParcelable("settings")!!
            startDate = state.getLong("startDate")
            selectedRecurrence = state.getParcelable("selectedRecurrence")!!
        }

        // Wrap recurrence picker theme to context
        val context = requireContext()
        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.recurrencePickerStyle))
        val style = ta.getResourceId(0, R.style.RecurrencePickerStyle)
        ta.recycle()
        val contextWrapper = ContextThemeWrapper(context, style)
        val localInflater = inflater.cloneInContext(contextWrapper)

        // Inflate layout
        val view = localInflater.inflate(R.layout.rp_fragment_picker, container, false)

        // Toolbar
        toolbar = view.findViewById(R.id.rp_toolbar)
        toolbar.setOnMenuItemClickListener {
            presenter?.onConfirm()
            true
        }
        toolbar.setNavigationOnClickListener {
            presenter?.onCancel()
        }

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
        periodAdapter = DropdownAdapter(contextWrapper)
        periodDropdown.setAdapter(periodAdapter)
        periodDropdown.setOnItemClickListener { _, _, position, _ ->
            presenter?.onPeriodItemSelected(position)
            periodDropdown.requestLayout()  // Force view to wrap width to new text
        }

        // Days of the week
        weeklyGroup = view.findViewById(R.id.rp_picker_weekly_group)
        val weekBtnTa = resources.obtainTypedArray(R.array.rp_picker_week_btn_ids)
        weekBtns = List(weekBtnTa.length()) {
            val btn: ToggleButton = view.findViewById(weekBtnTa.getResourceId(it, 0))
            btn.setOnCheckedChangeListener { _, isChecked ->
                presenter?.onWeekBtnChecked(it + 1, isChecked)
            }
            btn
        }
        weekBtnTa.recycle()

        // Monthly setting
        monthlyGroup = view.findViewById(R.id.rp_picker_monthly_group)
        monthlyDropdown = view.findViewById(R.id.rp_picker_monthly_dropdown)
        monthlyAdapter = DropdownAdapter(contextWrapper)
        monthlyDropdown.setAdapter(monthlyAdapter)
        monthlyDropdown.setOnItemClickListener { _, _, position, _ ->
            presenter?.onMonthlySettingItemSelected(position)
        }

        // End never
        endNeverView = view.findViewById(R.id.rp_picker_end_never_view)
        endNeverRadio = view.findViewById(R.id.rp_picker_end_never_radio)
        val endNeverClick = View.OnClickListener { presenter?.onEndNeverClicked() }
        endNeverView.setOnClickListener(endNeverClick)
        endNeverRadio.setOnClickListener(endNeverClick)

        // End by date
        endDateView = view.findViewById(R.id.rp_picker_end_date_view)
        endDateRadio = view.findViewById(R.id.rp_picker_end_date_radio)
        endDateInput = view.findViewById(R.id.rp_picker_end_date_input)
        endDatePrefixLabel = view.findViewById(R.id.rp_picker_end_date_prefix_label)
        endDateSuffixLabel = view.findViewById(R.id.rp_picker_end_date_suffix_label)
        val endDateClick = View.OnClickListener { presenter?.onEndDateClicked() }
        endDateView.setOnClickListener(endDateClick)
        endDateRadio.setOnClickListener(endDateClick)
        endDateInput.setOnClickListener { presenter?.onEndDateInputClicked() }

        // End by date
        endCountView = view.findViewById(R.id.rp_picker_end_count_view)
        endCountRadio = view.findViewById(R.id.rp_picker_end_count_radio)
        endCountInput = view.findViewById(R.id.rp_picker_end_count_input)
        endCountPrefixLabel = view.findViewById(R.id.rp_picker_end_count_prefix_label)
        endCountSuffixLabel = view.findViewById(R.id.rp_picker_end_count_suffix_label)
        val endCountClick = View.OnClickListener { presenter?.onEndCountClicked() }
        endCountView.setOnClickListener(endCountClick)
        endCountRadio.setOnClickListener(endCountClick)
        endCountInput.addTextChangedListener {
            presenter?.onEndCountChanged(it.toString())
        }
        endCountInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) endCountInput.clearFocus()
            false
        }

        return view
    }

    override fun clearFocus() {
        // Clear focus from input fields
        frequencyInput.clearFocus()
        endCountInput.clearFocus()

        // Hide keyboard too
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    override fun onViewStateRestored(state: Bundle?) {
        super.onViewStateRestored(state)

        // Attach the presenter
        presenter = RecurrencePickerPresenter()
        presenter?.attach(this, state)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)

        state.putParcelable("settings", settings)
        state.putLong("startDate", startDate)
        state.putParcelable("selectedRecurrence", selectedRecurrence)

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
        fragmentManager?.popBackStack()
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
        periodDropdown.requestLayout()  // Force view to wrap width to new text
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
        monthlyAdapter.add(res.getString(R.string.rp_format_monthly_same_day))  // on the same day each month
        monthlyAdapter.add(RecurrenceFormatter.getDayOfWeekInMonthText(res, dayOfWeekInMonth, weekInMonth))  // on every <first> <Sunday>
        if (showLastDay) monthlyAdapter.add(res.getString(R.string.rp_format_monthly_last_day))  // on the last day of the month
    }

    override fun setSelectedMonthlySettingItem(index: Int) {
        monthlyDropdown.setText(monthlyAdapter.getItem(index))
    }

    override fun setEndNeverChecked(checked: Boolean) {
        endNeverRadio.isChecked = checked
    }

    override fun setEndDateChecked(checked: Boolean) {
        endDateRadio.isChecked = checked
        endDateView.visibility = if (checked) View.INVISIBLE else View.VISIBLE
    }

    override fun setEndDateView(date: String) {
        endDateInput.setText(date)
        endDateInput.requestLayout()  // Force view to wrap width to new text
    }

    override fun setEndDateViewEnabled(enabled: Boolean) {
        endDateInput.isEnabled = enabled
    }

    override fun setEndDateLabels(prefix: String, suffix: String) {
        endDatePrefixLabel.text = prefix
        endDateSuffixLabel.text = suffix
        endDatePrefixLabel.isVisible = prefix.isNotEmpty()
    }

    override fun showEndDateDialog(date: Long, minDate: Long) {
        dateDialog.date = date
        dateDialog.minDate = minDate
        dateDialog.maxDate = Recurrence.DATE_NONE
        dateDialog.show(this.childFragmentManager, "recurrence_end_date_dialog")
    }

    override fun setEndCountChecked(checked: Boolean) {
        endCountRadio.isChecked = checked
        endCountView.visibility = if (checked) View.INVISIBLE else View.VISIBLE
    }

    override fun setEndCountView(count: String) {
        endCountInput.setText(count)
        endCountInput.setSelection(count.length)
    }

    override fun setEndCountViewEnabled(enabled: Boolean) {
        endCountInput.isEnabled = enabled
    }

    override fun setEndCountLabels(prefix: String, suffix: String) {
        endCountPrefixLabel.text = prefix
        endCountSuffixLabel.text = suffix
        endCountPrefixLabel.isVisible = prefix.isNotEmpty()
    }

    override fun setEndCountMaxLength(length: Int) {
        endCountInput.filters = arrayOf(InputFilter.LengthFilter(length))
    }

    override fun setCancelResult() {
        callback?.onRecurrencePickerCancelled()
    }

    override fun setConfirmResult(recurrence: Recurrence) {
        callback?.onRecurrenceCreated(recurrence)
    }

    private val callback: Callback?
        get() = (parentFragment as? Callback)
                ?: (targetFragment as? Callback)
                ?: (activity as? Callback)

    /**
     * Interface to be implemented by either the parent fragment of this fragment, the target fragment
     * or the parent activity. If none of these implements it, there won't be any callback.
     */
    interface Callback {
        /**
         * Called if the "Done" button is clicked and a custom [recurrence] is created.
         */
        fun onRecurrenceCreated(recurrence: Recurrence)

        /**
         * Called if the recurrence picker fragment back arrow is clicked.
         */
        fun onRecurrencePickerCancelled() = Unit
    }

    /** Custom AutoCompleteTextView adapter to disable filtering since we want it to act like a spinner. */
    private class DropdownAdapter(context: Context) :
            ArrayAdapter<String>(context, R.layout.rp_item_dropdown) {
        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?) = null
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
            }
        }
    }


    companion object {
        /**
         * Create a new instance of the fragment with [settings].
         * More settings can be set with the returned fragment instance later.
         */
        @JvmStatic
        fun newInstance(settings: RecurrencePickerSettings): RecurrencePickerFragment {
            val dialog = RecurrencePickerFragment()
            dialog.settings = settings
            return dialog
        }
    }

}
