package org.example.gear;

public enum BaitType {
    //                  display           cost  qty  junkDelta  [COMMON, UNCOMMON, RARE,  EPIC,  LEGENDARY]
    NONE    ("No Bait",       0,  0,  0.00f, new float[]{0f,    0f,     0f,    0f,    0f    }),
    STANDARD("Std. Bait",     5,  5, -0.05f, new float[]{0f,    0f,     0f,    0f,    0f    }),
    PREMIUM ("Prem. Bait",   25,  5, -0.05f, new float[]{0f,    0.05f,  0f,    0f,    0f    }),
    EXOTIC  ("Exotic Bait",  70,  5, -0.05f, new float[]{0f,    0f,     0f,    0f,    0.01f });

    public final String  displayName;
    public final int     costEach;
    public final int     buyQty;
    public final float   junkDelta;    // added to rod junkProb; negative = less junk
    public final float[] rarityBoosts; // added to rod rarityProbs [COMMON..LEGENDARY]

    BaitType(String displayName, int costEach, int buyQty, float junkDelta, float[] rarityBoosts) {
        this.displayName  = displayName;
        this.costEach     = costEach;
        this.buyQty       = buyQty;
        this.junkDelta    = junkDelta;
        this.rarityBoosts = rarityBoosts;
    }

    public int totalCost() { return costEach * buyQty; }
}
