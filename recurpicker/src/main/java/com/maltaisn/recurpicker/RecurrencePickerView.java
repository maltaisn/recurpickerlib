package com.maltaisn.recurpicker;


import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;

public class RecurrencePickerView extends LinearLayout implements RecurrencePickerSettings {

    private static final String TAG = RecurrencePickerView.class.getSimpleName();

    private boolean restoredState;

    private RelativeLayout headerLayout;
    private TextView headerTitle;
    private Spinner recurPeriodSpin;
    private Switch repeatSwitch;
    
    private LinearLayout optionListLayout;
    private LinearLayout[] optionListItems;
    private LinearLayout creatorLayout;

    private LinearLayout freqLayout;
    private EditText freqEdit;
    private TextView freqEventLabel;

    private LinearLayout weekButtonLayout1;
    private LinearLayout weekButtonLayout2;
    private ToggleButton[] weekButtons;

    private RadioGroup monthlySettingsGroup;
    private RadioButton sameWeekRadio;
    private RadioButton lastDayRadio;

    private LinearLayout endLayout;
    private Spinner endTypeSpin;
    private EditText endDateEdit;
    private EditText endCountEdit;
    private TextView endValueLabel;
    
    private Button cancelBtn;
    private Button doneBtn;

    private int[] optionItemTextColor;

    private boolean creatorShown;
    private int selectedOption;
    private Recurrence recurrence;
    private long startDate;
    private Calendar endDate;
    private int endCount;

    @Nullable private OnRecurrenceSelectedListener listener;
    @Nullable private OnCreatorShownListener creatorListener;

    // Additional settings
    public static final int MAX_FIELD_VALUE = 999999999;
    public static final int DEFAULT_MAX_END_COUNT = 999;
    public static final int DEFAULT_MAX_FREQUENCY = 99;
    public static final int DEFAULT_MAX_END_DATE = -1;
    public static final int DEFAULT_END_COUNT = 5;
    public static final boolean DEFAULT_END_DATE_USE_PERIOD = true;
    public static final int DEFAULT_END_DATE_INTERVAL = 3;
    public static final boolean DEFAULT_SKIP_IN_LIST = false;
    public static final boolean DEFAULT_SHOW_DONE_IN_LIST = false;
    public static final boolean DEFAULT_SHOW_HEADER_IN_LIST = true;
    public static final boolean DEFAULT_SHOW_CANCEL_BTN = false;

    private DateFormat endDateFormat;
    private DateFormat optionListDateFormat;
    private int maxEndCount;
    private int maxFrequency;
    private long maxEndDate;
    private int defaultEndCount;
    private boolean defaultEndDateUsePeriod;
    private int defaultEndDateInterval;
    private boolean skipOptionList;
    private boolean showDoneButtonInList;
    private boolean showHeaderInList;
    private boolean showCancelBtn;

    private Calendar poolCal;  // Used to prevent many instantiations of Calendar


    public RecurrencePickerView(Context context) {
        this(context, null, R.attr.recurrencePickerStyle, R.style.RecurrencePickerStyle);
    }

