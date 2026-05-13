package org.example.save;

public class SaveData {

    public int    coins;
    public int    minuteOfDay  = 6 * 60; // default 6:00 AM
    public int    totalCatches;
    public int    totalMisses;
    public String rod;          // RodTier.name()
    public String equippedBait; // BaitType.name()
    public int    standardBait;
    public int    premiumBait;
    public int    exoticBait;
    public int         bagUpgradeLevel;
    public FishEntry[] bag          = new FishEntry[0];
    public RecordEntry[] fishRecords = new RecordEntry[0];
    public String[]      achievements = new String[0];

    public static class RecordEntry {
        public String  fishId;
        public int     totalCaught;
        public double  personalBest;
        public boolean shinyUnlocked;
        public RecordEntry() {}
        public RecordEntry(String fishId, int totalCaught, double personalBest, boolean shinyUnlocked) {
            this.fishId        = fishId;
            this.totalCaught   = totalCaught;
            this.personalBest  = personalBest;
            this.shinyUnlocked = shinyUnlocked;
        }
    }

    public static class FishEntry {
        public String  fishId;
        public String  junkId; // JunkType.name() if junk, null if fish
        public double  weight;
        public boolean shiny;
        public int     value;
        public FishEntry() {}
        public FishEntry(String fishId, String junkId, double weight, boolean shiny, int value) {
            this.fishId = fishId;
            this.junkId = junkId;
            this.weight = weight;
            this.shiny  = shiny;
            this.value  = value;
        }
    }
}
