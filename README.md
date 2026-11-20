# LifeAC Lighting 0.1 — onsite test build

## Start on the projector PC

For a normal installation, run **LifeAC-Lighting-Setup-0.1.0.exe**. It creates Lighting and Update Lighting shortcuts for this Windows user. Portable ZIP use is also available.

1. Extract the complete ZIP to a folder on the projector PC. Close ProDMX and the earlier diagnostic/sample apps.
2. Open **LifeAC Lighting.exe**. Your normal browser opens the branded interface. The lighting host stays in the Windows tray when the browser closes.
3. During a suitable test window, select **Take control**, choose a familiar starting scene, and confirm. This is the first operation that opens USB and sends a starting frame; opening the app alone sends no output.
4. Select scene cards to fade directly between scenes. **Hold** freezes the currently calculated values. **Blackout** fades dimmers and colour-only fixtures out while holding movement, shutters and protected channels.

The previously verified USB runtime is reused automatically if present. Otherwise it is downloaded and checked when preparing/taking control (about 281 MB once). No USB driver is installed/replaced. Keep the PC online for initial dependency preparation if its runtime is missing.

For a Start-menu/desktop shortcut, close the lighting host using its tray menu, then run **LifeAC-Lighting-Setup-0.1.0.exe**. It installs only for the current Windows user and leaves the existing show, Welcome app, projector software and drivers untouched. Portable use is also supported; keep all supplied subfolders beside the executable.

## Scenes and editing

Your original 32-fixture patch and named scene groups are included. Sixty imported single-step scenes are enabled. The other eleven are empty or need sequence/effect timing verification. They remain visible and preserved; they are not silently flattened into runnable scenes.

Use **Edit** to stage a scene's brightness/colour/position changes and fade time, or **Duplicate** to make a new scene from an existing one. Nothing is sent while staging. Live Preview is optional and explicit. Cancel returns the engine to its previous commanded values. Save stores a PC scene, not standalone memory. Editing an unsupported scene explicitly converts it to a static scene while retaining the old show in backups.

Fixtures with coarse/fine movement channels are handled as paired 16-bit controls. Unidentified, reset/macro/internal-program channels are protected. Shutters are stepped at transition completion rather than interpolated. Do not assume a profile's shutter value 0 means open: two moving heads still need an onsite profile/mode check.

The first takeover starts from the recorded commissioning frame of 18 November with the selected scene overlaid. It may move or switch lights off. This is a chosen reference, not a capture of the rig's current physical state. Subsequent scene changes retain state without reopening/resetting USB. The trial app's all-zero/reconstructed baseline routine is not run on startup.

## Add lights

Open **Lights → Add light**. Choose an existing profile/mode, quantity and group; a free address range is suggested. Set that address and mode on each physical light, then save the patch. Overlapping/out-of-range addresses are rejected. Adding a fixture does not send an identification sweep or guess its model.

For a different model, use **Create profile** with the channel chart from its manual, or import a single-mode ProDMX .cp1 / LifeAC profile JSON. Leave reset/internal/unknown channels protected. Sixteen-bit coarse/fine entries must be adjacent and paired. Saving a profile sends no output.

**Details / address** renames, groups, removes or repatches a light while output is released. Repatching moves that fixture's saved scene values/masks with it; still review affected scenes before use. Newly added lights are not automatically inserted into all existing scenes. Use Live Preview and Save preview into a scene, or edit a scene's controls deliberately.

## LAN control

Local operation works without networking. For remote access, on the projector PC:

1. In **Settings**, add a named administrator account and any operator accounts, using passwords of at least 12 characters. Operator accounts select scenes, Hold and Blackout; administrators edit the show.
2. Set up LAN access and choose the PC's church-network IPv4 address. The app creates a private HTTPS certificate for that address without installing it into Windows trust.
3. Download the public certificate and install/trust it only on authorised devices, after comparing its displayed fingerprint. On Windows, the user/IT administrator can import the .cer into the current user's Trusted Root Certification Authorities using the certificate UI. On other devices, follow their managed certificate-installation procedure. Do not bypass browser certificate warnings.
4. If Windows Firewall blocks remote access, have the administrator allow the bundled node.exe or TCP 47832 only on the church's Private network/interface. The app does not create a rule or change trust automatically. Avoid enabling access on a public/guest network. No router port forwarding is needed.
5. Open the HTTPS URL shown in Settings on the authorised device and sign in. Keep the projector PC's IP stable; an address change needs a new certificate and updated trust on clients.

