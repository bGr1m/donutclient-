package dev.donut.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.donut.client.automation.AutoEatController;
import dev.donut.client.automation.AutoToolController;
import dev.donut.client.automation.RtpPlusController;
import dev.donut.client.automation.RtpStashController;
import dev.donut.client.config.ClientConfig;
import dev.donut.client.config.ConfigStore;
import dev.donut.client.finder.FinderScanner;
import dev.donut.client.freecam.FreecamController;
import dev.donut.client.gui.DonutScreen;
import dev.donut.client.gui.ModuleArrayList;
import dev.donut.client.input.ModuleKeybinds;
import dev.donut.client.render.HighlightRenderer;
import dev.donut.client.safety.MlgController;
import dev.donut.client.safety.PearlSaveController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class DonutClient implements ClientModInitializer {
    public static final String MOD_ID = "donut";
    private ConfigStore store;
    private ClientConfig config;
    private FinderScanner scanner;

    @Override
    public void onInitializeClient() {
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("donut-client.json"));
        config = store.load();
        scanner = new FinderScanner(config);

        FreecamController freecam = new FreecamController(config);
        ClientTickEvents.START_CLIENT_TICK.register(freecam::tick);

        PearlSaveController pearlSave = new PearlSaveController(config.pearlSave);
        MlgController mlg = new MlgController(config.mlg);
        AutoEatController autoEat = new AutoEatController(config.autoEat);
        AutoToolController autoTool = new AutoToolController(config.autoTool);
        RtpStashController rtpStash = new RtpStashController(config.rtpStash, scanner);
        RtpPlusController rtpPlus = new RtpPlusController(config.rtpPlus, config, scanner, rtpStash);
        ClientTickEvents.START_CLIENT_TICK.register(pearlSave::tick);
        ClientTickEvents.START_CLIENT_TICK.register(mlg::tick);
        ClientTickEvents.START_CLIENT_TICK.register(autoEat::tick);
        ClientTickEvents.START_CLIENT_TICK.register(autoTool::tick);
        ClientTickEvents.START_CLIENT_TICK.register(rtpStash::tick);
        ClientTickEvents.START_CLIENT_TICK.register(rtpPlus::tick);

        HighlightRenderer renderer = new HighlightRenderer(config, scanner);
        renderer.register();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "client"));
        KeyMapping open = bind("open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
        KeyMapping storage = bind("toggle_storage", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping spawner = bind("toggle_spawner", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping camera = bind("toggle_freecam", GLFW.GLFW_KEY_F6, category);
        KeyMapping pearl = bind("toggle_pearl_save", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping mlgKey = bind("toggle_mlg", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping autoEatKey = bind("toggle_auto_eat", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping autoToolKey = bind("toggle_auto_tool", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping rtpPlusKey = bind("toggle_rtp_plus", InputConstants.UNKNOWN.getValue(), category);
        KeyMapping blockEsp = bind("toggle_block_esp", InputConstants.UNKNOWN.getValue(), category);
        ModuleKeybinds.initialize(storage, spawner, camera, pearl, mlgKey, autoEatKey, autoToolKey, rtpPlusKey, blockEsp);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean inputBlocked = ModuleKeybinds.inputBlocked(client);
            while (open.consumeClick()) {
                if (!inputBlocked && client.screen == null) client.setScreen(new DonutScreen(config, this::save));
            }
            if (ModuleKeybinds.consumePress(client, 0)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.storage.enabled = !config.storage.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 1)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.spawner.enabled = !config.spawner.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 2)) {
                if (!inputBlocked && client.screen == null && client.player != null) freecam.setEnabled(!freecam.isActive());
            }
            if (ModuleKeybinds.consumePress(client, 3)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.pearlSave.enabled = !config.pearlSave.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 4)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.mlg.enabled = !config.mlg.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 5)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.autoEat.enabled = !config.autoEat.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 6)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.autoTool.enabled = !config.autoTool.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 7)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.rtpPlus.enabled = !config.rtpPlus.enabled; save(); }
            }
            if (ModuleKeybinds.consumePress(client, 8)) {
                if (!inputBlocked && client.screen == null && client.player != null) { config.blockEsp.enabled = !config.blockEsp.enabled; save(); }
            }
            scanner.tick(client);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                Screens.getButtons(screen).add(Button.builder(Component.literal("Donut"), button ->
                        client.setScreen(new DonutScreen(config, this::save)))
                        .bounds(width - 66, height - 28, 58, 20).build());
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "status"), (graphics, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (!config.showHud || client.player == null || client.screen != null || client.options.hideGui) return;
            ModuleArrayList.render(graphics, client.font, client.getWindow().getGuiScaledWidth(), config, freecam.isActive());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { scanner.clear(); freecam.disable(); });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            freecam.disable();
            save();
            renderer.close();
        });
    }

    private KeyMapping bind(String name, int key, KeyMapping.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.donut." + name, InputConstants.Type.KEYSYM, key, category));
    }

    private void save() { store.save(config); }
}
