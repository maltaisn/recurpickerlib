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

package com.maltaisn.recurpicker;

import android.content.Context;
import android.content.res.Resources;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.core.os.ConfigurationCompat;

public class RecurrenceFormat {

    private static final String TAG = RecurrenceFormat.class.getSimpleName();

    private Resources res;
    private DateFormat dateFormat;

    private Calendar calendar;

    /**
     * Create a new recurrence formatter
     * @param context    any context
     * @param dateFormat date format for the end date, can be null but must be set before formatting
     */
    public RecurrenceFormat(Context context, DateFormat dateFormat) {
        res = context.getResources();
        this.dateFormat = dateFormat;

        calendar = Calendar.getInstance();
    }

    /**
     * Set date format used for formatting the end date
     * @param dateFormat date format
     */
    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Format a recurrence to a string
     * @param r recurrence to format
     * @return the formatted string
     */
    public String format(Recurrence r) {
        // Generate first part of the text -> ex: Repeats every 2 days
        StringBuilder recurSb = new StringBuilder();
        int period = r.getPeriod();
        int freq = r.getFrequency();

        // Base text: does not repeat, every [freq] day/week/month/year.
        String baseText = "";
        switch (period) {
            case Recurrence.NONE:
                baseText = res.getString(R.string.rp_format_none);
                break;
            case Recurrence.DAILY:
                baseText = res.getQuantityString(R.plurals.rp_format_day, freq);
                break;
            case Recurrence.WEEKLY:
                baseText = res.getQuantityString(R.plurals.rp_format_week, freq);
                break;
            case Recurrence.MONTHLY:
                baseText = res.getQuantityString(R.plurals.rp_format_month, freq);
                break;
            case Recurrence.YEARLY:
                baseText = res.getQuantityString(R.plurals.rp_format_year, freq);
                break;
        }
        recurSb.append(String.format(baseText.replace("|", ""), freq));

        // Day setting
        if (!r.isDefault()) {
            if (period == Recurrence.WEEKLY) {
                // Make a list of days of week
                StringBuilder weekOptionStr = new StringBuilder();
                if (r.getDaySetting() == Recurrence.EVERY_DAY_OF_WEEK) {
                    // on every day of the week
                    weekOptionStr.append(res.getString(R.string.rp_format_weekly_all));
                } else {
                    // on [Sun, Mon, Wed, ...]
                    String[] daysAbbr = res.getStringArray(R.array.rp_days_of_week_abbr);
                    for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                        if (r.isRepeatedOnDaysOfWeek(1 << day)) {
                            weekOptionStr.append(daysAbbr[day - 1]);
                            weekOptionStr.append(", ");
                        }
                    }
                    weekOptionStr.delete(weekOptionStr.length() - 2, weekOptionStr.length());  // Remove extra separator
                }
                recurSb.append(' ');
                recurSb.append(res.getString(R.string.rp_format_weekly_option, weekOptionStr.toString()));

            } else if (period == Recurrence.MONTHLY) {
                recurSb.append(" (");
                switch (r.getDaySetting()) {
                    case Recurrence.SAME_DAY_OF_MONTH:
                        // on the same day of each month
                        recurSb.append(res.getString(R.string.rp_format_monthly_same_day));
                        break;

                    case Recurrence.SAME_DAY_OF_WEEK:
                        // on every [nth] [Sunday]
                        recurSb.append(getSameDayOfSameWeekString(r.getStartDate()));
                        break;

                    case Recurrence.LAST_DAY_OF_MONTH:
                        // on the last day of each month
                        recurSb.append(res.getString(R.string.rp_format_monthly_last_day));
                        break;
                }
                recurSb.append(")");
            }
        }

        // End type
        int endType = r.getEndType();
        if (endType != Recurrence.END_NEVER) {
            recurSb.append("; ");
            if (endType == Recurrence.END_BY_DATE) {
                // until [date]
                recurSb.append(res.getString(R.string.rp_format_end_date,
                        dateFormat.format(new Date(r.getEndDate()))));
            } else {
                // for [endCount] event(s)
                int endCount = r.getEndCount();
                recurSb.append(res.getQuantityString(R.plurals.rp_format_end_count, endCount, endCount));
            }
        }

        return recurSb.toString();
    }

    /**
     * Get the text for a date to display on a monthly recurrence repeated on the same day of week of same week
     * @param date date to get it for
     * @return eg: "on third Sunday" or "on last Friday"
     */
    String getSameDayOfSameWeekString(long date) {
        calendar.setTimeInMillis(date);

        Locale locale = ConfigurationCompat.getLocales(res.getConfiguration()).get(0);
        String[] daysStr = res.getStringArray(R.array.rp_format_monthly_same_week);
        String[] numbersStr = res.getStringArray(R.array.rp_format_monthly_ordinal);
        String weekNbStr = numbersStr[calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH) - 1];
        return String.format(locale, daysStr[calendar.get(Calendar.DAY_OF_WEEK) - 1], weekNbStr);
    }

}
