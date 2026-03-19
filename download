package com.seedcracker.mixin;

import com.seedcracker.core.SeedCracker;
import com.seedcracker.cracker.DungeonData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts block updates on the client level.
 * When a spawner block is placed (dungeon loaded), we scan the surrounding
 * 5x5 floor to read the mossy cobblestone pattern.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void onBlocksDirty(int x, int y, int z, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (newState.is(Blocks.SPAWNER)) {
            ClientLevel level = (ClientLevel)(Object)this;
            BlockPos spawnerPos = new BlockPos(x, y, z);
            scanDungeonFloor(level, spawnerPos);
        }
    }

    private void scanDungeonFloor(ClientLevel level, BlockPos spawnerPos) {
        // The dungeon floor is the layer directly below the spawner.
        // We scan a 5x5 area. Corner is at (spawnerX - 2, spawnerZ - 2).
        int floorY = spawnerPos.getY() - 1;
        int cornerX = spawnerPos.getX() - 2;
        int cornerZ = spawnerPos.getZ() - 2;

        // Verify this looks like a dungeon: at least 4 walls must be cobblestone/mossy
        int mossyOrCobble = 0;
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                BlockState state = level.getBlockState(new BlockPos(cornerX + dx, floorY, cornerZ + dz));
                if (state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.COBBLESTONE)) {
                    mossyOrCobble++;
                }
            }
        }

        // If fewer than 10 of the 25 floor tiles are dungeon material, skip
        if (mossyOrCobble < 10) return;

        // Build the bitmask: bit = 1 for mossy, 0 for cobblestone
        int floorBits = 0;
        for (int dz = 0; dz < 5; dz++) {
            for (int dx = 0; dx < 5; dx++) {
                BlockState state = level.getBlockState(new BlockPos(cornerX + dx, floorY, cornerZ + dz));
                if (state.is(Blocks.MOSSY_COBBLESTONE)) {
                    floorBits |= (1 << (dz * 5 + dx));
                }
            }
        }

        DungeonData data = new DungeonData(spawnerPos, floorBits);
        SeedCracker.LOGGER.info("[SeedCracker] Dungeon scanned at {}: {}", spawnerPos, data);
        SeedCracker.getDataStorage().addDungeonData(data);
    }
}
