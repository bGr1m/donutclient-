# Donut Client

A client-side [Fabric](https://fabricmc.net/) mod for **Minecraft Java 1.21.11** with a click GUI,
configurable highlights, fall-safety "clutch" modules, and stash-hunting automation.

Press **Right Shift** to open the menu. Every module can be toggled from the GUI, from its own
keybind, or from Minecraft's **Controls** screen.

## Modules

| Module | What it does |
| --- | --- |
| **StorageFinder** | Highlights chests, trapped chests, ender chests, shulker boxes and droppers in loaded chunks. |
| **SpawnerFinder** | Highlights monster and trial spawners. |
| **Block ESP** | Highlights any blocks you pick from a catalogue of 25 (ores, ancient debris, nether ores, chests, barrels, spawners), each with its own colour. |
| **Freecam** | Camera-only detached flight (default **F6**). Never moves your player. |
| **PearlSave** | Throws an ender pearl straight down when a fall would be lethal, resetting your fall distance. |
| **MLG** | Places a water bucket just before a lethal landing, and can scoop the water back up once you land unharmed. |
| **AutoEat** | Holds right-click on food in your hotbar whenever your hunger drops below a threshold. |
| **Auto Tool** | Swaps to the fastest hotbar tool for the block you are mining. |
| **RTP Plus** | Automated stash hunting: sends `/rtp`, digs straight down to a target depth, and repeats until the StorageFinder finds enough storage, then logs off. Re-RTPs instantly on lava / after 2 s in water and when health is low, and logs off when a stash or a spawner is found. |

The GUI also has favorites, module search, per-module keybinds, an editable accent colour, and a
names-only HUD list of the active modules.

## Requirements

- Minecraft **1.21.11**
- **Fabric Loader** 0.18.4 or newer
- **Fabric API** 0.141.4+1.21.11 or newer
- **Java 21** or newer

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11 in your `mods` folder.
3. Put `donut-client-1.1.0.jar` in your `mods` folder.
4. Launch the Fabric profile.

## Build from source

You need **JDK 21**.

```powershell
# Windows
.\gradlew.bat build
```

```bash
# Linux / macOS
./gradlew build
```

The installable, remapped jar is written to `build/libs/donut-client-1.1.0.jar`.
The unit tests run as part of `build`. To run the in-game smoke test instead (needs a working
graphics device):

```powershell
.\gradlew.bat runClientGameTest
```

## Configuration

All settings are stored in `config/donut-client.json` inside your Minecraft directory and are saved
automatically. Visual modules (finders, Block ESP) and module keybinds persist between sessions;
safety/automation modules start disabled each session.

## Notes

- Only blocks in **already-loaded chunks** can be highlighted. The finders never request chunks, so
  nothing loads on its own.
- Everything is **client-side**. Several modules move or act for you and can be flagged by server
  anti-cheats — use at your own risk and only where the server's rules allow it.

## Credits

- [Fabric](https://fabricmc.net/) — mod loader.
- **Inter** font (SIL Open Font License).

## License

MIT — see [LICENSE](LICENSE).
