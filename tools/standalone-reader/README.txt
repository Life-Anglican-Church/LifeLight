LifeAC consolidated standalone tester 0.3

Use on the PROJECTOR PC with its existing Lighting USB runtime installed.
1. Exit LifeAC Lighting from the tray menu. Close ProDMX and earlier testers.
2. Run LifeAC-Standalone-Tester-0.3.exe. Click Run complete check & export report.
3. Wait for the report, then click Show report file and share that ZIP in chat.

One run collects firmware, driver details, configured PC show and MIDI mappings,
retained troubleshooting logs, controller configuration, scene headers, triggers,
step timings and used channel values. Scene/step reads are repeated and compared.
Counts, model and packet responses are checked; unsupported cases fail closed.
Partial results and the exact failing operation are retained. Allow up to 10 minutes.

This may pause standalone playback while reading. No live DMX frames, scene-memory
writes/erase, firmware updates, driver installs or Windows startup changes occur.
Nothing is uploaded automatically. Accounts, passwords, PINs, updater credentials,
device keys, USB serial numbers and certificate private keys are excluded. Names
and contents of the lighting show remain in the report so we can diagnose it.

This is the most comprehensive tester implemented so far, not a promise that all
device memory is readable. Device-wide tables, flash allocation/commit and verified
write/restore remain unresolved. A successful scene read is NOT a full flash backup.
No fixture identity discovery is claimed: ordinary DMX provides no such feedback.

Offline packet/inventory and redaction checks pass. Actual scene reading still
requires this on-site run. The normal Lighting updater does not run this tester.

Source is included. build.ps1 builds with the Windows .NET Framework compiler;
the worker's --self-test mode uses allocated memory only and never loads USB code.
No proprietary vendor binaries or credentials are included in this tester package.
