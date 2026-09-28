package dev.donut.client.automation;

import com.mojang.blaze3d.platform.InputConstants;
import dev.donut.client.config.AutoEatSettings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

/** Holds right-click on food from the hotbar whenever the hunger bar runs low. */
public final class AutoEatController {
    private final AutoEatSettings settings;
    private boolean eating;
    private int cooldown;

    public AutoEatController(AutoEatSettings settings) {
        this.settings = settings;
    }

    public void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                || !settings.enabled || client.screen != null || client.isPaused()) {
            if (eating) {
                stop(client);
                eating = false;
            }
            return;
        }
        FoodData food = player.getFoodData();
        if (eating) {
            // Done when the item finishes, or hunger is high enough again.
            if (!player.isUsingItem() || food.getFoodLevel() > settings.hungerThreshold) {
                stop(client);
                eating = false;
                cooldown = 10;
            }
            return;
        }
        if (cooldown > 0 || food.getFoodLevel() > settings.hungerThreshold) return;
        int slot = foodSlot(player);
        if (slot < 0) return;
        if (player.getInventory().getSelectedSlot() != slot) player.getInventory().setSelectedSlot(slot);
        // One click starts the use, then keep the button held like a player would.
        client.options.keyUse.setDown(true);
        InputConstants.Key key = InputConstants.getKey(client.options.keyUse.saveString());
        if (!key.equals(InputConstants.UNKNOWN)) KeyMapping.click(key);
        eating = true;
    }

    private static void stop(Minecraft client) {
        client.options.keyUse.setDown(false);
        while (client.options.keyUse.consumeClick()) { }
    }

    private static int foodSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getComponents().has(DataComponents.FOOD)) return i;
        }
        return -1;
    }
}
