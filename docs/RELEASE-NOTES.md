# LifeAC Lighting 0.3.1 — manual fixture holds and 11am mode

Exit Lighting from its tray menu, then run the updater. Existing shows and accounts are preserved.

- Lights > Manual controls adjusts one fixture without stopping scenes, timed steps, effects or MIDI. Each changed channel stays held through subsequent scene changes, including a dimmer held at zero. The shared banner lists lights with held changes. Manual holds live on the host, so closing a client or using another LAN client does not clear them. Automatic USB reconnect keeps them. Release clears all holds; stopping the host also ends the session. They are not restored on a new host session or after shutdown.
- Save manual changes to a scene applies the held values to every step in the selected scene, saves a normal show backup, clears the holds and plays that scene. Saving from the scene editor also includes held values and clears the holds. Saving requires Admin mode.
- The 11am button toggles 75% brightness across the show without changing saved scenes. It remains active through scene and MIDI changes. Fixtures with a dedicated dimmer scale that dimmer; colour-only fixtures scale their colour channels. Pan, tilt, shutters and protected channels are unchanged. Sixteen-bit dimmers scale as one value. The toggle works when selected; it does not schedule itself by day or time. It resets to normal in a new host session.
- Manual holds apply after effects, then 11am brightness applies. Consequently a held zero remains zero, and a held nonzero dimmer is also reduced by 25%. Repeated scene selection never compounds the reduction.

Validated with 21 simulation/server/security tests, including new override, fade/effect/step, preview, reconnect, 16-bit dimmer, save and viewer-permission checks. Desktop/mobile browser checks cover the manual-control dialog, holds across scene changes, shared banner, 11am toggle and save-to-scene flow. No native USB or MIDI was opened on the laptop. Physical fixture response still requires on-site confirmation. Standalone writing remains disabled.

The expanded standalone tester remains separately available on the 0.3.0 release. It is not run by the updater.