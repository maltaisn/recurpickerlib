### v2.1.8
- Updated dependencies.
- Updated translations.

### v2.1.7
- Added Ukrainian translation, thanks to @Serhii_Ka.

### v2.1.6
- Fixed layout issue in 2.1.5.

### v2.1.5
- Add Arabic translation, thanks to @afmbsr.
- Fix untranslated string in French translation.
- Mostly a test release for new publishing script.
- There's a major layout issue in this version! Please use 2.1.6.

### v2.1.4
- Updated kotlin to 1.4.21.
- Fixed layout issues with constrained width.
- Fixed missing left and right margin in recurrence picker dialog for API <= 16.
- Fixed dropdown widgets not wrapping width to content (introduced in 2.0.2).
- Fixed some issues with weekly recurrence with no days set (aka on same day as start date).
    - `Recurrence.toString()` now returns "on same day as start date" for this case.
    - `RRuleFormatter` correctly formats this case which otherwise resulted in invalid RRule.
- Fix untranslated string in French translation.

### v2.1.3
- Introduced `rpListDialogMaxWidth` and `rpPickerDialogMaxWidth` attributes to limit the maximum size of dialogs on
screen. Previously dialogs used at least 65% width in portrait and 100% in landscape which resulted in very wide dialogs
on large screens. Maximum widths are set to 500dp for both by default.
- `Recurrence.toString()` now returns the same output in release builds.
- Day of week buttons in picker dialog are now bigger on larger screens.
- Fixed `RecurrenceFormatter` not adding days of the week for a weekly recurrence recurring on multiple days of which
one is the same as start date's.
- Fixed end date dialog layout on larger screens.
- Increased Kotlin version to 1.4.0.

### v2.1.2
- Added support for changing time zone in `RecurrenceFinder` and `RRuleFormatter`.
- `RecurrenceFinder` now returns an empty list instead of an exception when trying to find 0 events.
- Better `Recurrence.Builder` syntax when used from Java.
- Changed date pattern for RRule to date only `yyyyMMdd` instead of date and time `yyyyMMddT000000`.
- Fixed recurrence builder allowing creation of non-equal recurrences of period `NONE`, leading to equality issues.
All recurrences built with `NONE` period now return the same `Recurrence.DOES_NOT_REPEAT` instance.
- Fixed `RRuleFormatter` not thread-safe due to use of static date format for formatting and parsing.

### v2.1.1
- `RecurrencePickerFragment` now handles back press by itself.

