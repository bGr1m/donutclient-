# Donut Client

A client-side Fabric mod for Minecraft Java 1.21.11 with storage and spawner highlights, Freecam, module keybinds, and a minimal HUD.

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API 0.141.4 or newer for Minecraft 1.21.11.
3. Copy `donut-client-1.1.0.jar` into your Minecraft `mods` folder.
4. Start Minecraft with the Fabric profile.

Press **Right Shift** to open the click GUI. Freecam defaults to **F6**. Other module keys can be assigned in the click GUI or under Minecraft's keybind settings.

## Build

Install JDK 21, then run:

```powershell
.\gradlew.bat build
```

The finished mod is written to `build/libs/donut-client-1.1.0.jar`.

## License

MIT. The bundled Inter font uses the SIL Open Font License.
