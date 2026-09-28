package dev.donut.client.automation;

import com.mojang.blaze3d.platform.InputConstants;
import dev.donut.client.config.RtpStashSettings;
import dev.donut.client.finder.FinderScanner;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Sends /rtp, digs straight down to the target Y level, then checks the storage finder. It repeats
 * until enough storage is found, then disables itself (and optionally logs the player off).
 * Dropping into water or lava triggers a fresh /rtp.
 */
public final class RtpStashController {
    private enum Phase { RTP, WAIT, DIG, SCAN }

    private static final int WATER_RTP_COOLDOWN = 40;
    /** Water must be occupied this long before it re-RTPs, so MLG water does not trigger it. */
    private static final int WATER_RTP_TICKS = 40;

    private final RtpStashSettings settings;
    private final FinderScanner scanner;

    private boolean running;
    private Phase phase = Phase.RTP;
    private int timer;
    private int digTicks;
    private double startX;
    private double startZ;
    private double lastY;
    private int stuckTicks;
    private float savedPitch;
    private boolean attackStarted;
    private int waterCooldown;
    private int waterTicks;

    public RtpStashController(RtpStashSettings settings, FinderScanner scanner) {
        this.settings = settings;
        this.scanner = scanner;
    }

    public boolean isRunning() { return running; }

    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean ready = player != null && client.level != null && client.gameMode != null && client.getConnection() != null;
        if (!settings.enabled || !ready) {
            if (running) shutdown(client, false);
            return;
        }
        if (!running) begin(player);
        if (waterCooldown > 0) waterCooldown--;

        if (phase != Phase.RTP && waterCooldown <= 0) {
            if (player.isInLava()) {
                requestRtp(client);
                return;
            }
            if (player.isInWater()) {
                if (++waterTicks >= WATER_RTP_TICKS) {
                    requestRtp(client);
                    return;
                }
            } else {
                waterTicks = 0;
            }
        }

        switch (phase) {
            case RTP -> doRtp(player);
            case WAIT -> doWait(player);
            case DIG -> doDig(client, player);
            case SCAN -> doScan(client);
        }
    }

    /** Jump straight back to a fresh /rtp (used by RTP Plus on danger). */
    public void requestRtp(Minecraft client) {
        if (!running) return;
        stopDigging(client);
        waterCooldown = WATER_RTP_COOLDOWN;
        waterTicks = 0;
        phase = Phase.RTP;
    }

    private void begin(LocalPlayer player) {
        running = true;
        phase = Phase.RTP;
        digTicks = 0;
        stuckTicks = 0;
        attackStarted = false;
        waterCooldown = 0;
        waterTicks = 0;
        savedPitch = player.getXRot();
    }

    private void doRtp(LocalPlayer player) {
        player.connection.sendCommand("rtp");
        startX = player.position().x;
        startZ = player.position().z;
        timer = settings.rtpWaitTicks;
        waterCooldown = WATER_RTP_COOLDOWN;
        phase = Phase.WAIT;
    }

    private void doWait(LocalPlayer player) {
        var pos = player.position();
        double moved = Math.hypot(pos.x - startX, pos.z - startZ);
        if (--timer <= 0 || moved > 64.0) {
            digTicks = 0;
            stuckTicks = 0;
            lastY = pos.y;
            phase = Phase.DIG;
        }
    }

    private void doDig(Minecraft client, LocalPlayer player) {
        var pos = player.position();
        if (pos.y <= settings.targetY) {
            stopDigging(client);
            timer = settings.scanTicks;
            phase = Phase.SCAN;
            return;
        }
        if (++digTicks > settings.maxDigTicks) {
            stopDigging(client);
            phase = Phase.RTP;
            return;
        }
        if (Math.abs(pos.y - lastY) < 0.001) {
            if (++stuckTicks > 120) {
                stopDigging(client);
                phase = Phase.RTP;
                return;
            }
        } else {
            stuckTicks = 0;
            lastY = pos.y;
        }
        // Look straight down and hold left click, exactly like a player mining.
        player.setXRot(90.0f);
        client.options.keyAttack.setDown(true);
        if (!attackStarted) {
            InputConstants.Key key = InputConstants.getKey(client.options.keyAttack.saveString());
            if (!key.equals(InputConstants.UNKNOWN)) KeyMapping.click(key);
            attackStarted = true;
        }
    }

    private void doScan(Minecraft client) {
        if (--timer > 0) return;
        if (scanner.countNow(client, false) >= settings.minStorages) {
            shutdown(client, true);
        } else {
            phase = Phase.RTP;
        }
    }

    private void stopDigging(Minecraft client) {
        if (attackStarted) {
            client.options.keyAttack.setDown(false);
            while (client.options.keyAttack.consumeClick()) { }
            attackStarted = false;
        }
        if (client.gameMode != null) client.gameMode.stopDestroyBlock();
    }

    private void shutdown(Minecraft client, boolean success) {
        running = false;
        stopDigging(client);
        LocalPlayer player = client.player;
        if (player != null) player.setXRot(savedPitch);
        if (success) {
            settings.enabled = false;
            if (settings.logOff) {
                var connection = client.getConnection();
                if (connection != null) connection.getConnection().disconnect(Component.literal("Donut: stash found"));
            }
        }
    }
}
