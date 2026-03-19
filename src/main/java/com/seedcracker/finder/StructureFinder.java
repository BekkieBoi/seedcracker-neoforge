package com.seedcracker.finder;

import com.seedcracker.core.SeedCracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StructureFinder {

    private static final List<FoundStructure> foundStructures = new CopyOnWriteArrayList<>();
    private static long lastSearchSeed = Long.MIN_VALUE;
    private static BlockPos lastSearchPos = null;
    private static final int SEARCH_RADIUS_CHUNKS = 64;

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

    public static List<FoundStructure> getFoundStructures() {
        return Collections.unmodifiableList(foundStructures);
    }

    public static void clearResults() {
        foundStructures.clear();
        lastSearchSeed = Long.MIN_VALUE;
        lastSearchPos = null;
    }

    public static void findStructures(long worldSeed, BlockPos searchPos, int maxResults) {
        if (worldSeed == lastSearchSeed && searchPos.equals(lastSearchPos)) return;
        lastSearchSeed = worldSeed;
        lastSearchPos = searchPos;
        foundStructures.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            findStructuresSingleplayer(mc, searchPos, maxResults);
        } else {
            findStructuresMultiplayer(worldSeed, searchPos, maxResults);
        }
        SeedCracker.LOGGER.info("[StructureFinder] Found {} structures.", foundStructures.size());
    }

    public static void findSpecificStructure(long worldSeed, BlockPos searchPos, String structureName, int radiusBlocks) {
        foundStructures.removeIf(s -> s.type().location().getPath().equals(structureN
