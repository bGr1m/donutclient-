package dev.donut.client.config;

public final class PearlSaveSettings implements ModuleSettings {
    /** Combat safety modules always start off when joining a new session. */
    public transient boolean enabled = false;
    public boolean favorite = false;
    /** Only arm once the player has fallen at least this many blocks. */
    public double minFallDistance = 4.0;
    /** Treat a fall as lethal this many health points earlier (safety margin). */
    public double extraSafety = 1.0;
    /** Ticks to wait before a retry after a throw. */
    public int cooldownTicks = 20;
    /** Return to the hotbar slot the player held before the throw. */
    public boolean switchBack = false;

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

    public void sanitize() {
        minFallDistance = Double.isFinite(minFallDistance) ? Math.clamp(minFallDistance, 3.0, 40.0) : 4.0;
        extraSafety = Double.isFinite(extraSafety) ? Math.clamp(extraSafety, 0.0, 20.0) : 1.0;
        cooldownTicks = Math.clamp(cooldownTicks, 5, 100);
    }
}
