package com.seedcracker.render;

import com.seedcracker.config.SeedCrackerConfig;
import com.seedcracker.core.SeedCracker;
import com.seedcracker.cracker.DataStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the on-screen status HUD showing:
 *  - Current crack state (idle / cracking / cracked / failed)
 *  - Number of data points collected
 *  - The cracked seed (if found)
 */
public final class SeedCrackerHud {

    private SeedCrackerHud() {}

    public static void render(GuiGraphics graphics) {
        if (!SeedCrackerConfig.showHud()) return;

        DataStorage storage = SeedCracker.getDataStorage();
        Minecraft mc = Minecraft.getInstance();

        List<String> lines = new ArrayList<>();

        switch (storage.getState()) {
            case IDLE -> {
                lines.add("§7[SeedCracker] Collecting data...");
                lines.add(String.format("§7  Dungeons: §f%d", storage.getDungeonCount()));
                lines.add(String.format("§7  Biomes:   §f%d", storage.getBiomeCount()));
                lines.add(String.format("§7  Decors:   §f%d", storage.getDecoratorCount()));
            }
            case CRACKING -> {
                lines.add("§e[SeedCracker] Cracking seed...");
                lines.add(String.format("§7  Dungeons: §f%d  Biomes: §f%d",
                        storage.getDungeonCount(), storage.getBiomeCount()));
            }
            case CRACKED -> {
                long seed = storage.getCrackedSeed().orElse(0L);
                lines.add("§a[SeedCracker] Seed found!");
                lines.add(String.format("§a  Seed: §f%d", seed));
                lines.add(String.format("§7  (hex: §f%s§7)", Long.toHexString(seed)));
            }
            case FAILED -> {
                lines.add("§c[SeedCracker] Crack failed.");
                lines.add("§7  Explore more to gather data.");
            }
        }

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int x = SeedCrackerConfig.hudOnRight() ? screenWidth - 120 : 4;
        int y = 4;

        for (String line : lines) {
            graphics.drawString(mc.font, Component.literal(line), x, y, 0xFFFFFFFF, true);
            y += 10;
        }
    }
}
