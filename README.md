# Solace

An Android companion app for people in therapy. It keeps your sessions, care team,
costs and mood check-ins in one place, on your phone.

Solace is an independent app. It is not affiliated with any therapy provider or
marketplace and does not connect to one: everything you enter is stored in the
app's private storage on the device and is never sent anywhere.

## What it does

- **Home** – your next session with a countdown and a Join video button, balance due,
  a one-tap mood check-in, and a list of things to bring up next time.
- **Sessions** – upcoming and past appointments with length, location or video,
  cost, notes, and a reminder notification (15 minutes, 1 hour or 1 day before).
- **Care team** – your providers with call, email and video-link shortcuts.
- **Billing** – what you owe, what you've paid this year, per-session charges,
  and your insurance details with deductible progress.
- **Journal** – mood check-ins with notes and a chart of your recent mood.

## Build

Requires JDK 17+ and the Android SDK (platform 35, build-tools 35.0.0).
Set `ANDROID_HOME` or put `sdk.dir=/path/to/sdk` in `local.properties`.

```
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # logic + UI flow tests; screenshots in app/build/screenshots
```

Install on a phone with `adb install app/build/outputs/apk/debug/app-debug.apk`,
or copy the APK to the phone and open it (allow "install unknown apps" when asked).

Requires Android 8.0 or newer.
