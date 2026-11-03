# GitHub APK signing

`scripts/build-release.sh` builds the unsigned release variant, aligns and signs it, verifies the signature and package ID, then writes `dist/BatteryNG.apk`. Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` to an Android SDK with Build Tools 35.0.0 before running it.

The script reads the private PKCS#12 keystore and its password from `~/.config/BatteryNG/release.p12` and `~/.config/BatteryNG/release.pass`. Both files must have owner-only permissions. They are not in this repository. **Back up both files securely and separately from the source tree.** Losing the key prevents updates to existing GitHub APK installations under the same app ID.

The release certificate used from version 0.12.1 has SHA-256 fingerprint `1d20a8b1f940b32183358af60c684db62ceeff2737a65e3e24630fa32da4dcab`. Check a built APK with Android SDK's `apksigner verify --verbose --print-certs dist/BatteryNG.apk` before publishing it.

The `exportDebugApk` Gradle task writes `dist/BatteryNG-debug.apk` for local testing. Do not publish that file as a GitHub release. If BatteryNG is accepted into F-Droid, F-Droid will build and sign its own APK independently.
