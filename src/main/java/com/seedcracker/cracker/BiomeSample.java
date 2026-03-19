package com.seedcracker.cracker;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * A single observed biome at a given block position.
 * Used to constrain or verify candidate seeds against the world's biome layout.
 */
public record BiomeSample(BlockPos pos, ResourceKey<Biome> biomeKey) {

    @Override
    public String toString() {
        return String.format("BiomeSample[pos=%s, biome=%s]", pos, biomeKey.location());
    }
}
