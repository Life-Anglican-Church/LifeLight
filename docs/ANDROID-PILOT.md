# LifeLIGHT combined Windows host patch for LifeAC Lighting 0.8.2

This patch includes LifeLIGHT console and connection branding, QR pairing and removal for Android devices, and shared light footprints. Use **LifeLIGHT Android pilot 0.2.11** or later to see the saved footprints on a phone or tablet. The APK updates previous LifeLIGHT pilots signed with the same key.

## Apply the patch

Use a test copy of the 0.8.2 release if available. If applying to the church desk, use a maintenance window because stopping the host releases DMX.

1. Export the show and close LifeAC Lighting from the Windows notification-area tray. Copy the existing release directory and application data to a separate backup location.
2. Find the installed release directory in the `directory` field of `%LOCALAPPDATA%\LifeAC-Lighting\install.json`. This is the folder containing the launcher and the `src`, `web` and `desktop` subfolders.
3. Extract this ZIP. Merge its `src`, `web` and `desktop` folders into the 0.8.2 release directory and replace matching files. Copy `THIRD-PARTY-NOTICES.txt` to the release directory. Start with the normal Windows launcher.
4. Install the matching Android APK. Begin with simulated output. At the primary desk, unlock Admin mode, open **Settings → Paired Android devices → Pair Android device**, and scan with the Android app on the same Wi-Fi. Pair the other device with a fresh code.
5. At the desk, open **Settings → Edit light footprints**. Choose a fixture and shape, drag on the stage photo to place it, and adjust size, rotation, gradient or softness. **Save footprint** saves only the selected fixture's footprint. The live views use saved footprints, and each view has a show/hide toggle. The Android editor remains available in Demo Mode.
6. Verify cue recall, live adjustments, desk **Save**, release, Wi-Fi reconnect, and device removal. A manual hold remains active until released or the host stops. **Release Custom** on the desk and **Release all adjustments** on Android clear holds. The desk's red **Release All** darkens output.

Two desk windows can open a footprint editor. The host checks the show revision before saving. If the first window saves, a stale second window retains its draft and shows an error; note or copy its settings before reopening the editor on the latest show.

## Rollback

Stop the host and restore the backed-up 0.8.2 release directory, then restart using the normal launcher. The patch does not replace the launcher executable, native helpers, updater, or installation metadata. Approved Android devices can be removed before rollback. Device approvals are stored as token hashes in the application's `mobile-devices.json` file. Keep the show backup available in case it is needed.