The Windows launcher grants local administrative control to its Windows user; Volunteer view is a simplified view, not a security lock. Use named operator sign-in for restricted remote control. Named sessions expire after 12 hours. Changes use revision conflict checks and an edit lease. A disconnected browser does not replay commands when it reconnects, and Wi-Fi loss does not stop the local lighting engine.

## MIDI

Connect the actual MIDI controller to the projector PC, then choose its input in Settings. A received message is shown so you can map its note/controller, channel and key to a scene. Imported ProDMX MIDI trigger strings are preserved for reference but not automatically interpreted as verified Windows port mappings. The host listens independently of the browser. Held/repeated controls are edge-triggered; they do not continually restart scenes or reopen USB.

## Standalone controller

The Standalone page displays your imported button plan and supports PC-side edits/export. **Actual device-memory read/write is not implemented:** neither the tested SDK nor the current November 2026 SDK exposes stored-scene functions. This feature requires a supported vendor route; it is not replaced by calling a PC copy a live snapshot. The newer SDK also uses a different USB driver, so it was not substituted for the working driver.

This is a live-control test build, not completion of the full standalone-memory requirement. There is no reverse-engineered memory writer, formatting operation or firmware/key write.

## Backups, reports and stopping

Show data and automatic backups live in **%LOCALAPPDATA%\LifeAC-Lighting**. Every saved show change creates a backup (last 60 retained). Settings can export the show/diagnostics or restore a backup while output is released. Keep exported shows private: they include internal scene/fixture labels. Credentials/private certificates are not included in diagnostics or show exports. SDK-accepted outgoing values are checkpointed separately for recovery investigation; they are never presented as physical telemetry or automatically resumed after restart.

Release output before handing control back to ProDMX or a known standalone scene. Use the tray menu **Stop lighting host** to shut down the engine. Hardware/driver failure may prevent confirmation of final writes/close; verify the stage physically before services. Returning to an old file is not the same as restoring a fixture's current physical position.

To roll back, stop LifeAC Lighting, use the existing controller software/standalone scene and original ProDMX show, and verify the lights. Existing USB drivers were left intact. Per-user installation keeps each installed release in a separate directory. To remove, delete its shortcuts and installed release directory after stopping it; show data/backups are separate and can be retained.

## Tested and still to commission

Run **LifeAC-Lighting-Updater.exe** or the **Update LifeAC Lighting** shortcut after stopping the lighting host. It downloads the private GitHub release, verifies the package SHA-256 and installs a separate version directory. Existing show data/accounts/backups remain in place. As explicitly requested, the read-access token is embedded in the installer/updater binaries and is recoverable from them. The publishing token is never embedded or committed. A Windows-user-protected credential copy is stored at installation. Apps & Features uninstall retains show data/backups.

The engine/import/storage/security tests and simulation-only desktop/mobile browser checks passed. The Windows host and helpers compile; certificate creation and HTTPS use were tested without installing trust. The application was not launched against USB on the laptop. This new engine-to-helper pipeline, installer, real MIDI input, real LAN clients and physical transitions still need checks on the projector PC. Previous trials establish that the pinned SDK can control this interface; they do not establish every behaviour of this new build.

Only universe 1 output is enabled until independent port routing/licensing is physically established. The SDK's all-zero read buffer is not used as output readback. Use diagnostics to report any failure and mention which physical lights responded.

See **docs/VALIDATION.md** for the checks and **docs/COMMISSIONING.md** for the short onsite checklist. Source is included separately. Windows 10/11 x64, .NET Framework 4.8, and a current Edge/Chrome browser are expected. Node.js is bundled and requires no separate installation. If Windows blocks the unsigned prototype, have the administrator review/build it; do not change security settings to run it.
