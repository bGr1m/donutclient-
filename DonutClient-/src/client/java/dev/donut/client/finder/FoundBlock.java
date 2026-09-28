package dev.donut.client.finder;

import net.minecraft.core.BlockPos;

public record FoundBlock(BlockPos pos, TargetKind kind) { }
