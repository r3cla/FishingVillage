package org.example.gear;

public enum RodTier {
    //                 display           cost  [COMMON,  UNCOMMON, RARE,   EPIC,   LEGENDARY]  junk
    BASIC   ("Basic Rod",      0, new float[]{0.5999f, 0.12f,  0.06f,  0.02f,  0.0001f}, 0.30f),
    ADVANCED("Advanced Rod", 700, new float[]{0.4999f, 0.18f,  0.09f,  0.03f,  0.0001f}, 0.30f),
    MASTER  ("Master Rod",  3500, new float[]{0.4050f, 0.18f,  0.15f,  0.06f,  0.0050f}, 0.30f);

    public final String  displayName;
    public final int     cost;
    public final float[] rarityProbs; // [COMMON..LEGENDARY]; sum + junkProb = 1.0
    public final float   junkProb;

    RodTier(String displayName, int cost, float[] rarityProbs, float junkProb) {
        this.displayName = displayName;
        this.cost        = cost;
        this.rarityProbs = rarityProbs;
        this.junkProb    = junkProb;
    }
}
