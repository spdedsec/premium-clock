# Premium Clock

Premium Clock is a matched web and Android clock suite designed around **Chronographic Modernism**: warm-paper surfaces, graphite typography, fine instrument lines, and Signal Vermilion for active time states. The repository is intentionally divided by target so either application can be developed and released independently.

| Directory | Purpose |
|---|---|
| [`web/`](./web) | React 19, Vite, Tailwind 4 browser application. It includes clock styles, alarms, multiple timers, stopwatch laps, world time, focus tools, local insights, search, theme settings, and browser-local persistence. |
| [`android/`](./android) | Kotlin and Jetpack Compose Android application with Room/DataStore, notifications, alarm actions, background timers, widgets, and JSON backup/restore. |
| `release-assets/` | Local staging location for the debug APK and Android source archive. These assets are excluded from Git history and attached to the GitHub release. |

## Run the browser application

```bash
cd web
pnpm install --frozen-lockfile
pnpm dev
```

To build the production static bundle, run `pnpm build`. The generated site is written to `web/dist/public`.

## Build the Android application

Install Java 17 and Android SDK Platform 35, then run:

```bash
cd android
export JAVA_HOME=/path/to/java-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug
```

The debug APK is output to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Vercel deployment configuration

When importing this repository into Vercel, set the **Root Directory** to `web`. Vercel should use the `pnpm build` command and publish `dist/public`; `web/vercel.json` pins these settings. All branded visual assets live in `web/client/public/assets`, so the application does not require any managed-storage proxy at runtime.

## Release assets

The `v1.0.0` GitHub release ships the Android debug APK and a standalone Android source archive. The browser application is released directly from the repository source and can be deployed as the static Vite application described above.
