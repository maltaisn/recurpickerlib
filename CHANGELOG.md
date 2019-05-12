

## v1.6.0
- Added `Recurrence.findRecurrencesBetween` to find recurrences between two dates.
- Added parent fragment as a callback target for the dialog.

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

## v1.2
- Setting to change default recurrences in list
- Renamed some layout files, attributes and styles to follow conventions

## v1.1
- Styling attributes for strings: spinner items, option list and days of week
- Setting to change enabled periods and end types
- Setting to disable default recurrences list and recurrence creator separatedly
- Method to serialize recurrence object to byte array and constructor to get it back
- Changed license from LGPLv3.0 to MIT
- Added French translation

## v1.0 (first release)
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
