package org.example.fish;

import org.example.game.JunkType;
import org.example.gear.BaitType;
import org.example.gear.RodTier;
import org.example.time.TimeOfDay;
import org.example.weather.Weather;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class FishRegistry {

    private static final List<Fish> ALL_FISH = List.of(
        // Common
        new Fish("sardine",   "Sardine",   Rarity.COMMON,    0.05,  0.12,   3,  "Shoals in vast silver clouds, descending to 25–100 m during the day and rising toward the surface at night. One of the most abundant fish in European coastal waters."),
        new Fish("herring",   "Herring",   Rarity.COMMON,    0.1,   0.9,    4,  "Follows migration routes learned from older fish to reach ancestral spawning grounds. Filters copepods near the surface using gill rakers."),
        new Fish("carp",      "Carp",      Rarity.COMMON,    0.5,   12.0,   5,  "Roots through lake beds at dusk for invertebrates and plant matter. Thrives in slow, warm, weedy water and adapts readily to almost any conditions."),
        new Fish("catfish",   "Catfish",   Rarity.COMMON,    0.8,   25.0,   7,  "Europe's largest freshwater predator, confirmed at 130 kg in the wild. Hunts nocturnally from the riverbed, preying on fish and other aquatic vertebrates."),
        new Fish("perch",     "Perch",     Rarity.COMMON,    0.2,   3.0,    6,  "Tiger-striped with vivid red fins. Juveniles school near the surface; big adults lurk deeper and will eat almost anything that moves."),

        // Uncommon
        new Fish("bass",      "Bass",      Rarity.UNCOMMON,  0.5,   8.0,   13,  "Builds a nest on a muddy bottom in shallow water and guards it ferociously. An ambush predator — can go from motionless to striking speed in an instant."),
        new Fish("trout",     "Trout",     Rarity.UNCOMMON,  0.3,   12.0,  16,  "Rises readily to surface insects in cold, clear streams. Sea-run 'steelhead' return from the ocean after years of open-water growth, dwarfing their river-bound relatives."),
        new Fish("pike",      "Pike",      Rarity.UNCOMMON,  1.0,   18.0,  17,  "Patient apex predator of weedy shallows. Females grow far larger than males. Cannibalism is common in isolated lake populations."),
        new Fish("eel",       "Eel",       Rarity.UNCOMMON,  0.4,   4.0,   12,  "Spawns in the Sargasso Sea and drifts 5,000 km to European rivers as a larva. Migrates back just once to breed, navigating by the Earth's magnetic field."),
        new Fish("bream",     "Bream",     Rarity.UNCOMMON,  0.3,   4.0,   11,  "Deep-bodied shoaling fish of slow, weedy water. Switches to filter-feeding when plankton is abundant. Makes long upstream migrations to spawn."),

        // Rare
        new Fish("salmon",    "Salmon",    Rarity.RARE,      2.0,   35.0,  27,  "Returns unerringly to its natal river after years at sea, developing a hooked jaw and vivid colour for the spawning run. Fights hard against the current."),
        new Fish("tuna",      "Tuna",      Rarity.RARE,      5.0,  300.0,  42,  "Warm-blooded and built like a torpedo. Crosses entire ocean basins to spawn, chasing anchovy and saury schools at extraordinary speed."),
        new Fish("swordfish", "Swordfish", Rarity.RARE,     10.0,  300.0,  64,  "Wields its flat rostrum as a slashing weapon. Dives to nearly 3 km and migrates seasonally between tropical and temperate oceans."),
        new Fish("sturgeon",  "Sturgeon",  Rarity.RARE,      5.0,  400.0,  49,  "The largest freshwater fish ever documented, confirmed above 3 tonnes historically. Critically endangered, known to live past 100 years."),

        // Epic
        new Fish("shark",     "Shark",     Rarity.EPIC,     20.0,  900.0, 255, "Ambushes prey from below using the bite-and-wait strategy. Pups are born live at nearly 1.5 metres. The apex predator of every ocean."),
        new Fish("manta_ray", "Manta Ray", Rarity.EPIC,     30.0, 1200.0, 240, "Filters tonnes of plankton near productive upwelling coasts. Disc width can reach 9 metres. Leaps fully clear of the water during courtship."),
        new Fish("oarfish",   "Oarfish",   Rarity.EPIC,      8.0,  200.0, 220, "The world's longest bony fish — the source of every sea serpent legend. Hunts motionless and head-up. Severs its own tail when threatened and does not regrow it."),

        // Legendary
        new Fish("dragon_koi",         "Dragon Koi",         Rarity.LEGENDARY,  2.0,  15.0, 1000, "An ornamental koi grown to mythological size, its scales shimmering like embers in deep water. Ancient fishermen called it a water dragon and refused to eat it."),
        new Fish("ancient_coelacanth", "Ancient Coelacanth", Rarity.LEGENDARY, 15.0,  90.0, 1500, "Unchanged for 400 million years. Shelters in deep volcanic caves by day and drifts on lobed, limblike fins through the night. A living page of prehistory."),
        new Fish("leviathan_eel",      "Leviathan Eel",      Rarity.LEGENDARY, 50.0, 300.0, 2000, "A creature of impossible scale, glimpsed only in the deepest water. No record exists of anyone landing one and returning with dry hands.")
    );

    // ~30% junk chance relative to sum of all rarity catchWeights
    private static final double JUNK_WEIGHT = 0.43;

    private static final Set<String> DAY_FISH = Set.of(
        "sardine", "herring", "perch", "bream", "tuna", "manta_ray"
    );

    private static final Set<String> NIGHT_FISH = Set.of(
        "catfish", "pike", "eel", "sturgeon", "oarfish", "ancient_coelacanth"
    );

    // negligible weight during day
    private static final Set<String> NIGHT_ONLY = Set.of(
        "leviathan_eel"
    );

    private static final Map<String, Set<Weather>> WEATHER_BONUSES = Map.ofEntries(
        Map.entry("carp",      Set.of(Weather.RAIN)),
        Map.entry("catfish",   Set.of(Weather.RAIN)),
        Map.entry("trout",     Set.of(Weather.RAIN)),
        Map.entry("salmon",    Set.of(Weather.RAIN)),
        Map.entry("eel",       Set.of(Weather.RAIN)),
        Map.entry("tuna",      Set.of(Weather.CLEAR)),
        Map.entry("manta_ray", Set.of(Weather.CLEAR)),
        Map.entry("bass",      Set.of(Weather.CLEAR))
    );

    private static final double TIME_BONUS_MULTIPLIER    = 3.0;
    private static final double WEATHER_BONUS_MULTIPLIER = 2.0;

    public static List<Fish> getAll() { return ALL_FISH; }

    public static Optional<Fish> getById(String id) {
        return ALL_FISH.stream().filter(f -> f.id().equals(id)).findFirst();
    }

    public static List<Fish> getByRarity(Rarity rarity) {
        return ALL_FISH.stream().filter(f -> f.rarity() == rarity).toList();
    }

    public static CatchResult rollCatch(Random rng) {
        return rollCatch(rng, null, null);
    }

    public static CatchResult rollCatch(Random rng, TimeOfDay time) {
        return rollCatch(rng, time, null);
    }

    public static CatchResult rollCatch(Random rng, TimeOfDay time, Weather weather, RodTier rod, BaitType bait) {
        Rarity[] rarities = Rarity.values();
        float[] probs = new float[rarities.length];
        for (int i = 0; i < rarities.length; i++) {
            probs[i] = rod.rarityProbs[i] + bait.rarityBoosts[i];
        }

        float junkProb = Math.max(0f, rod.junkProb + bait.junkDelta);

        // storm: boost rare+ 1.5x, common fills remainder
        if (weather == Weather.STORM) {
            for (int i = 1; i < rarities.length; i++) {
                probs[i] = Math.min(probs[i] * 1.5f, 1f);
            }
        }

        // common absorbs remaining probability
        float nonCommon = junkProb;
        for (int i = 1; i < probs.length; i++) nonCommon += probs[i];
        probs[0] = Math.max(0f, 1.0f - nonCommon);

        double roll = rng.nextDouble();
        if (roll < junkProb) {
            return new CatchResult.JunkCatch(randomJunk(rng));
        }

        // roll within fish space (junk already consumed)
        double fishRoll = roll - junkProb;
        double cumulative = 0;
        Rarity rarity = rarities[0];
        for (int i = 0; i < rarities.length; i++) {
            cumulative += probs[i];
            if (fishRoll < cumulative) { rarity = rarities[i]; break; }
        }

        return new CatchResult.FishCatch(rollFishInRarity(rng, rarity, time, weather));
    }

    public static CatchResult rollCatch(Random rng, TimeOfDay time, Weather weather) {
        double fishTotal = 0;
        for (Rarity r : Rarity.values()) fishTotal += r.catchWeight;

        double totalWeight = fishTotal + JUNK_WEIGHT;
        double roll = rng.nextDouble() * totalWeight;

        if (roll >= fishTotal) {
            return new CatchResult.JunkCatch(randomJunk(rng));
        }

        Rarity rarity = rollRarity(rng, roll);
        return new CatchResult.FishCatch(rollFishInRarity(rng, rarity, time, weather));
    }

    private static Fish rollFishInRarity(Random rng, Rarity rarity, TimeOfDay time, Weather weather) {
        List<Fish> pool = getByRarity(rarity);
        double total = 0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            weights[i] = fishWeight(pool.get(i), time, weather);
            total += weights[i];
        }
        double fishRoll = rng.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights[i];
            if (fishRoll < cumulative) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private static double fishWeight(Fish fish, TimeOfDay time, Weather weather) {
        double w = 1.0;
        if (time != null) {
            boolean isDay   = time == TimeOfDay.DAY;
            boolean isNight = time == TimeOfDay.NIGHT;
            String id = fish.id();
            if (NIGHT_ONLY.contains(id)) {
                w *= isNight ? TIME_BONUS_MULTIPLIER : 0.02;
            } else if (DAY_FISH.contains(id) && isDay) {
                w *= TIME_BONUS_MULTIPLIER;
            } else if (NIGHT_FISH.contains(id) && isNight) {
                w *= TIME_BONUS_MULTIPLIER;
            }
        }
        if (weather != null) {
            Set<Weather> bonusWeathers = WEATHER_BONUSES.get(fish.id());
            if (bonusWeathers != null && bonusWeathers.contains(weather)) w *= WEATHER_BONUS_MULTIPLIER;
        }
        return w;
    }

    private static Rarity rollRarity(Random rng, double roll) {
        double cumulative = 0;
        for (Rarity r : Rarity.values()) {
            cumulative += r.catchWeight;
            if (roll < cumulative) return r;
        }
        return Rarity.COMMON;
    }

    private static JunkType randomJunk(Random rng) {
        JunkType[] junks = JunkType.values();
        return junks[rng.nextInt(junks.length)];
    }
}