    public RecurrencePickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.recurrencePickerStyle, R.style.RecurrencePickerStyle);
    }

    public RecurrencePickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.RecurrencePickerStyle);
    }

    public RecurrencePickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(createThemeWrapper(context, R.attr.recurrencePickerStyle, R.style.RecurrencePickerStyle),
                attrs, defStyleAttr, defStyleRes);
        initLayout();
    }

    // This method is actually needed for styling but I don't know what it does exactly, see SublimePicker
    @SuppressLint("RestrictedApi")
    private static Context createThemeWrapper(Context context, int styleAttr, int defaultStyle) {
        final TypedArray ta = context.obtainStyledAttributes(new int[]{styleAttr});
        int style = ta.getResourceId(0, defaultStyle);
        ta.recycle();
        return new ContextThemeWrapper(context, style);
    }

    private void initLayout() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_picker, this);

        // Get colors from theme
        final TypedArray ta = getContext().obtainStyledAttributes(R.styleable.RecurrencePickerView);
        CharSequence[] optionListItemsText;
        CharSequence[] periodSpinnerItemsText;
        CharSequence[] endSpinnerItemsText;
        CharSequence[] endSpinnerItemsTextAbbr;
        CharSequence[] weekButtonsText;
        try {
            optionItemTextColor = new int[]{
                    ta.getColor(R.styleable.RecurrencePickerView_optionItemSelectedColor, 0),
                    ta.getColor(R.styleable.RecurrencePickerView_optionItemUnselectedColor, 0),
            };

            optionListItemsText = ta.getTextArray(R.styleable.RecurrencePickerView_optionListItemsText);
            periodSpinnerItemsText = ta.getTextArray(R.styleable.RecurrencePickerView_periodSpinnerItemsText);
            endSpinnerItemsText = ta.getTextArray(R.styleable.RecurrencePickerView_endSpinnerItemsText);
            endSpinnerItemsTextAbbr = ta.getTextArray(R.styleable.RecurrencePickerView_endSpinnerItemsTextAbbr);
            weekButtonsText = ta.getTextArray(R.styleable.RecurrencePickerView_weekButtonsText);
        } finally {
            ta.recycle();
        }
        // Find views
        headerLayout = findViewById(R.id.header_layout);
        headerTitle = findViewById(R.id.header_title);
        recurPeriodSpin = findViewById(R.id.spin_recur_type);
        repeatSwitch = findViewById(R.id.switch_repeat);

        optionListLayout = findViewById(R.id.option_list_layout);
        creatorLayout = findViewById(R.id.creator_layout);

        freqLayout = findViewById(R.id.freq_layout);
        freqEdit = findViewById(R.id.edit_freq);
        freqEventLabel = findViewById(R.id.text_freq_event);

        weekButtonLayout1 = findViewById(R.id.week_button_row1);
        weekButtonLayout2 = findViewById(R.id.week_button_row2);

        monthlySettingsGroup = findViewById(R.id.radiogroup_monthly_options);
        sameWeekRadio = findViewById(R.id.radio_same_week);
        lastDayRadio = findViewById(R.id.radio_last_day);

        endLayout = findViewById(R.id.recur_end_layout);
        endTypeSpin = findViewById(R.id.spin_end_type);
        endDateEdit = findViewById(R.id.edit_end_date);
        endCountEdit = findViewById(R.id.edit_end_count);
        endValueLabel = findViewById(R.id.text_end_value);

        cancelBtn = findViewById(R.id.cancel_btn);
        doneBtn = findViewById(R.id.done_btn);

        // Create the recurrence list
        optionListItems = new LinearLayout[7];
        for (int i = 0; i < 7; i++) {
            optionListItems[i] = (LinearLayout) inflater.inflate(R.layout.item_option, null);
            optionListLayout.addView(optionListItems[i]);
            if (i > 0) ((TextView) optionListItems[i].findViewById(R.id.text)).setText(optionListItemsText[i-1]);

            final int j = i;
            optionListItems[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (j != selectedOption) {  // If selection changed
                        if (j == 6) {
                            if (recurrence.getPeriod() == Recurrence.NONE) {
                                // If current recurrence is "does not repeat" create a daily recurrence
                                // if "does not repeat" option is selected, otherwise create a
                                // recurrence of the selected option
                                recurrence = new Recurrence(recurrence.getStartDate(),
                                        selectedOption == 1 ? Recurrence.DAILY : selectedOption - 2);
                            }

                            if (creatorListener != null) creatorListener.onCreatorShown();
                            changeMode(true);

                        } else {
                            // Change option items text color
                            ((TextView) optionListItems[j].findViewById(R.id.text))
                                    .setTextColor(optionItemTextColor[0]);
                            ((TextView) optionListItems[selectedOption].findViewById(R.id.text))
                                    .setTextColor(optionItemTextColor[1]);

                            // Change option items check icon
                            optionListItems[selectedOption].findViewById(R.id.icon_check).setVisibility(View.INVISIBLE);  // Hide last selected check
                            optionListItems[j].findViewById(R.id.icon_check).setVisibility(View.VISIBLE);  // Show new check

                            selectedOption = j;

                            // Call listener if done button is not used for that
                            if (!showDoneButtonInList) selectDefaultOption(selectedOption);
                        }
                    }
                }
            });
        }

        // Set up recurrence type spinner
        CharSeqAdapter recurPeriodAdapter = new CharSeqAdapter(getContext(),
                R.layout.item_period, R.layout.item_period_dropdown,
                periodSpinnerItemsText, periodSpinnerItemsText);
        recurPeriodSpin.setAdapter(recurPeriodAdapter);
        recurPeriodSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // Change period
                String text = freqEdit.getText().toString();
                freqEdit.setText(text.isEmpty() ? "1" : text);  // Will also update the frequency unit

                weekButtonLayout1.setVisibility(position == Recurrence.WEEKLY ? View.VISIBLE : View.GONE);
                if (weekButtonLayout2 != null)
                    weekButtonLayout2.setVisibility(position == Recurrence.WEEKLY ? View.VISIBLE : View.GONE);
                monthlySettingsGroup.setVisibility(position == Recurrence.MONTHLY ? View.VISIBLE : View.GONE);

                updateDoneButtonEnabled();
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // Set up days of week list
        weekButtons = new ToggleButton[7];
        int[] weekBtnId = new int[]{R.id.week_btn_1, R.id.week_btn_2, R.id.week_btn_3,
                R.id.week_btn_4, R.id.week_btn_5, R.id.week_btn_6, R.id.week_btn_7};
        for (int i = 0; i < 7; i++) {
            weekButtons[i] = findViewById(weekBtnId[i]);
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
                R.layout.item_end_type, R.layout.item_end_type_dropdown,
                endSpinnerItemsText, endSpinnerItemsTextAbbr);
        endTypeAdapter.setDropDownViewResource(R.layout.item_end_type);
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
                            if (Recurrence.isOnSameDayOrAfter(endDate, poolCal))
                                endDate.setTimeInMillis(maxEndDate);
                        }
                    }

                    endDateEdit.setText(endDateFormat.format(endDate.getTime()));

                } else if (position == Recurrence.END_BY_COUNT) {
                    endCountEdit.setText(String.valueOf(endCount));
                }

                updateDoneButtonEnabled();
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // Set up end date and end count inputs
        endCountEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                updateDoneButtonEnabled();
                if (text.isEmpty()) return;

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
                endValueLabel.setText(MessageFormat.format(getContext().getString(R.string.event), endCount));
            }
        });

        endDateEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        endDate.set(year, month, day);
                        endDateEdit.setText(endDateFormat.format(endDate.getTime()));
                    }
                }, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(startDate);  // Event cannot stop recurring before it begins
                if (maxEndDate != -1) dialog.getDatePicker().setMaxDate(maxEndDate);
                dialog.show();
            }
        });

        // Set up frequency input
        freqEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                updateDoneButtonEnabled();
                if (text.isEmpty()) return;

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
                String unitFormat = getResources().getStringArray(R.array.recur_units)
                        [recurPeriodSpin.getSelectedItemPosition()];
                freqEventLabel.setText(MessageFormat.format(unitFormat, freq));
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
                                if (weekButtons[i].isChecked()) days += 1 << (i + 1);
                            }
                            recurrence.setWeeklySetting(days);
                        } else if (period == Recurrence.MONTHLY) {
                            int option = monthlySettingsGroup.indexOfChild(findViewById(
                                    monthlySettingsGroup.getCheckedRadioButtonId()));
                            recurrence.setMonthlySetting(option);
                        }

                        int endType = endTypeSpin.getSelectedItemPosition();
                        if (endType == Recurrence.END_BY_DATE)
                            recurrence.setEndByDate(endDate.getTimeInMillis());
                        else if (endType == Recurrence.END_BY_COUNT) recurrence.setEndByCount(endCount);
                    } else {
                        recurrence = new Recurrence(startDate, Recurrence.NONE);
                    }

                    if (listener != null) listener.onRecurrenceSelected(recurrence);

                    if (!skipOptionList) changeMode(false);  // Go back to option list
                } else {
                    selectDefaultOption(selectedOption);
                }
            }
        });

        cancelBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onCancelled(recurrence);
            }
        });

        // Set default settings
        if (!restoredState) {
            creatorShown = false;
            setRecurrence(null, 0);
            endDateFormat = android.text.format.DateFormat.getDateFormat(getContext());  // System default format
            optionListDateFormat = endDateFormat;
            setMaxEventCount(DEFAULT_MAX_END_COUNT);
            setMaxFrequency(DEFAULT_MAX_FREQUENCY);
            setMaxEndDate(DEFAULT_MAX_END_DATE);
            setDefaultEndCount(DEFAULT_END_COUNT);
            setDefaultEndDate(DEFAULT_END_DATE_USE_PERIOD, DEFAULT_END_DATE_INTERVAL);
            setSkipOptionList(DEFAULT_SKIP_IN_LIST);
            setShowDoneButtonInOptionList(DEFAULT_SHOW_DONE_IN_LIST);
            setShowHeaderInOptionList(DEFAULT_SHOW_HEADER_IN_LIST);
            setShowCancelButton(DEFAULT_SHOW_CANCEL_BTN);

            updateMode();
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
            sameWeekRadio.setText(recurrence.getSameDayOfSameWeekString(getContext()));

            // Show "on last day of month" radio if start date is on last day of month
            poolCal.setTimeInMillis(recurrence.getStartDate());
            boolean isOnLastDayOfMonth = poolCal.get(Calendar.DAY_OF_MONTH) == poolCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            lastDayRadio.setVisibility(isOnLastDayOfMonth ? View.VISIBLE : View.GONE);

            // Select days of week matching recurrence's settings
            if (recurrence.getPeriod() == Recurrence.WEEKLY) {
                for (int i = 0; i < 7; i++) {
                    weekButtons[i].setChecked(recurrence.isRepeatedOnDayOfWeek(i+1));
                }
            } else {
                // If weekly is not the current period, set the default for when it will be selected
                // Default is repeat on the same day of week as start date
                poolCal.setTimeInMillis(recurrence.getStartDate());
                int day = poolCal.get(Calendar.DAY_OF_WEEK)-1;
                for (int i = 0; i < 7; i++) {
                    weekButtons[i].setChecked(i == day);
                }
            }

            // Select radio matching recurrence's settings
            if (recurrence.getPeriod() == Recurrence.MONTHLY) {
                monthlySettingsGroup.check(monthlySettingsGroup.getChildAt(recurrence.getDaySetting()).getId());
            } else {
                // If monthly is not the current period, set the default for when it will be selected
                monthlySettingsGroup.check(R.id.radio_same_day);
            }

            // Set up end type and values
            long end = recurrence.getEndDate();
            if (end != -1) {
                if (endDate == null) endDate = Calendar.getInstance();
                endDate.setTimeInMillis(end);
            }
            endCount = recurrence.getEndCount();
            if (endCount == -1) endCount = defaultEndCount;
            endTypeSpin.setSelection(recurrence.getEndType());

        } else {
            // Select the list item matching recurrence and deselect last one
            ((TextView) optionListItems[selectedOption].findViewById(R.id.text))
                    .setTextColor(optionItemTextColor[1]);

            if (recurrence.isDefault()) {
                int selection = recurrence.getPeriod() + 2;
                if (selectedOption != selection) {
                    optionListItems[0].setVisibility(View.GONE);
                    optionListItems[selectedOption].findViewById(R.id.icon_check).setVisibility(View.INVISIBLE);  // Hide last selected check
                    selectedOption = selection;
                    optionListItems[selectedOption].findViewById(R.id.icon_check).setVisibility(View.VISIBLE);  // Show new check
                }
            } else {
                optionListItems[0].setVisibility(View.VISIBLE); // Show top item in list
                ((TextView) optionListItems[0].findViewById(R.id.text)).setText(recurrence.format(getContext(), optionListDateFormat));
                optionListItems[selectedOption].findViewById(R.id.icon_check).setVisibility(View.INVISIBLE);  // Hide last selected check
                selectedOption = 0;
                optionListItems[0].findViewById(R.id.icon_check).setVisibility(View.VISIBLE);  // Show new check
            }

            // Change selected item text color
            ((TextView) optionListItems[selectedOption].findViewById(R.id.text))
                    .setTextColor(optionItemTextColor[0]);
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
            if (freqEdit.getText().toString().isEmpty())
                enabled = false;
            else if (endTypeSpin.getSelectedItemPosition() == Recurrence.END_BY_COUNT &&
                    endCountEdit.getText().toString().isEmpty())
                enabled = false;
            else if (recurPeriodSpin.getSelectedItemPosition() == Recurrence.WEEKLY) {
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
        if (pos != 0) {
            // If pos==0, the custom recurrence that was already selected is selected
            // pos-2 matches Recurrence.DAILY-YEARLY
            recurrence = new Recurrence(startDate, pos - 2);
        }
        if (listener != null) listener.onRecurrenceSelected(recurrence);
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
     * Set a listener to call when picker produces a recurrence or is cancelled
     * @param listener the listener
     */
    public RecurrencePickerSettings setOnRecurrenceSelectedListener(@Nullable OnRecurrenceSelectedListener listener) {
        this.listener = listener;
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
        if (startDate == 0) startDate = System.currentTimeMillis();  // No start date, use today
        if (recurrence == null) recurrence = new Recurrence(startDate, Recurrence.NONE);  // Does not repeat if not set

        this.recurrence = recurrence;
        this.startDate = startDate;

        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxFrequency(int max) {
        if (max == 0 || max < -1) {
            throw new IllegalArgumentException("Max frequency must be -1 or greater than 0");
        }

        if (max == -1 || max > MAX_FIELD_VALUE) max = MAX_FIELD_VALUE;
        maxFrequency = max;
        
        // Update max length of frequency edit text
        freqEdit.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter((int) (Math.log10(max) + 1))});

        return this;
    }

    @Override
    public RecurrencePickerSettings setMaxEventCount(int max) {
        if (max == 0 || max < -1) {
            throw new IllegalArgumentException("Max event count must be -1 or greater than 0");
        }

        if (max == -1 || max > MAX_FIELD_VALUE) max = MAX_FIELD_VALUE;
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
    public RecurrencePickerSettings setSkipOptionList(boolean skip) {
        skipOptionList = skip;
        if (skipOptionList && !creatorShown) changeMode(true);
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowDoneButtonInOptionList(boolean show) {
        showDoneButtonInList = show;
        doneBtn.setVisibility(creatorShown || showDoneButtonInList ? View.VISIBLE : View.GONE);
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowHeaderInOptionList(boolean show) {
        showHeaderInList = show;
        headerLayout.setVisibility(creatorShown || showHeaderInList ? View.VISIBLE : View.GONE);
        return this;
    }

    @Override
    public RecurrencePickerSettings setShowCancelButton(boolean show) {
        showCancelBtn = show;
        cancelBtn.setVisibility(showCancelBtn ? View.VISIBLE : View.GONE);
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
        if (endDate != null) bundle.putLong("endDate", endDate.getTimeInMillis());
        bundle.putInt("endCount", endCount);

        bundle.putInt("maxEndCount", maxEndCount);
        bundle.putInt("maxFrequency", maxFrequency);
        bundle.putLong("maxEndDate", maxEndDate);
        bundle.putInt("defaultEndCount", defaultEndCount);
        bundle.putBoolean("defaultEndDateUsePeriod", defaultEndDateUsePeriod);
        bundle.putInt("defaultEndDateInterval", defaultEndDateInterval);
        bundle.putBoolean("skipOptionList", skipOptionList);
        bundle.putBoolean("showDoneButtonInList", showDoneButtonInList);
        bundle.putBoolean("showHeaderInList", showHeaderInList);
        bundle.putBoolean("showCancelBtn", showCancelBtn);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            restoredState = true;

            Bundle bundle = (Bundle) state;

            boolean mode = bundle.getBoolean("creatorShown");
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
            skipOptionList = bundle.getBoolean("skipOptionList");
            showDoneButtonInList = bundle.getBoolean("showDoneButtonInList");
            showHeaderInList = bundle.getBoolean("showHeaderInList");
            showCancelBtn = bundle.getBoolean("showCancelBtn");

            changeMode(mode);
            setShowCancelButton(showCancelBtn);
            
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    public interface OnRecurrenceSelectedListener {
        void onRecurrenceSelected(Recurrence r);
        void onCancelled(Recurrence r);
    }

    public interface OnCreatorShownListener {
        void onCreatorShown();
    }

    private class CharSeqAdapter extends ArrayAdapter<CharSequence>{

        private final CharSequence[] selected;
        private int selectedResId;
        private int dropdownResId;

        CharSeqAdapter(Context context, int selectedResId, int dropdownResId,
                       CharSequence[] dropdownItems, CharSequence[] selectedItems) {
            super(context, selectedResId, dropdownItems);
            this.selectedResId = selectedResId;
            this.dropdownResId = dropdownResId;
            this.selected = selectedItems;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView tv = getCustomView(parent, selectedResId);
            tv.setText(selected[position]);
            return tv;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView tv = getCustomView(parent, dropdownResId);
            tv.setText(getItem(position));
            return tv;
        }

        TextView getCustomView(ViewGroup parent, int resId) {
            return (TextView) LayoutInflater.from(getContext()).inflate(resId, parent, false);
        }
    }

}