## v2.1.0
- Backported to API 14.
- Fixed day of week toggle buttons color on API < 23 (but they don't have shadow anymore).
- Fixed end date not being set at start of the day (time 00:00:00.000).
- Fixed end date selection not working on API 21.
- Fixed end date picker not keeping all state on configuration change. For example if year selection
was active, date picker is reset to day selection on configuration change.
- Fixed end date picker initially showing year 1964 or 2100 on API < 21.

### v2.0.3
- Fixed weekly recurrence missing events on the first week on OEM Samsung devices.

### v2.0.2
- Fixed rare crashes happening in `RecurrencePickerSettings` unparcelization due to
Android bug on API >= 28.

### v2.0.1
- Fixed `RecurrencePickerSettings` not being unparceled completely.
- Updated versions of dependencies.

# v2.0.0
- The library was completely rewritten in Kotlin with MVP architecture.
- **`Recurrence`**:
    - Now immutable, can be constructed with `Recurrence.Builder` in Java or constructor-like syntax in Kotlin.
    - `startDate` field was removed since it's a property of the event, not the recurrence. 
      `isDefault` field was also removed. The information set in these fields is now set in `byDay` and `byMonthDay`.
    - `daySetting` was replaced with more standard `byDay` and `byMonthDay` fields.
    - `period` and `endType` are now enums.
- **`RecurrenceFinder`**:
    - Replaces `Recurrence.find` methods.
    - Since recurrences don't have a start date anymore, the methods of this class take a `startDate` parameter.
    - Start date/base date is now included in the returned list of events by default. `includeStart` can be
      set to false to prevent this.
    - Fixed yearly recurrence on Feb 29 happening every year on Feb 28.
- **`RRuleFormat`**:
    - Now supports rule parsing.
    - Start date and isDefault no longer included to follow standard.
    - Backward-compatible with v1 RRule format.
    - This is now the recommended way of serializing recurrence objects.
- **`RecurrenceSerializer`**:
    - Replaces `Recurrence(byte[])` and `Recurrence.toByteArray()`.
    - Backward-compatible with v1 binary format.
    - Deprecated in favor of RRule serialization.
- **UI**:
    - UI was completely changed to follow material design guidelines.
      Google's material components and themes are used.
    - Dark theme supported out of the box.
    - UI Classes:
        - `RecurrenceListDialog`: used to select a recurrence from a list of presets.
        - `RecurrencePickerDialog`: used to create a custom recurrence.
        - `RecurrencePickerFragment`: same as the dialog but as a fullscreen fragment.
    - `RecurrencePickerView` was removed, use fragment instead.
    - Navigation must now be done manually which is more flexible. For example:
        - Parent fragment opens `RecurrenceListDialog`.
        - User clicks on the "Custom..." item which sends the `onRecurrenceListCustomClicked` callback to parent fragment.
        - Parent fragment decides to show the `RecurrencePickerFragment` on callback.

## v1.6.0
- Added `Recurrence.findRecurrencesBetween` to find recurrences between two dates.
- Added parent fragment as a callback target for the dialog.
- Fixed date not being set in "until date" date picker dialog.

### v1.5.2
- Fixed German translation.

### v1.5.1
- Fixed default date formats not being set in dialog.

## v1.5.0
- Migrated to AndroidX.
- Added 5 translations thanks to bezysoftware.

### v1.4.5
- Added 10 translations: Albanian, Czech, German, Italian, Lithuanian, Norwegian, Polish, Russian, Slovak and Turkish.
- Added strings for AboutLibraries.

### v1.4.4
- Removed 4 strings for faster translation.

### v1.4.3
- Changed "Every 1 day/month/..." formatting to "Every day/month/...".
- Removed the "Repeats" part of the formatted recurrence.
- Fixed bug where changing start date of dialog didn't update the text of custom defaults in option list.

### v1.4.2
- Fixed French translation.
- Fixed bug where days of the week of weekly recurrence were not checked.
- Target API changed to 28.
- Better Recurrence toString() method.

### v1.4.1
- Fixed library dependencies not being added to the project

## v1.4.0
- Backported to API 19, re-added AppCompat dependencies

### v1.3.1
- Setting start date on the same day or after end date, or setting end date on the same day as start date will make recurrence become "Does not repeat".
- Fixed bug where setting end by count to less than 1 didn't change end type to never.
- Fixed bug where end date could be set on the same day as start date in RecurrencePickerView

## v1.3.0
- Refactored code and created unit tests.
- Added the `RRuleFormat` class to convert a `Recurrence` object to a RFC 5545 recurrence rule string. However, it is still not possible to create a recurrence from a string rule and this feature isn't planned because the recurrence picker only supports a thin subset of what RRule supports.
- Removed `END_BY_DATE_OR_COUNT` in recurrence object, which RFC 5545 doesn't support, plus, it wasn't used.
- Moved formatting methods from `Recurrence` to new `RecurrenceFormat` class.
- Changed `isRepeatedOnDayOfWeek` to `isRepeatedOnDaysOfWeek`, accepting multiple days.
- Added day of week constants for setting weekly day setting, instead of using `1 << Calendar.SUNDAY`.

### v1.2.6
- Callbacks can now be made to fragments
- Changed default dialog buttons style to fit material style

### v1.2.4 & 1.2.5
- Removed AppCompat dependencies
- Made all `RecurrencePickerView.DEFAULT_*` constants package-private
- Prefixed all attributes and strings with `rp` to avoid mixing them with the user's own attributes
- Added copyright license header to source files
- Made some resources private (strings, drawables, colors, layouts)

### v1.2.3
- Fixed `Recurrence.isOnSameDay` returning wrong result
- Changed target build API to 27
- Change license (again) to Apache 2.0
- Added IntDef annotations for recurrence periods, end types and monthly settings

### v1.2.2
- Made end date picker dialog be restored on configuration change.

### v1.2.1
- Made recurrence object byte array length public
- Removed `RecurrencePickerDialog.newInstance()`, it was useless
- Renamed dialog callbacks to be less general ex. onCancelled becomes onRecurrencePickerCancelled

## v1.2.0
- Setting to change default recurrences in list
- Renamed some layout files, attributes and styles to follow conventions

## v1.1.0
- Styling attributes for strings: spinner items, option list and days of week
- Setting to change enabled periods and end types
- Setting to disable default recurrences list and recurrence creator separatedly
- Method to serialize recurrence object to byte array and constructor to get it back
- Changed license from LGPLv3.0 to MIT
- Added French translation

# v1.0.0 (first release)
- Recurrence picker view and dialog
- Same recurrence options as all the other pickers
- Default recurrences list and custom recurrence creator
- Utilities for formatting and finding recurrences
- Styling attributes for views
- Settings
    - Show done and cancel buttons
    - Skip default recurrences list
    - Maximum frequency, end date and end count
    - Default end date and end count
