package dev.donut.client.automation;

import dev.donut.client.config.AutoToolSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Swaps to the fastest hotbar tool for whatever block is being mined. */
public final class AutoToolController {
    private final AutoToolSettings settings;

    public AutoToolController(AutoToolSettings settings) {
        this.settings = settings;
    }

    public void tick(Minecraft client) {
        if (!settings.enabled) return;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.screen != null) return;
        if (!client.options.keyAttack.isDown()) return;
        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) return;

        Inventory inventory = player.getInventory();
        int current = inventory.getSelectedSlot();
        float bestSpeed = speed(inventory.getItem(current), state);
        int best = current;
        for (int i = 0; i < 9; i++) {
            float candidate = speed(inventory.getItem(i), state);
            if (candidate > bestSpeed + 0.01f) {
                bestSpeed = candidate;
                best = i;
            }
        }
        if (best != current) inventory.setSelectedSlot(best);
    }

    private static float speed(ItemStack stack, BlockState state) {
        return stack.isEmpty() ? 1.0f : stack.getDestroySpeed(state);
    }
}
