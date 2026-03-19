package com.seedcracker.finder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Represents a structure location found by the StructureFinder.
 *
 * @param pos       Approximate block position of the structure.
 * @param type      The structure type key (e.g. minecraft:village).
 * @param color     ARGB color used for the 3D highlight box.
 * @param distance  Approximate distance in blocks from the player when found.
 */
public record FoundStructure(
        BlockPos pos,
        ResourceKey<Structure> type,
        int color,
        double distance
) {
    public String displayName() {
        String path = type.location().getPath();
        // Convert snake_case to Title Case for display
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return String.format("FoundStructure[%s @ %s, dist=%.0f]",
                displayName(), pos, distance);
    }
}
