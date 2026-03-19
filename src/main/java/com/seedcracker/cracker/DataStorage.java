package com.seedcracker.cracker;

import com.seedcracker.core.SeedCracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central storage for all seed-relevant data collected during a session.
 * Accumulates dungeon floors, biome samples, and decorator observations,
 * then triggers cracking attempts when enough data is present.
 */
public class DataStorage {

    // Minimum data points required before attempting a crack
    private static final int MIN_DUNGEON_SAMPLES = 3;
    private static final int MIN_BIOME_SAMPLES   = 5;

    private final List<DungeonData>    dungeonSamples = new ArrayList<>();
    private final List<BiomeSample>    biomeSamples   = new ArrayList<>();
    private final List<DecoratorData>  decoratorSamples = new ArrayList<>();

    private Optional<Long> crackedSeed = Optional.empty();
    private CrackState state = CrackState.IDLE;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SeedCracker-Worker");
        t.setDaemon(true);
        return t;
    });

    // -------------------------------------------------------------------------
    // Data ingestion
    // -------------------------------------------------------------------------

    public synchronized void addDungeonData(DungeonData data) {
        if (crackedSeed.isPresent()) return;
        dungeonSamples.add(data);
        SeedCracker.LOGGER.info("[SeedCracker] Dungeon data added ({} total)", dungeonSamples.size());
        tryScheduleCrack();
    }

    public synchronized void addBiomeSample(BiomeSample sample) {
        if (crackedSeed.isPresent()) return;
        biomeSamples.add(sample);
        SeedCracker.LOGGER.info("[SeedCracker] Biome sample added ({} total)", biomeSamples.size());
        tryScheduleCrack();
    }

    public synchronized void addDecoratorData(DecoratorData data) {
        if (crackedSeed.isPresent()) return;
        decoratorSamples.add(data);
        SeedCracker.LOGGER.info("[SeedCracker] Decorator data added ({} total)", decoratorSamples.size());
        tryScheduleCrack();
    }

    // -------------------------------------------------------------------------
    // Crack scheduling
    // -------------------------------------------------------------------------

    private void tryScheduleCrack() {
        if (state == CrackState.CRACKING || crackedSeed.isPresent()) return;

        boolean hasDungeons  = dungeonSamples.size() >= MIN_DUNGEON_SAMPLES;
        boolean hasBiomes    = biomeSamples.size() >= MIN_BIOME_SAMPLES;

        if (hasDungeons || hasBiomes) {
            scheduleCrack();
        }
    }

    private void scheduleCrack() {
        state = CrackState.CRACKING;
        SeedCracker.LOGGER.info("[SeedCracker] Starting crack attempt...");
        executor.submit(this::attemptCrack);
    }

    private void attemptCrack() {
        try {
            SeedCracker.LOGGER.info("[SeedCracker] Cracking with {} dungeon(s), {} biome(s), {} decorator(s)",
                    dungeonSamples.size(), biomeSamples.size(), decoratorSamples.size());

            Optional<Long> result = SeedCrackerLogic.crack(
                    List.copyOf(dungeonSamples),
                    List.copyOf(biomeSamples),
                    List.copyOf(decoratorSamples)
            );

            synchronized (this) {
                if (result.isPresent()) {
                    crackedSeed = result;
                    state = CrackState.CRACKED;
                    SeedCracker.LOGGER.info("[SeedCracker] Seed cracked: {}", crackedSeed.get());
                } else {
                    state = CrackState.FAILED;
                    SeedCracker.LOGGER.info("[SeedCracker] Crack failed - need more data.");
                }
            }
        } catch (Exception e) {
            synchronized (this) { state = CrackState.FAILED; }
            SeedCracker.LOGGER.error("[SeedCracker] Error during crack attempt", e);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public synchronized Optional<Long> getCrackedSeed() { return crackedSeed; }
    public synchronized CrackState getState()           { return state; }
    public synchronized int getDungeonCount()           { return dungeonSamples.size(); }
    public synchronized int getBiomeCount()             { return biomeSamples.size(); }
    public synchronized int getDecoratorCount()         { return decoratorSamples.size(); }

    public synchronized void reset() {
        dungeonSamples.clear();
        biomeSamples.clear();
        decoratorSamples.clear();
        crackedSeed = Optional.empty();
        state = CrackState.IDLE;
        SeedCracker.LOGGER.info("[SeedCracker] DataStorage reset.");
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public enum CrackState {
        IDLE, CRACKING, CRACKED, FAILED
    }
}
