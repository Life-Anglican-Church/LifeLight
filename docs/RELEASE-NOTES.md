# LifeAC Lighting 0.5.0 — Live scene ordering, fixture groups and lighting capture

Close Lighting from its tray menu, then run the updater or the new installer. Existing shows, accounts, scene order, MIDI mappings, photo positions and installation role are kept. Windows only.

## Main screen and ordering
- Drag a scene using its six-dot handle in Admin mode. A full-size tile follows the pointer and the grid animates into its new order underneath. Only dropping inside the grid saves the order; cancelling restores it. Dragging never runs the scene.
- The stage image uses substantially more of the right side. Fixture pins are smaller squares with readable labels on light and dark colours. Scene cards remain four across at normal desktop widths.
- Removed obsolete arrow-control styling and replaced the font-based handle with six drawn dots. Scene settings remain in the right-click menu.
- Settings and Volunteer/Admin controls use the same dimensions and button style as 11am. Toolbar actions, forms and settings buttons have more consistent spacing. Dialog errors now appear inside the dialog.

## Light groups and brightness
- In Admin mode, select lights and open Choose lights > Create light group. Give the group a name and choose its members. Each group has one photo pin and one named button; clicking it selects all members and displays their shared controls. Right-click the named group button to rename it, change members or ungroup it. Individual selection remains available under Choose lights and in the scene editor's light selector. The settings-only placement configurator always shows individual pins and live identification controls.
- Every currently configured fixture now has a Dimmer control. Fixtures with a hardware dimmer use that channel. Colour-only/white-only fixtures get a software dimmer that scales their emitter channels together, retaining colour through zero and leaving movement, shutter and protected/function channels untouched. When all colour channels start at zero, raising brightness starts with white emitters, or RGB white if no white emitter exists.
- Manual software-dimmer values hold across scene/MIDI changes like existing manual controls. Release all lights clears both kinds of hold and keeps USB connected. The separate 11am reduction still applies once after the manual values.

## Save current lighting
- Save is next to Release all lights in Admin mode. All configured lights are selected by default. Use the photo, All lights/Clear selection or individual fixture buttons to choose a subset.
- Save to an existing scene or create a named scene in a chosen folder. The action is labelled Save & use scene because it activates the saved scene. Selected lights' manual holds are cleared; other lights' holds remain.
- Capture uses the host's current commanded values before the 11am reduction. It is not a physical-state readback. Existing scene timing and unselected fixture values are kept; selected fixture values are copied to every step and effects on those selected fixtures are removed. New scenes are static and include the current scene's other known channel values. Protected channels are never rewritten by capture.

## Validation and limits
32 automated engine/server/security/protocol tests pass. Windows desktop simulation checks live drag preview/reflow and cancellation, saved order, real and software dimmers, group creation/control/ungrouping, photo-selected capture/new scene, held-value release, equal header controls and desktop layout. The real hidden Electron client opens the dashboard, exits and leaves its independent simulation host running. Native helpers compile. No physical USB, native MIDI input, ProDMX or installer is run on the laptop.

Host MIDI, LAN authentication, background engine ownership and role-aware installation/updating are retained. Their physical operation still needs the projector PC. Standalone memory writing remains unfinished and disabled; this release does not add it or claim a complete verified restoration backup. Existing standalone reads remain available in Settings after live output is disconnected.
