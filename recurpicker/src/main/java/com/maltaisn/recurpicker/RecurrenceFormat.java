package com.maltaisn.recurpicker;

import android.content.Context;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

public class RecurrenceFormat {

    private static final String TAG = RecurrenceFormat.class.getSimpleName();

    private Context context;
    private DateFormat dateFormat;

    private Calendar calendar;

    /**
     * Create a new recurrence formatter
     * @param context any context
     * @param dateFormat date format for the end date
     */
    public RecurrenceFormat(Context context, DateFormat dateFormat) {
        this.context = context;
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
        String[] recurFormats = context.getResources().getStringArray(R.array.rp_recur_formats);
        String recurFormat = null;
        switch (r.getPeriod()) {
            case Recurrence.NONE:
                recurFormat = recurFormats[0];  // Does not repeat
                break;

            case Recurrence.DAILY:
                recurFormat = MessageFormat.format(recurFormats[1], r.getFrequency());
                break;

            case Recurrence.WEEKLY:
                StringBuilder dayList = null;
                if (!r.isDefault()) {
                    // Make a list of days of week
                    dayList = new StringBuilder();
                    if (r.getDaySetting() == Recurrence.EVERY_DAY_OF_WEEK) {
                        dayList.append(context.getString(R.string.rp_days_of_week_list_all));
                    } else {
                        String[] daysAbbr = context.getResources().getStringArray(R.array.rp_days_of_week_abbr);
                        String listSep = context.getString(R.string.rp_days_of_week_list_sep);
                        for (int day = 1; day <= 7; day++) {
                            if (r.isRepeatedOnDaysOfWeek(1 << day)) {
                                dayList.append(daysAbbr[day - 1]);
                                dayList.append(listSep);
                            }
                        }
                        dayList.delete(dayList.length() - listSep.length(), dayList.length());  // Remove extra separator
                    }
                }
                recurFormat = MessageFormat.format(recurFormats[2], r.getFrequency(), dayList == null ? 0 : 1, dayList);
                break;

            case Recurrence.MONTHLY:
                String when = "";
                int daySetting = r.getDaySetting();
                if (!r.isDefault() && daySetting == Recurrence.SAME_DAY_OF_MONTH)
                    when = context.getString(R.string.rp_repeat_monthly_same_day);
                else if (daySetting == Recurrence.SAME_DAY_OF_WEEK) when = getSameDayOfSameWeekString(r.getStartDate());
                else if (daySetting == Recurrence.LAST_DAY_OF_MONTH)
                    when = context.getString(R.string.rp_repeat_monthly_last_day);

                recurFormat = MessageFormat.format(recurFormats[3], r.getFrequency(), when.isEmpty() ? 0 : 1, when);
                break;

            case Recurrence.YEARLY:
                recurFormat = MessageFormat.format(recurFormats[4], r.getFrequency());
                break;
            default:
                // Never happens
                recurFormat = "";
                break;
        }

        // Generate second part of the text (how recurrence ends) -> ex: until 31-12-2017
        String[] endFormats = context.getResources().getStringArray(R.array.rp_end_formats);
        String endFormat;
        switch (r.getEndType()) {
            case Recurrence.END_NEVER:
                endFormat = endFormats[0];
                break;
            case Recurrence.END_BY_DATE:
                endFormat = MessageFormat.format(endFormats[1],
                        dateFormat.format(new Date(r.getEndDate())));
                break;
            case Recurrence.END_BY_COUNT:
                endFormat = MessageFormat.format(endFormats[2], r.getEndCount());
                break;
            case Recurrence.END_BY_DATE_OR_COUNT:
                endFormat = MessageFormat.format(endFormats[3],
                        dateFormat.format(new Date(r.getEndDate())), r.getEndCount());
                break;
            default:
                // Never happens
                endFormat = "";
                break;
        }

        String result;
        if (endFormat.isEmpty()) {
            result = recurFormat;
        } else {
            result = MessageFormat.format(context.getString(R.string.rp_merge_format), recurFormat, endFormat);
        }

        return result;
    }

    /**
     * Get the text for a date to display on a monthly recurrence repeated on the same day of week of same week
     * @param date date to get it for
     * @return ex: "on third Sunday" or "on last Friday"
     */
    String getSameDayOfSameWeekString(long date) {
        calendar.setTimeInMillis(date);

        String[] daysOfWeek = context.getResources().getStringArray(R.array.rp_days_of_week);
        String[] ordinalNbs = context.getResources().getStringArray(R.array.rp_ordinal_numbers);
        String weekOrd = ordinalNbs[calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH) - 1];
        String dayOfWeek = daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1];
        return MessageFormat.format(context.getString(R.string.rp_repeat_monthly_same_week), weekOrd, dayOfWeek);
    }

}
