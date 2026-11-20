# Build validation — 20 November 2026

Built: C# Windows tray launcher, private x86 frame worker, MIDI helper and certificate helper. No installer, browser-driven USB session or live lighting host was run on the laptop.

Eleven automated tests passed:

- Compiled frame parser accepts 512 byte-valued integers and rejects short, out-of-range and fractional frames without loading USB.

- Original project: all 32 fixtures, seven profiles, original names, 71 preserved scenes, 60 directly supported static scenes.
- Bounded XML/decompression and malformed/entity/overlap rejection.
- Atomic 16-bit interpolation, protected channels and end-only discrete changes.
- Interrupted fades, Hold, explicit preview/cancel restoration.
- Same-scene reselect without pulses, dimmer blackout and colour-only blackout with movement/control channels held.
- No startup output; transport failure stops output progression and cannot silently reopen USB.
- Atomic persistence, conflict refusal and backups.
- Authenticated server bootstrap, Origin/CSRF checks, operator permissions, independent edit leases, patch additions/repatching, oversized-address refusal, preview values/mask persistence and revision conflicts.
- Actual Windows certificate helper: private PFX generated in a temporary test directory, accepted by Node HTTPS, and trusted by an explicit test CA. No certificate was installed in the system/user trust store.

Simulation-only Edge browser checks passed on desktop and 390px mobile layouts: branded UI, no page errors/overflow, deliberate simulated takeover, scene selection, Hold, adding a fixture, live preview/cancel and disabled unsupported device-memory controls. Screenshots were inspected; a delayed slider event after cancellation was found and fixed. The UI check passed after the fix.

The portable runtime is official Node.js 24.22.3, from the Node distribution archive, verified against its published SHA-256 list. The Node licence accompanies it. No vendor SDK DLL or driver is redistributed.

The current vendor Windows SDK dated 12 November 2026 was inspected. It provides a plain-C interface and WinUSB dependencies but still no stored-scene memory APIs. The proven 2023 SDK/driver path was retained; no driver migration performed.

Outstanding onsite validation: new frame-helper pipeline on the real controller, all six heads/shutter modes, output timing under real load, source address convention, port routing, physical restoration/handover, real MIDI, actual remote browser/client trust, Windows installation and coexistence with projector/Welcome. No claim of completed onsite deployment or standalone-memory support.
