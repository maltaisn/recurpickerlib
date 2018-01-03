package com.maltaisn.recurpickerdemo;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.maltaisn.recurpicker.Recurrence;
import com.maltaisn.recurpicker.RecurrencePickerDialog;
import com.maltaisn.recurpicker.RecurrencePickerView;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecurrencePickerDialog.RecurrenceSelectedCallback {

    private CheckBox maxFreqCheck;
    private CheckBox maxEndCountCheck;
    private EditText maxFreqValue;
    private EditText maxEndCountValue;
    private EditText defaultEndDateValue;
    private EditText defaultEndCountValue;

    private Button dialogPickerBtn;
    private TextView dialogPickerValue;
    private TextView dialogPickerNextValue;
    private ImageButton dialogPickerPreviousBtn;
    private ImageButton dialogPickerNextBtn;

    private Calendar startDate;
    private DateFormat dateFormatLong;
    private Recurrence recurrence;
    private int selectedRecur;
    private int recurCount;
    private ArrayList<Long> recurrenceList;

    private static final long DAYS_100 = 8640000000L;
    private Calendar maxEndDate;

    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        Recurrence rec = new Recurrence(0, 0);

        startDate = Calendar.getInstance();
        recurrence = new Recurrence(startDate.getTimeInMillis(), Recurrence.NONE);  // Does not repeat
        recurrenceList = new ArrayList<>();
        if (state != null) {
            recurrence = state.getParcelable("recurrence");
            startDate.setTimeInMillis(state.getLong("startDate"));
            selectedRecur = state.getInt("selectedRecur");
            recurCount = state.getInt("recurCount");
            maxEndDate = Calendar.getInstance();
            maxEndDate.setTimeInMillis(state.getLong("maxEndDate"));

            // Convert back long array to arraylist
            for (long r : state.getLongArray("recurrenceList")) {
                recurrenceList.add(r);
            }
        }

        // Get views
        final EditText startDateValue = findViewById(R.id.start_date_value);
        maxFreqCheck = findViewById(R.id.max_freq_check);
        final CheckBox maxEndDateCheck = findViewById(R.id.max_end_date_check);
        maxEndCountCheck = findViewById(R.id.max_end_count_check);
        maxFreqValue = findViewById(R.id.max_freq_value);
        final EditText maxEndDateValue = findViewById(R.id.max_end_date_value);
        maxEndCountValue = findViewById(R.id.max_end_count_value);
        final CheckBox defaultEndDateCheck = findViewById(R.id.default_end_date_use_period_check);
        final TextView defaultEndDateUnit = findViewById(R.id.default_end_date_value_unit);
        defaultEndDateValue = findViewById(R.id.default_end_date_value);
        defaultEndCountValue = findViewById(R.id.default_end_count_value);

        final CheckBox skipOptionListCheck = findViewById(R.id.skip_option_list_check);
        final CheckBox showHeaderCheck = findViewById(R.id.show_header_check);
        final CheckBox showDoneBtnCheck = findViewById(R.id.show_done_btn_check);
        final CheckBox showCancelBtnCheck = findViewById(R.id.show_cancel_btn_check);

        dialogPickerBtn = findViewById(R.id.dialog_picker_btn);
        dialogPickerValue = findViewById(R.id.dialog_picker_value);
        dialogPickerNextValue = findViewById(R.id.dialog_picker_next_value);
        dialogPickerPreviousBtn = findViewById(R.id.dialog_picker_previous_btn);
        dialogPickerNextBtn = findViewById(R.id.dialog_picker_next_btn);

        // Set the date formats
        Locale locale = getResources().getConfiguration().locale;
        dateFormatLong = new SimpleDateFormat("EEE MMM dd, yyyy", locale);  // Sun Dec 31, 2017
        final DateFormat dateFormatShort = new SimpleDateFormat("dd-MM-yyyy", locale);  // 31-12-2017

        // Start date picker edit text
        startDateValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        startDate.set(year, month, day);
                        startDateValue.setText(dateFormatLong.format(startDate.getTimeInMillis()));

                        // Also needs to update current recurrence
                        if (recurrence != null) {
                            recurrence.setStartDate(startDate.getTimeInMillis());
                            selectRecurrence(recurrence);  // Update interface
                        }

                        // Check if end date is before start date
                        if (maxEndDate != null && !Recurrence.isOnSameDayOrAfter(maxEndDate, startDate)) {
                            // Change end date to 100 days after start date
                            maxEndDate.setTimeInMillis(startDate.getTimeInMillis() + DAYS_100);
                            if (maxEndDateCheck.isChecked()) maxEndDateValue
                                    .setText(dateFormatLong.format(maxEndDate.getTime()));
                        }
                    }
                }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });
        startDateValue.setText(dateFormatLong.format(startDate.getTime()));

        // Max frequency views
        maxFreqCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maxFreqValue.setEnabled(isChecked);
                updateDialogPickerBtnEnabled();
            }
        });
        maxFreqCheck.setChecked(RecurrencePickerView.DEFAULT_MAX_FREQUENCY != -1);
        maxFreqValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) return;

                int freq = Integer.valueOf(text);
                if (freq == 0) maxFreqValue.getText().replace(0, 1, "1");
            }
        });
        maxFreqValue.setText(String.valueOf(RecurrencePickerView.DEFAULT_MAX_FREQUENCY));
        maxFreqValue.setEnabled(maxFreqCheck.isChecked());

        // Max end count views
        maxEndCountCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maxEndCountValue.setEnabled(isChecked);
                updateDialogPickerBtnEnabled();
            }
        });
        maxEndCountCheck.setChecked(RecurrencePickerView.DEFAULT_MAX_END_COUNT != -1);
        maxEndCountValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) return;

                int count = Integer.valueOf(text);
                if (count == 0) {
                    maxFreqValue.getText().replace(0, 1, "1");
                } else {
                    // Change default end count value if larger than maximum
                    String defaultCountStr = defaultEndCountValue.getText().toString();
                    if (!defaultCountStr.isEmpty()) {
                        int defaultCount = Integer.valueOf(defaultCountStr);
                        if (defaultCount > count) defaultEndCountValue.setText(String.valueOf(count));
                    }
                }
            }
        });
        maxEndCountValue.setText(String.valueOf(RecurrencePickerView.DEFAULT_MAX_END_COUNT));
        maxEndCountValue.setEnabled(maxEndCountCheck.isChecked());

        // Max end date picker edit text
        maxEndDateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maxEndDateValue.setEnabled(isChecked);
                if (isChecked) {
                    if (maxEndDate == null) {
                        // No max end date set, make it 100 days after start date
                        maxEndDate = Calendar.getInstance();
                        maxEndDate.setTimeInMillis(startDate.getTimeInMillis() + DAYS_100);
                    }
                    maxEndDateValue.setText(dateFormatLong.format(maxEndDate.getTime()));
                } else {
                    maxEndDateValue.setText(getString(R.string.max_end_date_none));
                }
            }
        });
        maxEndDateCheck.setChecked(RecurrencePickerView.DEFAULT_MAX_END_DATE != -1);
        if (!maxEndDateCheck.isChecked()) maxEndDateValue.setText(getString(R.string.max_end_date_none));
        maxEndDateValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        maxEndDate.set(year, month, day);
                        maxEndDateValue.setText(dateFormatLong.format(maxEndDate.getTime()));
                    }
                }, maxEndDate.get(Calendar.YEAR), maxEndDate.get(Calendar.MONTH), maxEndDate.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
                dialog.show();
            }
        });
        maxEndDateValue.setEnabled(maxEndDateCheck.isChecked());
        if (maxEndDateCheck.isChecked()) {
            maxEndDate.setTimeInMillis(RecurrencePickerView.DEFAULT_MAX_END_DATE);
            maxEndCountValue.setText(dateFormatLong.format(maxEndDate.getTime()));
        }

        // Default end date views
        defaultEndDateCheck.setChecked(RecurrencePickerView.DEFAULT_END_DATE_USE_PERIOD);
        defaultEndDateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String text = defaultEndDateValue.getText().toString();
                int count = 1;
                if (!text.isEmpty()) count = Integer.valueOf(text);
                defaultEndDateUnit.setText(MessageFormat.format(getString(defaultEndDateCheck.isChecked() ?
                        R.string.default_end_date_periods : R.string.default_end_date_days), count));
            }
        });
        defaultEndDateValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) return;

                int count = Integer.valueOf(text);
                if (count == 0) {
                    count = 1;
                    maxFreqValue.getText().replace(0, 1, "1");
                }

                defaultEndDateUnit.setText(MessageFormat.format(getString(defaultEndDateCheck.isChecked() ?
                        R.string.default_end_date_periods : R.string.default_end_date_days), count));
            }
        });
        defaultEndDateValue.setText(String.valueOf(RecurrencePickerView.DEFAULT_END_DATE_INTERVAL));
        defaultEndDateUnit.setText(MessageFormat.format(getString(defaultEndDateCheck.isChecked() ?
                R.string.default_end_date_periods : R.string.default_end_date_days), RecurrencePickerView.DEFAULT_END_DATE_INTERVAL));

        // Default end count view
        defaultEndCountValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) return;

                int count = Integer.valueOf(text);
                if (count == 0) {
                    defaultEndCountValue.getText().replace(0, 1, "1");
                } else {
                    // Change default end count if greater than maximum end count
                    String maxCountStr = maxEndCountValue.getText().toString();
                    if (!maxCountStr.isEmpty()) {
                        int maxCount = Integer.valueOf(maxCountStr);
                        if (count > maxCount) defaultEndCountValue.getText().replace(
                                0, text.length(), String.valueOf(maxCount));
                    }
                }
            }
        });
        defaultEndCountValue.setText(String.valueOf(RecurrencePickerView.DEFAULT_END_COUNT));

        // Set up checkbox options
        skipOptionListCheck.setChecked(RecurrencePickerView.DEFAULT_SKIP_IN_LIST);
        showHeaderCheck.setChecked(RecurrencePickerView.DEFAULT_SHOW_HEADER_IN_LIST);
        showDoneBtnCheck.setChecked(RecurrencePickerView.DEFAULT_SHOW_DONE_IN_LIST);
        showCancelBtnCheck.setChecked(RecurrencePickerView.DEFAULT_SHOW_CANCEL_BTN);

        // Set up dialog recurrence picker
        final RecurrencePickerDialog pickerDialog = RecurrencePickerDialog.newInstance();
        pickerDialog.setDateFormat(dateFormatShort, dateFormatLong);
        dialogPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the settings
                pickerDialog.setMaxFrequency(maxFreqCheck.isChecked() ?
                        Integer.valueOf(maxFreqValue.getText().toString()) : -1);
                pickerDialog.setMaxEndDate(maxEndDateCheck.isChecked() ? maxEndDate.getTimeInMillis() : -1);
                pickerDialog.setMaxEventCount(maxEndCountCheck.isChecked() ?
                        Integer.valueOf(maxEndCountValue.getText().toString()) : -1);
                pickerDialog.setDefaultEndDate(defaultEndDateCheck.isChecked(),
                        Integer.valueOf(defaultEndDateValue.getText().toString()));
                pickerDialog.setDefaultEndCount(Integer.valueOf(defaultEndCountValue.getText().toString()));
                pickerDialog.setSkipOptionList(skipOptionListCheck.isChecked());
                pickerDialog.setShowHeaderInOptionList(showHeaderCheck.isChecked());
                pickerDialog.setShowDoneButtonInOptionList(showDoneBtnCheck.isChecked());
                pickerDialog.setShowCancelButton(showCancelBtnCheck.isChecked());
                pickerDialog.setRecurrence(recurrence, startDate.getTimeInMillis());

                // Not necessary, but if a cancel button is shown, often dialog isn't cancelable
                pickerDialog.setCancelable(!showCancelBtnCheck.isChecked());

                // Show the recurrence dialog
                pickerDialog.show(getFragmentManager(), "recur_picker_dialog");
            }
        });

        // Set up recurrence list views
        dialogPickerValue.setText(recurrence.format(this, dateFormatLong));

        dialogPickerPreviousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Find previous recurrence in the list
                selectedRecur--;
                Date date = new Date(recurrenceList.get(selectedRecur));
                dialogPickerNextValue.setText(MessageFormat.format(getString(R.string.dialog_picker_value),
                        selectedRecur+1, dateFormatLong.format(date)));

                setButtonEnabled(dialogPickerPreviousBtn, selectedRecur > 0);
                setButtonEnabled(dialogPickerNextBtn, true);
            }
        });

        dialogPickerNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show already computed next recurrence
                selectedRecur++;
                Date date = new Date(recurrenceList.get(selectedRecur));
                dialogPickerNextValue.setText(MessageFormat.format(getString(R.string.dialog_picker_value),
                        selectedRecur+1, dateFormatLong.format(date)));

                setButtonEnabled(dialogPickerPreviousBtn, true);

                if (recurCount == selectedRecur+1) {
                    // If this is last recurrence, disable next button
                    setButtonEnabled(dialogPickerNextBtn, false);
                } else if (recurrenceList.size() == selectedRecur+1) {
                    // If not already done, compute next recurrence based on current one
                    List<Long> next = recurrence.findRecurrencesBasedOn(recurrenceList.get(selectedRecur),
                            selectedRecur+1, -1, 1);
                    if (next.size() == 0) {
                        recurCount = recurrenceList.size();
                        setButtonEnabled(dialogPickerNextBtn,false);
                    } else {
                        recurrenceList.add(next.get(0));
                    }
                }
            }
        });

        // Set up interface for current recurrence
        updateUIToSelection();
    }

    private void updateDialogPickerBtnEnabled() {
        dialogPickerBtn.setEnabled(
                (!maxFreqCheck.isChecked() || !maxFreqValue.getText().toString().isEmpty()) &&
                (!maxEndCountCheck.isChecked() || !maxEndCountValue.getText().toString().isEmpty()) &&
                (!defaultEndDateValue.getText().toString().isEmpty()) &&
                (!defaultEndCountValue.getText().toString().isEmpty())
        );
    }

    private void updateUIToSelection() {
        int visibility = recurrence.getPeriod() == Recurrence.NONE ? View.GONE : View.VISIBLE;
        dialogPickerPreviousBtn.setVisibility(visibility);
        dialogPickerNextBtn.setVisibility(visibility);

        setButtonEnabled(dialogPickerNextBtn, selectedRecur+1 < recurrenceList.size());
        setButtonEnabled(dialogPickerPreviousBtn, selectedRecur > 0);
        if (recurrenceList.size() == 0) {
            dialogPickerNextValue.setText(getString(R.string.dialog_picker_value_none));
        } else {
            dialogPickerNextValue.setText(MessageFormat.format(getString(R.string.dialog_picker_value),
                    selectedRecur+1, dateFormatLong.format(recurrenceList.get(selectedRecur))));
        }
    }

    private void selectRecurrence(Recurrence r) {
        recurrence = r;
        dialogPickerValue.setText(recurrence.format(MainActivity.this, dateFormatLong));

        // Compute first two recurrences
        List<Long> next = recurrence.findRecurrences(-1, 2);
        recurrenceList = new ArrayList<>();
        selectedRecur = 0;
        recurCount = -1;
        if (next.size() == 0) {
            // No recurrences found
            recurCount = 0;
        } else {
            // One or more found
            recurrenceList.add(next.get(0));
            if (next.size() == 1) {
                recurCount = 1;
            } else {
                // Two or more found
                recurrenceList.add(next.get(1));
            }
        }

        updateUIToSelection();
    }

    private void setButtonEnabled(ImageButton btn, boolean enabled) {
        btn.setEnabled(enabled);
        btn.setAlpha(enabled ? 1f : 0.3f);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("startDate", startDate.getTimeInMillis());
        state.putParcelable("recurrence", recurrence);
        state.putInt("selectedRecur", selectedRecur);
        state.putInt("recurCount", recurCount);
        state.putLong("maxEndDate", maxEndDate == null ? - 1 : maxEndDate.getTimeInMillis());

        // Save recurrence list, have to convert it to primitive array
        long[] list = new long[recurrenceList.size()];
        for (int i = 0; i < list.length; i++) {
            list[i] = recurrenceList.get(i);
        }
        state.putLongArray("recurrenceList", list);
    }

    @Override
    public void onRecurrenceSelected(Recurrence r) {
        selectRecurrence(r);
    }

    @Override
    public void onCancelled(Recurrence r) {
        // Nothing happens
    }
}
