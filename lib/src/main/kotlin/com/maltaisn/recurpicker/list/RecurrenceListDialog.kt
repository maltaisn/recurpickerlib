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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RadioButton
import androidx.core.content.res.use
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.maltaisn.recurpicker.getCallback
import com.maltaisn.recurpicker.getPickerContextWrapper
import com.maltaisn.recurpicker.list.RecurrenceListContract.ItemView
import com.maltaisn.recurpicker.list.RecurrenceListContract.Presenter
import com.maltaisn.recurpicker.picker.RecurrencePickerDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerFragment

/**
 * Dialog fragment displaying a list of recurrence presets. This aims to provide
 * a simpler approach at choosing a recurrence for the user. Custom recurrence can later
 * be created with a "Custom..." item, defined by a `null` value in the presets list.
 * Custom recurrences can be created with either [RecurrencePickerFragment] or [RecurrencePickerDialog].
 * Note: due to the MVP architecture, some interface methods are public but shouldn't be used.
 */
public class RecurrenceListDialog : DialogFragment(), RecurrenceListContract.View {

    private var presenter: Presenter? = null

    /**
     * The settings defining the recurrence list dialog behavior and content.
     */
    override lateinit var settings: RecurrencePickerSettings

    /**
     * The start date of the event for which a recurrence is selected.
     * This is not a required parameter and can be set to [Recurrence.DATE_NONE].
     * It will however provide more consise recurrence formatting to text, see [RecurrenceFormatter.format].
     */
    override var startDate: Long = Recurrence.DATE_NONE

    /**
     * The previously selected recurrence that will be selected initially. Can be set to `null`
     * if no recurrence was selected previously. In this case, the first preset will be selected
     * by default. If a recurrence is set, a preset will be selected if it's equal to any of them,
     * and if it's not equal to any preset, an additional list item will be shown before the others.
     */
    override var selectedRecurrence: Recurrence? = null

    @SuppressLint("InflateParams")
    override fun onCreateDialog(state: Bundle?): Dialog {
        if (state != null) {
            settings = state.getParcelable("settings")!!
            selectedRecurrence = state.getParcelable("selectedRecurrence")!!
        }

        val contextWrapper = getPickerContextWrapper()
        val localInflater = LayoutInflater.from(contextWrapper)

        val view = localInflater.inflate(R.layout.rp_dialog_list, null, false)
        setupViews(contextWrapper, view)

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(contextWrapper)
            .setView(view)
            .create()
        dialog.setOnShowListener {
            // Get dialog's width and padding.
            val fgPadding = Rect()
            val window = dialog.window!!
            window.decorView.background.getPadding(fgPadding)
            val padding = fgPadding.left + fgPadding.right
            var width = requireContext().resources.displayMetrics.widthPixels - padding

            // Set dialog's dimensions, with maximum width.
            val dialogMaxWidth = contextWrapper.obtainStyledAttributes(R.styleable.RecurrencePicker).use {
                it.getDimensionPixelSize(R.styleable.RecurrencePicker_rpListDialogMaxWidth, -1)
            }
            if (width > dialogMaxWidth) {
                width = dialogMaxWidth
            }
            window.setLayout(width + padding, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.layoutParams = FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Attach the presenter
        presenter = RecurrenceListPresenter()
        presenter?.attach(this, state)

        return dialog
    }

    private fun setupViews(context: Context, view: View) {
        // Recurrence list
        val rcv: RecyclerView = view.findViewById(R.id.rp_list_rcv)
        rcv.layoutManager = LinearLayoutManager(context)
        rcv.adapter = Adapter()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)

        state.putParcelable("settings", settings)
        state.putLong("startDate", startDate)
        state.putParcelable("selectedRecurrence", selectedRecurrence)

        presenter?.saveState(state)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Detach the presenter
        presenter?.detach()
        presenter = null
    }

    override fun onCancel(dialog: DialogInterface) {
        presenter?.onCancel()
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.rp_item_list, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            presenter?.onBindItemView(holder, position)
        }

        override fun getItemCount() = presenter?.itemCount ?: 0
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), ItemView {

        val label: RadioButton = view.findViewById(R.id.rp_list_item_label)

        init {
            view.setOnClickListener {
                presenter?.onItemClicked(bindingAdapterPosition)
            }
        }

        override fun bindRecurrenceView(
            formatter: RecurrenceFormatter,
            recurrence: Recurrence,
            startDate: Long,
            checked: Boolean
        ) {
            label.text = formatter.format(requireContext(), recurrence, startDate)
            label.isChecked = checked
        }

        override fun bindCustomView() {
            label.setText(R.string.rp_list_custom)
        }
    }

    override fun exit() {
        dismiss()
    }

    override fun setRecurrenceResult(recurrence: Recurrence) {
        getCallback<RecurrenceListCallback>()?.onRecurrencePresetSelected(recurrence)
    }

    override fun setCustomResult() {
        getCallback<RecurrenceListCallback>()?.onRecurrenceCustomClicked()
    }

    override fun setCancelResult() {
        getCallback<RecurrenceListCallback>()?.onRecurrenceListDialogCancelled()
    }

    public companion object {
        /**
         * Create a new instance of the dialog with [settings].
         * More settings can be set with the returned dialog instance later.
         */
        @JvmStatic
        public fun newInstance(settings: RecurrencePickerSettings): RecurrenceListDialog {
            val dialog = RecurrenceListDialog()
            dialog.settings = settings
            return dialog
        }
    }
}
