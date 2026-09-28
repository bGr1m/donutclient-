package dev.donut.client.config;

public final class FreecamSettings implements ModuleSettings {
    /** Freecam always starts off when joining a new session. */
    public transient boolean enabled = false;
    public boolean favorite = false;
    public double speed = 1.0;
    public double boostMultiplier = 3.0;

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
        speed = Double.isFinite(speed) ? Math.clamp(speed, 0.1, 5.0) : 1.0;
        boostMultiplier = Double.isFinite(boostMultiplier) ? Math.clamp(boostMultiplier, 1.0, 10.0) : 3.0;
    }
}
