# Validation — 21 November 2026, v0.3.1

21 simulation/server/security tests pass. New coverage checks manual holds through fades, timed steps, effects and preview cancellation; release clearing; unscaled saves; 16-bit brightness; reconnect without cumulative dimming; shared host state; and viewer permission refusal. Expanded headless Edge flow passes manual faders, holds across scene changes, the 11am toggle, save-to-scene and mobile width. Native helpers compile. Installer and update package are built without installing or opening USB/MIDI on the laptop. Physical light response still requires the projector PC.

# Validation — 20 November 2026, v0.3.0

18 engine/server/security tests pass, including two competing sessions, explicit preview takeover, stale edit rejection, preview expiry, host-side MIDI triggering with no clients, simulated USB appearance/recovery, model preservation, show export/import round trip, redacted persistent logs and rotation. Native helpers and tray launcher compile. Headless Edge checks a 12-second page change, automatic live faders, scene saves and desktop/mobile layouts. The real Electron client renders the four default scenes, exits, and leaves its independent simulation host producing frames. Installer payload and manifest hashes are checked without executing the installer. No native USB/MIDI was opened on this laptop. Physical controller stability, actual MIDI input and two-PC USB switching still require on-site validation. Both user-supplied read reports successfully returned the same 128-byte standalone configuration; full scene backup/writes remain unfinished.

## Earlier validation history

# Validation — 20 November 2026, v0.2.0

14 tests pass: project import (32 fixtures, 7 profiles, 71 scenes, 68 playable), inherited multi-step IDs, folder migration, timed fades/holds/loop, non-accumulating effects, 16-bit movement, protected controls, interrupted fades, preview restoration, output failure/no automatic reconnect, patch validation and atomic backups, authenticated HTTP/CSRF/role/conflict checks, MIDI matching, compiled strict frame parser and private TLS certificate use without trust installation.

Headless Edge simulation flow passes: four default scene tiles, default connection folder/scene, continued frame output across a 3.5-second page change, PIN unlock, photo pin placement, multi-step edit/save/effect, scene moves, Program Change mapping, account creation and desktop/mobile layout without JavaScript errors. No USB or native MIDI helper was opened in browser tests.

C# launcher/helpers compile. Electron official Windows runtime SHA-256 matches official release sums. Packaging and embedded payload are checked without running the installer. The dedicated desktop window itself, real MIDI devices, LAN access from another PC and physical USB output have not been exercised on this disconnected laptop.

The standalone API was rechecked against current official USB headers, SDK page and June 2026 UDP protocol. No USB scene-memory read/write entry point is published. This requirement remains blocked; the interface does not pretend to write memory.

0.2.1 hotfix: the REAL Electron runtime now has an invisible-window regression that renders all four scene buttons and shuts down via authenticated Windows named pipe. The 0.2.0 tray-only startup failure was reproduced first. No native USB/MIDI access occurs in this test. Fourteen core tests and the browser flow pass again.
