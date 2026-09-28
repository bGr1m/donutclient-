# Validation

Automated configuration tests cover defaults, JSON persistence and replacement, invalid-file recovery/backups, missing and future fields, unknown modes, numeric boundaries, NaN/infinity, and color sanitization.

Run `./gradlew build` with JDK 21. The installable, remapped artifact is `build/libs/donut-client-1.1.0.jar`.

Run `./gradlew runClientGameTest` for the isolated Minecraft smoke test. It creates a disposable world with all seven target types, checks scanner counts and filtering, clicks the GUI controls, and captures screenshots of both depth policies for each render mode. Test code and fixtures are excluded from the installable jar. This requires a working graphics device.

The Freecam runtime test compares both the actual client player and integrated server player before and after camera movement, checks speed/boost/vertical movement and mouse look, verifies action queues are cleared, and checks that disabling restores normal input without undoing a server teleport. It also checks disconnect cleanup and clicks the Freecam GUI controls. Configuration tests cover persistence of speed/favorites and deliberately non-persistent activation.

## Verified on 2026-09-27

- `build`: passed; all 15 configuration tests passed.
- `runClientGameTest`: all three runtime suites passed (finders, Freecam, and module keybinds).
- Assigned keys to all three modules through the production click GUI; verified keyboard/mouse capture, Escape cancellation (including key repeat), Delete/Backspace clearing, `options.txt` persistence, and reloading through Minecraft Options.
- Verified real shortcuts toggle each module, native key repeats do not retoggle a held binding, menu input does not replay after closing, and holding Sprint across menu closure does not block shortcuts.
- Reviewed captured GUI screenshots at GUI scales 1 and 2, including the scaled binding-capture control. Rounded edges and icons use antialiased geometry.
- Reviewed the names-only array list at both scales and confirmed F1 hides it. All-disabled state was also captured.
- Updated representative screenshots in `docs/screenshots/`. Third-party renderers and shader packs were not tested.

## Verified on 2026-09-23

- `build`: passed; all 15 configuration tests passed, including Freecam settings persistence, disabled activation on load, and numeric sanitization.
- `runClientGameTest`: both the finder and Freecam runtime suites passed on Windows with Minecraft 1.21.11, Fabric Loader 0.18.4 and Fabric API 0.141.4+1.21.11.
- GUI module switches, mode selection, opacity input, save callbacks, and Freecam speed/boost sliders passed assertions.
- All seven target types and dropper filtering passed loaded-world assertions.
- Captured all six combinations of render mode and depth policy. Visual review confirmed outlines, fill, and wall occlusion. Representative screenshots are in `docs/screenshots/`.
- Freecam forward, sprint-boosted, and vertical movement passed while both the actual client player and integrated-server player retained their positions and rotations. Real mouse input and direct camera rotation also preserved both player poses.
- Freecam interaction queues were cleared; disabling restored the original player input and camera. A server teleport received during Freecam remained authoritative after disabling, and disconnect cleanup reset activation.
- External multiplayer servers, third-party shader packs, and alternate renderers have not been tested.

## Gameplay checks

Use a separate creative singleplayer world with commands enabled:

1. Open the Donut title-screen button and check the module list, tabs and typography. Enter a world and open the GUI with Right Shift.
2. Place a chest, double chest, trapped chest, ender chest, regular and colored shulker boxes, dropper, monster spawner and trial spawner. Dispensers should not match the dropper filter.
3. Enable each finder independently; switch between Outline, Filled and Both. Verify the outlines track the block shapes while walking and turning the camera.
4. Place an opaque wall between the player and targets. Toggle Through walls to compare rendering with and without depth testing.
5. Set each opacity to zero and full, and compare line widths. Change base and family colors using presets, RGB sliders and hex entry.
6. Turn each filter off and confirm only the corresponding blocks disappear. Break a highlighted block; its highlight should disappear immediately.
7. Reduce range and walk across its boundary. Test distance fading, unload chunks, and change dimension/world; previous-world targets should disappear.
8. Scroll all settings, use favorites/search, edit the accent and HUD setting, close/reopen, and restart the game to check persistence. Test another window size and GUI scale.
9. Assign each module's key in the click GUI and confirm the same binding appears in Minecraft Controls. Test keyboard and mouse bindings, Escape to cancel, Delete/Backspace to clear, conflicts, and persistence after restarting. Capturing a binding must not toggle its module; shortcuts must stay inactive in chat and menus.
10. Enable all three modules and check the array list at the top right: names only, longest first, with soft shadows and no solid box or counters. Disable modules and the Array list appearance option, and check F1 hides the HUD.
11. Inspect rounded corners, favorite/search/module icons, toggle movement, and scrolling at GUI scales 1 and 2 and a smaller window. Test with the intended shader/rendering mods separately if using them.

Only checks actually executed are reported in the delivery message; this checklist is not a claim that every renderer/mod combination has been tested.
