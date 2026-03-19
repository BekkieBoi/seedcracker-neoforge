package com.seedcracker.finder;

import com.seedcracker.core.SeedCracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Searches for structures near the player using the server-side world gen
 * registry (available in singleplayer) or a seed-based approximation
 * for multiplayer.
 *
 * Structures searched:
 *   - Village (all biome variants)
 *   - Stronghold
 *   - Mineshaft
 *   - Desert Pyramid
 *   - Jungle Temple
 *   - Ocean Monument
 *   - Woodland Mansion
 *   - Nether Fortress
 *   - Bastion Remnant
 *   - End City
 *
 * Color coding:
 *   Village        → green  (0x00FF00)
 *   Stronghold     → gold   (0xFFD700)
 *   Mineshaft      → gray   (0x888888)
 *   Desert Pyramid → yellow (0xFFFF00)
 *   Jungle Temple  → lime   (0x00CC44)
 *   Ocean Monument → cyan   (0x00FFFF)
 *   Woodland Mansion → purple (0xAA00FF)
 *   Nether Fortress → red   (0xFF4400)
 *   Bastion        → orange (0xFF8800)
 *   End City       → white  (0xFFFFFF)
 */
public final class StructureFinder {

    private static final List<FoundStructure> foundStructures = new CopyOnWriteArrayList<>();
    private static long lastSearchSeed = Long.MIN_VALUE;
    private static BlockPos lastSearchPos = null;
    private static final int SEARCH_RADIUS_CHUNKS = 64;

    // Structure resource keys
    private static final Map<ResourceKey<Structure>, Integer> STRUCTURE_COLORS = new LinkedHashMap<>();

    static {
        reg("village",          0x00FF00);
        reg("stronghold",       0xFFD700);
        reg("mineshaft",        0x888888);
        reg("desert_pyramid",   0xFFFF00);
        reg("jungle_temple",    0x00CC44);
        reg("ocean_monument",   0x00FFFF);
        reg("woodland_mansion", 0xAA00FF);
        reg("fortress",         0xFF4400);
        reg("bastion_remnant",  0xFF8800);
        reg("end_city",         0xFFFFFF);
        reg("ruined_portal",    0xCC6600);
        reg("shipwreck",        0x4466AA);
        reg("pillager_outpost", 0xFF0000);
        reg("ancient_city",     0x6600CC);
    }

