package dev.donut.client.config;

public final class RtpPlusSettings implements ModuleSettings {
    /** Automation modules always start off when joining a new session. */
    public transient boolean enabled = false;
    public boolean favorite = false;
    /** Re-issue /rtp once health drops to this many hit points (2 hearts = 4.0). */
    public double dangerHealth = 4.0;
    /** Disconnect when the storage finder scores a stash. */
    public boolean logOffOnStorage = true;
    /** Disconnect when a spawner is detected. */
    public boolean logOffOnSpawner = true;
    /** Spawners needed before logging off. */
    public int spawnerThreshold = 1;

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
        dangerHealth = Double.isFinite(dangerHealth) ? Math.clamp(dangerHealth, 0.0, 20.0) : 4.0;
        spawnerThreshold = Math.clamp(spawnerThreshold, 1, 64);
    }
}
