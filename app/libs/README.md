# app/libs

`tuneurl-sdk.aar` is the TuneURL SDK: audio fingerprinting, Turl detection
(stream and Listen along) and the TuneURL web API client. It is built from a
private repository and committed here as a binary, like the iOS app's
`TuneURL.xcframework`.

- **Version:** see `TUNEURL_SDK_VERSION`.
- **Update:** Actions → **Update TuneURL SDK** → enter the version. The
  workflow downloads the AAR from the SDK release and opens a pull request.
- **Licence:** the AAR is proprietary (see `LICENSE-TuneURL-SDK.txt`). It is
  **not** covered by this repository's MIT licence.
- The AAR does not bundle its dependencies. The app declares them in
  `app/build.gradle.kts`: OkHttp, Gson, kotlinx-coroutines-android and
  androidx.core.
