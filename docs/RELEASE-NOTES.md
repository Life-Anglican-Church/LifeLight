# LifeAC Lighting 0.8.2 — Host LAN startup and firewall repair

Close Lighting from the host tray and close Network Controller windows, then run the lightweight updater on both PCs. Approve the Windows administrator prompt on the host. No separate checker is required.

- Windows firewall repair now uses the Windows firewall API, verifies its rules, and disables conflicting inbound block rules only for this exact Lighting runtime. It leaves other applications and outbound rules unchanged. Ordinary allow rules cannot override an existing Windows block rule. Ports remain TCP 47832 and UDP 47833, scoped to the Lighting executable.
- Firewall handling has its own host-only helper, independent of the updater executable and its version. This prevents an older updater from restoring older firewall handling after installing a new application.
- The host validates its saved HTTPS certificate/password pair. It repairs an unusable identity, preserves a valid identity when IPs change, and persists the identity before attempting to bind the listener. A busy port can no longer leave a new certificate paired with the previous password. Certificate generation has a timeout.
- Discovery starts independently before HTTPS initialization and automatically retries a failed UDP bind. A temporary startup failure no longer leaves discovery permanently absent for that session.
- Host Settings displays actual listener status and its startup error when unavailable. Network Controllers still have no tray process and exit when closed. Saved account access, scenes, layouts and DMX/MIDI ownership are unchanged.

These repairs address reproduced application failure paths. They do not establish which of those paths occurred on the church PC or prove that an external router/firewall allows the connection.
