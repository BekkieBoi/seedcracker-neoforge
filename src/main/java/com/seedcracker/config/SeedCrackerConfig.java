package com.seedcracker.config;

import com.seedcracker.core.SeedCracker;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;

@Config(name = SeedCracker.MOD_ID)
public class SeedCrackerConfig implements ConfigData {

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.Tooltip
    public boolean showHud = true;

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.Tooltip
    public boolean hudOnRight = false;

    @ConfigEntry.Category("display")
    @ConfigEntry.Gui.Tooltip
    public boolean showStructureHighlights = true;

    @ConfigEntry.Category("cracking")
    @ConfigEntry.Gui.Tooltip
    public boolean autoStartCrack = true;

    @ConfigEntry.Category("cracking")
    @ConfigEntry.Gui.Tooltip
    public boolean collectDungeons = true;

    @ConfigEntry.Category("cracking")
    @ConfigEntry.Gui.Tooltip
    public boolean collectBiomes = true;

    @ConfigEntry.Category("cracking")
    @ConfigEntry.Gui.Tooltip
    public boolean collectDecorators = true;

    @ConfigEntry.Category("structures")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int maxStructureResults = 5;

    @ConfigEntry.Category("debug")
    @ConfigEntry.Gui.Tooltip
    public boolean verboseLogging = false;

    // -------------------------------------------------------------------------
    // Static accessors
    // -------------------------------------------------------------------------

    private static SeedCrackerConfig instance;

    private static SeedCrackerConfig get() {
        if (instance == null) instance = new SeedCrackerConfig();
        return instance;
    }

    public static boolean showHud()                { return get().showHud; }
    public static boolean hudOnRight()             { return get().hudOnRight; }
    public static boolean showStructureHighlights(){ return get().showStructureHighlights; }
    public static boolean autoStartCrack()         { return get().autoStartCrack; }
    public static boolean collectDungeons()        { return get().collectDungeons; }
    public static boolean collectBiomes()          { return get().collectBiomes; }
    public static boolean collectDecorators()      { return get().collectDecorators; }
    public static int maxStructureResults()        { return get().maxStructureResults; }
    public static boolean verboseLogging()         { return get().verboseLogging; }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public static void register(ModContainer container) {
        AutoConfig.register(SeedCrackerConfig.class, GsonConfigSerializer::new);
        instance = AutoConfig.getConfigHolder(SeedCrackerConfig.class).getConfig();
        SeedCracker.LOGGER.info("[SeedCracker] Config loaded.");
    }

    public static Screen buildScreen(Screen parent) {
        return AutoConfig.getConfigScreen(SeedCrackerConfig.class, parent).get();
    }
}
