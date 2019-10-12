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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.maltaisn.recurpicker.list.RecurrenceListContract.ItemView
import com.maltaisn.recurpicker.list.RecurrenceListContract.Presenter


/**
 * TODO
 */
class RecurrenceListDialog : DialogFragment(), RecurrenceListContract.View {

    private lateinit var themeContext: Context
    private var presenter: Presenter? = null

    /**
     * The dialog settings.
     */
    override lateinit var settings: RecurrencePickerSettings
        private set

    /**
     *
     */
    override val startDate: Long = Recurrence.DATE_NONE

    /**
     *
     */
    override var selectedRecurrence: Recurrence? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Wrap recurrence picker theme to context
        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.recurrencePickerStyle))
        val style = ta.getResourceId(0, R.style.RecurrencePickerStyle)
        ta.recycle()
        themeContext = ContextThemeWrapper(context, style)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Attach the presenter
        presenter = RecurrenceListPresenter()
        presenter?.attach(this, savedInstanceState)

        // Create the dialog
        val builder = MaterialAlertDialogBuilder(themeContext)
        val view = LayoutInflater.from(themeContext).inflate(
                R.layout.rp_dialog_recur_list, null, false)
        builder.setView(view)

        // Recurrence list
        val rcv: RecyclerView = view.findViewById(R.id.rp_recur_list_rcv)
        rcv.layoutManager = LinearLayoutManager(themeContext)
        rcv.adapter = Adapter()

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter?.saveState(outState)
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
                        .inflate(R.layout.rp_item_recur_list, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            presenter?.onBindItemView(holder, position)
        }

        override fun getItemCount() = presenter?.itemCount ?: 0
    }

    private inner class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view), ItemView {

        val label: RadioButton = view.findViewById(R.id.rp_recur_list_item_label)

        init {
            view.setOnClickListener {
                presenter?.onItemClicked(adapterPosition)
            }
        }

        override fun bindRecurrenceView(formatter: RecurrenceFormatter,
                                        recurrence: Recurrence,
                                        startDate: Long,
                                        checked: Boolean) {
            label.text = formatter.format(requireContext(), recurrence)
            label.isChecked = checked
        }

        override fun bindCustomView() {
            label.setText(R.string.rp_custom)
        }
    }

    override fun exit() {
        dismiss()
    }

    override fun setRecurrenceResult(recurrence: Recurrence) {
        callback?.onRecurrencePresetSelected(recurrence)
    }

    override fun setCustomResult() {
        callback?.onRecurrenceCustomClicked()
    }

    override fun setCancelResult() {
        callback?.onRecurrenceListDialogCancelled()
    }

    private val callback: Callback?
        get() = (parentFragment as? Callback)
                ?: (targetFragment as? Callback)
                ?: (activity as? Callback)


    /**
     * Interface to be implemented by either the parent fragment of the dialog, the target fragment
     * or the parent activity. If none of these implements it, there won't be any callback.
     */
    interface Callback {
        /**
         * Called if a [recurrence] preset is selected in the list.
         */
        fun onRecurrencePresetSelected(recurrence: Recurrence)

        /**
         * Called if the "Custom..." item is selected in the list.
         * This is a good place to show the recurrence picker.
         */
        fun onRecurrenceCustomClicked()

        /**
         * Called if the recurrence list dialog is cancelled, either by
         * a click outside or by a back press.
         */
        fun onRecurrenceListDialogCancelled()
    }


    companion object {
        /**
         * Create a new instance of the dialog with [settings].
         * The previously selected [recurrence] can be passed, or `null` if there's none selected.
         * FIXME what if user wants to reuse dialog instance? have a way to set selected recurrence
         */
        @JvmStatic
        fun newInstance(settings: RecurrencePickerSettings,
                        recurrence: Recurrence? = null): RecurrenceListDialog {
            val dialog = RecurrenceListDialog()
            dialog.settings = settings
            dialog.selectedRecurrence = recurrence
            return dialog
        }
    }

}
