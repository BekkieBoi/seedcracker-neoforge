package com.seedcracker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seedcracker.render.SeedCrackerOverlay;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the level renderer to draw the structure finder overlays
 * (highlight beams/boxes at found structure positions).
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
        method = "renderLevel",
        at = @At("RETURN")
    )
    private void onRenderLevel(
            net.minecraft.client.renderer.RenderBuffers renderBuffers,
            double partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        SeedCrackerOverlay.render(camera, new PoseStack(), projectionMatrix);
    }
}
