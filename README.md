# LifeAC Lighting 0.2.1

Install LifeAC-Lighting-Setup-0.2.1.exe on the projector PC. Close the previous Lighting host from its tray menu first. Existing show data and backups are kept. Use this new installer or the NEW updater EXE for the 0.1 to 0.2 upgrade: the original updater has a file-size limit below the desktop runtime size.

Open the LifeAC Lighting shortcut. It now opens a dedicated Windows application window. Closing its window leaves lighting running in the tray; the tray menu reopens the window or stops the host. Page changes do not close the USB connection. Launching alone sends no DMX output.

The app starts in Volunteer mode. Unlock Admin using PIN 5433. You can change the PIN in Settings. Remote accounts have separate View, Volunteer or Admin permissions; Admin accounts also unlock editing with the PIN.

Sunday Services is the default scene folder, with Pre-Service, Full Stage, Centre Focus and Lights off. Other scenes are in General. Choose the folder at top right. Admin can create folders and move scenes. Connecting defaults to Sunday Services / Pre-Service.

In Stage, select an unplaced light and click its position in the supplied photo. Drag pins to adjust. Pins show configured addresses, not discovered identities. DMX cannot discover ordinary fixture identities. All 32 configured lights are included; the Imported Lights grouping is removed.

Edit a scene to choose a light in the photo or dropdown, change brightness/colour/position, and add timed steps. Each step has fade and hold times; Repeat loops the sequence. Effects include brightness pulse, colour cycle and moving-head sweep, with light selection, cycle time and amount. Draft edits send nothing until Preview is selected. Restore/Cancel returns to the previous commanded scene; Save keeps the PC show. Imported eight multi-step sequences are recovered with inherited channel identifiers; 68 scenes are playable and three empty scenes remain editable.

MIDI: choose the input source once in Settings. The host remembers its NAME and reconnects automatically. Under each scene, use MIDI / Learn next input or enter Note, Control Change or Program Change parameters. Channel numbers are 1-16; keys/program numbers are 0-127. Optional exact value matches controller buttons reliably. Mapping conflicts are reported. MIDI remains in the host, independent of which page is shown.

LAN access starts automatically when a network is available. Settings shows its HTTPS address and lets Admin add/edit/remove users. Passwords require 12 characters; leave blank when editing to retain the current password. A local certificate is generated without installing trust or changing the firewall. Windows/network firewall rules may still need to allow the app for access from other devices.

Add lights using a known profile and free address; set the same channel mode and address on the actual fixture. Import fixture JSON profiles for other models. Repatching requires output to be released. Export show, diagnostics and restore backups from Settings.

Standalone device memory reading/writing remains unavailable. Both the tested 2023 USB SDK and current 2026 USB SDK expose live control only. The June 2026 UDP protocol targets Chromateq software or Ethernet-capable devices and does not add USB memory writing with ProDMX closed. The Standalone screen shows imported project assignments only. No SDK or driver installation can manufacture the missing public API. This requires a supported vendor programming API/tool integration before it can be implemented.

The app reuses the verified private USB runtime or downloads the official archive on first takeover. No existing USB driver, projector, Welcome installation or ProDMX project is replaced. Test the new build onsite before relying on its output; the two previously dark moving heads still need physical verification.

Source: src/, web/, native/, desktop/. Build native helpers with build.cmd. Desktop runtime is official Electron 45.3.0 Windows x64, verified against its release SHASUMS256.txt, extracted into desktop-runtime/. Existing Node runtime remains bundled for diagnostics/compatibility. Installer packaging: scripts/build-release.ps1; requires the owner-provided read credential outside source. Publishing credentials are never bundled.
