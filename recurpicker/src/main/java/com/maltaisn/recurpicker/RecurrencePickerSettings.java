/*
 * Copyright (c) Nicolas Maltais
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.maltaisn.recurpicker;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.DateFormat;

/**
 * As RecurrencePickerView and RecurrencePickerDialog are using the same methods
 * for the settings, this interface improves consistency
 */
public interface RecurrencePickerSettings {

    int MAX_FIELD_VALUE = 999999999;
    int DEFAULT_MAX_END_COUNT = 999;
    int DEFAULT_MAX_FREQUENCY = 99;
    int DEFAULT_MAX_END_DATE = -1;
    int DEFAULT_END_COUNT = 5;
    boolean DEFAULT_END_DATE_USE_PERIOD = true;
    int DEFAULT_END_DATE_INTERVAL = 3;
    boolean DEFAULT_OPTION_LIST_ENABLED = true;
    boolean DEFAULT_CREATOR_ENABLED = true;
    boolean DEFAULT_SHOW_DONE_IN_LIST = false;
    boolean DEFAULT_SHOW_HEADER_IN_LIST = true;
    boolean DEFAULT_SHOW_CANCEL_BTN = false;
    int DEFAULT_ENABLED_PERIODS = 0b1111;
    int DEFAULT_ENABLED_END_TYPES = 0b111;
    Recurrence[] DEFAULT_OPTION_LIST_DEFAULTS = null;
    CharSequence[] DEFAULT_OPTION_LIST_TITLES = null;

    /**
     * Set the date format to use
     * By default, the system default date format is used
     * You should call this method before {@link #setRecurrence(Recurrence, long)}
     * @param endDateFormat Date format to use for the end date
     *                      This date should usually be shorter
     * @param optionListDateFormat Date format to use for the custom item in the option list
     *                         This date should usually be more verbose
     */
    RecurrencePickerSettings setDateFormat(@NonNull DateFormat endDateFormat, @NonNull DateFormat optionListDateFormat);

    /**
     * Call this method to initialize the recurrence picker with a recurrence and a date
     * @param recurrence Recurrence to display, use null for the default "does not repeat"
     * @param startDate Starting date of recurrence to be returned, use 0 for today
     */
    RecurrencePickerSettings setRecurrence(@Nullable Recurrence recurrence, long startDate);

    /**
     * Set the maximum event count for a recurrence
     * By default, the maximum number of events is 999
     * Must be set before showing the creator or the dialog
     * @param max Maximum count, set to -1 to allow any number of events
     *            Even if set to -1, maximum is {@link RecurrencePickerView#MAX_FIELD_VALUE}
     */
    RecurrencePickerSettings setMaxEventCount(int max);

    /**
     * Set the maximum frequency for a period
     * By default, maximum frequency is 99
     * Must be set before showing the creator or the dialog
     * ex: if 10, maximum will be: every 10 days, every 10 weeks, every 10 months...
     * @param max Maximum frequency, set to -1 to allow any value
     *            Even if set to -1, maximum is {@link RecurrencePickerView#MAX_FIELD_VALUE}
     */
    RecurrencePickerSettings setMaxFrequency(int max);

    /**
     * Set the maximum end date for a recurrence
     * By default, there is no maximum end date
     * Must be set before showing the creator or the dialog
     * @param time Time in millis for a date, set to -1 for no maximum
     */
    RecurrencePickerSettings setMaxEndDate(long time);

    /**
     * Change how the end date is set relative to the start date by default
     * By default, default end date is 3 periods after start date
     * Must be set before showing the creator or the dialog
     * @param usePeriod If true, will use the currently selected period as the unit of the interval
     *                  ex: if repeating weekly and interval is 3, default end date will be 3 weeks after start date
     *                  If false, will use days as the unit of the interval
     * @param interval How many periods/days after start date to set default end date
     */
    RecurrencePickerSettings setDefaultEndDate(boolean usePeriod, int interval);

    /**
     * Set the number of events to show by default
     * By default, recurrence ends after 5 events
     * Must be set before showing the creator or the dialog
     * @param count Number of events to show by default
     */
    RecurrencePickerSettings setDefaultEndCount(int count);

    /**
     * Select which modes are enabled: default options list and recurrence creator
     * By default, both modes are enabled, and at least one of them should be
     * @param optionListEnabled whether to enable the default options list
     * @param creatorEnabled whether to enabled the recurrence creator
     */
    RecurrencePickerSettings setEnabledModes(boolean optionListEnabled, boolean creatorEnabled);

    /**
     * Set whether to show the Done button in the first screen, the list of default recurrence options,
     * or to hide it. If shown, listener won't be called when option is clicked, only when button is.
     * By default, done button is not shown in option list.
     * @param show Whether to show it or not
     */
    RecurrencePickerSettings setShowDoneButtonInOptionList(boolean show);

    /**
     * Set whether the header should be displayed in the first screen, the list of default recurrence options,
     * or not. If you wish to also hide the header in the creator mode, you can change its background color.
     * By default, header is shown.
     * @param show Whether to show it or not
     */
    RecurrencePickerSettings setShowHeaderInOptionList(boolean show);

    /**
     * Set whether to show a cancel button or not. Cancel button is placed at the left side of the done button.
     * By default, cancel button is hidden.
     * @param show Whether to show it or not
     */
    RecurrencePickerSettings setShowCancelButton(boolean show);

    /**
     * Set which periods are available to be used. Note that NONE period cannot be disabled
     * You must set at least one period.
     * Must be set before showing the creator or the dialog
     * @param periods bit field of 1 << [Recurrence.DAILY to Recurrence.YEARLY]. Ex:
     *                periods = (1 << Recurrence.DAILY) | (1 << Recurrence.WEEKLY)
     *                Don't forget the parentheses!
     *                A quicker way would be to use binary literal ex: periods = 0b1111 (all periods)
     */
    RecurrencePickerSettings setEnabledPeriods(int periods);

    /**
     * Set which end types are available to be used. Note that END_BY_DATE_OR_COUNT is not implemented
     * You must set at least one type. If that's the case, end type spinner will be disabled.
     * If the only end type set is forever, end type spinner completely disappears.
     * Must be set before showing the creator or the dialog
     * @param types bit field of 1 << [Recurrence.END_NEVER, END_BY_DATE and END_BY_COUNT]. Ex:
     *              periods = (1 << Recurrence.END_BY_DATE) | (1 << Recurrence.END_BY_COUNT)
     *              Don't forget the parentheses!
     *              A quicker way would be to use binary literal ex: types = 0b111 (all types)
     */
    RecurrencePickerSettings setEnabledEndTypes(int types);

    /**
     * Change defaults in option list. Note that "Does not repeat" will always be shown.
     * By default, a daily, weekly, monthly and yearly recurrences with frequency of 1 are used
     * MUST be set before setting the recurrence if using the view by itself
     * @param defaults Array of recurrence to use as defaults, leave null to use default defaults (exactly)
     *                 or leave null to only change the titles
     *                 You can set any start date for these recurrences because it gets changed
     * @param titles Array of titles to use for each recurrence, leave null to use
     *               {@link RecurrenceFormat#format(Recurrence)} instead. You can also leave
     *               specific items in array null to only format those.
     */
    RecurrencePickerSettings setOptionListDefaults(@Nullable Recurrence[] defaults, @Nullable CharSequence[] titles);

}
