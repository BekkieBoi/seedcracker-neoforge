package com.seedcracker.event;

import com.seedcracker.core.SeedCracker;
import com.seedcracker.render.SeedCrackerHud;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Handles NeoForge client-side events:
 *  - World load/unload (reset data storage)
 *  - HUD rendering (status overlay)
 */
public class ClientEventHandler {

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            SeedCracker.resetDataStorage();
            SeedCracker.LOGGER.info("[SeedCracker] New world loaded, data storage reset.");
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            SeedCracker.getDataStorage().shutdown();
            SeedCracker.LOGGER.info("[SeedCracker] World unloaded.");
        }
    }

    @SubscribeEvent
    public void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        SeedCrackerHud.render(event.getGuiGraphics());
    }
}
