#### v1.2.3
- Fixed `Recurrence.isOnSameDay` returning wrong result
- Changed target build API to 27
- Change license (again) to Apache 2.0
- Added IntDef annotations for recurrence periods, end types and monthly settings

#### v1.2.2
- Made end date picker dialog be restored on configuration change.

#### v1.2.1
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
