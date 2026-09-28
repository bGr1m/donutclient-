package dev.donut.client.automation;

import dev.donut.client.config.ClientConfig;
import dev.donut.client.config.RtpPlusSettings;
import dev.donut.client.finder.FinderScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * RTP stash hunter with two extra safety behaviours: it re-issues /rtp when health is low, and it
 * logs off when a stash or a spawner is detected. It does not touch the other modules; enable MLG,
 * AutoEat or AutoTool separately if you want them.
 */
public final class RtpPlusController {
    private static final int DANGER_COOLDOWN = 100;

    private final RtpPlusSettings settings;
    private final ClientConfig config;
    private final FinderScanner scanner;
    private final RtpStashController rtp;

    private boolean running;
    private int dangerCooldown;
    private boolean prevRtp;

    public RtpPlusController(RtpPlusSettings settings, ClientConfig config, FinderScanner scanner,
                             RtpStashController rtp) {
        this.settings = settings;
        this.config = config;
        this.scanner = scanner;
        this.rtp = rtp;
    }

    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean ready = player != null && client.level != null && client.gameMode != null && client.getConnection() != null;
        if (!settings.enabled || !ready) {
            if (running) stop();
            return;
        }
        if (!running) begin();
        if (dangerCooldown > 0) dangerCooldown--;

        if (settings.logOffOnSpawner && scanner.countNow(client, true) >= settings.spawnerThreshold) {
            finish(client, "spawner");
            return;
        }
        if (settings.logOffOnStorage && scanner.countNow(client, false) >= config.rtpStash.minStorages) {
            finish(client, "stash");
            return;
        }
        if (dangerCooldown <= 0 && player.getHealth() <= settings.dangerHealth) {
            rtp.requestRtp(client);
            dangerCooldown = DANGER_COOLDOWN;
        }
    }

    private void begin() {
        running = true;
        prevRtp = config.rtpStash.enabled;
        config.rtpStash.enabled = true;
    }

    private void stop() {
        running = false;
        config.rtpStash.enabled = prevRtp;
    }

    private void finish(Minecraft client, String reason) {
        settings.enabled = false;
        var connection = client.getConnection();
        if (connection != null) connection.getConnection().disconnect(Component.literal("Donut: " + reason + " found"));
        stop();
    }
}
