package com.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.seedcracker.core.SeedCracker;
import com.seedcracker.cracker.DataStorage;
import com.seedcracker.finder.FoundStructure;
import com.seedcracker.finder.StructureFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Registers the /sf command tree on the client side.
 *
 * Commands:
 *   /sf status                        - Show current cracking status and data counts
 *   /sf seed                          - Display the cracked seed (if found)
 *   /sf reset                         - Clear all collected data and start over
 *   /sf find [maxResults]             - Find all structure types near the player
 *   /sf locate <structure>            - Find the nearest of a specific structure type
 *   /sf locate <structure> [radius]   - Same but with a custom search radius in chunks
 *   /sf list                          - List all found structures with coordinates
 *   /sf list clear                    - Clear the structure list
 *   /sf help                          - Show all available commands
 */
public class SeedCrackerCommands {

    // All structure names the mod knows about, for tab-completion
    private static final String[] KNOWN_STRUCTURES = {
        "village", "stronghold", "mineshaft", "desert_pyramid", "jungle_temple",
        "ocean_monument", "woodland_mansion", "fortress", "bastion_remnant",
        "end_city", "ruined_portal", "shipwreck", "pillager_outpost", "ancient_city"
    };

    private static final SuggestionProvider<CommandSourceStack> STRUCTURE_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(KNOWN_STRUCTURES, builder);

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("sf")
                .then(Commands.literal("status")
                    .executes(ctx -> cmdStatus(ctx.getSource())))

                .then(Commands.literal("seed")
                    .executes(ctx -> cmdSeed(ctx.getSource())))

                .then(Commands.literal("reset")
                    .executes(ctx -> cmdReset(ctx.getSource())))

