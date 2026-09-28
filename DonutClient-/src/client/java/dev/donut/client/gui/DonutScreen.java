package dev.donut.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.donut.client.config.ClientConfig;
import dev.donut.client.config.BlockEspCatalog;
import dev.donut.client.config.BlockEspSettings;
import dev.donut.client.config.FinderSettings;
import dev.donut.client.config.ModuleSettings;
import dev.donut.client.config.RenderMode;
import dev.donut.client.input.ModuleKeybinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static dev.donut.client.gui.UiPaint.*;

/** A compact, self-contained click GUI. Settings take effect immediately. */
public final class DonutScreen extends Screen {
    private static final int W = 650, H = 408;
    private static final int TEXT = 0xFFE9EDF5, MUTED = 0xFF949DAD, DIM = 0xFF606A7B;
    private static final int PANEL = 0xFF101319, CARD = 0xFF1B202B, BORDER = 0xFF292F3B;
    private static final int CX = 154, CW = 474, TOP = 111, BOTTOM = 369;
    private static final int[] PALETTE = {0x4C86FF, 0x68B4FF, 0x6DDAC5, 0x7EDD8E, 0xC5E774,
            0xFFC66B, 0xFF975F, 0xFF707B, 0xEC80B5, 0xBA8CFF, 0xC8D1E8, 0xFFFFFF};

    private final ClientConfig config;
    private final Runnable save;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Double> animations = new HashMap<>();
    private float scale = 1;
    private float originX, originY;
    private double mouseX, mouseY;
    private int tab;
    private int selected = -1;
    private double scroll;
    private double scrollTarget;
    private long lastFrame;
    private double frameSeconds;
    private int contentHeight;
    private String query = "";
    private boolean searchFocused;
    private boolean dirty;
    private Hit dragging;
    private String keyboardFocus;
    private ColorEditor colorEditor;
    private boolean collectingClip;
    private int binding = -1;
    private int hoveredBinding = -1;
    private InputConstants.Key capturedKey;
    private double sideScroll;
    private double sideScrollTarget;

