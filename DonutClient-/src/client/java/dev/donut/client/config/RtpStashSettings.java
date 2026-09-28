package dev.donut.client.config;

public final class RtpStashSettings implements ModuleSettings {
    /** Automation modules always start off when joining a new session. */
    public transient boolean enabled = false;
    public boolean favorite = false;
    /** Dig down until the player's feet reach this Y level. */
    public int targetY = -59;
    /** Storage blocks that must be detected before the run is considered a success. */
    public int minStorages = 3;
    /** Ticks to wait after /rtp for the server teleport and chunk load. */
    public int rtpWaitTicks = 120;
    /** Ticks to let the storage finder scan before counting. */
    public int scanTicks = 60;
    /** Give up digging and /rtp again after this many ticks. */
    public int maxDigTicks = 6000;
    /** Disconnect once enough storage has been found. */
    public boolean logOff = true;

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
        targetY = Math.clamp(targetY, -63, 100);
        minStorages = Math.clamp(minStorages, 1, 200);
        rtpWaitTicks = Math.clamp(rtpWaitTicks, 20, 600);
        scanTicks = Math.clamp(scanTicks, 20, 400);
        maxDigTicks = Math.clamp(maxDigTicks, 200, 20000);
    }
}
