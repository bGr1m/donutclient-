package dev.donut.client.finder;

import net.minecraft.core.BlockPos;

/** One Block ESP highlight: a position and the colour chosen for that block type. */
public record EspBox(BlockPos pos, int color) { }
