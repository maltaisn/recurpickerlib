/*
 * Copyright (c) 2019 Nicolas Maltais
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
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
public class RecurrencePickerView extends LinearLayout implements RecurrencePickerSettings {

    private static final String TAG = RecurrencePickerView.class.getSimpleName();

    private static final int[] WEEK_BTN_IDS = new int[]{
            R.id.rp_btn_day_1,
            R.id.rp_btn_day_2,
            R.id.rp_btn_day_3,
            R.id.rp_btn_day_4,
            R.id.rp_btn_day_5,
            R.id.rp_btn_day_6,
            R.id.rp_btn_day_7
    };

    private boolean isInDialog;

    private DatePickerDialog dateDialog;
    private boolean dateDialogShown;
    private Runnable restoreDateDialogRunnable;

    private LinearLayout headerLayout;
    private TextView headerTitle;
    private Spinner recurPeriodSpin;
    private SwitchCompat repeatSwitch;

    private LinearLayout optionListLayout;
    private LinearLayout creatorLayout;
    private EditText freqEdit;
    private ToggleButton[] weekButtons;
    private RadioGroup monthlySettingsGroup;
    private RadioButton sameWeekRadio;
    private RadioButton lastDayRadio;
    private LinearLayout endLayout;
    private Spinner endTypeSpin;
    private EditText endDateEdit;
    private EditText endCountEdit;
    private Button cancelBtn;
    private Button doneBtn;

    private OnClickListener optionListItemsClick;

    private boolean initialized;
    private boolean creatorShown;
    private int selectedOption;
    private Recurrence recurrence;
    private long startDate;
    private Calendar endDate;
    private int endCount;

    private @Nullable
    OnRecurrenceSelectedListener listener;
    private @Nullable
    OnRecurrencePickerCancelledListener cancelListener;
    private @Nullable
    OnCreatorShownListener creatorListener;

    private int[] optionItemTextColor;

    private DateFormat endDateFormat;
    private DateFormat optionListDateFormat;
    private int maxEndCount;
    private int maxFrequency;
    private long maxEndDate;
    private int defaultEndCount;
    private boolean defaultEndDateUsePeriod;
    private int defaultEndDateInterval;
    private boolean optionListEnabled;
    private boolean creatorEnabled;
    private boolean showDoneButtonInList;
    private boolean showHeaderInList;
    private boolean showCancelBtn;
    private int enabledPeriods;
    private int enabledEndTypes;
    private Recurrence[] optionListDefaults;
    private CharSequence[] optionListDefaultsTitle;

    private RecurrenceFormat formatter;
    private Calendar poolCal;  // Used to prevent many instantiations of Calendar


    public RecurrencePickerView(Context context) {
        this(context, null, R.attr.recurrencePickerStyle);
    }

    public RecurrencePickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.recurrencePickerStyle);
    }

    public RecurrencePickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(createThemeWrapper(context, R.attr.recurrencePickerStyle, R.style.RecurrencePickerStyle),
                attrs, defStyleAttr);
        initLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecurrencePickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(createThemeWrapper(context, R.attr.recurrencePickerStyle, defStyleRes),
                attrs, defStyleAttr, defStyleRes);
        initLayout();
    }

    @SuppressLint("RestrictedApi")
    private static Context createThemeWrapper(Context context, int styleAttr, int defaultStyle) {
        final TypedArray ta = context.obtainStyledAttributes(new int[]{styleAttr});
        int style = ta.getResourceId(0, defaultStyle);
        ta.recycle();
        return new ContextThemeWrapper(context, style);
    }

    private void initLayout() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.rp_view_picker, this);

        formatter = new RecurrenceFormat(getContext(), null);

        // Get colors from theme
        final TypedArray ta = getContext().obtainStyledAttributes(R.styleable.RecurrencePickerView);
        CharSequence[] periodSpinnerItemsText;
        CharSequence[] endSpinnerItemsText;
        CharSequence[] endSpinnerItemsTextAbbr;
        CharSequence[] weekButtonsText;
        CharSequence optionListNoneText;
        CharSequence optionListCustomText;
        try {
            optionItemTextColor = new int[]{
                    ta.getColor(R.styleable.RecurrencePickerView_rpOptionItemSelectedColor, 0),
                    ta.getColor(R.styleable.RecurrencePickerView_rpOptionItemUnselectedColor, 0),
            };

            periodSpinnerItemsText = ta.getTextArray(R.styleable.RecurrencePickerView_rpPeriodSpinnerItemsText);
            endSpinnerItemsText = ta.getTextArray(R.styleable.RecurrencePickerView_rpEndSpinnerItemsText);
            endSpinnerItemsTextAbbr = ta.getTextArray(R.styleable.RecurrencePickerView_rpEndSpinnerItemsTextAbbr);
            weekButtonsText = ta.getTextArray(R.styleable.RecurrencePickerView_rpWeekButtonsText);
            optionListNoneText = ta.getText(R.styleable.RecurrencePickerView_rpOptionListNoneText);
            optionListCustomText = ta.getText(R.styleable.RecurrencePickerView_rpOptionListCustomText);

            if (optionListNoneText == null) {
                optionListNoneText = formatter.format(new Recurrence(0, Recurrence.NONE));
            }

        } finally {
            ta.recycle();
        }
        // Find views
        headerLayout = findViewById(R.id.rp_layout_header);
        headerTitle = findViewById(R.id.rp_txv_header_title);
        recurPeriodSpin = findViewById(R.id.rp_spn_period);
        repeatSwitch = findViewById(R.id.rp_sw_enabled);

        optionListLayout = findViewById(R.id.rp_layout_default_list);
        creatorLayout = findViewById(R.id.rp_layout_creator);

        final LinearLayout freqLayout = findViewById(R.id.rp_layout_freq);
        freqEdit = findViewById(R.id.rp_edt_freq);
        final TextView freqLabel = findViewById(R.id.rp_txv_freq_suffix);
        final TextView freqEventLabel = findViewById(R.id.rp_txv_freq_prefix);

        final LinearLayout weekButtonLayout1 = findViewById(R.id.rp_layout_weekbtn_1);
        final LinearLayout weekButtonLayout2 = findViewById(R.id.rp_layout_weekbtn_2);

        monthlySettingsGroup = findViewById(R.id.rp_rg_monthly_options);
        sameWeekRadio = findViewById(R.id.rp_rb_same_week);
        lastDayRadio = findViewById(R.id.rp_rb_last_day);

        endLayout = findViewById(R.id.rp_layout_end);
        endTypeSpin = findViewById(R.id.rp_spn_end);
        endDateEdit = findViewById(R.id.rp_edt_end_date);
        endCountEdit = findViewById(R.id.rp_edt_end_count);
        final TextView endValueLabel = findViewById(R.id.rp_txv_end_prefix);

        cancelBtn = findViewById(R.id.rp_btn_cancel);
        doneBtn = findViewById(R.id.rp_btn_done);

        // Create the recurrence list
        optionListItemsClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = (int) view.getTag();
                if (pos != selectedOption) {  // If selection changed
                    changeSelection(pos);

                    // Call listener if done button is not used for that
                    if (!showDoneButtonInList) {
                        selectDefaultOption(selectedOption);
                    }
                }
            }
        };

        for (int i = 0; i < 3; i++) {
            @SuppressLint("InflateParams")
            LinearLayout item = (LinearLayout) inflater.inflate(R.layout.rp_item_option, null);
            item.setTag(i);
            TextView itemText = item.findViewById(R.id.rp_txv_title);
            if (i == 0) {
                // Recurrence created in creator
                item.setOnClickListener(optionListItemsClick);
            } else if (i == 1) {
                // Does not repeat option
                itemText.setText(optionListNoneText);
                item.setOnClickListener(optionListItemsClick);
            } else {
                // Custom... item
                itemText.setText(optionListCustomText);
                item.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (recurrence.getPeriod() == Recurrence.NONE) {
                            int period = 0;
                            if (selectedOption == 1) {
                                // If current recurrence is "does not repeat" create a recurrence with the shortest enabled period
                                for (int i = 0; i < 4; i++) {
                                    if ((enabledPeriods & (1 << i)) == (1 << i)) {
                                        period = i;
                                        break;
                                    }
                                }
                            } else {
                                // Otherwise create a recurrence of the selected option
                                period = selectedOption - 2;
                            }
                            recurrence = new Recurrence(recurrence.getStartDate(), period);
                        }

                        if (creatorListener != null) {
                            creatorListener.onRecurrenceCreatorShown();
                        }
                        changeMode(true);
                    }
                });
            }

            optionListLayout.addView(item);
        }

        // Set up recurrence type spinner
        CharSeqAdapter recurPeriodAdapter = new CharSeqAdapter(getContext(),
                R.layout.rp_item_period, R.layout.rp_item_period_dropdown,
                periodSpinnerItemsText, periodSpinnerItemsText) {
            @Override
            boolean isDropdownViewVisible(int position) {
                // Only show enabled periods
                return (enabledPeriods & (1 << position)) == (1 << position);
            }
        };
        recurPeriodSpin.setAdapter(recurPeriodAdapter);
        recurPeriodSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // Change period
                String text = freqEdit.getText().toString();
                freqEdit.setText(text.isEmpty() ? "1" : text);  // Will also update the frequency unit

                weekButtonLayout1.setVisibility(position == Recurrence.WEEKLY ? View.VISIBLE : View.GONE);
                if (weekButtonLayout2 != null) {
                    weekButtonLayout2.setVisibility(position == Recurrence.WEEKLY ? View.VISIBLE : View.GONE);
                }
                monthlySettingsGroup.setVisibility(position == Recurrence.MONTHLY ? View.VISIBLE : View.GONE);

                updateDoneButtonEnabled();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Set up days of week list
        weekButtons = new ToggleButton[7];
        for (int i = 0; i < 7; i++) {
            weekButtons[i] = findViewById(WEEK_BTN_IDS[i]);
            weekButtons[i].setBackgroundDrawable(new WeekBtnDrawable(getContext()));
            weekButtons[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    updateDoneButtonEnabled();
                }
            });

            CharSequence day = weekButtonsText[i];
            weekButtons[i].setTextOn(day);
            weekButtons[i].setTextOff(day);
            weekButtons[i].setText(day);
        }

        // Set up end type spinner
        CharSeqAdapter endTypeAdapter = new CharSeqAdapter(getContext(),
                R.layout.rp_item_end_type, R.layout.rp_item_end_type_dropdown,
                endSpinnerItemsText, endSpinnerItemsTextAbbr) {
            @Override
            boolean isDropdownViewVisible(int position) {
                // Only show enabled end types
                return (enabledEndTypes & (1 << position)) == (1 << position);
            }
        };
        endTypeAdapter.setDropDownViewResource(R.layout.rp_item_end_type);
        endTypeSpin.setAdapter(endTypeAdapter);
        endTypeSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                endValueLabel.setVisibility(position == Recurrence.END_BY_COUNT ? View.VISIBLE : View.GONE);
                endCountEdit.setVisibility(position == Recurrence.END_BY_COUNT ? View.VISIBLE : View.GONE);
                endDateEdit.setVisibility(position == Recurrence.END_BY_DATE ? View.VISIBLE : View.GONE);

                if (position == Recurrence.END_BY_DATE) {
                    if (endDate == null) {
                        // No end date set, create a default one from specified settings
                        endDate = Calendar.getInstance();
                        endDate.setTimeInMillis(startDate);
                        int period = recurPeriodSpin.getSelectedItemPosition();
                        if (defaultEndDateUsePeriod && period != Recurrence.DAILY) {
                            if (period == Recurrence.WEEKLY) {
                                endDate.add(Calendar.DATE, defaultEndDateInterval * 7);
                            } else if (period == Recurrence.MONTHLY) {
                                endDate.add(Calendar.MONTH, defaultEndDateInterval);
                            } else {
                                endDate.add(Calendar.YEAR, defaultEndDateInterval);
                            }
                        } else {
                            endDate.add(Calendar.DATE, defaultEndDateInterval);
                        }

                        // Check if default end date is not after maximum end date (if set)
                        if (maxEndDate != -1) {
                            poolCal.setTimeInMillis(maxEndDate);
                            if (Recurrence.isOnSameDayOrAfter(endDate, poolCal)) {
                                endDate.setTimeInMillis(maxEndDate);
                            }
                        }
                    }

                    endDateEdit.setText(endDateFormat.format(endDate.getTime()));

                } else if (position == Recurrence.END_BY_COUNT) {
                    endCountEdit.setText(String.valueOf(endCount));
                }

                updateDoneButtonEnabled();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Set up end date and end count inputs
        endCountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                updateDoneButtonEnabled();
                if (text.isEmpty()) {
                    return;
                }

                endCount = Integer.valueOf(text);
                if (endCount == 0) {
                    endCount = 1;
                    editable.replace(0, 1, "1");
                } else if (maxEndCount != -1 && endCount > maxEndCount) {
                    endCount = maxEndCount;
                    editable.clear();
                    editable.append(String.valueOf(endCount));
                }

                // Change between "event" and "events"
                endValueLabel.setText(getResources().getQuantityString(R.plurals.rp_end_count_event, endCount));
            }
        });

        endDateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateDialog(endDate.get(Calendar.YEAR),
                        endDate.get(Calendar.MONTH),
                        endDate.get(Calendar.DAY_OF_MONTH));
            }
        });

        // Set up frequency input
        freqEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                updateDoneButtonEnabled();
                if (text.isEmpty()) {
                    return;
                }

                int freq = Integer.valueOf(text);
                if (freq == 0) {
                    freq = 1;
                    editable.replace(0, 1, "1");
                } else if (maxFrequency != -1 && freq > maxFrequency) {
                    freq = maxFrequency;
                    editable.clear();
                    editable.append(String.valueOf(freq));
                }

                // Change between week and weeks (or other time unit)
                int formatId = 0;
                switch (recurPeriodSpin.getSelectedItemPosition()) {
                    case 0:
                        formatId = R.plurals.rp_format_day;
                        break;
                    case 1:
                        formatId = R.plurals.rp_format_week;
                        break;
                    case 2:
                        formatId = R.plurals.rp_format_month;
                        break;
                    case 3:
                        formatId = R.plurals.rp_format_year;
                        break;
                }
                String[] parts = getResources().getQuantityString(formatId, freq)
                        .replace("%d", "").split("\\|", 2);
                freqLabel.setText(parts[0].trim());
                freqEventLabel.setText(parts[1].trim());
            }
        });

        // Set up repeat switch
        repeatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                // If switch is off, disable every view
                recurPeriodSpin.setEnabled(checked);
                for (int i = 0; i < freqLayout.getChildCount(); i++) {
                    View child = freqLayout.getChildAt(i);
                    child.setEnabled(checked);
                }
                for (ToggleButton btn : weekButtons) {
                    btn.setEnabled(checked);
                    btn.setAlpha(checked ? 1.0f : 0.5f);  // Must be set manually because we are using a state drawable
                }
                for (int i = 0; i < monthlySettingsGroup.getChildCount(); i++) {
                    View child = monthlySettingsGroup.getChildAt(i);
                    child.setEnabled(checked);
                }
                for (int i = 0; i < endLayout.getChildCount(); i++) {
                    View child = endLayout.getChildAt(i);
                    child.setEnabled(checked);
                }

                // Only enable switch and done button
                repeatSwitch.setEnabled(true);
                updateDoneButtonEnabled();
            }
        });

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create a recurrence from the inputs
                if (creatorShown) {
                    if (repeatSwitch.isChecked()) {
                        int period = recurPeriodSpin.getSelectedItemPosition();
                        int freq = Integer.valueOf(freqEdit.getText().toString());

                        recurrence = new Recurrence(startDate, period).setFrequency(freq);

                        if (period == Recurrence.WEEKLY) {
                            int days = 0;
                            for (int i = 0; i < 7; i++) {
                                if (weekButtons[i].isChecked()) {
                                    days += 1 << (i + 1);
                                }
                            }
                            recurrence.setWeeklySetting(days);
                        } else if (period == Recurrence.MONTHLY) {
                            int option = monthlySettingsGroup.indexOfChild(findViewById(
                                    monthlySettingsGroup.getCheckedRadioButtonId()));
                            recurrence.setMonthlySetting(option);
                        }

                        int endType = endTypeSpin.getSelectedItemPosition();
                        if (endType == Recurrence.END_BY_DATE) {
                            recurrence.setEndByDate(endDate.getTimeInMillis());
                        } else if (endType == Recurrence.END_BY_COUNT) {
                            recurrence.setEndByCount(endCount);
                        }
                    } else {
                        recurrence = new Recurrence(startDate, Recurrence.NONE);
                    }

                    if (listener != null) {
                        listener.onRecurrenceSelected(recurrence);
                    }

                    if (optionListEnabled) {
                        // Go back to option list
                        changeMode(false);
                    }
                } else {
                    selectDefaultOption(selectedOption);
                }
            }
        });

        cancelBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cancelListener != null) {
                    cancelListener.onRecurrencePickerCancelled(recurrence);
                }
            }
        });

        // Set default settings
        if (!initialized) {
            creatorShown = false;
            setRecurrence(null, 0);

            endDateFormat = android.text.format.DateFormat.getDateFormat(getContext());  // System default format
            optionListDateFormat = endDateFormat;

            setMaxEventCount(DEFAULT_MAX_END_COUNT);
            setMaxFrequency(DEFAULT_MAX_FREQUENCY);
            setMaxEndDate(DEFAULT_MAX_END_DATE);
            setDefaultEndCount(DEFAULT_END_COUNT);
            setDefaultEndDate(DEFAULT_END_DATE_USE_PERIOD, DEFAULT_END_DATE_INTERVAL);
            setEnabledModes(DEFAULT_OPTION_LIST_ENABLED, DEFAULT_CREATOR_ENABLED);
            setShowDoneButtonInOptionList(DEFAULT_SHOW_DONE_IN_LIST);
            setShowHeaderInOptionList(DEFAULT_SHOW_HEADER_IN_LIST);
            setShowCancelButton(DEFAULT_SHOW_CANCEL_BTN);
            setEnabledPeriods(DEFAULT_ENABLED_PERIODS);
            setEnabledEndTypes(DEFAULT_ENABLED_END_TYPES);

            setOptionListDefaults(DEFAULT_OPTION_LIST_DEFAULTS, DEFAULT_OPTION_LIST_TITLES);

            updateMode();
            initialized = true;
        }

        poolCal = Calendar.getInstance();
    }

    /**
     * Update the editing mode of the recurrence picker and updates the current views to match the recurrence
     * This should be called after calling {@link RecurrencePickerView#setRecurrence(Recurrence, long)}
     * if the currently selected option needs to be updated
     */
    public void updateMode() {
        changeMode(creatorShown);
    }

    /**
     * Change the editing mode of the recurrence picker and updates the current views to match the recurrence
     * @param creatorShown if not shown, the list of default options is shown
     */
    public void changeMode(boolean creatorShown) {
        this.creatorShown = creatorShown;

        // Update views to match current recurrence
        if (creatorShown) {
            repeatSwitch.setChecked(recurrence.getPeriod() != Recurrence.NONE);

            recurPeriodSpin.setSelection(Math.max(recurrence.getPeriod(), 0), false);
            freqEdit.setText(String.valueOf(recurrence.getFrequency()));

            // Set text for "on third Sunday of month" radio according to start date
            sameWeekRadio.setText(formatter.getSameDayOfSameWeekString(recurrence.getStartDate()));

            // Show "on last day of month" radio if start date is on last day of month
            poolCal.setTimeInMillis(recurrence.getStartDate());
            boolean isOnLastDayOfMonth = poolCal.get(Calendar.DAY_OF_MONTH) == poolCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            lastDayRadio.setVisibility(isOnLastDayOfMonth ? View.VISIBLE : View.GONE);

            // Select days of week matching recurrence's settings
            if (recurrence.getPeriod() == Recurrence.WEEKLY) {
                for (int i = 0; i < 7; i++) {
                    int day = 1 << (i + 1);
                    weekButtons[i].setChecked(recurrence.isRepeatedOnDaysOfWeek(day));
                }
            } else {
                // If weekly is not the current period, set the default for when it will be selected
                // Default is repeat on the same day of week as start date
                poolCal.setTimeInMillis(recurrence.getStartDate());
                int day = poolCal.get(Calendar.DAY_OF_WEEK) - 1;
                for (int i = 0; i < 7; i++) {
                    weekButtons[i].setChecked(i == day);
                }
            }

            // Select radio matching recurrence's settings
            if (recurrence.getPeriod() == Recurrence.MONTHLY) {
                monthlySettingsGroup.check(monthlySettingsGroup.getChildAt(recurrence.getDaySetting()).getId());
            } else {
                // If monthly is not the current period, set the default for when it will be selected
                monthlySettingsGroup.check(R.id.rp_rb_same_day);
            }

            // Set up end type and values
            long end = recurrence.getEndDate();
            if (end != -1) {
                if (endDate == null) endDate = Calendar.getInstance();
                endDate.setTimeInMillis(end);
            }
            endCount = recurrence.getEndCount();
            if (endCount == -1) {
                endCount = defaultEndCount;
            }
            endTypeSpin.setSelection(recurrence.getEndType());

        } else {
            // Show or hide custom option item
            int pos = 0;
            boolean showCustom = true;

            if (recurrence.getPeriod() == Recurrence.NONE) {
                // Does not repeat
                pos = 1;
                showCustom = false;
            } else {
                // Check if selected recurrence matches one of the defaults
                for (int i = 0; i < optionListDefaults.length; i++) {
                    if (recurrence.equals(optionListDefaults[i], true)) {
                        pos = i + 2;
                        showCustom = false;
                        break;
                    }
                }
            }

            // Set up top item to selected recurrence
            if (showCustom) {
                formatter.setDateFormat(optionListDateFormat);
                optionListLayout.getChildAt(0).setVisibility(View.VISIBLE);
                ((TextView) optionListLayout.getChildAt(0).findViewById(R.id.rp_txv_title))
                        .setText(formatter.format(recurrence));
            } else {
                optionListLayout.getChildAt(0).setVisibility(View.GONE);
            }

            // Change selection
            changeSelection(pos);
        }

        // Show and hide views
        int visible = this.creatorShown ? View.GONE : View.VISIBLE;
        int invisible = this.creatorShown ? View.VISIBLE : View.GONE;

        headerLayout.setVisibility(creatorShown || showHeaderInList ? View.VISIBLE : View.GONE);
        headerTitle.setVisibility(visible);
        recurPeriodSpin.setVisibility(invisible);
        repeatSwitch.setVisibility(invisible);

        optionListLayout.setVisibility(visible);
        creatorLayout.setVisibility(invisible);

        doneBtn.setVisibility(creatorShown || showDoneButtonInList ? View.VISIBLE : View.GONE);
        updateDoneButtonEnabled();
    }

    // Called when a value changes to update the done button state
    private void updateDoneButtonEnabled() {
        boolean enabled = true;
        if (creatorShown && repeatSwitch.isChecked()) {
            if (freqEdit.getText().toString().isEmpty()) {
                enabled = false;
            } else if (endTypeSpin.getSelectedItemPosition() == Recurrence.END_BY_COUNT
                    && endCountEdit.getText().toString().isEmpty()) {
                enabled = false;
            } else if (recurPeriodSpin.getSelectedItemPosition() == Recurrence.WEEKLY) {
                // Enabled only if at least one day of week is checked
                boolean oneChecked = false;
                for (ToggleButton btn : weekButtons) {
                    if (btn.isChecked()) {
                        oneChecked = true;
                        break;
                    }
                }
                enabled = oneChecked;
            }
        }
        doneBtn.setEnabled(enabled);
    }

    private void selectDefaultOption(int pos) {
        // User selected a default option, create the recurrence
        // If pos==0, the custom recurrence that was already selected is selected
        if (pos == 1) {
            // Does not repeat
            recurrence = new Recurrence(startDate, Recurrence.NONE);
        } else if (pos > 1) {
            // Use set default for that option
            recurrence = new Recurrence(optionListDefaults[pos - 2]);
        }
        if (listener != null) {
            listener.onRecurrenceSelected(recurrence);
        }
    }

    private void changeSelection(int pos) {
        View oldItem = optionListLayout.getChildAt(selectedOption);
        View newItem = optionListLayout.getChildAt(pos);

        // Change option items text color
        ((TextView) oldItem.findViewById(R.id.rp_txv_title)).setTextColor(optionItemTextColor[1]);
        ((TextView) newItem.findViewById(R.id.rp_txv_title)).setTextColor(optionItemTextColor[0]);

        // Change option items check icon
        oldItem.findViewById(R.id.rp_imv_check).setVisibility(View.INVISIBLE);
        newItem.findViewById(R.id.rp_imv_check).setVisibility(View.VISIBLE);

        selectedOption = pos;
    }

    private void showDateDialog(int day, int month, int year) {
        dateDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                endDate.set(year, month, day);
                endDateEdit.setText(endDateFormat.format(endDate.getTime()));
            }
        }, year, month, day);
        dateDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dateDialogShown = false;
            }
        });

        dateDialog.getDatePicker().setMinDate(startDate + DateUtils.DAY_IN_MILLIS);  // Event cannot stop recurring before it begins
        if (maxEndDate != -1) {
            dateDialog.getDatePicker().setMaxDate(maxEndDate);
        }
        dateDialog.setTitle(null);  // Fixes issue with date being set as title when max date is set

        dateDialog.show();
        dateDialogShown = true;
    }

    private void setOptionListDefaultsText() {
        if (initialized) {
            for (int i = 0; i < optionListDefaults.length; i++) {
                CharSequence text;
                if (optionListDefaultsTitle == null || optionListDefaultsTitle.length <= i
                        || optionListDefaultsTitle[i] == null) {
                    formatter.setDateFormat(optionListDateFormat);
                    text = formatter.format(optionListDefaults[i]);
                } else {
                    text = optionListDefaultsTitle[i];
                }
                ((TextView) optionListLayout.getChildAt(i + 2)
                        .findViewById(R.id.rp_txv_title)).setText(text);
            }
        }
    }

    /**
     * Call this method to notice the view that it is in a dialog
     * When restoring, the view needs to know when to show the date dialog (if it was opened)
     * to prevent it from being displayed being its parent dialog.
     * @param isInDialog whether in a dialog or not
     */
    public void setIsInDialog(boolean isInDialog) {
        this.isInDialog = isInDialog;
    }

    /**
     * Call this method in your dialog's onShow listener to notice the
     * view that it can restore the date dialog.
     */
    public void showDateDialogIfNeeded() {
        if (dateDialogShown) {
            restoreDateDialogRunnable.run();
        }
    }

    /**
     * Set a listener to call when picker goes from option list to creator
     * Can be used to hide the view while it rearranges its views
     * @param listener the listener
     */
    public RecurrencePickerSettings setOnCreatorShownListener(@Nullable OnCreatorShownListener listener) {
        creatorListener = listener;
        return this;
    }

    /**
     * Set a listener to call when picker produces a recurrence
     * @param listener the listener
     */
    public RecurrencePickerSettings setOnRecurrenceSelectedListener(@Nullable OnRecurrenceSelectedListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Set the cancel lsitener to call when the picker is cancelled
     * @param listener the listener
     * @return the picker
     */
    public RecurrencePickerSettings setOnRecurrencePickerCancelledListener(@Nullable OnRecurrencePickerCancelledListener listener) {
        cancelListener = listener;
        return this;
    }

    @Override
    public RecurrencePickerSettings setDateFormat(@NonNull DateFormat endDateFormat, @NonNull DateFormat optionListDateFormat) {
        this.endDateFormat = endDateFormat;
        this.optionListDateFormat = optionListDateFormat;

        return this;
    }

    @Override
    public RecurrencePickerSettings setRecurrence(@Nullable Recurrence recurrence, long startDate) {
        if (startDate == 0) {
            startDate = System.currentTimeMillis();  // No start date, use today
        }
        if (recurrence == null) {
            recurrence = new Recurrence(startDate, Recurrence.NONE);  // Does not repeat if not set
        }

        this.recurrence = recurrence;
        this.startDate = startDate;

        if (optionListDefaults != null) {
            // Adjust defaults start dates
            for (Recurrence r : optionListDefaults) {
                r.setStartDate(startDate);

                // Setting start date might change the text too
                setOptionListDefaultsText();
            }
        }

        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxFrequency(int max) {
        if (max == maxFrequency) {
            return this;
        }

        if (max == 0 || max < -1) {
            throw new IllegalArgumentException("Max frequency must be -1 or greater than 0");
        }

        if (max == -1 || max > MAX_FIELD_VALUE) {
            max = MAX_FIELD_VALUE;
        }
        maxFrequency = max;

        // Update max length of frequency edit text
        freqEdit.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter((int) (Math.log10(max) + 1))});

        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxEventCount(int max) {
        if (max == maxEndCount) {
            return this;
        }

        if (max == 0 || max < -1) {
            throw new IllegalArgumentException("Max event count must be -1 or greater than 0");
        }

        if (max == -1 || max > MAX_FIELD_VALUE) {
            max = MAX_FIELD_VALUE;
        }
        maxEndCount = max;

        // Update max length of end count edit text
        endCountEdit.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter((int) (Math.log10(max) + 1))});

        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxEndDate(long time) {
        maxEndDate = time;
        return this;
    }

    @Override
    public RecurrencePickerSettings setDefaultEndDate(boolean usePeriod, int interval) {
        if (interval < 1) {
            throw new IllegalArgumentException("Interval must be 1 or greater");
        }

        defaultEndDateUsePeriod = usePeriod;
        defaultEndDateInterval = interval;
        return this;
    }

    @Override
    public RecurrencePickerSettings setDefaultEndCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Default end count must be 1 or greater");
        }

        defaultEndCount = count;
        return this;
    }

    @Override
    public RecurrencePickerSettings setEnabledModes(boolean optionListEnabled, boolean creatorEnabled) {
        if (this.optionListEnabled == optionListEnabled
                && this.creatorEnabled == creatorEnabled) {
            return this;
        }

        if (!optionListEnabled && !creatorEnabled) {
            throw new IllegalArgumentException("Both recurrence picker view modes cannot be disabled");
        }

        this.optionListEnabled = optionListEnabled;
        this.creatorEnabled = creatorEnabled;

        if (!optionListEnabled && !creatorShown) {
            // Option list disabled and current mode is option list, change that
            changeMode(true);
        } else if (!creatorEnabled) {
            // Hide "custom..." list item
            optionListLayout.getChildAt(optionListLayout.getChildCount() - 1).setVisibility(View.GONE);

            if (creatorShown) {
                // Creator disabled and current mode is creator, change that
                changeMode(false);
            }
        }

        return this;
    }

    @Override
    public RecurrencePickerSettings setShowDoneButtonInOptionList(boolean show) {
        if (show == showDoneButtonInList) {
            return this;
        }
        showDoneButtonInList = show;
        doneBtn.setVisibility(creatorShown || showDoneButtonInList ? View.VISIBLE : View.GONE);
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowHeaderInOptionList(boolean show) {
        if (show == showHeaderInList) {
            return this;
        }
        showHeaderInList = show;
        headerLayout.setVisibility(creatorShown || showHeaderInList ? View.VISIBLE : View.GONE);
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowCancelButton(boolean show) {
        if (show == showCancelBtn) {
            return this;
        }
        showCancelBtn = show;
        cancelBtn.setVisibility(showCancelBtn ? View.VISIBLE : View.GONE);
        return this;
    }

    @Override
    public RecurrencePickerSettings setEnabledPeriods(int periods) {
        if (periods == enabledPeriods) {
            return this;
        }

        if (periods < 1 || periods > 0b1111) {
            throw new IllegalArgumentException("Invalid enabled periods parameter");
        }

        enabledPeriods = periods;

        int count = 0;
        boolean selected = false;
        for (int i = 0; i < 4; i++) {
            boolean enabled = (enabledPeriods & (1 << i)) == (1 << i);
            if (enabled) {
                count++;
                if (!selected) {
                    selected = true;
                    recurPeriodSpin.setSelection(i);  // Set selection to shortest enabled period
                }
            }
        }
        if (count == 1) {
            // Only one item, disable spinner
            recurPeriodSpin.setOnTouchListener(new OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }

        return this;
    }

    @Override
    public RecurrencePickerSettings setEnabledEndTypes(int types) {
        if (types == enabledEndTypes) {
            return this;
        }

        if (types < 1 || types > 0b111) {
            throw new IllegalArgumentException("Invalid enabled end types parameter");
        }

        enabledEndTypes = types;

        // Hide end layout if only forever end type is enabled
        endLayout.setVisibility(enabledEndTypes == 1 ? View.GONE : View.VISIBLE);

        int count = 0;
        boolean selected = false;
        for (int i = 0; i < 4; i++) {
            boolean enabled = (enabledEndTypes & (1 << i)) == (1 << i);
            if (enabled) {
                count++;
                if (!selected) {
                    selected = true;
                    endTypeSpin.setSelection(i);  // Set selection to first enabled end type
                }
            }
        }
        if (count == 1) {
            // Only one item, disable spinner
            endTypeSpin.setOnTouchListener(new OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }

        return this;
    }

    @Override
    public RecurrencePickerSettings setOptionListDefaults(@Nullable Recurrence[] defaults, @Nullable CharSequence[] titles) {
        if (defaults != null) {
            if (defaults.length == 0) {
                throw new IllegalArgumentException("Option list default recurrences array has 0 length");
            }

            for (Recurrence r : defaults) {
                if (r == null) {
                    throw new NullPointerException("Option list default recurrences array contains a null reference");
                }
            }
        }

        boolean defaultsChanged = !initialized || optionListDefaults != DEFAULT_OPTION_LIST_DEFAULTS && defaults == null || defaults != null;
        boolean titlesChanged = optionListDefaults == DEFAULT_OPTION_LIST_DEFAULTS && titles != null || defaultsChanged;

        if (defaultsChanged) {
            if (initialized) {
                optionListLayout.removeViews(2, optionListDefaults.length);
            }

            optionListDefaults = defaults == null ? DEFAULT_OPTION_LIST_DEFAULTS : defaults;
            for (Recurrence r : optionListDefaults) {
                r.setStartDate(recurrence.getStartDate());
            }

            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0; i < optionListDefaults.length; i++) {
                @SuppressLint("InflateParams")
                LinearLayout item = (LinearLayout) inflater.inflate(R.layout.rp_item_option, null);
                item.setTag(i + 2);
                item.setOnClickListener(optionListItemsClick);

                // Add item, but before custom... item
                optionListLayout.addView(item, i + 2);
            }
        }

        if (titlesChanged) {
            optionListDefaultsTitle = titles;
            setOptionListDefaultsText();
        }

        return this;
    }

    /**
     * @return The currently selected recurrence
     */
    public Recurrence getRecurrence() {
        return recurrence;
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());

        bundle.putBoolean("creatorShown", creatorShown);
        bundle.putParcelable("recurrence", recurrence);
        bundle.putLong("startDate", startDate);
        if (endDate != null) {
            bundle.putLong("endDate", endDate.getTimeInMillis());
        }
        bundle.putInt("endCount", endCount);

        bundle.putInt("maxEndCount", maxEndCount);
        bundle.putInt("maxFrequency", maxFrequency);
        bundle.putLong("maxEndDate", maxEndDate);
        bundle.putInt("defaultEndCount", defaultEndCount);
        bundle.putBoolean("defaultEndDateUsePeriod", defaultEndDateUsePeriod);
        bundle.putInt("defaultEndDateInterval", defaultEndDateInterval);
        bundle.putBoolean("optionListEnabled", optionListEnabled);
        bundle.putBoolean("creatorEnabled", creatorEnabled);
        bundle.putBoolean("showDoneButtonInList", showDoneButtonInList);
        bundle.putBoolean("showHeaderInList", showHeaderInList);
        bundle.putBoolean("showCancelBtn", showCancelBtn);
        bundle.putInt("enabledPeriods", enabledPeriods);
        bundle.putInt("enabledEndTypes", enabledEndTypes);
        if (optionListDefaults != DEFAULT_OPTION_LIST_DEFAULTS) {
            bundle.putParcelableArray("optionListDefaults", optionListDefaults);
        }
        if (optionListDefaultsTitle != null) {
            bundle.putCharSequenceArray("optionListDefaultsTitle", optionListDefaultsTitle);
        }

        bundle.putBoolean("dateDialogShown", dateDialogShown);
        if (dateDialogShown) {
            bundle.putInt("dateDialogDay", dateDialog.getDatePicker().getDayOfMonth());
            bundle.putInt("dateDialogMonth", dateDialog.getDatePicker().getMonth());
            bundle.putInt("dateDialogYear", dateDialog.getDatePicker().getYear());
        }

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            initialized = true;

            final Bundle bundle = (Bundle) state;

            creatorShown = bundle.getBoolean("creatorShown");
            recurrence = bundle.getParcelable("recurrence");
            startDate = bundle.getLong("startDate");
            long end = bundle.getLong("endDate", -1);
            if (end != -1) {
                endDate = Calendar.getInstance();
                endDate.setTimeInMillis(end);
            }
            endCount = bundle.getInt("endCount");

            maxEndCount = bundle.getInt("maxEndCount");
            maxFrequency = bundle.getInt("maxFrequency");
            maxEndDate = bundle.getLong("maxEndDate");
            defaultEndCount = bundle.getInt("defaultEndCount");
            defaultEndDateUsePeriod = bundle.getBoolean("defaultEndDateUsePeriod");
            defaultEndDateInterval = bundle.getInt("defaultEndDateInterval");
            optionListEnabled = bundle.getBoolean("optionListEnabled");
            creatorEnabled = bundle.getBoolean("creatorEnabled");
            showDoneButtonInList = bundle.getBoolean("showDoneButtonInList");
            showHeaderInList = bundle.getBoolean("showHeaderInList");
            enabledPeriods = bundle.getInt("enabledPeriods");
            enabledEndTypes = bundle.getInt("enabledEndTypes");

            setOptionListDefaults((Recurrence[]) bundle.getParcelableArray("optionListDefaults"),
                    bundle.getCharSequenceArray("optionListDefaultsTitle"));

            dateDialogShown = bundle.getBoolean("dateDialogShown");
            if (dateDialogShown) {
                restoreDateDialogRunnable = new Runnable() {
                    @Override
                    public void run() {
                        int day = bundle.getInt("dateDialogDay");
                        int month = bundle.getInt("dateDialogMonth");
                        int year = bundle.getInt("dateDialogYear");
                        showDateDialog(day, month, year);
                    }
                };

                // If not in a dialog, no need to wait before showing the date dialog
                if (!isInDialog) {
                    restoreDateDialogRunnable.run();
                }
            }

            // Change mode, select recurrence, show header and done button if needed
            updateMode();

            setShowCancelButton(bundle.getBoolean("showCancelBtn"));

            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    public interface OnRecurrenceSelectedListener {
        void onRecurrenceSelected(Recurrence r);
    }

    public interface OnRecurrencePickerCancelledListener {
        void onRecurrencePickerCancelled(Recurrence r);
    }

    public interface OnCreatorShownListener {
        void onRecurrenceCreatorShown();
    }

    private class CharSeqAdapter extends ArrayAdapter<CharSequence> {

        private final CharSequence[] selectedItems;
        private int selectedResId;
        private int dropdownResId;

        CharSeqAdapter(Context context, int selectedResId, int dropdownResId,
                       CharSequence[] dropdownItems, CharSequence[] selectedItems) {
            super(context, selectedResId, dropdownItems);
            this.selectedResId = selectedResId;
            this.dropdownResId = dropdownResId;
            this.selectedItems = selectedItems;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView tv = getCustomView(parent, selectedResId);
            tv.setText(selectedItems[position]);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView tv;
            if (!isDropdownViewVisible(position)) {
                // This item is not visible, make random item with 0px height
                // Setting the item gone doesn't work, it's height is still there
                tv = new TextView(getContext());
                tv.setHeight(0);
            } else {
                tv = getCustomView(parent, dropdownResId);
                tv.setText(getItem(position));
            }
            return tv;
        }

        TextView getCustomView(ViewGroup parent, int resId) {
            return (TextView) LayoutInflater.from(getContext()).inflate(resId, parent, false);
        }

        boolean isDropdownViewVisible(int position) {
            return true;
        }
    }

}
