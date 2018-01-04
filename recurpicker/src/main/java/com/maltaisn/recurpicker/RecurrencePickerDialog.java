package com.maltaisn.recurpicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;

import java.text.DateFormat;

public class RecurrencePickerDialog extends DialogFragment implements RecurrencePickerSettings {

    private static final String TAG = RecurrencePickerDialog.class.getSimpleName();

    private RecurrencePickerView picker;

    private Recurrence recurrence;
    private long startDate;

    private DateFormat endDateFormat;
    private DateFormat optionListDateFormat;
    private int maxEndCount = RecurrencePickerView.DEFAULT_MAX_END_COUNT;
    private int maxFrequency = RecurrencePickerView.DEFAULT_MAX_FREQUENCY;
    private long maxEndDate = RecurrencePickerView.DEFAULT_MAX_END_DATE;
    private int defaultEndCount = RecurrencePickerView.DEFAULT_END_COUNT;
    private boolean defaultEndDateUsePeriod = RecurrencePickerView.DEFAULT_END_DATE_USE_PERIOD;
    private int defaultEndDateInterval = RecurrencePickerView.DEFAULT_END_DATE_INTERVAL;
    private boolean optionListEnabled = RecurrencePickerView.DEFAULT_OPTION_LIST_ENABLED;
    private boolean creatorEnabled = RecurrencePickerView.DEFAULT_CREATOR_ENABLED;
    private boolean showDoneButtonInList = RecurrencePickerView.DEFAULT_SHOW_DONE_IN_LIST;
    private boolean showHeaderInList = RecurrencePickerView.DEFAULT_SHOW_HEADER_IN_LIST;
    private boolean showCancelBtn = RecurrencePickerView.DEFAULT_SHOW_CANCEL_BTN;
    private int enabledPeriods = RecurrencePickerView.DEFAULT_ENABLED_PERIODS;
    private int enabledEndTypes = RecurrencePickerView.DEFAULT_ENABLED_END_TYPES;
    private Recurrence[] optionListDefaults = null;
    private CharSequence[] optionListDefaultsTitle = null;

    private static final int CREATOR_TRANSITION_DURATION = 200;

    /**
     * Create a dialog to customize the recurrence
     * @return the created dialog
     */
    public static RecurrencePickerDialog newInstance() {
        return new RecurrencePickerDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // On configuration change, onCreateDialog is always called
        // A new picker is created every time and its state is restored from the old picker state
        // This way the new picker as an updated context which solves the memory leak
        // made if the same picker is kept after every configuration change

        RecurrencePickerView newPicker = (RecurrencePickerView) LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_picker, null);
        newPicker.setOnRecurrenceSelectedListener(new RecurrencePickerView.OnRecurrenceSelectedListener() {
            @Override
            public void onRecurrenceSelected(Recurrence r) {
                dismiss();
                try {
                    ((RecurrenceSelectedCallback) getActivity()).onRecurrenceSelected(r);
                } catch (Exception e) {
                    // Interface callback is not implemented in activity
                }
            }

            @Override
            public void onCancelled(Recurrence r) {
                dismiss();
                try {
                    ((RecurrenceSelectedCallback) getActivity()).onCancelled(r);
                } catch (Exception e) {
                    // Interface callback is not implemented in activity
                }
            }
        });
        newPicker.setOnCreatorShownListener(new RecurrencePickerView.OnCreatorShownListener() {
            @Override
            public void onCreatorShown() {
                // Briefly hide the dialog while it is rearranging its view because that looks weird
                getDialog().hide();
                final Handler handler  = new Handler();
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

        return builder.create();
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
        try {
            ((RecurrenceSelectedCallback) getActivity()).onCancelled(recurrence);
        } catch (Exception e) {
            // Interface callback is not implemented in activity
        }
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
        void onRecurrenceSelected(Recurrence r);
        void onCancelled(Recurrence r);
    }

}

