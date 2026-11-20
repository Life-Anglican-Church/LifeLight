# LifeAC Lighting 0.1.0

First onsite live-control preview for the church projector PC.

- LifeAC Welcome branding, loading screen and church artwork.
- Original 32-fixture patch and 71 named scenes; 60 static scenes enabled, with unsupported/empty sequences preserved for review.
- Persistent scene transitions, paired 16-bit movement, Hold, Blackout and explicit live preview/cancel.
- Add/repatch fixtures, create/import profiles, edit/duplicate scenes, automatic backups and diagnostics.
- Authenticated HTTPS LAN setup and host-side MIDI scene mappings.
- Per-user Windows installer and GitHub updater, preserving show data and existing USB drivers.

Download **LifeAC-Lighting-Setup-0.1.0.exe** for first installation. Use **LifeAC-Lighting-Updater.exe** for subsequent updates; stop the lighting host before updating. The read-access credential requested by the owner is embedded in both utilities; the publishing credential is not. The token is not a substitute for Windows account security and can be recovered from those utilities.

Actual standalone-device scene-memory read/write is not supported by the available SDK. The Standalone screen manages an imported PC plan only. Eleven imported scenes and the two previously dark moving heads need commissioning. The new application pipeline has been tested in simulation; complete the supplied onsite checks before service use.

The app sends no output on startup. Take control explicitly during a suitable maintenance window. Existing show files, Welcome software and drivers are not replaced.
