# Date Reminder (Android · Kotlin + Jetpack Compose)

- Month calendar; days with pending reminders show a dot
- Add reminders for any date + time (date/time pickers)
- Fires at the exact minute (AlarmManager.setExactAndAllowWhileIdle); re-armed after reboot / time change
- Dark theme in the app and the home-screen widget
- Widget: next 5 reminders, "+" button opens the Add dialog directly

## Build
1. Open this folder in Android Studio (Koala or newer); it syncs Gradle and creates the wrapper.
2. Run on a device (Android 8.0+) or Build > Build APK(s).
3. Long-press home screen > Widgets > "Date Reminder".

Android 12+: tap the orange banner once and allow "Alarms & reminders".
Android 13+: allow notifications when asked.
