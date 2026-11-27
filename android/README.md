# LifeAC Lighting Android pilot

The pilot has two entry points. **Pair at the desk** connects to a primary Windows host that includes the Android pairing changes. **Try demo** runs entirely on the device and includes Sunday Services and General sample folders.

## Pair and try it

1. Allow camera access when prompted. Connect the phone or tablet to the same network as the primary desk.
2. On the primary desk, unlock Admin mode and open **Settings → Paired Android devices → Pair Android device**.
3. Scan the code from the Android app and name the device. The code expires after two minutes and works once.
4. In the app, browse a scene folder and recall a scene. In Fixtures, choose a fixture or a group and adjust supported controls. Adjustments are held through scene cues. Use **Release adjustment** to return the selected fixtures to the running scene. Use **Save** on the desk to capture the held look.
5. Use **Settings → Paired Android devices → Remove** to revoke access. Use **Refresh devices** if the list was left open during pairing. Output continues at the desk.

## Demo

Dim fixture 5 from 80% to 25% in Pre-Service, then tap **Simulate ProPresenter cue** on Scenes. Welcome goes live and fixture 5 remains at 25%. Release the adjustment to see Welcome's 60% level.

## Build

On Linux or macOS, install a JDK, Android SDK Platform 35, Android SDK Build Tools 35.0.0, and zip. Set `ANDROID_SDK_ROOT` and `ZXING_JAR` to the SDK and [ZXing Core 3.5.3 JAR](https://repo.maven.apache.org/maven2/com/google/zxing/core/3.5.3/core-3.5.3.jar). Run `android/build-pilot.sh` from the repository root. The APK is written to `dist/LifeAC-Lighting-Pilot.apk`.

The pilot uses a test signing key and installs alongside the earlier offline demo. Preserve the generated pilot keystore for updates within the pilot. Arrange church-owned release signing before distributing to volunteers.


## Publishing pilot updates

The app checks `pilot-downloads/latest-android.json` on the public `pilot-downloads` branch when launched. Increment `android:versionCode` on every release and sign with the existing pilot signing key. Publish the APK and manifest in the same commit. The manifest contains the APK's integer `versionCode`, numeric `version` (for example, `0.2.14`), and lowercase `sha256` of the signed APK. Name the file `LifeLIGHT-Pilot-<version>.apk`.

From 0.2.14, **Update now** downloads into private cache and checks the SHA-256, Android package ID, version code and signing certificates before opening Android's installer. Android may first ask the user to allow installations from LifeLIGHT. The user confirms the update in Android. Existing pairings and preferences carry over on an in-place install. Version 0.2.13 can still offer 0.2.14 through its browser-download flow.
