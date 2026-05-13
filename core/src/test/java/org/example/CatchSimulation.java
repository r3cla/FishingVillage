package org.example;

import org.example.fish.CatchResult;
import org.example.fish.Rarity;
import org.example.fish.FishRegistry;
import org.example.game.JunkType;
import org.example.gear.BaitType;
import org.example.gear.RodTier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Headless fishing simulation — run with:
 *   ./gradlew :core:test --tests "org.example.CatchSimulation"
 *
 * Output is printed to stdout. Results show catch distribution
 * across all rod + bait combos to validate RNG tuning.
 */
public class CatchSimulation {

    private static final int CASTS      = 50_000;
    private static final double SHINY_CHANCE = 0.01;

    @Test
    void simulate() {
        System.out.println("\n=== Fishing Catch Simulation (" + CASTS + " casts per combo) ===\n");

        for (RodTier rod : RodTier.values()) {
            System.out.println("-- " + rod.displayName + " ---------------------------------");
            System.out.printf("  %-16s %8s %8s %8s %8s %8s %8s %8s%n",
                "Bait", "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGEND", "JUNK", "SHINY");
            System.out.println("  " + "-".repeat(76));

            for (BaitType bait : BaitType.values()) {
                // Pass -Dsim.random=true to use a non-deterministic seed
                boolean useRandom = Boolean.getBoolean("sim.random");
                Random rng = useRandom ? new Random() : new Random(42);

                Map<Rarity, Integer> rarityCounts = new EnumMap<>(Rarity.class);
                for (Rarity r : Rarity.values()) rarityCounts.put(r, 0);
                int junkCount  = 0;
                int shinyCount = 0;

                for (int i = 0; i < CASTS; i++) {
                    CatchResult result = FishRegistry.rollCatch(rng, null, null, rod, bait);
                    if (result instanceof CatchResult.JunkCatch) {
                        junkCount++;
                    } else if (result instanceof CatchResult.FishCatch fc) {
                        rarityCounts.merge(fc.fish().rarity(), 1, Integer::sum);
                        if (rng.nextDouble() < SHINY_CHANCE) shinyCount++;
                    }
                }

                System.out.printf("  %-16s %7.1f%% %7.1f%% %7.1f%% %7.1f%% %7.1f%% %7.1f%% %7.1f%%%n",
                    bait.displayName,
                    pct(rarityCounts.get(Rarity.COMMON)),
                    pct(rarityCounts.get(Rarity.UNCOMMON)),
                    pct(rarityCounts.get(Rarity.RARE)),
                    pct(rarityCounts.get(Rarity.EPIC)),
                    pct(rarityCounts.get(Rarity.LEGENDARY)),
                    pct(junkCount),
                    pct(shinyCount)
                );
            }
            System.out.println();
        }
    }

    private double pct(int count) {
        return count * 100.0 / CASTS;
    }
}
