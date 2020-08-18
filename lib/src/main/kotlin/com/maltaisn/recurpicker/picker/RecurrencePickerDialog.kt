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
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.getPickerContextWrapper
import com.maltaisn.recurpicker.picker.RecurrencePickerContract.Presenter

/**
 * Dialog fragment used to create a custom recurrence with nearly all available options.
 * Provides all the options available in [RecurrencePickerFragment], only in a more compact way.
 * Note: due to the MVP architecture, some interface methods are public but shouldn't be used.
 */
class RecurrencePickerDialog private constructor(
    private val delegate: RecurrencePickerDelegate
) : DialogFragment(),
    RecurrencePickerContract.View by delegate,
    DateDialogFragment.Callback by delegate {

    constructor() : this(RecurrencePickerDelegate())

    private var presenter: Presenter? = null

    private var _binding: Binding? = null
    private val binding get() = _binding!!

    private var endAdapter: ArrayAdapter<String>? = null
    private var endAdapterList = mutableListOf<String>()

    /**
     * Whether to show the dialog title or not.
     */
    var showTitle = false

    @SuppressLint("InflateParams")
    override fun onCreateDialog(state: Bundle?): Dialog {
        // Wrap recurrence picker theme to context
        val contextWrapper = getPickerContextWrapper()
        val localInflater = LayoutInflater.from(contextWrapper)

        _binding = Binding(localInflater.inflate(R.layout.rp_dialog_picker, null, false))
        setupEndViews(contextWrapper)

        // Attach the presenter
        val presenter = RecurrencePickerPresenter()
        this.presenter = presenter

        delegate.attach(presenter, this, contextWrapper, binding.root)
        delegate.setupViews()
        if (state != null) {
            delegate.onRestoreInstanceState(state)
            showTitle = state.getBoolean("showTitle")
        }

        presenter.attach(this, state)

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(contextWrapper)
            .setView(binding.root)
            .setTitle(if (showTitle) getString(R.string.rp_picker_title) else null)
            .setPositiveButton(R.string.rp_picker_done) { _, _ -> presenter.onConfirm() }
            .setNegativeButton(R.string.rp_picker_cancel) { _, _ -> presenter.onCancel() }
            .create()
        dialog.setOnShowListener {
            // Get dialog's width and padding
            val fgPadding = Rect()
            val window = dialog.window!!
            window.decorView.background.getPadding(fgPadding)
            val padding = fgPadding.left + fgPadding.right
            var width = requireContext().resources.displayMetrics.widthPixels - padding

            // Set dialog's dimensions, with maximum width.
            val dialogMaxWidth = contextWrapper.obtainStyledAttributes(R.styleable.RecurrencePicker).use {
                it.getDimensionPixelSize(R.styleable.RecurrencePicker_rpPickerDialogMaxWidth, -1)
            }
            if (width > dialogMaxWidth) {
                width = dialogMaxWidth
            }
            window.setLayout(width + padding, ViewGroup.LayoutParams.WRAP_CONTENT)
            binding.root.layoutParams = FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        return dialog
    }

    private fun setupEndViews(context: Context) {
        // End dropdown
        endAdapterList = mutableListOf(
            getString(R.string.rp_picker_end_never),
            getString(R.string.rp_picker_end_date_prefix_fallback),
            getString(R.string.rp_picker_end_count_prefix_fallback))
        endAdapter = DropdownAdapter(context, endAdapterList)
        binding.endDropdown.setAdapter(endAdapter)
        binding.endDropdown.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> presenter?.onEndNeverClicked()
                1 -> presenter?.onEndDateClicked()
                2 -> presenter?.onEndCountClicked()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        delegate.onCancel()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        delegate.onSaveInstanceState(state)

        state.putBoolean("showTitle", showTitle)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        endAdapter = null

        // Detach the presenter
        presenter?.detach()
        presenter = null

        delegate.detach()
    }

    override fun exit() {
        dismiss()
    }

    override fun setEndNeverChecked(checked: Boolean) {
        setEndDropdownItemChecked(0, checked)
    }

    override fun setEndDateChecked(checked: Boolean) {
        setEndDropdownItemChecked(1, checked)
    }

    override fun setEndDateLabels(prefix: String, suffix: String) {
        delegate.setEndDateLabels(prefix, suffix)
        if (prefix.isNotEmpty()) {
            endAdapterList[1] = prefix
            endAdapter?.notifyDataSetChanged()
        }
    }

    override fun setEndCountChecked(checked: Boolean) {
        setEndDropdownItemChecked(2, checked)
    }

    override fun setEndCountLabels(prefix: String, suffix: String) {
        delegate.setEndCountLabels(prefix, suffix)
        if (prefix.isNotEmpty()) {
            endAdapterList[2] = prefix
            endAdapter?.notifyDataSetChanged()
        }
    }

    private fun setEndDropdownItemChecked(index: Int, checked: Boolean) {
        if (checked) {
            binding.endDropdown.setText(endAdapterList[index])
            binding.endDropdown.requestLayout() // Force view to wrap width to new text
            binding.endDateGroup.isVisible = (index == 1)
            binding.endCountGroup.isVisible = (index == 2)
        }
    }

    private class Binding(val root: View) {
        var endDropdown: AutoCompleteTextView = root.findViewById(R.id.rp_picker_end_dropdown)
        var endDateGroup: Group = root.findViewById(R.id.rp_picker_end_date_group)
        var endCountGroup: Group = root.findViewById(R.id.rp_picker_end_count_group)
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
