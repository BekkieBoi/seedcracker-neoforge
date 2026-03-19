package com.seedcracker.mixin;

import com.seedcracker.core.SeedCracker;
import com.seedcracker.cracker.BiomeSample;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts chunk data packets to sample biome information.
 * We read the biome at the chunk center to build a spatial biome map
 * that can constrain the world seed search space.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(
        method = "handleLevelChunkWithLight",
        at = @At(value = "RETURN")
    )
    private void onChunkReceived(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        if (SeedCracker.getDataStorage().getCrackedSeed().isPresent()) return;

        int chunkX = packet.getX();
        int chunkZ = packet.getZ();

        // Sample biome at the approximate center of the chunk, at sea level
        // We defer actual biome reading to the ClientEventHandler once the chunk is loaded.
        // Here we just flag that this chunk needs biome sampling.
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        var level = listener.getLevel();
        if (level == null) return;

        // Sample a point near the center of the chunk at y=64
        int sampleX = (chunkX << 4) + 8;
        int sampleZ = (chunkZ << 4) + 8;
        BlockPos samplePos = new BlockPos(sampleX, 64, sampleZ);

        try {
            var biomeHolder = level.getBiome(samplePos);
            var biomeKey = biomeHolder.unwrapKey().orElse(null);
            if (biomeKey != null) {
                BiomeSample sample = new BiomeSample(samplePos, biomeKey);
                SeedCracker.getDataStorage().addBiomeSample(sample);
            }
        } catch (Exception ignored) {
            // Chunk may not be fully loaded yet; silently skip
        }
    }
}
