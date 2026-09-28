package dev.donut.client.safety;

import dev.donut.client.config.PearlSaveSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Throws an ender pearl straight down when a fall would otherwise be fatal. */
public final class PearlSaveController {
    private static final int GROUND_SEARCH = 96;

    private final PearlSaveSettings settings;
    private int cooldown;
    private int restoreSlot = -1;
    private int restoreTicks;

    public PearlSaveController(PearlSaveSettings settings) {
        this.settings = settings;
    }

    public void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        LocalPlayer player = client.player;
        if (restoreSlot >= 0 && --restoreTicks <= 0) {
            if (player != null) player.getInventory().setSelectedSlot(restoreSlot);
            restoreSlot = -1;
        }
        if (player == null || !settings.enabled || cooldown > 0) return;
        if (client.screen != null || client.isPaused() || client.gameMode == null || client.level == null) return;
        if (player.fallDistance < settings.minFallDistance) return;
        if (!FallSafety.isFalling(player) || !FallSafety.isLethal(player, settings.extraSafety)) return;
        var pos = player.position();
        if (!Double.isFinite(FallSafety.groundDistance(client.level, pos.x, pos.y, pos.z, GROUND_SEARCH))) return;
        int slot = FallSafety.hotbarSlot(player, Items.ENDER_PEARL);
        if (slot < 0) return;

        int previous = player.getInventory().getSelectedSlot();
        if (previous != slot) player.getInventory().setSelectedSlot(slot);
        float savedPitch = player.getXRot();
        player.setXRot(90.0f);
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.setXRot(savedPitch);

        cooldown = settings.cooldownTicks;
        if (settings.switchBack && previous != slot) {
            restoreSlot = previous;
            restoreTicks = 3;
        }
    }
}
