package com.seedcracker.cracker;

import net.minecraft.core.BlockPos;

/**
 * Represents an observed dungeon (monster spawner room).
 * The floor pattern (mossy vs cobblestone) is seeded and can be used
 * to reverse the JavaRandom call chain back to the world seed.
 *
 * @param pos       The block position of the spawner block.
 * @param floorBits A 5x5 bitmask (bits 0-24) where 1 = mossy cobblestone, 0 = cobblestone.
 *                  The floor is sampled left-to-right, bottom-to-top relative to the spawner.
 */
public record DungeonData(BlockPos pos, int floorBits) {

    /**
     * Returns the number of mossy cobblestone blocks in the floor pattern.
     */
    public int mossyCount() {
        return Integer.bitCount(floorBits);
    }

    /**
     * Returns whether the bit at position (x, z) relative to the dungeon corner is mossy.
     * x and z are in range [0, 4].
     */
    public boolean isMossy(int x, int z) {
        return (floorBits & (1 << (z * 5 + x))) != 0;
    }

    @Override
    public String toString() {
        return String.format("DungeonData[pos=%s, mossy=%d/25]", pos, mossyCount());
    }
}
