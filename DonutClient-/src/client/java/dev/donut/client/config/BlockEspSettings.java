package dev.donut.client.config;

import java.util.HashMap;
import java.util.Map;

public final class BlockEspSettings implements ModuleSettings {
    /** Visual modules persist; Block ESP stays on across sessions like the finders. */
    public boolean enabled = false;
    public boolean favorite = false;
    public double range = 48;
    public double fillOpacity = 0.2;
    public double outlineOpacity = 0.9;
    public boolean throughWalls = true;
    public Map<String, Entry> entries = new HashMap<>();

    /** Per-block selection: whether it is shown and in what colour. */
    public static final class Entry {
        public boolean enabled;
        public int color;

        public Entry() { }

        public Entry(boolean enabled, int color) {
            this.enabled = enabled;
            this.color = color;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public boolean isFavorite() {
        return favorite;
    }

    @Override
    public void setFavorite(boolean value) {
        favorite = value;
    }

    public Entry entry(BlockEspCatalog.Target target) {
        if (entries == null) entries = new HashMap<>();
        return entries.computeIfAbsent(target.name(), key -> new Entry(target.defaultEnabled, target.defaultColor));
    }

    public void sanitize() {
        if (entries == null) entries = new HashMap<>();
        range = Double.isFinite(range) ? Math.clamp(range, 16, 128) : 48;
        fillOpacity = Double.isFinite(fillOpacity) ? Math.clamp(fillOpacity, 0, 1) : 0.2;
        outlineOpacity = Double.isFinite(outlineOpacity) ? Math.clamp(outlineOpacity, 0, 1) : 0.9;
        // Deliberately avoid touching the block catalogue here: this runs during config load,
        // before the Minecraft registries exist. Missing entries are created lazily by entry().
        for (Entry entry : entries.values()) {
            entry.color &= 0xFFFFFF;
        }
    }
}
