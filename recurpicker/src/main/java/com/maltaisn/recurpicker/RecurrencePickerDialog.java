/*
 * Copyright (c) 2018 Nicolas Maltais
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.maltaisn.recurpicker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;

import java.text.DateFormat;

@SuppressWarnings("unused")
public class RecurrencePickerDialog extends AppCompatDialogFragment implements RecurrencePickerSettings {

    private static final String TAG = RecurrencePickerDialog.class.getSimpleName();

    private RecurrencePickerView picker;

    private Recurrence recurrence;
    private long startDate;

    private DateFormat endDateFormat;
    private DateFormat optionListDateFormat;

    private int maxEndCount = DEFAULT_MAX_END_COUNT;
    private int maxFrequency = DEFAULT_MAX_FREQUENCY;
    private long maxEndDate = DEFAULT_MAX_END_DATE;
    private int defaultEndCount = DEFAULT_END_COUNT;
    private boolean defaultEndDateUsePeriod = DEFAULT_END_DATE_USE_PERIOD;
    private int defaultEndDateInterval = DEFAULT_END_DATE_INTERVAL;
    private boolean optionListEnabled = DEFAULT_OPTION_LIST_ENABLED;
    private boolean creatorEnabled = DEFAULT_CREATOR_ENABLED;
    private boolean showDoneButtonInList = DEFAULT_SHOW_DONE_IN_LIST;
    private boolean showHeaderInList = DEFAULT_SHOW_HEADER_IN_LIST;
    private boolean showCancelBtn = DEFAULT_SHOW_CANCEL_BTN;
    private int enabledPeriods = DEFAULT_ENABLED_PERIODS;
    private int enabledEndTypes = DEFAULT_ENABLED_END_TYPES;
    private Recurrence[] optionListDefaults = DEFAULT_OPTION_LIST_DEFAULTS;
    private CharSequence[] optionListDefaultsTitle = DEFAULT_OPTION_LIST_TITLES;

    private static final int CREATOR_TRANSITION_DURATION = 200;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // On configuration change, onCreateDialog is always called
        // A new picker is created every time and its state is restored from the old picker state
        // This way the new picker as an updated context which solves the memory leak
        // made if the same picker is kept after every configuration change

        @SuppressLint("InflateParams")
        RecurrencePickerView newPicker = (RecurrencePickerView) LayoutInflater.from(getActivity())
                .inflate(R.layout.rp_dialog_picker, null);
        newPicker.setIsInDialog(true);

        newPicker.setOnRecurrenceSelectedListener(new RecurrencePickerView.OnRecurrenceSelectedListener() {
            @Override
            public void onRecurrenceSelected(Recurrence r) {
                dismiss();

                RecurrenceSelectedCallback callback = getCallback();
                if (callback != null) callback.onRecurrencePickerSelected(r);
            }
        });
        newPicker.setOnRecurrencePickerCancelledListener(new RecurrencePickerView.OnRecurrencePickerCancelledListener() {
            @Override
            public void onRecurrencePickerCancelled(Recurrence r) {
                dismiss();

                RecurrenceSelectedCallback callback = getCallback();
                if (callback != null) callback.onRecurrencePickerCancelled(r);
            }
        });

        newPicker.setOnCreatorShownListener(new RecurrencePickerView.OnCreatorShownListener() {
            @Override
            public void onRecurrenceCreatorShown() {
                // Briefly hide the dialog while it is rearranging its view because that looks weird
                getDialog().hide();
                final Handler handler = new Handler();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        getDialog().show();
                    }
                };
                handler.postDelayed(runnable, CREATOR_TRANSITION_DURATION);
            }
        });

        if (savedInstanceState == null || picker == null) {
            newPicker.setDateFormat(endDateFormat, optionListDateFormat)
                    .setMaxEventCount(maxEndCount)
                    .setMaxFrequency(maxFrequency)
                    .setMaxEndDate(maxEndDate)
                    .setDefaultEndCount(defaultEndCount)
                    .setDefaultEndDate(defaultEndDateUsePeriod, defaultEndDateInterval)
                    .setEnabledModes(optionListEnabled, creatorEnabled)
                    .setShowDoneButtonInOptionList(showDoneButtonInList)
                    .setShowHeaderInOptionList(showHeaderInList)
                    .setShowCancelButton(showCancelBtn)
                    .setEnabledPeriods(enabledPeriods)
                    .setEnabledEndTypes(enabledEndTypes)
                    .setOptionListDefaults(optionListDefaults, optionListDefaultsTitle)
                    .setRecurrence(recurrence, startDate);
            newPicker.updateMode();
        } else {
            newPicker.onRestoreInstanceState(picker.onSaveInstanceState());
        }
        picker = newPicker;
        builder.setView(picker);

        Dialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                picker.showDateDialogIfNeeded();
            }
        });

        return dialog;
    }

    /**
     * Get the dialog's picker view. Note than the view changes every time the dialog is shown,
     * do not keep it in memory, that's a memory leak
     * @return the dialog's RecurrencePickerView, null if not yet created
     */
    public RecurrencePickerView getPickerView() {
        return picker;
    }

    @Override
    public RecurrencePickerSettings setDateFormat(@NonNull DateFormat endDateFormat, @NonNull DateFormat optionListDateFormat) {
        this.endDateFormat = endDateFormat;
        this.optionListDateFormat = optionListDateFormat;
        return this;
    }

    @Override
    public RecurrencePickerSettings setRecurrence(@Nullable Recurrence recurrence, long startDate) {
        this.recurrence = recurrence;
        this.startDate = startDate;
        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxEventCount(int max) {
        maxEndCount = max;
        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxFrequency(int max) {
        maxFrequency = max;
        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxEndDate(long time) {
        maxEndDate = time;
        return this;
    }

    @Override
    public RecurrencePickerSettings setDefaultEndDate(boolean usePeriod, int interval) {
        defaultEndDateUsePeriod = usePeriod;
        defaultEndDateInterval = interval;
        return this;
    }

    @Override
    public RecurrencePickerSettings setDefaultEndCount(int count) {
        defaultEndCount = count;
        return this;
    }

    @Override
    public RecurrencePickerSettings setEnabledModes(boolean optionListEnabled, boolean creatorEnabled) {
        this.optionListEnabled = optionListEnabled;
        this.creatorEnabled = creatorEnabled;
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowDoneButtonInOptionList(boolean show) {
        showDoneButtonInList = show;
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowHeaderInOptionList(boolean show) {
        showHeaderInList = show;
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowCancelButton(boolean show) {
        showCancelBtn = show;
        return this;
    }

    @Override
    public RecurrencePickerSettings setEnabledPeriods(int periods) {
        enabledPeriods = periods;
        return this;
    }

    @Override
    public RecurrencePickerSettings setEnabledEndTypes(int types) {
        enabledEndTypes = types;
        return this;
    }

    @Override
    public RecurrencePickerSettings setOptionListDefaults(@Nullable Recurrence[] defaults, @Nullable CharSequence[] titles) {
        optionListDefaults = defaults;
        optionListDefaultsTitle = titles;
        return this;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        RecurrenceSelectedCallback callback = getCallback();
        if (callback != null) callback.onRecurrencePickerCancelled(recurrence);
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    public interface RecurrenceSelectedCallback {
        void onRecurrencePickerSelected(Recurrence r);
        void onRecurrencePickerCancelled(Recurrence r);
    }

    @Nullable
    private RecurrenceSelectedCallback getCallback() {
        try {
            if (getTargetFragment() != null) {
                return (RecurrenceSelectedCallback) getTargetFragment();
            } else {
                return (RecurrenceSelectedCallback) getActivity();
            }
        } catch (ClassCastException e) {
            // Interface callback is not implemented in activity
            return null;
        }
    }

}

