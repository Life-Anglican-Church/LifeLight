# Experimental standalone reader

This is a separate research tool, not an enabled writer in Lighting. It implements one stored-configuration read traced from the supplied ProDMX binary. It does not yet decode scenes or make a full memory backup. Its first hardware response is needed before extending the decoder or implementing writes.

On the projector PC: close Lighting, ProDMX and earlier lighting test apps, then run LifeAC-Standalone-Reader.exe and click its one read button. It exports a ZIP under the current Windows user's LifeAC-Lighting/Reports folder; click Show report file and attach that ZIP in the chat. The first maintenance connection may pause playback. Restore the normal controller afterwards if needed.

The worker accepts only the exact previously verified 2023 SDK and libusb DLL fingerprints and firmware 2.4.2.3. It uses NoDmx mode, binds no DMX-frame output or flash-write exports, and has no erase, firmware update, key-read or key-write operation. It reads legacy standalone configuration (group 0x50, opcode 0x21), not the newer raw-flash read command. Unknown firmware, binary signatures, reply format or payload size stops the read. The worker is isolated and bounded by a 60-second GUI watchdog. No USB runtime or driver is installed. An existing verified private runtime is required; the previous diagnostic runtime can be reused after fingerprint verification.

Raw configuration is opaque and private. No Windows usernames, hostnames, passwords or SDK/device keys are deliberately collected, and nothing is uploaded automatically. Because configuration fields are not fully decoded, this is not a guarantee that every byte of raw show data is free of private information. Attach it privately here, not in a public issue.

Offline verification has passed for the SDK hash and all private function signatures. The GUI and isolated x86 worker compile. No hardware read or write has been run on the disconnected laptop. Hardware operation remains experimental and unverified.

Build with the Windows .NET Framework C# compiler: compile StandaloneReaderWorker.cs as an x86 console EXE with System.Web.Extensions, then compile StandaloneReader.cs as a Windows Forms EXE with System.Drawing, System.IO.Compression and System.IO.Compression.FileSystem, embedding the worker as LifeAC.StandaloneReaderWorker. The GUI executable contains its own worker.
