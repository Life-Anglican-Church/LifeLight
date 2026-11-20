# LifeAC Lighting 0.2.0

- Dedicated Windows desktop window, with retained host/USB output across pages and window hiding.
- Sunday Services and General folders; move scenes and create folders. Default takeover: Sunday Services / Pre-Service.
- Volunteer by default; Admin unlock PIN 5433, with clear mode indicator.
- Full stage photo layout, draggable fixture pins and photo scene builder.
- Timed scene steps, repeat and selectable brightness, colour and movement effects. Eight imported sequences recovered; 68 playable scenes, three empty.
- MIDI learning per scene, Note/CC/Program support, exact-value matching and remembered source with automatic reconnection.
- Automatic HTTPS LAN access and editable user accounts/permissions.
- Removed Hold and Blackout controls and Imported Lights fixture groups.
- Backups and existing user data preserved through migration.

Install **LifeAC-Lighting-Setup-0.2.0.exe**, or use the **new LifeAC-Lighting-Updater.exe** from this release. Close the previous host from its tray menu first. The 0.1 updater cannot extract the larger desktop runtime; use this new installer/updater for this upgrade.

Standalone controller memory read/write is still unavailable through Chromateq's published USB SDK. Installing another SDK does not add that API. Imported standalone assignments are shown as project data, never as device readback.

Fourteen automated core/security/parser/TLS tests and the full simulated desktop/mobile editing flow pass. No native desktop app or lighting output was run on the disconnected laptop. Physical USB, MIDI and LAN access from another device require onsite verification. No existing drivers were replaced.
