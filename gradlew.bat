package com.seedcracker.config;

import com.seedcracker.core.SeedCracker;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SeedCrackerConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_HUD;
    public static final ModConfigSpec.BooleanValue HUD_ON_RIGHT;
    public static final ModConfigSpec.BooleanValue AUTO_START_CRACK;
    public static final ModConfigSpec.BooleanValue SHOW_STRUCTURE_HIGHLIGHTS;
    public static final ModConfigSpec.IntValue MAX_STRUCTURE_RESULTS;
    public static final ModConfigSpec.BooleanValue COLLECT_DUNGEONS;
    public static final ModConfigSpec.BooleanValue COLLECT_BIOMES;
    public static final ModConfigSpec.BooleanValue COLLECT_DECORATORS;
    public static final ModConfigSpec.BooleanValue VERBOSE_LOGGING;

    static {
        BUILDER.comment("SeedCracker client configuration");

        BUILDER.push("display");
        SHOW_HUD = BUILDER
            .comment("Show the status overlay in the corner of your screen.")
            .translation("seedcracker.config.show_hud")
            .define("show_hud", true);
        HUD_ON_RIGHT = BUILDER
            .comment("Move the HUD to the top-right corner instead of top-left.")
            .translation("seedcracker.config.hud_on_right")
            .define("hud_on_right", false);
        SHOW_STRUCTURE_HIGHLIGHTS = BUILDER
            .comment("Draw coloured boxes at found structure positions in the world.")
            .translation("seedcracker.config.show_structure_highlights")
            .define("show_structure_highlights", true);
        BUILDER.pop();

        BUILDER.push("cracking");
        AUTO_START_CRACK = BUILDER
            .comment("Automatically begin cracking when enough data is collected.")
            .translation("seedcracker.config.auto_start_crack")
            .define("auto_start_crack", true);
        COLLECT_DUNGEONS = BUILDER
            .comment("Scan dungeon floors when a spawner block is loaded nearby.")
            .translation("seedcracker.config.collect_dungeons")
            .define("collect_dungeons", true);
        COLLECT_BIOMES = BUILDER
            .comment("Sample biome data from incoming chunk packets.")
            .translation("seedcracker.config.collect_biomes")
            .define("collect_biomes", true);
        COLLECT_DECORATORS = BUILDER
            .comment("Scan nearby blocks for decorator positions (emerald ore, fungi, etc.).")
            .translation("seedcracker.config.collect_decorators")
            .define("collect_decorators", true);
        BUILDER.pop();

        BUILDER.push("structures");
        MAX_STRUCTURE_RESULTS = BUILDER
            .comment("How many of each structure type to show after /sf find.")
            .translation("seedcracker.config.max_structure_results")
            .defineInRange("max_structure_results", 5, 1, 20);
        BUILDER.pop();

        BUILDER.push("debug");
        VERBOSE_LOGGING = BUILDER
            .comment("Print detailed cracking progress to the game log.")
            .translation("seedcracker.config.verbose_logging")
            .define("verbose_logging", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static boolean showHud()                { return SHOW_HUD.get(); }
    public static boolean hudOnRight()             { return HUD_ON_RIGHT.get(); }
    public static boolean autoStartCrack()         { return AUTO_START_CRACK.get(); }
    public static boolean showStructureHighlights(){ return SHOW_STRUCTURE_HIGHLIGHTS.get(); }
    public static int maxStructureResults()        { return MAX_STRUCTURE_RESULTS.get(); }
    public static boolean collectDungeons()        { return COLLECT_DUNGEONS.get(); }
    public static boolean collectBiomes()          { return COLLECT_BIOMES.get(); }
    public static boolean collectDecorators()      { return COLLECT_DECORATORS.get(); }
    public static boolean verboseLogging()         { return VERBOSE_LOGGING.get(); }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        SeedCracker.LOGGER.info("[SeedCracker] Config registered (config/seedcracker-client.toml).");
    }
}
