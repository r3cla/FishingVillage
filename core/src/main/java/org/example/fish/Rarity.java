package org.example.fish;

public enum Rarity {
    COMMON    (0.55, 1.0,  "Common"),
    UNCOMMON  (0.25, 1.8,  "Uncommon"),
    RARE      (0.12, 4.0,  "Rare"),
    EPIC      (0.06,  3.0, "Epic"),
    LEGENDARY (0.02,  2.5, "Legendary");

    public final double catchWeight;   // relative weight used in weighted RNG
    public final double valueMultiplier;
    public final String displayName;

    Rarity(double catchWeight, double valueMultiplier, String displayName) {
        this.catchWeight = catchWeight;
        this.valueMultiplier = valueMultiplier;
        this.displayName = displayName;
    }
}
