package org.example.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import org.example.fish.Fish;
import org.example.fish.FishRegistry;
import org.example.game.Achievement;
import org.example.game.CaughtFish;
import org.example.game.FishRecord;
import org.example.game.GameState;
import org.example.game.JunkType;
import org.example.gear.BaitType;
import org.example.gear.RodTier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SaveManager {

    private static final String SAVE_FILE = "save.json";

    public static void save(GameState state) {
        SaveData data = new SaveData();
        data.coins        = state.getCoins();
        data.minuteOfDay  = state.getClockMinute();
        data.totalCatches = state.getTotalCatches();
        data.totalMisses  = state.getTotalMisses();
        data.rod          = state.getRod().name();
        data.equippedBait = state.getEquippedBait().name();
        data.standardBait    = state.getBaitCount(BaitType.STANDARD);
        data.premiumBait     = state.getBaitCount(BaitType.PREMIUM);
        data.exoticBait      = state.getBaitCount(BaitType.EXOTIC);
        data.bagUpgradeLevel = state.getBagUpgradeLevel();

        List<CaughtFish> slots = state.getFishBag().getSlots();
        data.bag = new SaveData.FishEntry[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            CaughtFish cf = slots.get(i);
            if (cf.isJunk()) {
                data.bag[i] = new SaveData.FishEntry(null, cf.junk().name(), 0, false, cf.value());
            } else {
                data.bag[i] = new SaveData.FishEntry(cf.fish().id(), null, cf.weight(), cf.shiny(), cf.value());
            }
        }

        Map<String, FishRecord> records = state.getFishRecords();
        data.fishRecords = new SaveData.RecordEntry[records.size()];
        int ri = 0;
        for (Map.Entry<String, FishRecord> e : records.entrySet()) {
            FishRecord r = e.getValue();
            data.fishRecords[ri++] = new SaveData.RecordEntry(e.getKey(), r.totalCaught, r.personalBest, r.shinyUnlocked);
        }
        Set<Achievement> achs = state.getAchievements();
        data.achievements = achs.stream().map(Achievement::name).toArray(String[]::new);

        try {
            Json json = new Json();
            String text = json.prettyPrint(data);
            Gdx.files.local(SAVE_FILE).writeString(text, false);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to save: " + e.getMessage());
        }
    }

    public static void load(GameState state) {
        try {
            if (!Gdx.files.local(SAVE_FILE).exists()) return;

            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            SaveData data = json.fromJson(SaveData.class, Gdx.files.local(SAVE_FILE).readString());
            if (data == null) return;

            RodTier rod = parseSafe(RodTier.class,  data.rod,          RodTier.BASIC);
            BaitType equipped = parseSafe(BaitType.class, data.equippedBait, BaitType.NONE);

            java.util.Map<BaitType, Integer> baitStock = new java.util.EnumMap<>(BaitType.class);
            if (data.standardBait > 0) baitStock.put(BaitType.STANDARD, data.standardBait);
            if (data.premiumBait  > 0) baitStock.put(BaitType.PREMIUM,  data.premiumBait);
            if (data.exoticBait   > 0) baitStock.put(BaitType.EXOTIC,   data.exoticBait);

            List<CaughtFish> bag = new ArrayList<>();
            if (data.bag != null) {
                for (SaveData.FishEntry entry : data.bag) {
                    if (entry.junkId != null) {
                        JunkType junk = parseSafe(JunkType.class, entry.junkId, null);
                        if (junk != null) bag.add(new CaughtFish(null, junk, 0, false, junk.value));
                    } else {
                        Optional<Fish> fish = FishRegistry.getById(entry.fishId);
                        fish.ifPresent(f -> bag.add(new CaughtFish(f, entry.weight, entry.shiny, entry.value)));
                    }
                }
            }

            state.restoreState(data.coins, data.minuteOfDay, data.totalCatches, data.totalMisses,
                               rod, equipped, baitStock, bag, data.bagUpgradeLevel);

            Map<String, FishRecord> fishRecords = new HashMap<>();
            if (data.fishRecords != null) {
                for (SaveData.RecordEntry entry : data.fishRecords) {
                    FishRecord r = new FishRecord();
                    r.discovered    = true;
                    r.totalCaught   = entry.totalCaught;
                    r.personalBest  = entry.personalBest;
                    r.shinyUnlocked = entry.shinyUnlocked;
                    fishRecords.put(entry.fishId, r);
                }
            }
            Set<Achievement> achievements = EnumSet.noneOf(Achievement.class);
            if (data.achievements != null) {
                for (String name : data.achievements) {
                    Achievement a = parseSafe(Achievement.class, name, null);
                    if (a != null) achievements.add(a);
                }
            }
            state.restoreJournalData(fishRecords, achievements);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to load: " + e.getMessage());
        }
    }

    private static <T extends Enum<T>> T parseSafe(Class<T> cls, String name, T fallback) {
        if (name == null) return fallback;
        try { return Enum.valueOf(cls, name); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}
