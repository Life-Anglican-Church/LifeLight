# Projector-PC commissioning

Use a current maintenance window. Do not commission on the disconnected laptop. Keep the original ProDMX show and existing controller available for recovery.

1. Close other controller/diagnostic apps. Open Lighting and check the named patch/groups; app launch must send no DMX.
2. Take control with a familiar supported scene. Verify one clearly visible dimmer and the actual fixture address convention before trusting the whole patch.
3. Check each of the six moving heads, especially the two previously dark heads. The imported profiles lack shutter preset ranges; consult the actual modes/manuals before changing shutter/control values. Do not sweep reset/internal channels.
4. Select two scenes with identical positions and different brightness: no movement or shutter pulse should occur. Then check a small pan/tilt change and a 16-bit pair. Interrupt a fade with a new scene and verify continuity. Verify Hold and Blackout on a dimmer and a colour-only fixture.
5. Confirm cancellation of Live Preview restores the prior commanded values, save/reload a duplicated scene, and add a known spare light without patch overlap. Review address/mode on the physical fixture.
6. Verify USB release and original-controller handover. Check browser closure keeps host output running, then tray Stop releases the host. Check projector and Welcome operation remain unaffected.
7. Test LAN only after accounts, certificate trust and Private-network firewall access are deliberately configured. Confirm an operator cannot edit fixtures and Wi-Fi loss does not interrupt the host.
8. Test the real MIDI input and a mapped scene; a held note/controller must not restart its fade repeatedly.
9. Independent port/universe behaviour remains disabled until verified. Standalone scene-memory reads/writes remain blocked pending a documented vendor API/workflow.

If a check fails, Hold/release as appropriate, recover with the original controller and export diagnostics. Do not run an automatic reconnect/blackout/firmware update as a recovery shortcut.