                .then(Commands.literal("find")
                    .executes(ctx -> cmdFind(ctx.getSource(), 5))
                    .then(Commands.argument("maxResults", IntegerArgumentType.integer(1, 20))
                        .executes(ctx -> cmdFind(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "maxResults")))))

                .then(Commands.literal("locate")
                    .then(Commands.argument("structure", StringArgumentType.word())
                        .suggests(STRUCTURE_SUGGESTIONS)
                        .executes(ctx -> cmdLocate(ctx.getSource(),
                                StringArgumentType.getString(ctx, "structure"), 128))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(16, 512))
                            .executes(ctx -> cmdLocate(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "structure"),
                                    IntegerArgumentType.getInteger(ctx, "radius"))))))

                .then(Commands.literal("list")
                    .executes(ctx -> cmdList(ctx.getSource()))
                    .then(Commands.literal("clear")
                        .executes(ctx -> cmdListClear(ctx.getSource()))))

                .then(Commands.literal("help")
                    .executes(ctx -> cmdHelp(ctx.getSource())))
        );
    }

    // -------------------------------------------------------------------------
    // Command implementations
    // -------------------------------------------------------------------------

    private static int cmdStatus(CommandSourceStack source) {
        DataStorage storage = SeedCracker.getDataStorage();
        msg(source, "§6[SF] Status:");
        msg(source, String.format("  State:      §f%s", storage.getState()));
        msg(source, String.format("  Dungeons:   §f%d", storage.getDungeonCount()));
        msg(source, String.format("  Biomes:     §f%d", storage.getBiomeCount()));
        msg(source, String.format("  Decorators: §f%d", storage.getDecoratorCount()));
        storage.getCrackedSeed().ifPresentOrElse(
            seed -> msg(source, String.format("  §aSeed: §f%d §7(0x%s)", seed, Long.toHexString(seed))),
            () -> msg(source, "  §7Seed not yet cracked.")
        );
        return 1;
    }

    private static int cmdSeed(CommandSourceStack source) {
        DataStorage storage = SeedCracker.getDataStorage();
        if (storage.getCrackedSeed().isPresent()) {
            long seed = storage.getCrackedSeed().get();
            msg(source, "§a[SF] Seed cracked:");
            msg(source, String.format("  §fDecimal: §e%d", seed));
            msg(source, String.format("  §fHex:     §e0x%s", Long.toHexString(seed)));
        } else {
            msg(source, String.format("§c[SF] No seed yet. State: §f%s", storage.getState()));
            msg(source, "§7  Find dungeons — the mod scans floors automatically.");
        }
        return 1;
    }

    private static int cmdReset(CommandSourceStack source) {
        SeedCracker.resetDataStorage();
        StructureFinder.clearResults();
        msg(source, "§e[SF] Reset. All collected data cleared.");
        return 1;
    }

    private static int cmdFind(CommandSourceStack source, int maxResults) {
        DataStorage storage = SeedCracker.getDataStorage();
        if (storage.getCrackedSeed().isEmpty()) {
            msg(source, "§c[SF] Seed not cracked yet — crack the seed before searching.");
            return 0;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;

        long seed = storage.getCrackedSeed().get();
        BlockPos playerPos = mc.player.blockPosition();

        msg(source, String.format("§e[SF] Searching all structure types (max %d each)...", maxResults));
        StructureFinder.findStructures(seed, playerPos, maxResults);

        List<FoundStructure> results = StructureFinder.getFoundStructures();
        if (results.isEmpty()) {
            msg(source, "§c  Nothing found in search radius.");
        } else {
            msg(source, String.format("§a  Found %d total. Use §f/sf list§a to see them, or §f/sf locate <type>§a for one type.", results.size()));
        }
        return 1;
    }

    /**
     * Locates the nearest instance of a specific structure type.
     * Searches a configurable radius (in blocks) and returns the single closest match.
     */
    private static int cmdLocate(CommandSourceStack source, String structureName, int radiusBlocks) {
        DataStorage storage = SeedCracker.getDataStorage();
        if (storage.getCrackedSeed().isEmpty()) {
            msg(source, "§c[SF] Seed not cracked yet.");
            return 0;
        }

        // Validate the structure name
        boolean known = Arrays.asList(KNOWN_STRUCTURES).contains(structureName.toLowerCase());
        if (!known) {
            msg(source, String.format("§c[SF] Unknown structure: §f%s", structureName));
            msg(source, "§7  Use §f/sf locate §7with tab-complete to see valid names.");
            return 0;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;

        long seed = storage.getCrackedSeed().get();
        BlockPos playerPos = mc.player.blockPosition();

        msg(source, String.format("§e[SF] Locating nearest §f%s§e within %d blocks...", structureName, radiusBlocks));

        // Run a targeted search for just this one structure type
        StructureFinder.findSpecificStructure(seed, playerPos, structureName, radiusBlocks);

        List<FoundStructure> results = StructureFinder.getFoundStructures().stream()
            .filter(s -> s.type().location().getPath().equals(structureName))
            .sorted(Comparator.comparingDouble(FoundStructure::distance))
            .toList();

        if (results.isEmpty()) {
            msg(source, String.format("§c  No %s found within %d blocks.", structureName, radiusBlocks));
            msg(source, "§7  Try a larger radius: §f/sf locate " + structureName + " 256");
        } else {
            FoundStructure nearest = results.get(0);
            msg(source, String.format("§a  Nearest %s:", nearest.displayName()));
            msg(source, String.format("  §fPosition: §e%s", formatPos(nearest.pos())));
            msg(source, String.format("  §fDistance: §e%.0f blocks", nearest.distance()));

            if (results.size() > 1) {
                msg(source, String.format("§7  (%d more found — use §f/sf list§7 to see all)", results.size() - 1));
            }
        }
        return 1;
    }

    private static int cmdList(CommandSourceStack source) {
        List<FoundStructure> results = StructureFinder.getFoundStructures();
        if (results.isEmpty()) {
            msg(source, "§7[SF] No results. Run §f/sf find§7 or §f/sf locate <type>§7 first.");
            return 1;
        }
        msg(source, String.format("§6[SF] %d structure(s) found:", results.size()));
        for (FoundStructure s : results) {
            msg(source, String.format("  §f%-20s §7@ §e%s §7(%.0f blocks)",
                    s.displayName(), formatPos(s.pos()), s.distance()));
        }
        return 1;
    }

    private static int cmdListClear(CommandSourceStack source) {
        StructureFinder.clearResults();
        msg(source, "§e[SF] Structure list cleared.");
        return 1;
    }

    private static int cmdHelp(CommandSourceStack source) {
        msg(source, "§6[SF] Commands:");
        msg(source, "  §f/sf status              §7- Cracking state and data counts");
        msg(source, "  §f/sf seed                §7- Show the cracked seed");
        msg(source, "  §f/sf reset               §7- Clear all data and start over");
        msg(source, "  §f/sf find [max]          §7- Find all structure types nearby");
        msg(source, "  §f/sf locate <type>       §7- Find nearest of a specific structure");
        msg(source, "  §f/sf locate <type> <r>   §7- Same, with custom radius in blocks");
        msg(source, "  §f/sf list                §7- List all found structures");
        msg(source, "  §f/sf list clear          §7- Clear the structure list");
        msg(source, "§7  Structure types: §f" + String.join("§7, §f", KNOWN_STRUCTURES));
        return 1;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void msg(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static String formatPos(BlockPos pos) {
        return String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
    }
}
