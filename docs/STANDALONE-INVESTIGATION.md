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
