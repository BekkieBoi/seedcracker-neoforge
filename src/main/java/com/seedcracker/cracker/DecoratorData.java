package com.seedcracker.cracker;

import net.minecraft.core.BlockPos;

/**
 * Represents an observed decorator placement (e.g., ore veins, vegetation).
 * Each decorator call within a chunk consumes a predictable sequence of
 * random numbers, allowing the decorator seed to be reverse-engineered.
 *
 * @param chunkX   Chunk X coordinate.
 * @param chunkZ   Chunk Z coordinate.
 * @param blockPos The exact position of the placed decorator.
 * @param type     What kind of decorator this is (ore, plant, etc.).
 */
public record DecoratorData(int chunkX, int chunkZ, BlockPos blockPos, DecoratorType type) {

    public enum DecoratorType {
        EMERALD_ORE,
        WARPED_FUNGUS,
        CRIMSON_FUNGUS,
        DUNGEON
    }

    @Override
    public String toString() {
        return String.format("DecoratorData[chunk=(%d,%d), pos=%s, type=%s]",
                chunkX, chunkZ, blockPos, type);
    }
}
