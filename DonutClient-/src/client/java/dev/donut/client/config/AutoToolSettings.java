package dev.donut.client.config;

public final class AutoToolSettings implements ModuleSettings {
    /** Automation modules always start off when joining a new session. */
    public transient boolean enabled = false;
    public boolean favorite = false;

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
        // No tunables; kept for symmetry with the other settings.
    }
}
