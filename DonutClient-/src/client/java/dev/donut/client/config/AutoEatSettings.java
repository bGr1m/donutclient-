package dev.donut.client.config;

public final class AutoEatSettings implements ModuleSettings {
    /** Automation modules always start off when joining a new session. */
    public transient boolean enabled = false;
    public boolean favorite = false;
    /** Eat when the hunger bar is at or below this level (max 20). */
    public int hungerThreshold = 15;

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
        hungerThreshold = Math.clamp(hungerThreshold, 1, 20);
    }
}