    private static final int SIDE_TOP = 99, SIDE_ROW = 30, SIDE_VISIBLE = 9;
    private static final String[] SIDE_LABELS = {
            "Overview", "Storage", "Spawners", "Freecam", "PearlSave",
            "MLG", "AutoEat", "Auto Tool", "RTP Plus", "Block ESP"};
    private static final int[] SIDE_INDEX = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8};

    public DonutScreen(ClientConfig config, Runnable save) {
        super(Component.literal("Donut Client"));
        this.config = config;
        this.save = save;
    }

    @Override
    protected void init() {
        scale = Math.min(1.0f, Math.min((width - 16f) / W, (height - 16f) / H));
        scale = Math.max(0.1f, scale);
        originX = (width - W * scale) / 2f;
        originY = (height - H * scale) / 2f;
        dragging = null;
        hits.clear();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        long now = System.nanoTime();
        frameSeconds = lastFrame == 0 ? 1.0 / 60 : Math.min(.1, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        scroll = ease(scroll, scrollTarget, 18);
        sideScroll = ease(sideScroll, sideScrollTarget, 18);
        mouseX = (mx - originX) / scale;
        mouseY = (my - originY) / scale;
        hoveredBinding = -1;
        hits.clear();
        // Keep the live world visible under a quiet tint; no pause or expensive blur pass.
        g.fill(0, 0, width, height, 0x66050911);
        g.pose().pushMatrix();
        g.pose().translate(originX, originY);
        g.pose().scale(scale, scale);
        shadow(g, 0, 0, W, H, 13, 0x80000000);
        round(g, 0, 0, W, H, 13, BORDER);
        round(g, 1, 1, W - 2, H - 2, 12, PANEL);
        round(g, 1, 49, W - 2, 1, 0, 0xFF242A35);
        round(g, 137, 66, 1, H - 106, 0, 0xFF242A35);
        drawChrome(g);
        if (tab == 2) drawAppearance(g);
        else if (selected >= 0) drawSettings(g);
        else drawOverview(g);
        round(g, 16, H - 31, W - 32, 1, 0, 0xFF242A35);
        drawFooter(g);
        if (colorEditor != null) drawColorEditor(g);
        if (keyboardFocus != null) {
            hits.stream().filter(h -> h.id.equals(keyboardFocus)).findFirst().ifPresent(h ->
                    outline(g, h.x - 2, h.y - 2, h.w + 4, h.h + 4, 6, alpha(config.accentColor, .85)));
        }
        g.pose().popMatrix();
    }

    private void drawChrome(GuiGraphics g) {
        round(g, 17, 14, 23, 23, 11, alpha(config.accentColor, 1));
        round(g, 24, 21, 9, 9, 4, PANEL);
        round(g, 31, 16, 2, 3, 1, 0xFFFFFFFF);
        strong(g, font, "DONUT", 48, 21, TEXT);
        nav(g, "Modules", 191, 78, 0);
        nav(g, "Favorites", 273, 83, 1);
        nav(g, "Appearance", 361, 99, 2);
        double closeHover = animate("closeHover", inside(610, 13, 26, 25) ? 1 : 0);
        round(g, 610, 13, 26, 25, 6, blend(PANEL, CARD, closeHover));
        line(g, 619, 21, 626, 28, MUTED);
        line(g, 626, 21, 619, 28, MUTED);
        hit("close", 610, 13, 26, 25, this::onClose);

        text(g, font, tab == 2 ? "PERSONALIZE" : "VISUAL", 22, 72, DIM);
        if (tab == 2) {
            sidebar(g, "Theme & HUD", 99, true, () -> { });
        } else {
        int startRow = (int) Math.round(sideScroll);
        for (int i = startRow; i < Math.min(SIDE_LABELS.length, startRow + SIDE_VISIBLE); i++) {
            int y = SIDE_TOP + (i - startRow) * SIDE_ROW;
            int index = SIDE_INDEX[i];
            final int choice = index;
            sidebar(g, SIDE_LABELS[i], y, selected == index, () -> select(choice));
            if (index >= 0 && enabled(index)) round(g, 117, y + 12, 4, 4, 2, alpha(config.accentColor, 1));
        }
        }
        text(g, font, "DONUT CLIENT", 22, 55, DIM);
        text(g, font, activeCount() + " / 9 active", 22, 55 + 14, MUTED);
    }

    private void nav(GuiGraphics g, String label, int x, int w, int target) {
        double hover = animate("navHover" + target, inside(x, 13, w, 25) ? 1 : 0);
        double active = animate("navActive" + target, tab == target ? 1 : 0);
        round(g, x, 13, w, 25, 7, blend(blend(PANEL, 0xFF171B24, hover), CARD, active));
        text(g, font, label, x + 12, 21, tab == target ? TEXT : MUTED);
        hit("tab" + target, x, 13, w, 25, () -> { tab = target; select(-1); query = ""; searchFocused = false; });
    }

    private void sidebar(GuiGraphics g, String title, int y, boolean active, Runnable action) {
        double hover = animate("sideHover" + title, inside(14, y, 114, 29) ? 1 : 0);
        double selected = animate("sideActive" + title, active ? 1 : 0);
        round(g, 14, y, 114, 29, 6,
                blend(blend(PANEL, 0xFF191E27, hover), blend(PANEL, config.accentColor, .15), selected));
        if (active) round(g, 14, y + 7, 3, 15, 1, alpha(config.accentColor, 1));
        text(g, font, title, 25, y + 10, active ? TEXT : MUTED);
        hit("side" + title, 14, y, 114, 29, action);
    }

    private void select(int value) {
        selected = value;
        scroll = scrollTarget = 0;
        animations.clear();
        searchFocused = false;
        keyboardFocus = null;
    }

    private void drawOverview(GuiGraphics g) {
        strong(g, font, tab == 1 ? "Your favorites" : "Visual modules", CX, 73, TEXT);
        text(g, font, "A clearer view of your world.", CX, 91, MUTED);
        round(g, 421, 69, 207, 29, 7, searchFocused ? BORDER : CARD);
        search(g, 432, 78, MUTED);
        String shown = query.isEmpty() && !searchFocused ? "Search modules..." : query + (searchFocused && System.currentTimeMillis() % 1000 < 500 ? "|" : "");
        while (UiPaint.width(font, shown) > 166 && !shown.isEmpty()) shown = removeFirstCharacter(shown);
        text(g, font, shown, 450, 79, query.isEmpty() ? DIM : TEXT);
        hit("search", 421, 69, 207, 29, () -> searchFocused = true);

        beginContent(g);
        int y = 119 - (int) scroll;
        boolean any = false;
        for (int i = 0; i < 9; i++) {
            if (!matches(i)) continue;
            moduleCard(g, i, y);
            y += 77;
            any = true;
        }
        if (!any) {
            round(g, CX, y, CW, 125, 10, 0xFF161B24);
            text(g, font, query.isEmpty() ? "Keep your essentials close." : "No matching modules", CX + 22, y + 34, TEXT);
            text(g, font, query.isEmpty() ? "Click a star on a module to add it here." : "Try storage, spawner, freecam or pearl.", CX + 22, y + 57, MUTED);
            y += 125;
        }
        endContent(g, y);
    }

    private boolean matches(int index) {
        if (tab == 1 && !favorite(index)) return false;
        String words = switch (index) {
            case 0 -> "storagefinder chests trapped ender shulker droppers";
            case 1 -> "spawnerfinder spawners trial";
            case 2 -> "freecam free camera fly perspective spectator";
            case 3 -> "pearlsave pearl ender clutch save fall lethal";
            case 4 -> "mlg water bucket clutch save fall lethal";
            case 5 -> "autoeat eat food hunger feed apple";
            case 6 -> "auto tool best pickaxe axe shovel mining efficiency";
            case 7 -> "rtp plus stash hunter danger spawner logoff";
            default -> "block esp ore highlight xray see blocks colors";
        };
        return words.contains(query.toLowerCase(Locale.ROOT).trim());
    }

    private void moduleCard(GuiGraphics g, int index, int y) {
        boolean enabled = enabled(index);
        boolean hovered = inside(CX, y, CW, 66);
        double hover = animate("cardHover" + index, hovered ? 1 : 0);
        double active = animate("cardActive" + index, enabled ? 1 : 0);
        int bg = blend(blend(CARD, 0xFF252C39, hover), blend(CARD, config.accentColor, .28), active);
        round(g, CX, y, CW, 66, 10, bg);
        round(g, CX + 13, y + 14, 37, 37, 9, enabled ? alpha(config.accentColor, .35) : 0xFF2B3241);
        if (index == 0) chest(g, CX + 23, y + 23, TEXT);
        else if (index == 1) spawner(g, CX + 23, y + 23, TEXT);
        else if (index == 2) camera(g, CX + 23, y + 23, TEXT);
        else if (index == 3) pearl(g, CX + 23, y + 23, TEXT);
        else if (index == 4) droplet(g, CX + 23, y + 23, TEXT);
        else if (index == 5) apple(g, CX + 23, y + 23, TEXT);
        else if (index == 6) wrench(g, CX + 23, y + 23, TEXT);
        else if (index == 7) pickaxe(g, CX + 23, y + 23, TEXT);
        else blocks(g, CX + 23, y + 23, TEXT);
        strong(g, font, name(index), CX + 64, y + 16, TEXT);
        String description = switch (index) {
            case 0 -> "Chests, shulkers & droppers";
            case 1 -> "Monster & trial spawners";
            case 2 -> "Explore with a detached camera";
            case 3 -> "Ender pearl clutch on lethal falls";
            case 4 -> "Water bucket clutch on lethal falls";
            case 5 -> "Eat hotbar food when hunger is low";
            case 6 -> "Swap to the fastest tool while mining";
            case 7 -> "Auto /rtp, dig, danger & logoff";
            default -> "Highlight any blocks you choose";
        };
        text(g, font, description, CX + 64, y + 34, MUTED);
        text(g, font, index == 2 && !canUseFreecam() ? "Join a world to enable" : "Configure  >", CX + 64, y + 49, enabled ? 0xFFAFCAFF : DIM);
        hit("module" + index, CX, y, CW - 94, 66, () -> select(index));
        keybindButton(g, index, 434, y + 43, 92, 19);
        star(g, CX + CW - 87, y + 25, favorite(index) ? 0xFFFFC66B : DIM);
        hit("favorite" + index, CX + CW - 95, y + 15, 30, 36, () -> change(() -> toggleFavorite(index)));
        if (index == 2) freecamToggle(g, "enable2", CX + CW - 51, y + 25);
        else toggle(g, "enable" + index, CX + CW - 51, y + 25, enabled,
                () -> change(() -> module(index).setEnabled(!module(index).isEnabled())));
    }

    private ModuleSettings module(int index) {
        return switch (index) {
            case 0 -> config.storage;
            case 1 -> config.spawner;
            case 2 -> config.freecam;
            case 3 -> config.pearlSave;
            case 4 -> config.mlg;
            case 5 -> config.autoEat;
            case 6 -> config.autoTool;
            case 7 -> config.rtpPlus;
            default -> config.blockEsp;
        };
    }

    private FinderSettings finder(int index) { return index == 0 ? config.storage : config.spawner; }
    private boolean enabled(int index) { return module(index).isEnabled(); }
    private boolean favorite(int index) { return module(index).isFavorite(); }
    private void toggleFavorite(int index) { ModuleSettings s = module(index); s.setFavorite(!s.isFavorite()); }
    private int activeCount() {
        int count = 0;
        for (int i = 0; i < 9; i++) if (enabled(i)) count++;
        return count;
    }

    private String name(int index) {
        return switch (index) {
            case 0 -> "StorageFinder";
            case 1 -> "SpawnerFinder";
            case 2 -> "Freecam";
            case 3 -> "PearlSave";
            case 4 -> "MLG";
            case 5 -> "AutoEat";
            case 6 -> "Auto Tool";
            case 7 -> "RTP Plus";
            default -> "Block ESP";
        };
    }

    private boolean canUseFreecam() { return minecraft.level != null && minecraft.player != null; }

    private void freecamToggle(GuiGraphics g, String id, int x, int y) {
        if (!config.freecam.enabled && !canUseFreecam()) {
            round(g, x, y, 31, 17, 8, 0xFF262D39);
            round(g, x + 3, y + 3, 11, 11, 5, DIM);
            return;
        }
        toggle(g, id, x, y, config.freecam.enabled, () -> {
            if (config.freecam.enabled || canUseFreecam()) config.freecam.enabled = !config.freecam.enabled;
        });
    }

    private void drawSettings(GuiGraphics g) {
        if (selected == 2) { drawFreecamSettings(g); return; }
        if (selected == 3) { drawPearlSaveSettings(g); return; }
        if (selected == 4) { drawMlgSettings(g); return; }
        if (selected == 5) { drawAutoEatSettings(g); return; }
        if (selected == 6) { drawAutoToolSettings(g); return; }
        if (selected == 7) { drawRtpPlusSettings(g); return; }
        if (selected == 8) { drawBlockEspSettings(g); return; }
        FinderSettings s = finder(selected);
        strong(g, font, name(selected), CX, 73, TEXT);
        text(g, font, "Fine-tune every highlight.", CX, 91, MUTED);
        keybindButton(g, selected, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Enabled" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        boolean outline = s.mode != RenderMode.FILL;
        boolean filled = s.mode != RenderMode.OUTLINE;
        cube(g, CX + 22, y + 13, 30, s.color, outline ? s.outlineOpacity : 0, filled ? s.fillOpacity : 0);
        text(g, font, "HIGHLIGHT PREVIEW", CX + 89, y + 15, DIM);
        text(g, font, modeName(s.mode) + "  /  " + (int) s.range + " block range", CX + 89, y + 32, TEXT);
        text(g, font, s.throughWalls ? "Visible through blocks" : "Visible surfaces only", CX + 89, y + 49, MUTED);
        y += 84;
        section(g, "RENDERING", y); y += 18;
        modeRow(g, s, y); y += 40;
        slider(g, "outlineOpacity", "Outline opacity", y, () -> s.outlineOpacity, v -> s.outlineOpacity = v, 0, 1, .01, "%"); y += 47;
        slider(g, "fillOpacity", "Fill opacity", y, () -> s.fillOpacity, v -> s.fillOpacity = v, 0, 1, .01, "%"); y += 47;
        slider(g, "lineWidth", "Outline width", y, () -> s.lineWidth, v -> s.lineWidth = v, .5, 5, .1, " px"); y += 47;
        slider(g, "range", "Detection range", y, () -> s.range, v -> s.range = v, 16, 256, 1, " blocks"); y += 47;
        settingToggle(g, "walls", "Through walls", "Keep highlights visible behind blocks", y, () -> s.throughWalls, () -> s.throughWalls = !s.throughWalls); y += 43;
        settingToggle(g, "fade", "Distance fade", "Soften highlights near the range limit", y, () -> s.distanceFade, () -> s.distanceFade = !s.distanceFade); y += 51;
        section(g, "COLORS", y); y += 19;
        colorRow(g, "Base highlight", y, () -> s.color, value -> s.color = value); y += 36;
        if (selected == 0) {
            settingToggle(g, "perType", "Colors by block type", "Give each storage family its own color", y, () -> s.perTypeColors, () -> s.perTypeColors = !s.perTypeColors); y += 45;
            if (s.perTypeColors) {
                colorRow(g, "Chests", y, () -> s.chestColor, value -> s.chestColor = value); y += 36;
                colorRow(g, "Shulkers", y, () -> s.shulkerColor, value -> s.shulkerColor = value); y += 36;
                colorRow(g, "Droppers", y, () -> s.dropperColor, value -> s.dropperColor = value); y += 36;
            }
        }
        y += 9;
        section(g, "BLOCK FILTERS", y); y += 18;
        if (selected == 0) {
            settingToggle(g, "chests", "Chests", "Standard and double chests", y, () -> s.chests, () -> s.chests = !s.chests); y += 43;
            settingToggle(g, "trapped", "Trapped chests", "Redstone-triggering chests", y, () -> s.trappedChests, () -> s.trappedChests = !s.trappedChests); y += 43;
            settingToggle(g, "ender", "Ender chests", "Personal ender storage", y, () -> s.enderChests, () -> s.enderChests = !s.enderChests); y += 43;
            settingToggle(g, "shulkers", "Shulker boxes", "Every shulker color", y, () -> s.shulkers, () -> s.shulkers = !s.shulkers); y += 43;
            settingToggle(g, "droppers", "Droppers", "Dropper blocks", y, () -> s.droppers, () -> s.droppers = !s.droppers); y += 43;
        } else {
            text(g, font, "Monster spawners are always included.", CX + 3, y + 8, MUTED); y += 31;
            settingToggle(g, "trial", "Trial spawners", "Include trial chamber spawners", y, () -> s.trialSpawners, () -> s.trialSpawners = !s.trialSpawners); y += 43;
        }
        y += 12;
        text(g, font, "Only blocks in loaded chunks can be highlighted.", CX + 3, y, DIM); y += 23;
        endContent(g, y);
    }

    private void drawFreecamSettings(GuiGraphics g) {
        var s = config.freecam;
        strong(g, font, "Freecam", CX, 73, TEXT);
        text(g, font, "See your world from a new angle.", CX, 91, MUTED);
        keybindButton(g, 2, 386, 69, 111, 27);
        String status = s.enabled ? "Enabled" : canUseFreecam() ? "Disabled" : "Join a world";
        text(g, font, status, CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        freecamToggle(g, "detailEnabled", CX + CW - 33, 78);
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        camera(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "FREE CAMERA", CX + 76, y + 15, DIM);
        text(g, font, "Move your viewpoint independently of your player.", CX + 76, y + 32, TEXT);
        text(g, font, "Client-side perspective. Uses already loaded chunks.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "MOVEMENT", y); y += 18;
        slider(g, "freecamSpeed", "Camera speed", y, () -> s.speed, value -> s.speed = value, .1, 5, .1, " blocks/tick"); y += 47;
        slider(g, "freecamBoost", "Sprint boost", y, () -> s.boostMultiplier, value -> s.boostMultiplier = value, 1, 10, .1, "x"); y += 55;
        section(g, "YOUR CONTROLS", y); y += 20;
        controlHint(g, "Move", keyName(minecraft.options.keyUp) + " / " + keyName(minecraft.options.keyLeft)
                + " / " + keyName(minecraft.options.keyDown) + " / " + keyName(minecraft.options.keyRight), y); y += 23;
        controlHint(g, "Ascend / descend", keyName(minecraft.options.keyJump) + " / " + keyName(minecraft.options.keyShift), y); y += 23;
        controlHint(g, "Speed boost", keyName(minecraft.options.keySprint), y); y += 32;
        text(g, font, "Close this menu to fly. Reopen it to turn Freecam off.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Your camera returns to your player when disabled.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private String keyName(KeyMapping key) { return key.getTranslatedKeyMessage().getString(); }

    private void drawRtpPlusSettings(GuiGraphics g) {
        var s = config.rtpPlus;
        var rtp = config.rtpStash;
        strong(g, font, "RTP Plus", CX, 73, TEXT);
        text(g, font, "Automated stash hunter with safety.", CX, 91, MUTED);
        keybindButton(g, 7, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Running" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        pickaxe(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "AUTOMATED STASH RUN", CX + 76, y + 15, DIM);
        text(g, font, "/rtp, dig to depth, then loot-check.", CX + 76, y + 32, TEXT);
        text(g, font, "Re-RTPs on danger, logs off on finds.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "RUN", y); y += 18;
        slider(g, "rpTargetY", "Target depth", y, () -> rtp.targetY, v -> rtp.targetY = (int) Math.round(v), -63, 0, 1, " y"); y += 47;
        slider(g, "rpMin", "Storage needed", y, () -> rtp.minStorages, v -> rtp.minStorages = (int) Math.round(v), 1, 50, 1, ""); y += 47;
        slider(g, "rpWait", "RTP settle", y, () -> rtp.rtpWaitTicks, v -> rtp.rtpWaitTicks = (int) Math.round(v), 20, 400, 10, " ticks"); y += 47;
        slider(g, "rpScan", "Scan time", y, () -> rtp.scanTicks, v -> rtp.scanTicks = (int) Math.round(v), 20, 200, 10, " ticks"); y += 47;
        slider(g, "rpMaxDig", "Dig timeout", y, () -> rtp.maxDigTicks, v -> rtp.maxDigTicks = (int) Math.round(v), 200, 12000, 200, " ticks"); y += 47;
        slider(g, "rpDanger", "Danger health", y, () -> s.dangerHealth, v -> s.dangerHealth = v, 0, 20, 1, " hp"); y += 47;
        section(g, "SAFETY", y); y += 18;
        settingToggle(g, "rpOffStash", "Log off on stash", "Disconnect when storage is found", y, () -> s.logOffOnStorage, () -> s.logOffOnStorage = !s.logOffOnStorage); y += 43;
        settingToggle(g, "rpOffSpawner", "Log off on spawner", "Disconnect when a spawner is detected", y, () -> s.logOffOnSpawner, () -> s.logOffOnSpawner = !s.logOffOnSpawner); y += 43;
        slider(g, "rpSpawn", "Spawners needed", y, () -> s.spawnerThreshold, v -> s.spawnerThreshold = (int) Math.round(v), 1, 20, 1, ""); y += 47;
        section(g, "NOTES", y); y += 19;
        text(g, font, "Lava re-RTPs at once; water after 2s.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Uses StorageFinder range and filters.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Enable MLG / AutoTool separately if wanted.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private void drawBlockEspSettings(GuiGraphics g) {
        var s = config.blockEsp;
        strong(g, font, "Block ESP", CX, 73, TEXT);
        text(g, font, "Choose the blocks you want to see.", CX, 91, MUTED);
        keybindButton(g, 8, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Enabled" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        blocks(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "CUSTOM BLOCK HIGHLIGHTS", CX + 76, y + 15, DIM);
        text(g, font, "Toggle blocks and give each one a colour.", CX + 76, y + 32, TEXT);
        text(g, font, "Reads loaded chunks only.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "RENDERING", y); y += 18;
        slider(g, "espRange", "Range", y, () -> s.range, v -> s.range = v, 16, 128, 8, " blocks"); y += 47;        slider(g, "espFill", "Fill opacity", y, () -> s.fillOpacity, v -> s.fillOpacity = v, 0, 1, .01, "%"); y += 47;
        slider(g, "espOutline", "Outline opacity", y, () -> s.outlineOpacity, v -> s.outlineOpacity = v, 0, 1, .01, "%"); y += 47;
        settingToggle(g, "espWalls", "Through walls", "Keep highlights visible behind blocks", y, () -> s.throughWalls, () -> s.throughWalls = !s.throughWalls); y += 51;
        section(g, "BLOCKS", y); y += 18;
        for (BlockEspCatalog.Target target : BlockEspCatalog.Target.values()) {
            espRow(g, target, y);
            y += 30;
        }
        y += 6;
        endContent(g, y);
    }

    private void espRow(GuiGraphics g, BlockEspCatalog.Target target, int y) {
        BlockEspSettings.Entry entry = config.blockEsp.entry(target);
        text(g, font, target.label, CX + 3, y + 6, entry.enabled ? TEXT : MUTED);
        round(g, CX + CW - 92, y + 2, 30, 22, 5, CARD);
        round(g, CX + CW - 86, y + 7, 18, 12, 3, alpha(entry.color, 1));
        hit("espcolor" + target.name(), CX + CW - 92, y + 2, 30, 22, () -> {
            colorEditor = new ColorEditor(target.label,
                    () -> config.blockEsp.entry(target).color,
                    value -> config.blockEsp.entry(target).color = value);
            keyboardFocus = null;
        });
        toggle(g, "esp" + target.name(), CX + CW - 51, y + 4, entry.enabled,
                () -> change(() -> { BlockEspSettings.Entry e = config.blockEsp.entry(target); e.enabled = !e.enabled; }));
    }

    private void drawAutoEatSettings(GuiGraphics g) {
        var s = config.autoEat;
        strong(g, font, "AutoEat", CX, 73, TEXT);
        text(g, font, "Keep your hunger topped up.", CX, 91, MUTED);
        keybindButton(g, 5, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Enabled" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        apple(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "HOTBAR FEEDING", CX + 76, y + 15, DIM);
        text(g, font, "Holds right click on food in your hotbar.", CX + 76, y + 32, TEXT);
        text(g, font, "Stops the moment hunger is back up.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "DETECTION", y); y += 18;
        slider(g, "eatThreshold", "Eat below", y, () -> s.hungerThreshold, v -> s.hungerThreshold = (int) Math.round(v), 1, 20, 1, " hunger"); y += 47;
        section(g, "NOTES", y); y += 19;
        text(g, font, "Uses the first food found in slots 1-9.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Add food to your hotbar before enabling.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private void drawAutoToolSettings(GuiGraphics g) {
        var s = config.autoTool;
        strong(g, font, "Auto Tool", CX, 73, TEXT);
        text(g, font, "Mine with the right tool, every time.", CX, 91, MUTED);
        keybindButton(g, 6, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Enabled" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        wrench(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "SMART HOTBAR SWAP", CX + 76, y + 15, DIM);
        text(g, font, "Picks the fastest tool for the block you mine.", CX + 76, y + 32, TEXT);
        text(g, font, "Only acts while you hold left click.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "BEHAVIOUR", y); y += 19;
        text(g, font, "Switches to the best tool in slots 1-9.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Works with RTP Stash's auto-dig.", CX + 3, y, MUTED); y += 23;
        text(g, font, "No settings to tune.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private void drawRtpStashSettings(GuiGraphics g) {
        var s = config.rtpStash;
        strong(g, font, "RTP Stash", CX, 73, TEXT);
        text(g, font, "Automated /rtp stash hunting.", CX, 91, MUTED);
        keybindButton(g, 5, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Running" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        pickaxe(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "AUTOMATED STASH RUN", CX + 76, y + 15, DIM);
        text(g, font, "Sends /rtp, then digs down to the target depth.", CX + 76, y + 32, TEXT);
        text(g, font, "Repeats until the storage finder scores a hit.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "RUN", y); y += 18;
        slider(g, "rtpTargetY", "Target depth", y, () -> s.targetY, v -> s.targetY = (int) Math.round(v), -63, 0, 1, " y"); y += 47;
        slider(g, "rtpMin", "Storage needed", y, () -> s.minStorages, v -> s.minStorages = (int) Math.round(v), 1, 50, 1, ""); y += 47;
        slider(g, "rtpWait", "RTP settle", y, () -> s.rtpWaitTicks, v -> s.rtpWaitTicks = (int) Math.round(v), 20, 400, 10, " ticks"); y += 47;
        slider(g, "rtpScan", "Scan time", y, () -> s.scanTicks, v -> s.scanTicks = (int) Math.round(v), 20, 200, 10, " ticks"); y += 47;
        slider(g, "rtpMaxDig", "Dig timeout", y, () -> s.maxDigTicks, v -> s.maxDigTicks = (int) Math.round(v), 200, 12000, 200, " ticks"); y += 47;
        settingToggle(g, "rtpLogoff", "Log off when found", "Disconnect once enough storage is detected", y, () -> s.logOff, () -> s.logOff = !s.logOff); y += 51;
        section(g, "NOTES", y); y += 19;
        text(g, font, "Uses your StorageFinder range and filters.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Hold a pickaxe; it does not switch tools.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Toggle off at any time to stop the run.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private void drawPearlSaveSettings(GuiGraphics g) {
        var s = config.pearlSave;
        strong(g, font, "PearlSave", CX, 73, TEXT);
        text(g, font, "Clutch a lethal fall with an ender pearl.", CX, 91, MUTED);
        keybindButton(g, 3, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Enabled" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        pearl(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "ENDER PEARL CLUTCH", CX + 76, y + 15, DIM);
        text(g, font, "Looks straight down and throws a pearl.", CX + 76, y + 32, TEXT);
        text(g, font, "Teleports you on impact and resets fall distance.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "DETECTION", y); y += 18;
        slider(g, "pearlMinFall", "Minimum fall", y, () -> s.minFallDistance, v -> s.minFallDistance = v, 3, 20, 1, " blocks"); y += 47;
        slider(g, "pearlSafety", "Health safety", y, () -> s.extraSafety, v -> s.extraSafety = v, 0, 10, 1, " hp"); y += 47;
        slider(g, "pearlCooldown", "Retry cooldown", y, () -> s.cooldownTicks, v -> s.cooldownTicks = (int) Math.round(v), 5, 60, 1, " ticks"); y += 47;
        settingToggle(g, "pearlSwitch", "Restore hotbar slot", "Return to the slot you held before the throw", y, () -> s.switchBack, () -> s.switchBack = !s.switchBack); y += 51;
        section(g, "NOTES", y); y += 19;
        text(g, font, "Requires an ender pearl in your hotbar.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Ignored in creative, spectator, flight and water.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private void drawMlgSettings(GuiGraphics g) {
        var s = config.mlg;
        strong(g, font, "MLG", CX, 73, TEXT);
        text(g, font, "Clutch a lethal fall with a water bucket.", CX, 91, MUTED);
        keybindButton(g, 4, 386, 69, 111, 27);
        text(g, font, s.enabled ? "Enabled" : "Disabled", CX + CW - 111, 81, s.enabled ? 0xFF92BAFF : MUTED);
        toggle(g, "detailEnabled", CX + CW - 33, 78, s.enabled, () -> change(() -> s.enabled = !s.enabled));
        beginContent(g);
        int y = TOP + 3 - (int) scroll;
        round(g, CX, y, CW - 10, 68, 9, 0xFF161D2B);
        round(g, CX + 18, y + 15, 37, 37, 9, alpha(config.accentColor, .2));
        droplet(g, CX + 28, y + 25, alpha(config.accentColor, 1));
        text(g, font, "WATER BUCKET CLUTCH", CX + 76, y + 15, DIM);
        text(g, font, "Places water on the ground below you.", CX + 76, y + 32, TEXT);
        text(g, font, "Triggered just before the lethal landing.", CX + 76, y + 49, MUTED);
        y += 84;
        section(g, "DETECTION", y); y += 18;
        slider(g, "mlgPlace", "Place height", y, () -> s.placeDistance, v -> s.placeDistance = v, 1, 4, .5, " blocks"); y += 47;
        slider(g, "mlgMinFall", "Minimum fall", y, () -> s.minFallDistance, v -> s.minFallDistance = v, 3, 20, 1, " blocks"); y += 47;
        slider(g, "mlgSafety", "Health safety", y, () -> s.extraSafety, v -> s.extraSafety = v, 0, 10, 1, " hp"); y += 47;
        slider(g, "mlgCooldown", "Retry cooldown", y, () -> s.cooldownTicks, v -> s.cooldownTicks = (int) Math.round(v), 2, 40, 1, " ticks"); y += 47;
        settingToggle(g, "mlgSwitch", "Restore hotbar slot", "Return to the slot you held before placing", y, () -> s.switchBack, () -> s.switchBack = !s.switchBack); y += 51;
        settingToggle(g, "mlgPickup", "Pick water back up", "Scoop the water once you land unharmed", y, () -> s.pickupWater, () -> s.pickupWater = !s.pickupWater); y += 51;
        section(g, "NOTES", y); y += 19;
        text(g, font, "Requires a water bucket in your hotbar.", CX + 3, y, MUTED); y += 23;
        text(g, font, "Places only when ground is within reach.", CX + 3, y, DIM); y += 26;
        endContent(g, y);
    }

    private void keybindButton(GuiGraphics g, int index, int x, int y, int w, int h) {
        KeyMapping mapping = ModuleKeybinds.mapping(index);
        if (mapping == null) return;
        boolean capturing = binding == index;
        boolean hovered = inside(x, y, w, h);
        if (hovered) hoveredBinding = index;
        boolean conflict = !conflicts(index).isEmpty();
        double hover = animate("bindHover" + index, hovered || capturing ? 1 : 0);
        round(g, x, y, w, h, 5, blend(0xFF222938, 0xFF354159, hover));
        if (capturing) outline(g, x, y, w, h, 5, alpha(config.accentColor, .85));
        String value = capturing ? "Press a key..." : (conflict ? "! " : "Key: ")
                + (mapping.isUnbound() ? "None" : keyName(mapping));
        value = ellipsize(value, w - 12);
        text(g, font, value, x + (w - UiPaint.width(font, value)) / 2, y + (h - 8) / 2,
                capturing ? TEXT : conflict ? 0xFFFFC66B : MUTED);
        hit("keybind" + index, x, y, w, h, () -> {
            binding = index;
            dragging = null;
            searchFocused = false;
            keyboardFocus = null;
        });
    }

    private String conflicts(int index) {
        KeyMapping mapping = ModuleKeybinds.mapping(index);
        if (mapping == null || mapping.isUnbound()) return "";
        List<String> names = new ArrayList<>();
        for (KeyMapping other : minecraft.options.keyMappings) {
            if (other != mapping && mapping.same(other)) names.add(Component.translatable(other.getName()).getString());
        }
        return String.join(", ", names);
    }

    private void drawFooter(GuiGraphics g) {
        String message = "Changes save automatically";
        int color = MUTED;
        if (binding >= 0) {
            message = "Press a key or mouse button. Delete / Backspace clears.";
            color = TEXT;
        } else if (hoveredBinding >= 0 && colorEditor == null) {
            String conflict = conflicts(hoveredBinding);
            KeyMapping mapping = ModuleKeybinds.mapping(hoveredBinding);
            message = conflict.isEmpty()
                    ? (mapping.isUnbound() ? "Click to bind a key or mouse button."
                    : "Bound to " + keyName(mapping) + ". Click to change.")
                    : "Also used by: " + conflict;
            if (!conflict.isEmpty()) color = 0xFFFFC66B;
        }
        round(g, 19, H - 19, 5, 5, 2, binding >= 0 ? alpha(config.accentColor, 1) : 0xFF6DDAC5);
        text(g, font, ellipsize(message, W - 145), 30, H - 22, color);
        right(g, font, binding >= 0 ? "ESC  Cancel" : "ESC  Close", W - 20, H - 22, DIM);
    }

    private String ellipsize(String value, int maxWidth) {
        if (UiPaint.width(font, value) <= maxWidth) return value;
        while (!value.isEmpty() && UiPaint.width(font, value + "...") > maxWidth) value = removeLastCharacter(value);
        return value + "...";
    }

    private void controlHint(GuiGraphics g, String label, String keys, int y) {
        text(g, font, label, CX + 3, y, TEXT);
        String shown = keys;
        while (!shown.isEmpty() && UiPaint.width(font, shown) > CW - 165) shown = removeLastCharacter(shown);
        if (!shown.equals(keys)) shown += "...";
        right(g, font, shown, CX + CW - 15, y, MUTED);
    }

    private void beginContent(GuiGraphics g) {
        g.enableScissor(CX - 2, TOP, CX + CW, BOTTOM);
        g.pose().pushMatrix();
        g.pose().translate(0, (float) ((int) scroll - scroll));
        collectingClip = true;
    }

    private void endContent(GuiGraphics g, int endY) {
        contentHeight = endY + (int) scroll - TOP;
        collectingClip = false;
        g.pose().popMatrix();
        g.disableScissor();
        int viewHeight = BOTTOM - TOP;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Math.clamp(scroll, 0, maxScroll);
        scrollTarget = Math.clamp(scrollTarget, 0, maxScroll);
        if (maxScroll > 0) {
            round(g, CX + CW - 3, TOP + 2, 3, viewHeight - 4, 1, CARD);
            int thumbHeight = Math.max(25, viewHeight * viewHeight / contentHeight);
            int thumbY = TOP + (int) (scroll / maxScroll * (viewHeight - thumbHeight));
            round(g, CX + CW - 3, thumbY, 3, thumbHeight, 1, 0xFF58657B);
            dragHit("scrollbar", CX + CW - 8, TOP, 10, viewHeight,
                    value -> scroll = scrollTarget = Math.clamp(value, 0, 1) * maxScroll, true);
        }
    }

    private void section(GuiGraphics g, String title, int y) {
        text(g, font, title, CX + 3, y, DIM);
    }

    private void modeRow(GuiGraphics g, FinderSettings s, int y) {
        text(g, font, "Render mode", CX + 3, y + 9, TEXT);
        int x = CX + 175;
        for (RenderMode mode : RenderMode.values()) {
            final int bx = x;
            boolean active = s.mode == mode;
            round(g, x, y, 91, 29, 6, active ? alpha(config.accentColor, 1) : CARD);
            String label = modeName(mode);
            text(g, font, label, x + (91 - UiPaint.width(font, label)) / 2, y + 10, active ? 0xFFFFFFFF : MUTED);
            hit("mode" + mode, bx, y, 91, 29, () -> change(() -> s.mode = mode));
            x += 95;
        }
    }

    private String modeName(RenderMode mode) {
        return switch (mode) { case OUTLINE -> "Outline"; case FILL -> "Filled"; case BOTH -> "Both"; };
    }

    private void slider(GuiGraphics g, String id, String label, int y, DoubleSupplier getter,
                        DoubleConsumer setter, double min, double max, double step, String unit) {
        double value = getter.getAsDouble();
        text(g, font, label, CX + 3, y + 2, TEXT);
        String display = unit.equals("%") ? Math.round(value * 100) + "%"
                : step >= 1 ? Math.round(value) + unit : String.format(Locale.ROOT, "%.1f%s", value, unit);
        right(g, font, display, CX + CW - 15, y + 2, MUTED);
        int sx = CX + 5, sw = CW - 24;
        double t = Math.clamp((value - min) / (max - min), 0, 1);
        round(g, sx, y + 25, sw, 4, 2, 0xFF303847);
        round(g, sx, y + 25, Math.max(4, (int) (sw * t)), 4, 2, alpha(config.accentColor, 1));
        round(g, sx + (int) (sw * t) - 4, y + 22, 9, 10, 4, 0xFFE7EDFA);
        dragHit(id, sx, y + 15, sw, 25, position -> {
            double next = Math.round((min + Math.clamp(position, 0, 1) * (max - min)) / step) * step;
            setter.accept(Math.clamp(next, min, max)); dirty = true;
        }, false, direction -> { setter.accept(Math.clamp(getter.getAsDouble() + direction * step, min, max)); dirty = true; flush(); });
    }

    private void settingToggle(GuiGraphics g, String id, String label, String description, int y,
                               BooleanSupplier value, Runnable action) {
        text(g, font, label, CX + 3, y + 3, TEXT);
        text(g, font, description, CX + 3, y + 19, MUTED);
        toggle(g, id, CX + CW - 47, y + 8, value.getAsBoolean(), () -> change(action));
    }

    private void toggle(GuiGraphics g, String id, int x, int y, boolean on, Runnable action) {
        double position = animate("toggle" + id, on ? 1 : 0);
        round(g, x, y, 31, 17, 8, blend(0xFF343D4D, config.accentColor, position));
        round(g, x + 3 + (float) (14 * position), y + 3, 11, 11, 5, blend(0xFFADBACD, 0xFFFFFFFF, position));
        hit(id, x - 3, y - 5, 37, 27, action);
    }

    private void colorRow(GuiGraphics g, String label, int y, IntSupplier getter, IntConsumer setter) {
        text(g, font, label, CX + 3, y + 10, TEXT);
        round(g, CX + CW - 136, y + 2, 122, 27, 6, CARD);
        round(g, CX + CW - 127, y + 9, 12, 12, 3, alpha(getter.getAsInt(), 1));
        text(g, font, hex(getter.getAsInt()), CX + CW - 107, y + 10, MUTED);
        text(g, font, ">", CX + CW - 27, y + 10, DIM);
        hit("color" + label, CX + CW - 136, y + 2, 122, 27,
                () -> { colorEditor = new ColorEditor(label, getter, setter); keyboardFocus = null; });
    }

    private void drawAppearance(GuiGraphics g) {
        strong(g, font, "Make yourself at home", CX, 73, TEXT);
        text(g, font, "A small collection of personal touches.", CX, 91, MUTED);
        beginContent(g);
        int y = TOP + 10 - (int) scroll;
        section(g, "INTERFACE", y); y += 19;
        colorRow(g, "Accent color", y, () -> config.accentColor, value -> config.accentColor = value); y += 49;
        settingToggle(g, "hud", "Array list", "Active module names with a soft shadow", y, () -> config.showHud, () -> config.showHud = !config.showHud); y += 65;
        round(g, CX + 3, y, CW - 17, 79, 8, 0xFF171D29);
        text(g, font, "A few useful controls", CX + 17, y + 14, TEXT);
        text(g, font, "Click a module to customize it. Scroll for more settings.", CX + 17, y + 35, MUTED);
        text(g, font, "Drag sliders, choose colors, and star your favorites.", CX + 17, y + 52, MUTED);
        y += 94;
        endContent(g, y);
    }

    private void drawColorEditor(GuiGraphics g) {
        // Modal hit testing replaces the underlying settings' controls.
        hits.clear();
        g.fill(0, 0, W, H, 0xAC06090F);
        int x = 180, y = 78, w = 318, h = 252;
        round(g, x - 1, y - 1, w + 2, h + 2, 11, BORDER);
        round(g, x, y, w, h, 10, PANEL);
        strong(g, font, colorEditor.title, x + 18, y + 17, TEXT);
        text(g, font, "Choose a color", x + 18, y + 35, MUTED);
        round(g, x + w - 53, y + 16, 33, 33, 8, alpha(colorEditor.getter.getAsInt(), 1));
        for (int i = 0; i < PALETTE.length; i++) {
            int px = x + 18 + i * 24;
            int color = PALETTE[i];
            round(g, px, y + 61, 18, 18, 5, alpha(color, 1));
            if (colorEditor.getter.getAsInt() == color) outline(g, px - 2, y + 59, 22, 22, 6, TEXT);
            hit("swatch" + i, px - 2, y + 59, 22, 22, () -> { colorEditor.set(color); flush(); });
        }
        colorChannel(g, "Red", 16, 0xFFF17886, x, y + 99);
        colorChannel(g, "Green", 8, 0xFF84DCB3, x, y + 133);
        colorChannel(g, "Blue", 0, 0xFF81AEFF, x, y + 167);
        round(g, x + 18, y + 210, 141, 27, 6, colorEditor.hexFocused ? BORDER : CARD);
        String value = "#" + colorEditor.hex + (colorEditor.hexFocused && System.currentTimeMillis() % 1000 < 500 ? "|" : "");
        text(g, font, value, x + 29, y + 220, colorEditor.hex.length() == 6 ? TEXT : 0xFFFFC66B);
        hit("hex", x + 18, y + 210, 141, 27, () -> { colorEditor.hexFocused = true; colorEditor.replaceHex = true; });
        round(g, x + w - 111, y + 210, 93, 27, 6, alpha(config.accentColor, 1));
        text(g, font, "Done", x + w - 80, y + 220, 0xFFFFFFFF);
        hit("colorDone", x + w - 111, y + 210, 93, 27, this::closeColor);
    }

    private void colorChannel(GuiGraphics g, String label, int shift, int tint, int x, int y) {
        int value = (colorEditor.getter.getAsInt() >> shift) & 255;
        text(g, font, label, x + 18, y, tint);
        right(g, font, Integer.toString(value), x + 298, y, MUTED);
        int sx = x + 76, sw = 181;
        round(g, sx, y + 3, sw, 4, 2, BORDER);
        round(g, sx, y + 3, Math.max(4, (int) (sw * value / 255.0)), 4, 2, tint);
        round(g, sx + (int) (sw * value / 255.0) - 4, y, 9, 10, 4, TEXT);
        dragHit("rgb" + shift, sx, y - 5, sw, 20, t -> {
            int channel = (int) Math.round(Math.clamp(t, 0, 1) * 255);
            int next = (colorEditor.getter.getAsInt() & ~(255 << shift)) | channel << shift;
            colorEditor.set(next);
        }, false, direction -> {
            int channel = Math.clamp(((colorEditor.getter.getAsInt() >> shift) & 255) + direction, 0, 255);
            colorEditor.set((colorEditor.getter.getAsInt() & ~(255 << shift)) | channel << shift);
            flush();
        });
    }

    private void closeColor() { colorEditor = null; dragging = null; keyboardFocus = null; flush(); }
    private String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF); }

    private double ease(double current, double target, double speed) {
        double result = current + (target - current) * (1 - Math.exp(-speed * frameSeconds));
        return Math.abs(result - target) < .001 ? target : result;
    }

    private double animate(String id, double target) {
        double result = ease(animations.getOrDefault(id, target), target, 16);
        animations.put(id, result);
        return result;
    }

    private boolean inside(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void hit(String id, int x, int y, int w, int h, Runnable action) {
        if (collectingClip && (y + h <= TOP || y >= BOTTOM)) return;
        int top = collectingClip ? Math.max(y, TOP) : y;
        int bottom = collectingClip ? Math.min(y + h, BOTTOM) : y + h;
        hits.add(new Hit(id, x, top, w, bottom - top, action, null, false, null));
    }

    private void dragHit(String id, int x, int y, int w, int h, DoubleConsumer action, boolean vertical) {
        dragHit(id, x, y, w, h, action, vertical, null);
    }

    private void dragHit(String id, int x, int y, int w, int h, DoubleConsumer action, boolean vertical, IntConsumer nudge) {
        if (collectingClip && (y + h <= TOP || y >= BOTTOM)) return;
        int top = collectingClip ? Math.max(y, TOP) : y;
        int bottom = collectingClip ? Math.min(y + h, BOTTOM) : y + h;
        hits.add(new Hit(id, x, top, w, bottom - top, null, action, vertical, nudge));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (binding >= 0) {
            ModuleKeybinds.setKey(minecraft, binding, InputConstants.Type.MOUSE.getOrCreate(event.button()));
            binding = -1;
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        double x = (event.x() - originX) / scale, y = (event.y() - originY) / scale;
        keyboardFocus = null;
        searchFocused = false;
        if (colorEditor != null) colorEditor.hexFocused = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(x, y)) continue;
            if (h.drag != null) { dragging = h; h.move(x, y); }
            else h.action.run();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging == null) return false;
        dragging.move((event.x() - originX) / scale, (event.y() - originY) / scale);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) { dragging = null; flush(); return true; }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (colorEditor != null || binding >= 0) return true;
        double localX = (x - originX) / scale, localY = (y - originY) / scale;
        if (localX >= 14 && localX <= 128 && localY >= SIDE_TOP && localY <= SIDE_TOP + SIDE_VISIBLE * SIDE_ROW) {
            int maxScroll = Math.max(0, SIDE_LABELS.length - SIDE_VISIBLE);
            if (vertical != 0) sideScrollTarget = Math.clamp(sideScrollTarget - Math.signum(vertical), 0, maxScroll);
            keyboardFocus = null;
            return true;
        }
        if (localX >= CX && localX <= CX + CW && localY >= TOP && localY <= BOTTOM) {
            scrollTarget = Math.clamp(scrollTarget - vertical * 31, 0, Math.max(0, contentHeight - (BOTTOM - TOP)));
            keyboardFocus = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // A held capture/cancel key must not repeat into ordinary GUI navigation.
        InputConstants.Key pressed = InputConstants.getKey(event);
        if (pressed.equals(capturedKey)) return true;
        if (binding >= 0) {
            capturedKey = pressed;
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                InputConstants.Key key = event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE
                        ? InputConstants.UNKNOWN : pressed;
                ModuleKeybinds.setKey(minecraft, binding, key);
            }
            binding = -1;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (colorEditor != null) closeColor();
            else if (searchFocused) { searchFocused = false; query = ""; }
            else onClose();
            return true;
        }
        if (colorEditor != null && colorEditor.hexFocused) {
            if (event.key() == GLFW.GLFW_KEY_V && event.hasControlDown()) {
                String pasted = minecraft.keyboardHandler.getClipboard().trim().replaceFirst("^#", "");
                if (pasted.matches("(?i)[0-9a-f]{6}")) { colorEditor.set(Integer.parseInt(pasted, 16)); flush(); }
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_C && event.hasControlDown()) {
                minecraft.keyboardHandler.setClipboard("#" + colorEditor.hex); return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                colorEditor.hex = colorEditor.replaceHex ? "" : colorEditor.hex.substring(0, Math.max(0, colorEditor.hex.length() - 1));
                colorEditor.replaceHex = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER) { closeColor(); return true; }
            if (event.key() == GLFW.GLFW_KEY_A && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                colorEditor.replaceHex = true; return true;
            }
        }
        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_V && event.hasControlDown()) {
                String pasted = minecraft.keyboardHandler.getClipboard().replaceAll("[\\p{Cntrl}]", "");
                query = (query + pasted).substring(0, Math.min(64, query.length() + pasted.length())); return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) { query = removeLastCharacter(query); return true; }
            if (event.key() == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
            if (event.key() == GLFW.GLFW_KEY_A && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { query = ""; return true; }
        }
        if (event.key() == GLFW.GLFW_KEY_TAB && !hits.isEmpty()) {
            int index = -1;
            for (int i = 0; i < hits.size(); i++) if (hits.get(i).id.equals(keyboardFocus)) index = i;
            int direction = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
            keyboardFocus = hits.get(Math.floorMod(index + direction, hits.size())).id;
            searchFocused = false;
            return true;
        }
        if (keyboardFocus != null && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_SPACE)) {
            hits.stream().filter(h -> h.id.equals(keyboardFocus)).findFirst().ifPresent(h -> {
                if (h.action != null) h.action.run();
            });
            // Enter/Space that opens capture is not itself the new binding on repeat.
            if (binding >= 0) capturedKey = pressed;
            return true;
        }
        if (keyboardFocus != null && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT)) {
            hits.stream().filter(h -> h.id.equals(keyboardFocus)).findFirst().ifPresent(h -> {
                if (h.nudge != null) h.nudge.accept(event.key() == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            });
            return true;
        }
        if (colorEditor == null && (selected >= 0 || tab == 2)) {
            if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN || event.key() == GLFW.GLFW_KEY_PAGE_UP) {
                scrollTarget = Math.clamp(scrollTarget + (event.key() == GLFW.GLFW_KEY_PAGE_DOWN ? 170 : -170), 0, Math.max(0, contentHeight - (BOTTOM - TOP)));
                keyboardFocus = null; return true;
            }
            if (event.key() == GLFW.GLFW_KEY_HOME) { scrollTarget = 0; return true; }
            if (event.key() == GLFW.GLFW_KEY_END) { scrollTarget = Math.max(0, contentHeight - (BOTTOM - TOP)); return true; }
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (InputConstants.getKey(event).equals(capturedKey)) capturedKey = null;
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (binding >= 0 || capturedKey != null) return true;
        if (colorEditor != null && colorEditor.hexFocused) {
            int c = event.codepoint();
            if (Character.digit(c, 16) >= 0 && c < 128) {
                if (colorEditor.replaceHex) { colorEditor.hex = ""; colorEditor.replaceHex = false; }
                if (colorEditor.hex.length() < 6) colorEditor.hex += Character.toString(c).toUpperCase(Locale.ROOT);
                if (colorEditor.hex.length() == 6) { colorEditor.setter.accept(Integer.parseInt(colorEditor.hex, 16)); dirty = true; flush(); }
            }
            return true;
        }
        if (searchFocused && event.isAllowedChatCharacter() && query.length() < 64) {
            query += event.codepointAsString();
            return true;
        }
        return false;
    }

    private void change(Runnable action) { action.run(); dirty = true; flush(); }
    private void flush() { if (dirty) { save.run(); dirty = false; } }
    private String removeFirstCharacter(String value) { return value.isEmpty() ? value : value.substring(value.offsetByCodePoints(0, 1)); }
    private String removeLastCharacter(String value) { return value.isEmpty() ? value : value.substring(0, value.offsetByCodePoints(value.length(), -1)); }

    @Override
    public void onClose() { flush(); minecraft.setScreen(null); }

    @Override
    public void removed() {
        binding = -1;
        capturedKey = null;
        ModuleKeybinds.suppressUntilReleased(minecraft);
        flush();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Hit(String id, int x, int y, int w, int h, Runnable action, DoubleConsumer drag, boolean vertical, IntConsumer nudge) {
        boolean contains(double mx, double my) { return mx >= x && my >= y && mx < x + w && my < y + h; }
        void move(double mx, double my) { drag.accept(vertical ? (my - y) / h : (mx - x) / w); }
    }

    private final class ColorEditor {
        final String title;
        final IntSupplier getter;
        final IntConsumer setter;
        String hex;
        boolean hexFocused;
        boolean replaceHex;
        ColorEditor(String title, IntSupplier getter, IntConsumer setter) {
            this.title = title;
            this.getter = getter;
            this.setter = setter;
            this.hex = DonutScreen.this.hex(getter.getAsInt()).substring(1);
        }
        void set(int color) {
            setter.accept(color & 0xFFFFFF);
            hex = DonutScreen.this.hex(color).substring(1);
            dirty = true;
        }
    }
}
