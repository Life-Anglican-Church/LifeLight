# Standalone interoperability investigation — 20 November 2026

The supplied win_chromateq.zip is present in the user's Downloads folder. It contains setup_win_chromateq.exe (467,679,048 bytes), a custom Qt-based installer, not an Inno/NSIS setup. Static extraction found an appended Qt RCC v3 archive at file offset 21,785,088. Its nested Pro2.zip contains Pro2.exe; DeviceTool.zip and legacy/current USB libraries are also included. Neither installer nor vendor application was launched or installed.

The earlier statement that there was no memory implementation was too broad. The public SDK does not expose it, but ProDMX itself contains concrete read, write, reset and serialization routines. Those routines are mainly statically linked into the EXE rather than supplied as a separate public memory-programming DLL.

Static evidence in the supplied 32-bit Pro2.exe (preferred image base 0x400000):

- 0x797F60: newer standalone memory import, including 4-byte format/header validation.
- 0x77F870: bounded newer memory-block read loop; calls 0x4D4120 in blocks up to 512 bytes.
- 0x4D4120: newer raw-flash read, group 0x50 / command 0x11, big-endian count/address. It explicitly rejects the legacy protocol. Do not route this packet through the 2023 legacy SDK.
- 0x4CF5B0: erase/programming path with extended timeout. Not called by the reader.
- 0x4D3310: standalone configuration read, group 0x50 / command 0x21. It handles both communication variants and parses model-dependent configuration payloads.
- 0x78BAC0: legacy scene import, leading to scene header (0x4D39D0, command 0x51) and triggers (0x4D3B80, command 0x61). Legacy scene selection is encoded in header fields, not as the newer flash address.
- 0x7392C9 and related callers: actual standalone write UI flow, distinct from firmware flashing. No live write was attempted.

Two wire formats exist: legacy 55 AA 55 11 00 55 AA FF framing and newer BE EF 01 0A framing. Their payload layout and session handling differ. The legacy SDK retains its own authenticated/encrypted session handling. No keys are extracted or exported by the probe; it reuses the normal SDK session.

The exact locally verified 2023 SDK SHA-256 is 2c45136f7833f4096585ee319037d29cef6e91e5c0146e4d9c4410bcd1219b1b. Public firmware-read wrapper 0x6774C5C0 identifies the common transport instance and device-list layout. The read probe additionally checks the first 16 bytes at SDK RVAs 0x3F6DD0 (legacy fixed no-payload read helper), 0x3F23C0 and 0x3F3120 (matching transport pause/resume used by the public firmware wrapper). This is a private, version-specific mechanism and must fail closed for any other binary or firmware.

Next evidence required: a real 0x21 configuration response from the church controller. Use that to validate model-dependent layout and scene counts, then implement read-only scene/step inventory and a complete recoverable backup. Only after repeatable readback and offline round-trip checks should a full write transaction be exposed, with backup, verification, failure recovery and explicit commit. Existing device memory must not be treated as blank merely because a read fails. The first reader is not a full backup and does not make writing safe yet.

Research scripts in the workspace can reproduce RCC extraction and static PE/import/disassembly analysis. Proprietary ProDMX/SDK binaries are kept outside the source repository and are not redistributed with the reader.

## Hardware result — 20 November 2026
Both supplied read reports succeeded against one interface, firmware 2.4.2.3. Configuration command 0x21 returned 128 bytes, SHA-256 1617f80601e6e294a49bc4095827a017eb9de4e6ba43bd3e4446e9093af803a9. This proves the private configuration-read path on the actual hardware. It is not a complete memory backup or a decoded scene inventory. No write or erase operation was attempted. The next step is decoding the configuration and reading legacy scene headers/steps before implementing verified writes.

## Expanded scene reader — unpublished

`tools/standalone-reader/StandaloneReaderWorker.cs` now requests legacy scene headers (0x51), triggers (0x61), and per-universe steps (0x71), preserving raw replies alongside decoded channel values and 40 ms fade/hold ticks. It supports only the traced model layouts 6, 7, 9, 10 and 13, pins the SDK and firmware, bounds counts and payloads, checks packet sequence/response identity, and rereads configuration to detect changes. A partial read is never marked complete. No write, erase or DMX-output operation is enabled.

Compiled x86 worker and Windows GUI successfully. Worker `--self-test` passed an in-memory two-scene inventory plus rejection checks for wrong scene, wrong packet sequence, truncated channel data, changed configuration and a write opcode. `--verify-only` passed against the pinned SDK without loading it or accessing USB. These are offline checks, not proof of hardware scene reading.

Standalone writing remains unfinished. Tracing identified header, trigger, step and configuration write commands, but the full device-wide tables, flash allocation/commit sequence and restoration procedure remain unverified. Existing project files and the 128-byte configuration reports do not supply that missing backup. Do not label the existing v0.3.0 build as a completed standalone reader/writer or publish it under that claim. A successful expanded on-device read is the next required evidence; no native hardware check was run on the laptop.

## Actual scene report and corrected header fields — 21 November 2026
The supplied LifeAC-standalone-read-20261121-173741-23074a.zip contains a real report: firmware 2.4.2.3, SDK product code 8/layout model 7, 2 universes, configuration reporting 35 scenes and used channel counts [320,0]. It stopped at read command 0x71, scene 4, step 18. Headers 1–4 and many out-of-range step responses were retained. No writes occurred.

The prior reader confused scene fade ticks with step count. Static ProDMX evidence: 0x4C2710 decodes payload bytes 2–3 to core+0x1DCF4 and bytes 9–10 to core+0x1DCFC. Main-window core starts at +0x368. 0x78BC58 uses main+0x1E05C (core+0x1DCF4) multiplied by 40 as scene fade milliseconds, whereas 0x788049 uses main+0x1E064 (core+0x1DCFC) as step count. The actual header starts 00 01 00 19 00 00 00 00 00 00 01: fade 25 ticks = 1000 ms, step count 1. Reader now uses bytes 9–10 and exports scene fade separately. Its self-test includes this exact header regression.

Previously requested nonexistent steps crossed scene storage. For example scene-1 step-1 reply contains scene-2's header at byte 156, and scene-1 step-3 contains scene-3's header at byte 20. Do not import those old bogus frames into a show or use the old report as a restoration image.

The host now runs the isolated reader from Settings only after live USB disconnect, suspends automatic takeover, retains partial JSON, exposes a bounded summary to clients, and exports the full report separately. Read commands are idempotently retried up to three times and repeated for comparison. Parent cancellation closes a pipe; the worker checks it between reads and resumes/closes its maintenance connection. No write/erase opcode is exposed.

Further write-flow evidence: Pro2 0x728E32 writes configuration via 0x4CFCA0; 0x728E59 calls 0x4D0280; 0x728E89 calls erase 0x4CFBD0. The surrounding initializer 0x728DD0 onward also calls global programming routines 0x4D00E0, 0x4D0360/0x4D04D0, 0x4D0640/0x4D0730/0x4D08A0, 0x4D0A10/0x4D0B30/0x4D0CD0 depending on device/features. Per-scene header/trigger writes occur at 0x7262C8/0x7262F3; step writes at 0x727F4D/0x727F8F. These global sections, allocation/commit and recovery remain unverified. Full scene data alone must not be labelled complete flash backup, and no guessed write is enabled.
