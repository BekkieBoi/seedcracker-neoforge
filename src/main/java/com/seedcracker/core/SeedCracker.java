package com.seedcracker.core;

import com.mojang.logging.LogUtils;
import com.seedcracker.command.SeedCrackerCommands;
import com.seedcracker.config.SeedCrackerConfig;
import com.seedcracker.cracker.DataStorage;
import com.seedcracker.event.ClientEventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = SeedCracker.MOD_ID, dist = net.neoforged.api.distmarker.Dist.CLIENT)
public class SeedCracker {

    public static final String MOD_ID = "seedcracker";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static DataStorage dataStorage;

    public SeedCracker(IEventBus modBus, ModContainer container) {
        SeedCrackerConfig.register(container);

        NeoForge.EVENT_BUS.register(new ClientEventHandler());
        NeoForge.EVENT_BUS.register(new SeedCrackerCommands());

        // This one line makes the "Config" button appear in the Mods list screen
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("[SeedCracker] Client setup complete.");
        resetDataStorage();
    }

    public static DataStorage getDataStorage() {
        if (dataStorage == null) dataStorage = new DataStorage();
        return dataStorage;
    }

    public static void resetDataStorage() {
        dataStorage = new DataStorage();
        LOGGER.info("[SeedCracker] Data storage reset.");
    }
}
