# LifeAC Lighting 0.4.0

Windows church lighting control. Close the previous app from its tray menu before updating. Shows, accounts, mappings and photo positions are kept.

Run the new installer to choose the PC role. **Control another PC** installs a remote client without USB/MIDI host components; enter the host's HTTPS address and sign in. **Host USB/MIDI** installs the tray engine for a main or backup PC. New installations default to client; existing hosts remain hosts. Subsequent updates preserve the role. Host components are not installed in client-only application folders. Drivers, projector software and the Welcome app are not changed. Windows autostart is not enabled.

The host owns USB, MIDI and timing. Client windows can close while lighting continues. Host MIDI connects automatically to the saved input, sole loopMIDI port or sole input and reconnects if it reappears. If several ports are ambiguous, choose one in Settings; that immediately saves and connects it. Keep loopMIDI and ProPresenter running in the host's Windows login. Received notes and scene-match decisions appear in MIDI status and diagnostic logs. Scene note mappings are under the scene's right-click menu or Settings.

The main screen has scenes on the left and the stage on the right. Sunday Services is the default folder, containing Pre-Service, Full Stage, Centre Focus and Lights off. Left-click runs a scene; right-click opens scene actions. Admins drag the six-dot handle to save a new order. Open Admin using PIN 5433; change it in Settings if desired.

Select one or more fixture pins to control lights. Only shared supported controls appear for multiple fixtures. These create manual overrides that hold through scene, step, effect and MIDI changes. The Manual icon releases individual/all holds or saves them into every step of a scene. Release all lights clears manual holds and resumes the current scene without disconnecting USB. Disconnect controller releases USB and clears holds. Host shutdown also ends holds.

The 11am toggle applies 75% brightness to dedicated dimmers, or to colour channels on fixtures with no dimmer. Scene colour, movement, timing and stored values are retained. Toggling off restores normal brightness. It does not schedule itself by day/time.

Settings is a popup. Lights/profiles, accounts, MIDI, show backups, diagnostics and USB takeover options live there. Under Arrange stage photo, select a fixture, use live controls to identify it, then click its place on the photo or drag an existing pin. Release selected controls when identification is finished. Main-screen pin selection cannot move fixtures. Scene editing supports multi-selection and live shared faders; clicking the photo background clears the selection.

For LAN, use the host address shown in Settings and create accounts there. Remote clients sign in; the host's own client authenticates automatically. Compare the local certificate fingerprint when asked to trust a host. Network/firewall access still depends on the PC's configuration. USB standby takeover does not operate a physical USB switch or synchronize shows automatically; export/import the current show to the backup host and commission its USB runtime before use.

Troubleshooting logs are retained on the host under %LOCALAPPDATA%\LifeAC-Lighting\logs. Settings > Export diagnostics includes redacted retained logs. Nothing uploads automatically.

Standalone: Settings can run a read-only controller read after live USB output is disconnected and export the result. Partial reads are retained. The 21 November report failed at scene 4 step 18; a complete restoration image and write/restore sequence are not yet verified. Standalone memory writing remains disabled. Ordinary DMX cannot report fixture identity; fixture names, models and patch data come from the configured show.

See docs/RELEASE-NOTES.md and docs/VALIDATION.md for checks and limits. No physical USB or MIDI was tested on the development laptop.
