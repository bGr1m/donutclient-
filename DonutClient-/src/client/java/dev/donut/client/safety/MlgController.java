package dev.donut.client.safety;

import dev.donut.client.config.MlgSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/** Places a water bucket below the player when a fall would otherwise be fatal. */
public final class MlgController {
    private static final int GROUND_SEARCH = 8;
    private static final int PICKUP_TIMEOUT = 60;

    private final MlgSettings settings;
    private int cooldown;
    private int restoreSlot = -1;
    private int restoreTicks;

    private boolean pickupPending;
    private BlockPos waterPos;
    private float healthBefore;
    private int pickupTimer;

    public MlgController(MlgSettings settings) {
        this.settings = settings;
    }

    public void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        LocalPlayer player = client.player;
        if (restoreSlot >= 0 && --restoreTicks <= 0) {
            if (player != null) player.getInventory().setSelectedSlot(restoreSlot);
            restoreSlot = -1;
        }
        if (player == null) return;
        tickPickup(client, player);
        if (!settings.enabled || cooldown > 0) return;
        if (client.screen != null || client.isPaused() || client.gameMode == null || client.level == null) return;
        if (player.fallDistance < settings.minFallDistance) return;
        if (!FallSafety.isFalling(player) || !FallSafety.isLethal(player, settings.extraSafety)) return;

        var pos = player.position();
        double ground = FallSafety.groundDistance(client.level, pos.x, pos.y, pos.z, GROUND_SEARCH);
        if (!(ground <= settings.placeDistance)) return;
        if (!FallSafety.landingIsReplaceable(client.level, pos.x, pos.y, pos.z, GROUND_SEARCH)) return;
        int slot = FallSafety.hotbarSlot(player, Items.WATER_BUCKET);
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
        if (settings.pickupWater) {
            BlockPos solid = FallSafety.firstSolidBelow(client.level, pos.x, pos.y, pos.z, GROUND_SEARCH);
            if (solid != null) {
                waterPos = solid.above().immutable();
                healthBefore = player.getHealth();
                pickupTimer = PICKUP_TIMEOUT;
                pickupPending = true;
            }
        }
    }

    /** Once the player has landed unharmed, scoop the water back into the empty bucket. */
    private void tickPickup(Minecraft client, LocalPlayer player) {
        if (!pickupPending) return;
        if (client.level == null || client.gameMode == null || waterPos == null) {
            pickupPending = false;
            return;
        }
        if (pickupTimer-- <= 0 || player.getHealth() < healthBefore) {
            pickupPending = false;
            return;
        }
        if (!player.onGround() && !player.isInWater()) return;
        if (!client.level.getFluidState(waterPos).is(Fluids.WATER)) {
            pickupPending = false;
            return;
        }
        int slot = FallSafety.hotbarSlot(player, Items.BUCKET);
        if (slot < 0) {
            pickupPending = false;
            return;
        }
        if (player.getInventory().getSelectedSlot() != slot) player.getInventory().setSelectedSlot(slot);
        float savedYaw = player.getYRot();
        float savedPitch = player.getXRot();
        lookAt(player, waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5);
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.setYRot(savedYaw);
        player.setXRot(savedPitch);
        pickupPending = false;
    }

    private static void lookAt(LocalPlayer player, double tx, double ty, double tz) {
        double eyeX = player.getEyePosition().x;
        double eyeY = player.getEyePosition().y;
        double eyeZ = player.getEyePosition().z;
        double dx = tx - eyeX;
        double dy = ty - eyeY;
        double dz = tz - eyeZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, horizontal))));
    }
}
