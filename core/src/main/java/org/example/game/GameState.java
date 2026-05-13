package org.example.game;

import org.example.gear.BaitType;
import org.example.gear.RodTier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GameState {

    private int coins;
    private int totalCatches;
    private int totalMisses;
    private final FishBag fishBag = new FishBag();

    private int      clockMinute     = 6 * 60; // 6:00 AM default
    private RodTier  currentRod      = RodTier.BASIC;
    private BaitType equippedBait   = BaitType.NONE;
    private int      bagUpgradeLevel = 0;
    private final Map<BaitType, Integer> baitStock = new EnumMap<>(BaitType.class);

    private final Map<String, FishRecord> fishRecords          = new HashMap<>();
    private final Set<Achievement>        unlockedAchievements = EnumSet.noneOf(Achievement.class);

    private static final int[] BAG_UPGRADE_COSTS = {500, 1000, 2000, 4000};

    public void addCatch(int value) {
        coins += value;
        totalCatches++;
    }

    // coins credited on sell, not on catch
    public void recordCatch() { totalCatches++; }

    public void addCoins(int amount) { coins += amount; }

    public void addMiss() { totalMisses++; }

    public boolean buyRod(RodTier rod) {
        if (coins < rod.cost) return false;
        coins -= rod.cost;
        currentRod = rod;
        return true;
    }

    public boolean buyBagUpgrade() {
        if (bagUpgradeLevel >= BAG_UPGRADE_COSTS.length) return false;
        int cost = BAG_UPGRADE_COSTS[bagUpgradeLevel];
        if (coins < cost) return false;
        coins -= cost;
        bagUpgradeLevel++;
        fishBag.expandSlots(5);
        return true;
    }

    public boolean buyBait(BaitType bait) {
        int cost = bait.totalCost();
        if (coins < cost) return false;
        coins -= cost;
        baitStock.merge(bait, bait.buyQty, Integer::sum);
        if (equippedBait == BaitType.NONE) equippedBait = bait;
        return true;
    }

    public void setEquippedBait(BaitType bait) {
        equippedBait = bait;
    }

    // auto-clears equipped bait when stock hits 0
    public void consumeBait() {
        if (equippedBait == BaitType.NONE) return;
        int count = baitStock.getOrDefault(equippedBait, 0);
        if (count <= 1) {
            baitStock.remove(equippedBait);
            equippedBait = BaitType.NONE;
        } else {
            baitStock.put(equippedBait, count - 1);
        }
    }

    public void recordFishCatch(String fishId, double weight, boolean shiny) {
        fishRecords.computeIfAbsent(fishId, k -> new FishRecord()).record(weight, shiny);
    }

    public void unlockAchievement(Achievement a) { unlockedAchievements.add(a); }
    public boolean hasAchievement(Achievement a) { return unlockedAchievements.contains(a); }

    public FishRecord getFishRecord(String id) {
        return fishRecords.getOrDefault(id, new FishRecord());
    }

    public Map<String, FishRecord> getFishRecords()      { return Collections.unmodifiableMap(fishRecords); }
    public Set<Achievement>        getAchievements()     { return Collections.unmodifiableSet(unlockedAchievements); }

    public void restoreJournalData(Map<String, FishRecord> records, Set<Achievement> unlocked) {
        fishRecords.clear();
        fishRecords.putAll(records);
        unlockedAchievements.clear();
        unlockedAchievements.addAll(unlocked);
    }

    public int      getClockMinute()               { return clockMinute; }
    public void     setClockMinute(int m)          { clockMinute = m; }
    public RodTier  getRod()                       { return currentRod; }
    public BaitType getEquippedBait()              { return equippedBait; }
    public int      getBaitCount(BaitType bait)    { return baitStock.getOrDefault(bait, 0); }
    public int      getBagUpgradeLevel()           { return bagUpgradeLevel; }

    public FishBag getFishBag()      { return fishBag; }
    public int     getCoins()        { return coins; }
    public int     getTotalCatches() { return totalCatches; }
    public int     getTotalMisses()  { return totalMisses; }

    public void restoreState(int coins, int minuteOfDay, int totalCatches, int totalMisses,
                             RodTier rod, BaitType equipped,
                             java.util.Map<BaitType, Integer> stock,
                             java.util.List<CaughtFish> bag,
                             int bagUpgradeLevel) {
        this.coins            = coins;
        this.clockMinute      = minuteOfDay;
        this.totalCatches     = totalCatches;
        this.totalMisses      = totalMisses;
        this.currentRod       = rod;
        this.equippedBait     = equipped;
        this.bagUpgradeLevel  = bagUpgradeLevel;
        this.fishBag.setMaxSlots(10 + bagUpgradeLevel * 5);
        this.baitStock.clear();
        this.baitStock.putAll(stock);
        this.fishBag.clear();
        bag.forEach(this.fishBag::add);
    }
}
