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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.maltaisn.recurpicker.R
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.getPickerContextWrapper
import com.maltaisn.recurpicker.list.RecurrenceListDialog
import com.maltaisn.recurpicker.picker.RecurrencePickerContract.Presenter

/**
 * Fragment used to create a custom recurrence with nearly all available options.
 * Only some options of monthly recurrences are not available, like recurring on a day other than start date.
 * This fragment can be shown directly or after [RecurrenceListDialog] was shown.
 * Note: due to the MVP architecture, some interface methods are public but shouldn't be used.
 */
public class RecurrencePickerFragment private constructor(
    private val delegate: RecurrencePickerDelegate
) : Fragment(),
    RecurrencePickerContract.View by delegate,
    DateDialogFragment.Callback by delegate {

    public constructor() : this(RecurrencePickerDelegate())

    private var presenter: Presenter? = null

    private var contextWrapper: Context? = null

    private var _binding: Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        // Wrap recurrence picker theme to context
        contextWrapper = getPickerContextWrapper()
        val localInflater = inflater.cloneInContext(contextWrapper)

        // Inflate layout
        _binding = Binding(localInflater.inflate(R.layout.rp_fragment_picker, container, false))
        setupViews()

        // Back press callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    exit()
                }
            })

        return binding.root
    }

    private fun setupViews() {
        // Toolbar
        binding.toolbar.setOnMenuItemClickListener {
            presenter?.onConfirm()
            true
        }
        binding.toolbar.setNavigationOnClickListener {
            presenter?.onCancel()
        }

        setupEndViews()
    }

    private fun setupEndViews() {
        // End never
        val endNeverClick = View.OnClickListener { presenter?.onEndNeverClicked() }
        binding.endNeverView.setOnClickListener(endNeverClick)
        binding.endNeverRadio.setOnClickListener(endNeverClick)

        // End by date
        val endDateClick = View.OnClickListener { presenter?.onEndDateClicked() }
        binding.endDateView.setOnClickListener(endDateClick)
        binding.endDateRadio.setOnClickListener(endDateClick)

        // End by date
        val endCountClick = View.OnClickListener { presenter?.onEndCountClicked() }
        binding.endCountView.setOnClickListener(endCountClick)
        binding.endCountRadio.setOnClickListener(endCountClick)
    }

    override fun onViewStateRestored(state: Bundle?) {
        super.onViewStateRestored(state)

        val presenter = RecurrencePickerPresenter()
        this.presenter = presenter

        delegate.attach(presenter, this, contextWrapper!!, binding.root)
        delegate.setupViews()
        if (state != null) {
            delegate.onRestoreInstanceState(state)
        }

        presenter.attach(this, state)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        delegate.onSaveInstanceState(state)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        contextWrapper = null

        // Detach the presenter
        presenter?.detach()
        presenter = null

        delegate.detach()
    }

    override fun exit() {
        parentFragmentManager.popBackStack()
    }

    override fun setEndNeverChecked(checked: Boolean) {
        binding.endNeverRadio.isChecked = checked
    }

    override fun setEndDateChecked(checked: Boolean) {
        binding.endDateRadio.isChecked = checked
        binding.endDateView.visibility = if (checked) View.INVISIBLE else View.VISIBLE
    }

    override fun setEndCountChecked(checked: Boolean) {
        binding.endCountRadio.isChecked = checked
        binding.endCountView.visibility = if (checked) View.INVISIBLE else View.VISIBLE
    }

    private class Binding(val root: View) {
        val toolbar: Toolbar = root.findViewById(R.id.rp_toolbar)
        val endNeverView: View = root.findViewById(R.id.rp_picker_end_never_view)
        val endNeverRadio: RadioButton = root.findViewById(R.id.rp_picker_end_never_radio)
        val endDateView: View = root.findViewById(R.id.rp_picker_end_date_view)
        val endDateRadio: RadioButton = root.findViewById(R.id.rp_picker_end_date_radio)
        val endCountView: View = root.findViewById(R.id.rp_picker_end_count_view)
        val endCountRadio: RadioButton = root.findViewById(R.id.rp_picker_end_count_radio)
    }

    public companion object {
        /**
         * Create a new instance of the fragment with [settings].
         * More settings can be set with the returned fragment instance later.
         */
        @JvmStatic
        public fun newInstance(settings: RecurrencePickerSettings): RecurrencePickerFragment {
            val dialog = RecurrencePickerFragment()
            dialog.settings = settings
            return dialog
        }
    }
}