    private static void reg(String name, int color) {
        STRUCTURE_COLORS.put(
            ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.withDefaultNamespace(name)),
            color
        );
    }

    private StructureFinder() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static List<FoundStructure> getFoundStructures() {
        return Collections.unmodifiableList(foundStructures);
    }

    public static void clearResults() {
        foundStructures.clear();
        lastSearchSeed = Long.MIN_VALUE;
        lastSearchPos = null;
    }

    /**
     * Attempts to find structures near the player.
     * Works best in singleplayer where the server level is accessible.
     * In multiplayer, relies on the cracked seed + chunk-level RNG approximation.
     *
     * @param worldSeed  The cracked world seed.
     * @param searchPos  The centre of the search area (usually the player's position).
     * @param maxResults Maximum number of results per structure type.
     */
    public static void findStructures(long worldSeed, BlockPos searchPos, int maxResults) {
        if (worldSeed == lastSearchSeed && searchPos.equals(lastSearchPos)) return;

        lastSearchSeed = worldSeed;
        lastSearchPos = searchPos;
        foundStructures.clear();

        Minecraft mc = Minecraft.getInstance();

        // Singleplayer: use the real server-side structure locator
        if (mc.getSingleplayerServer() != null) {
            findStructuresSingleplayer(mc, searchPos, maxResults);
        } else {
            // Multiplayer: use seed-based chunk RNG approximation
            findStructuresMultiplayer(worldSeed, searchPos, maxResults);
        }

        SeedCracker.LOGGER.info("[StructureFinder] Found {} structures.", foundStructures.size());
    }

    /**
     * Searches for only one specific structure type near the player.
     * Used by /sf locate <structure> [radius].
     *
     * @param worldSeed     The cracked world seed.
     * @param searchPos     Centre of the search (player position).
     * @param structureName The path name of the structure (e.g. "village", "stronghold").
     * @param radiusBlocks  Search radius in blocks.
     */
    public static void findSpecificStructure(long worldSeed, BlockPos searchPos,
                                             String structureName, int radiusBlocks) {
        // Remove old results for this structure type so results stay fresh
        foundStructures.removeIf(s -> s.type().location().getPath().equals(structureName));

        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.withDefaultNamespace(structureName));
        int color = STRUCTURE_COLORS.getOrDefault(key, 0xFFFFFF);

        Minecraft mc = Minecraft.getInstance();

        if (mc.getSingleplayerServer() != null) {
            findSpecificSingleplayer(mc, searchPos, key, color, radiusBlocks, 10);
        } else {
            findSpecificMultiplayer(worldSeed, searchPos, key, color, radiusBlocks);
        }

        foundStructures.sort(Comparator.comparingDouble(FoundStructure::distance));
        SeedCracker.LOGGER.info("[StructureFinder] Locate '{}': {} result(s).",
                structureName, foundStructures.stream()
                        .filter(s -> s.type().location().getPath().equals(structureName)).count());
    }

    // -------------------------------------------------------------------------
    // Singleplayer: use server-level StructureManager
    // -------------------------------------------------------------------------

    private static void findSpecificSingleplayer(Minecraft mc, BlockPos searchPos,
                                                  ResourceKey<Structure> key, int color,
                                                  int radiusBlocks, int maxResults) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;

        ServerLevel level = server.overworld();
        var structureManager = level.structureManager();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        Structure structure = registry.get(key);
        if (structure == null) return;

        int radiusChunks = Math.max(1, radiusBlocks >> 4);
        int found = 0;

        for (int dx = -radiusChunks; dx <= radiusChunks && found < maxResults; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks && found < maxResults; dz++) {
                int cx = (searchPos.getX() >> 4) + dx;
                int cz = (searchPos.getZ() >> 4) + dz;
                ChunkPos chunkPos = new ChunkPos(cx, cz);

                StructureStart start = structureManager.getStartForStructure(
                        chunkPos, structure,
                        level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, false)
                );

                if (start != null && start.isValid()) {
                    BlockPos structPos = new BlockPos(
                            start.getBoundingBox().minX(),
                            start.getBoundingBox().minY(),
                            start.getBoundingBox().minZ()
                    );
                    double dist = Math.sqrt(structPos.distSqr(searchPos));
                    foundStructures.add(new FoundStructure(structPos, key, color, dist));
                    found++;
                }
            }
        }
    }

    private static void findSpecificMultiplayer(long worldSeed, BlockPos searchPos,
                                                 ResourceKey<Structure> key, int color,
                                                 int radiusBlocks) {
        // Derive spacing/spread for this structure type
        String name = key.location().getPath();
        int spacing = 34, spread = 8;
        switch (name) {
            case "ocean_monument"   -> { spacing = 32; spread = 5; }
            case "woodland_mansion" -> { spacing = 80; spread = 20; }
            case "fortress", "bastion_remnant" -> { spacing = 27; spread = 4; }
            case "stronghold"       -> { spacing = 6; spread = 2; }
            case "ancient_city"     -> { spacing = 24; spread = 8; }
        }

        int radiusChunks = Math.max(1, radiusBlocks >> 4);
        int playerRegionX = Math.floorDiv(searchPos.getX() >> 4, spacing);
        int playerRegionZ = Math.floorDiv(searchPos.getZ() >> 4, spacing);
        int r = Math.max(1, radiusChunks / spacing);

        for (int rx = playerRegionX - r; rx <= playerRegionX + r; rx++) {
            for (int rz = playerRegionZ - r; rz <= playerRegionZ + r; rz++) {
                long regionSeed = worldSeed ^ ((long)rx * 341873128712L) ^ ((long)rz * 132897987541L);
                Random rng = new Random(regionSeed);

                int chunkX = rx * spacing + rng.nextInt(spacing - spread);
                int chunkZ = rz * spacing + rng.nextInt(spacing - spread);

                BlockPos structPos = new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                double dist = Math.sqrt(structPos.distSqr(searchPos));

                if (dist <= radiusBlocks) {
                    foundStructures.add(new FoundStructure(structPos, key, color, dist));
                }
            }
        }
    }

    private static void findStructuresSingleplayer(Minecraft mc, BlockPos searchPos, int maxResults) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;

        ServerLevel level = server.overworld();
        var structureManager = level.structureManager();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (var entry : STRUCTURE_COLORS.entrySet()) {
            ResourceKey<Structure> key = entry.getKey();
            int color = entry.getValue();

            Structure structure = registry.get(key);
            if (structure == null) continue;

            int found = 0;
            int r = SEARCH_RADIUS_CHUNKS;

            for (int dx = -r; dx <= r && found < maxResults; dx++) {
                for (int dz = -r; dz <= r && found < maxResults; dz++) {
                    int cx = (searchPos.getX() >> 4) + dx;
                    int cz = (searchPos.getZ() >> 4) + dz;
                    ChunkPos chunkPos = new ChunkPos(cx, cz);

                    StructureStart start = structureManager.getStartForStructure(
                            chunkPos, structure,
                            level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, false)
                    );

                    if (start != null && start.isValid()) {
                        BlockPos structPos = new BlockPos(
                                start.getBoundingBox().minX(),
                                start.getBoundingBox().minY(),
                                start.getBoundingBox().minZ()
                        );
                        double dist = Math.sqrt(structPos.distSqr(searchPos));
                        foundStructures.add(new FoundStructure(structPos, key, color, dist));
                        found++;
                    }
                }
            }
        }

        foundStructures.sort(Comparator.comparingDouble(FoundStructure::distance));
    }

    // -------------------------------------------------------------------------
    // Multiplayer: seed-based approximate structure detection
    // -------------------------------------------------------------------------

    /**
     * Approximates structure positions using the world seed and Java's Random.
     *
     * Many Minecraft structures use a placement algorithm like:
     *   random.setSeed(worldSeed ^ (regionX * A) ^ (regionZ * B))
     *   int placementX = regionX * spacing + random.nextInt(spread)
     *   int placementZ = regionZ * spacing + random.nextInt(spread)
     *
     * This is accurate for regularly-spaced structures (villages, pillager outposts, etc.)
     * but less reliable for strongholds and mineshafts.
     */
    private static void findStructuresMultiplayer(long worldSeed, BlockPos searchPos, int maxResults) {
        // Village / pillager outpost style (separation = 34, spacing = 32)
        findRegularStructure(worldSeed, searchPos, maxResults,
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("village")),
                0x00FF00, 34, 8);

        findRegularStructure(worldSeed, searchPos, maxResults,
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("pillager_outpost")),
                0xFF0000, 32, 8);

        findRegularStructure(worldSeed, searchPos, maxResults,
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("desert_pyramid")),
                0xFFFF00, 32, 8);

        findRegularStructure(worldSeed, searchPos, maxResults,
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("jungle_temple")),
                0x00CC44, 32, 8);

        findRegularStructure(worldSeed, searchPos, maxResults,
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("ocean_monument")),
                0x00FFFF, 32, 5);

        findRegularStructure(worldSeed, searchPos, maxResults,
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("woodland_mansion")),
                0xAA00FF, 80, 20);
    }

    private static void findRegularStructure(
            long worldSeed, BlockPos searchPos, int maxResults,
            ResourceKey<Structure> key, int color, int spacing, int spread
    ) {
        int playerRegionX = Math.floorDiv(searchPos.getX() >> 4, spacing);
        int playerRegionZ = Math.floorDiv(searchPos.getZ() >> 4, spacing);

        int r = Math.max(1, SEARCH_RADIUS_CHUNKS / spacing);
        int found = 0;

        for (int rx = playerRegionX - r; rx <= playerRegionX + r && found < maxResults; rx++) {
            for (int rz = playerRegionZ - r; rz <= playerRegionZ + r && found < maxResults; rz++) {
                long regionSeed = worldSeed ^ ((long)rx * 341873128712L) ^ ((long)rz * 132897987541L);
                Random rng = new Random(regionSeed);

                int chunkX = rx * spacing + rng.nextInt(spacing - spread);
                int chunkZ = rz * spacing + rng.nextInt(spacing - spread);

                BlockPos structPos = new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                double dist = Math.sqrt(structPos.distSqr(searchPos));

                if (dist <= SEARCH_RADIUS_CHUNKS * 16.0) {
                    foundStructures.add(new FoundStructure(structPos, key, color, dist));
                    found++;
                }
            }
        }
    }
}
