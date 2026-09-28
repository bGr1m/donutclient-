package dev.donut.client.config;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The catalogue of blocks the Block ESP can highlight, with default colours. */
public final class BlockEspCatalog {
    private BlockEspCatalog() { }

    public enum Target {
        DIAMOND_ORE(Blocks.DIAMOND_ORE, "Diamond Ore", 0x4CF0FF, true),
        DEEPSLATE_DIAMOND_ORE(Blocks.DEEPSLATE_DIAMOND_ORE, "Deep Diamond", 0x2FD0E8, true),
        EMERALD_ORE(Blocks.EMERALD_ORE, "Emerald Ore", 0x38E06E, false),
        DEEPSLATE_EMERALD_ORE(Blocks.DEEPSLATE_EMERALD_ORE, "Deep Emerald", 0x2FC85E, false),
        GOLD_ORE(Blocks.GOLD_ORE, "Gold Ore", 0xFFD34C, false),
        DEEPSLATE_GOLD_ORE(Blocks.DEEPSLATE_GOLD_ORE, "Deep Gold", 0xE8B93C, false),
        IRON_ORE(Blocks.IRON_ORE, "Iron Ore", 0xE0A98B, false),
        DEEPSLATE_IRON_ORE(Blocks.DEEPSLATE_IRON_ORE, "Deep Iron", 0xC88E70, false),
        COPPER_ORE(Blocks.COPPER_ORE, "Copper Ore", 0xE07B4C, false),
        DEEPSLATE_COPPER_ORE(Blocks.DEEPSLATE_COPPER_ORE, "Deep Copper", 0xC8683C, false),
        REDSTONE_ORE(Blocks.REDSTONE_ORE, "Redstone Ore", 0xFF4C4C, false),
        DEEPSLATE_REDSTONE_ORE(Blocks.DEEPSLATE_REDSTONE_ORE, "Deep Redstone", 0xE03A3A, false),
        LAPIS_ORE(Blocks.LAPIS_ORE, "Lapis Ore", 0x4C74FF, false),
        DEEPSLATE_LAPIS_ORE(Blocks.DEEPSLATE_LAPIS_ORE, "Deep Lapis", 0x3A5CE0, false),
        COAL_ORE(Blocks.COAL_ORE, "Coal Ore", 0x9AA3B0, false),
        DEEPSLATE_COAL_ORE(Blocks.DEEPSLATE_COAL_ORE, "Deep Coal", 0x7C8492, false),
        ANCIENT_DEBRIS(Blocks.ANCIENT_DEBRIS, "Ancient Debris", 0xB35C3A, true),
        NETHER_GOLD_ORE(Blocks.NETHER_GOLD_ORE, "Nether Gold", 0xFFC94C, false),
        NETHER_QUARTZ_ORE(Blocks.NETHER_QUARTZ_ORE, "Nether Quartz", 0xF0EAD8, false),
        CHEST(Blocks.CHEST, "Chest", 0xFFC66B, true),
        TRAPPED_CHEST(Blocks.TRAPPED_CHEST, "Trapped Chest", 0xE8A24C, false),
        ENDER_CHEST(Blocks.ENDER_CHEST, "Ender Chest", 0x7EDD8E, false),
        BARREL(Blocks.BARREL, "Barrel", 0xC5A76B, false),
        SPAWNER(Blocks.SPAWNER, "Spawner", 0x9B6BFF, true),
        TRIAL_SPAWNER(Blocks.TRIAL_SPAWNER, "Trial Spawner", 0x6B9BFF, false);

        public final Block block;
        public final String label;
        public final int defaultColor;
        public final boolean defaultEnabled;

        Target(Block block, String label, int defaultColor, boolean defaultEnabled) {
            this.block = block;
            this.label = label;
            this.defaultColor = defaultColor;
            this.defaultEnabled = defaultEnabled;
        }
    }
}
