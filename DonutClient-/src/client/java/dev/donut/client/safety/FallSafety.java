package dev.donut.client.safety;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Shared fall-state helpers for the clutch modules. */
public final class FallSafety {
    /** Vanilla fall damage only starts past three blocks. */
    private static final double SAFE_FALL = 3.0;

    private FallSafety() { }

    /** True while the player is genuinely plunging and still able to be saved. */
    public static boolean isFalling(LocalPlayer player) {
        if (player.onGround() || player.isPassenger()) return false;
        if (player.getAbilities().flying || player.isFallFlying()) return false;
        if (player.isCreative() || player.isSpectator() || player.isDeadOrDying()) return false;
        if (player.isInWater() || player.isInLava()) return false;
        return player.fallDistance > 0.0 || player.getDeltaMovement().y < 0.0;
    }

    /** Approximate vanilla fall damage: lethal when it would meet the player's current health. */
    public static boolean isLethal(LocalPlayer player, double extraSafety) {
        double fall = player.fallDistance;
        if (fall <= SAFE_FALL) return false;
        double predicted = Math.ceil(fall - SAFE_FALL);
        return predicted + extraSafety >= player.getHealth();
    }

    /** Distance from the player's feet down to the first solid block, or +inf within maxSearch. */
    public static double groundDistance(Level level, double x, double y, double z, int maxSearch) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int startY = Mth.floor(y);
        for (int i = 0; i < maxSearch; i++) {
            int blockY = startY - i;
            pos.set(bx, blockY, bz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return y - (blockY + 1.0);
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /** The first solid block below the player's feet, or null within maxSearch blocks. */
    public static net.minecraft.core.BlockPos firstSolidBelow(Level level, double x, double y, double z, int maxSearch) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int startY = Mth.floor(y);
        for (int i = 0; i < maxSearch; i++) {
            int blockY = startY - i;
            pos.set(bx, blockY, bz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** True when the first solid block below has a replaceable space on top for a placed block. */
    public static boolean landingIsReplaceable(Level level, double x, double y, double z, int maxSearch) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int startY = Mth.floor(y);
        for (int i = 0; i < maxSearch; i++) {
            int blockY = startY - i;
            pos.set(bx, blockY, bz);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                pos.set(bx, blockY + 1, bz);
                return level.getBlockState(pos).canBeReplaced();
            }
            if (!state.canBeReplaced()) return false;
        }
        return false;
    }

    /** First hotbar slot (0-8) holding the given item, or -1. */
    public static int hotbarSlot(LocalPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) return slot;
        }
        return -1;
    }
}
