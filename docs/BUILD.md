# Building and publishing

Windows 10/11 x64 with .NET Framework 4.8 and Node.js 22+ is sufficient for development. There are no npm dependencies.

- `build.cmd` compiles the Windows launcher and the three native/system helpers.
- `node --test tests/*.test.mjs` runs the importer/engine/storage/security/TLS/frame-validation checks. These do not open a USB device. The TLS test creates only temporary files, never a trusted certificate installation.
- `tests/ui-check.mjs` is an optional simulation-only browser check; pass the absolute path to an installed Playwright index.mjs. Do not launch a real USB host on a disconnected developer laptop.
- `scripts/build-release.ps1` builds the updater, portable package and offline installer. The script downloads/checks the pinned official Node runtime if missing. It expects the read-access token in `../.release-secrets/read.txt`, outside the source tree, and embeds it as an assembly resource per the owner's instruction. Never commit that file. Use existing script-execution policy; no global policy change is required for the app.
- `scripts/github-release.mjs` uses `../.release-secrets/publish.txt` for release administration. Its commands inspect/create, publish-source, release, upload and finalize operate on the private LifeAnglicanChurch/LifeACLighting repository. Do not use the publishing token as the embedded read credential. Version/tag/manifest constants must be updated coherently for a subsequent release.

The installer contains a SHA-256-checked ZIP resource. It extracts only into a fresh, bounded per-user staging directory; validates required files; installs a separate release directory; creates shortcuts and HKCU uninstall registration; and preserves show data. The updater authenticates to api.github.com, follows signed asset redirects without forwarding the token, checks the downloaded package against the manifest and refuses to update while the lighting host is running. Credentials are not included in application diagnostics.

Installer/updater execution was not used to install onto the developer laptop. Inspecting embedded resources and validating remote package hashes are read-only alternatives. The owner must perform the first actual installation/USB commissioning on the projector PC.
