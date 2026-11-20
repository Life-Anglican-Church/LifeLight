# LifeAC Lighting 0.2.1 — desktop startup fix

Fixes the tray-only startup failure in 0.2.0. Electron readiness is now handled after entry-module evaluation, and tray commands use an authenticated Windows named pipe instead of unreliable GUI stdin. The window is created before backend preparation. Shutdown closes event streams and connections cleanly, avoiding a hidden process that blocks updates. Startup errors are written to the local startup log with bootstrap secrets redacted.

The regression test runs the real Electron runtime in simulation, renders all four Sunday Services scene buttons in an invisible window, then shuts down through the named pipe. No USB or native MIDI access occurs in this test. Core, parser, authentication, TLS and browser UI checks also pass.

Close the old Lighting host using its tray menu, then run the updater from this release. Existing shows and backups are preserved.

Standalone memory work is still in progress. The supplied ProDMX installer has now been statically unpacked, and its actual memory-read, memory-write and transport routines have been located. This hotfix does not enable unverified device-memory commands.
