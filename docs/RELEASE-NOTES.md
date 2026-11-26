# LifeAC Lighting 0.8.1 — LAN connections and automatic sign-in

Update the Primary/Backup host and every Network Controller. Close Lighting from its tray before using the lightweight updater. Approve the Windows administrator prompt during the host update, or use Settings → LAN & accounts → Allow LAN through Windows firewall afterwards.

- Firewall setup always refreshes the rules and requests administrator approval instead of silently accepting old rules. The host checks the helper exit code and shows success or refusal. TCP 47832 and UDP 47833 are allowed for the installed host Node executable across Windows network profiles and routed VPN peers. Host updates refresh the executable path in these rules.
- Bare IP addresses and computer names automatically use HTTPS port 47832. Explicit custom ports remain supported. The connection checks that the address is a Lighting host and reports reachability, certificate or interface-loading failures separately.
- Network Controllers scan at startup instead of opening the previous IP. Multiple interface addresses for a known host are probed and the reachable endpoint is used. Discovery probes each adapter's broadcast; remembered computer names are also resolved and queried directly. The host replies using the interface Windows routes to the requester, including VPN routes. Broadcast-free VPN discovery needs working host-name resolution or a manually entered IP; the application cannot force a VPN/router to forward broadcasts.
- Saved sign-in details are encrypted with Windows and matched to the trusted host certificate rather than its IP. The host retains its certificate across DHCP address changes. Changed credentials return to sign-in; a changed certificate requires trust again. First use of this version requires account sign-in once to create this cache.
- Network Controllers run as normal desktop applications. Closing their window exits; there is no tray icon or background host. Primary and Backup keep their independent host/tray behavior.
- The connection/retry/search/manual-IP screen now uses the same dark panels, blue controls, typography and church branding as the console. The separate native host-picker window is removed.

Existing shows, scenes, roles, accounts and device drivers are retained. No change to lighting playback or USB/MIDI ownership.
