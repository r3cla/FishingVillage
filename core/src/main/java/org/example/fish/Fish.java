package org.example.fish;

import java.util.Random;

public record Fish(
    String id,
    String name,
    Rarity rarity,
    double minWeight,
    double maxWeight,
    int baseValue,
    String description
) {
    private static final double SHINY_VALUE_MULTIPLIER = 3.0;
    private static final double SHINY_CHANCE = 0.01;

    public double rollWeight(Random rng) {
        return minWeight + (maxWeight - minWeight) * rng.nextDouble();
    }

    public boolean rollShiny(Random rng) {
        return rng.nextDouble() < SHINY_CHANCE;
    }

    public int sellValue(double weight, boolean shiny) {
        double avgWeight = (minWeight + maxWeight) / 2.0;
        double weightFactor = avgWeight > 0 ? weight / avgWeight : 1.0;
        int value = (int) (baseValue * rarity.valueMultiplier * weightFactor);
        return shiny ? (int) (value * SHINY_VALUE_MULTIPLIER) : value;
    }
}
