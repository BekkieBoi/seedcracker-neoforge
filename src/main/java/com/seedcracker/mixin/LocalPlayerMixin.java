package com.seedcracker.mixin;

import com.seedcracker.core.SeedCracker;
import com.seedcracker.cracker.DecoratorData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Monitors the player's surroundings for decorator-relevant blocks.
 * On each tick, if the player is near an emerald ore or nether fungus,
 * we record its position as a potential decorator data point.
 *
 * Note: This is a lightweight scan, not a full chunk analysis.
 * A full implementation would hook into block placement events per-chunk.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    private int tickCounter = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        // Scan every 40 ticks (2 seconds) to avoid performance impact
        if (++tickCounter < 40) return;
        tickCounter = 0;

        if (SeedCracker.getDataStorage().getCrackedSeed().isPresent()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        var level = player.clientLevel;
        BlockPos origin = player.blockPosition();

        // Scan a small radius for decorator-significant blocks
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    DecoratorData.DecoratorType type = null;
                    if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
                        type = DecoratorData.DecoratorType.EMERALD_ORE;
                    } else if (state.is(Blocks.WARPED_FUNGUS)) {
                        type = DecoratorData.DecoratorType.WARPED_FUNGUS;
                    } else if (state.is(Blocks.CRIMSON_FUNGUS)) {
                        type = DecoratorData.DecoratorType.CRIMSON_FUNGUS;
                    }

                    if (type != null) {
                        int cx = pos.getX() >> 4;
                        int cz = pos.getZ() >> 4;
                        DecoratorData data = new DecoratorData(cx, cz, pos, type);
                        SeedCracker.getDataStorage().addDecoratorData(data);
                    }
                }
            }
        }
    }
}
