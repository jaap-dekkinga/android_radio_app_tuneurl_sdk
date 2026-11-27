# TuneURL Radio – Android

Android app for KSAL radio: ksal.com news, sports and local information,
live station streams, and **Turls**: interactive moments (offers, polls,
links, phone numbers) that the app picks up from the radio audio, either from
the stream it plays or from a radio nearby ("Listen along").

## Tech stack

Kotlin · Jetpack Compose (Material 3) · MVI · Hilt · Room · DataStore ·
OkHttp · Coil · Media3 ExoPlayer · Coroutines/Flow · Navigation Compose.
minSdk 26, targetSdk 37. Build: AGP 9.5, Gradle 9.6 (JDK 17–25).

## Project layout

| Path | Contents |
|---|---|
| `ui/` | Compose screens: Home, Sports, Local, Listen, Turls, Settings, article and engagement detail |
| `data/ksal/` | ksal.com WordPress news, National Weather Service weather, Revive ads |
| `player/` | Media3 playback service and player manager |
| `tuneurl/TuneURLManager.kt` | Connects playback and Listen along to the TuneURL SDK and shows matches |
| `app/libs/tuneurl-sdk.aar` | The TuneURL SDK (prebuilt, proprietary; see `app/libs/README.md`) |

## TuneURL SDK

Turl detection and the TuneURL server API are provided by the TuneURL SDK,
included as a prebuilt AAR in `app/libs/`. Its source is not part of this
repository. To bring in a new SDK version, run the **Update TuneURL SDK**
workflow (Actions tab) with the version number.

## Build

```
./gradlew :app:assembleDebug
```

Release builds are signed with the key from `local.properties`
(`release.storeFile`, `release.storePassword`, `release.keyAlias`,
`release.keyPassword`) or the `KEYSTORE_*` CI secrets; without them the release
APK is left unsigned.

## Licence

The app source in this repository is MIT licensed (see `LICENSE`). The TuneURL
SDK binary in `app/libs/` is proprietary and **not** covered by the MIT
licence (see `app/libs/LICENSE-TuneURL-SDK.txt`).
