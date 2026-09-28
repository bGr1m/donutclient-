package dev.donut.client.finder;

import dev.donut.client.config.BlockEspCatalog;
import dev.donut.client.config.BlockEspSettings;
import dev.donut.client.config.ClientConfig;
import dev.donut.client.config.FinderSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Reads block entities and chosen blocks from already-loaded chunks; never requests chunks. */
public final class FinderScanner {
    public static final int MAX_TARGETS = 4096;
    private final ClientConfig config;
    private List<FoundBlock> targets = List.of();
    private List<EspBox> espTargets = List.of();
    private ClientLevel lastLevel;
    private int ticks;
    private int espTicks;

    public FinderScanner(ClientConfig config) { this.config = config; }

    public List<FoundBlock> snapshot() { return targets; }
    public List<EspBox> espSnapshot() { return espTargets; }
    public long storageCount() { return targets.stream().filter(t -> !t.kind().isSpawner()).count(); }
    public long spawnerCount() { return targets.stream().filter(t -> t.kind().isSpawner()).count(); }

    public void clear() {
        targets = List.of();
        espTargets = List.of();
        lastLevel = null;
        ticks = 0;
        espTicks = 0;
    }

    public void tick(Minecraft client) {
        if (client.level == null || client.player == null) { clear(); return; }
        if (client.level != lastLevel) {
            clear();
            lastLevel = client.level;
        }
        if (!config.storage.enabled && !config.spawner.enabled) {
            targets = List.of();
            ticks = 0;
        } else if (ticks++ % 10 == 0) {
            scanFinders(client);
        }
        if (config.blockEsp.enabled) {
            if (espTicks++ % 20 == 0) scanEsp(client);
        } else {
            espTargets = List.of();
            espTicks = 0;
        }
    }

    private void scanFinders(Minecraft client) {
        ClientLevel level = client.level;
        double range = Math.max(config.storage.enabled ? config.storage.range : 0,
                config.spawner.enabled ? config.spawner.range : 0);
        int radius = (int) Math.ceil(range / 16.0);
        int centerX = client.player.chunkPosition().x;
        int centerZ = client.player.chunkPosition().z;
        var origin = client.player.position();
        List<FoundBlock> found = new ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity.isRemoved()) continue;
                    TargetKind kind = TargetKind.from(entity.getBlockState());
                    if (kind == null) continue;
                    FinderSettings settings = kind.isSpawner() ? config.spawner : config.storage;
                    if (!settings.enabled || !kind.included(settings)) continue;
                    var pos = entity.getBlockPos();
                    if (pos.distToCenterSqr(origin) <= settings.range * settings.range) {
                        found.add(new FoundBlock(pos.immutable(), kind));
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(target -> target.pos().distToCenterSqr(origin)));
        targets = List.copyOf(found.subList(0, Math.min(found.size(), MAX_TARGETS)));
    }

    /**
     * Counts matching blocks in loaded chunks immediately, regardless of the finder toggles.
     * Used by the RTP automation so detection never depends on the render cadence.
     */
    public long countNow(Minecraft client, boolean spawners) {
        ClientLevel level = client.level;
        if (level == null || client.player == null) return 0;
        FinderSettings settings = spawners ? config.spawner : config.storage;
        double range = settings.range;
        int radius = (int) Math.ceil(range / 16.0);
        int centerX = client.player.chunkPosition().x;
        int centerZ = client.player.chunkPosition().z;
        var origin = client.player.position();
        long count = 0;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity.isRemoved()) continue;
                    TargetKind kind = TargetKind.from(entity.getBlockState());
                    if (kind == null || kind.isSpawner() != spawners) continue;
                    if (!kind.included(settings)) continue;
                    if (entity.getBlockPos().distToCenterSqr(origin) <= range * range) count++;
                }
            }
        }
        return count;
    }

    private void scanEsp(Minecraft client) {
        ClientLevel level = client.level;
        BlockEspSettings esp = config.blockEsp;
        double range = esp.range;
        int radius = (int) Math.ceil(range / 16.0);
        int centerX = client.player.chunkPosition().x;
        int centerZ = client.player.chunkPosition().z;
        var origin = client.player.position();
        int minSectionY = level.getMinY() >> 4;
        List<EspBox> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                var chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                for (int si = 0; si < sections.length; si++) {
                    LevelChunkSection section = sections[si];
                    if (section.hasOnlyAir() || !section.maybeHas(state -> espColor(state) != 0)) continue;
                    int baseY = (minSectionY + si) << 4;
                    int baseX = cx << 4;
                    int baseZ = cz << 4;
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                int color = espColor(section.getBlockState(x, y, z));
                                if (color == 0) continue;
                                pos.set(baseX + x, baseY + y, baseZ + z);
                                if (pos.distToCenterSqr(origin) > range * range) continue;
                                found.add(new EspBox(pos.immutable(), color));
                                if (found.size() >= MAX_TARGETS) { espTargets = List.copyOf(found); return; }
                            }
                        }
                    }
                }
            }
        }
        espTargets = List.copyOf(found);
    }

    /** Returns 0 for "not highlighted", otherwise a non-zero ARGB colour for the block. */
    private int espColor(BlockState state) {
        for (BlockEspCatalog.Target target : BlockEspCatalog.Target.values()) {
            if (!state.is(target.block)) continue;
            BlockEspSettings.Entry entry = config.blockEsp.entry(target);
            if (entry.enabled) return entry.color | 0xFF000000;
        }
        return 0;
    }
}
