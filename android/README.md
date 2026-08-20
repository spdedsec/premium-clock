# Premium Clock for Android

**Premium Clock** is an offline-first native Android clock application built with Kotlin and Jetpack Compose. It follows the same Chronographic Modernism design system as the companion web app: warm paper surfaces, graphite typography, hairline instruments, and Signal Vermilion for active time states.

## Included capabilities

| Area | Included functionality |
|---|---|
| Clock | Live local time, selectable large, compact, editorial, analog, and mono presentations, plus light/dark modes and 12/24-hour preference. |
| Alarms | Repeating weekday alarms, labels, snooze length, exact-alarm permission flow, full-screen ringing surface, and notification actions. |
| Timers & stopwatch | Multiple persistent timers with system notifications and background delivery, presets, elapsed-time stopwatch, and lap recording. |
| Utilities | World clocks, time-zone conversion, date planning, bedtime planning windows, focus intervals, and locally stored activity insights. |
| Data | Room storage for alarms, timers, cities, and events; DataStore settings; JSON export/import. No account or network connection is required. |
| Widgets | Clock, alarms, timer, and stopwatch home-screen widgets that open the corresponding experience. |

## Install the debug APK

The generated debug APK is located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Copy the APK to an Android device and allow installs from the selected file manager when Android prompts. This is a development build signed with the standard debug key; it is suitable for local testing rather than store distribution.

## Build from source

The repository includes the Gradle wrapper. Install Android Studio with Android SDK Platform 35 and Java 17, then run:

```bash
export JAVA_HOME=/path/to/java-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug
```

The project has been compiled with Kotlin and packaged successfully as a debug APK using Android SDK Platform 35.

## Android permissions

The first use of notifications requests notification permission on Android 13 or later. Exact alarms may also prompt for Android’s special **Alarms & reminders** access. The app remains usable without exact-alarm access, but Android may defer time-sensitive alarm delivery.
