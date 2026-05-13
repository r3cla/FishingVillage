package org.example.game;

import org.example.fish.Fish;
import org.example.fish.FishRegistry;
import org.example.time.GameClock;
import org.example.time.TimeOfDay;
import org.example.weather.Weather;
import org.example.weather.WeatherSystem;

import java.util.Random;
import java.util.Scanner;

public class FishingGame {

    private static final double BASE_CATCH_CHANCE  = 0.70;
    private static final int    MINUTES_PER_CAST   = 90;
    private static final int    CASTS_PER_WEATHER_CHANGE = 4;

    private static final String[] MISS_MESSAGES = {
        "Nothing. The bait drifts back untouched.",
        "A nibble... then silence.",
        "The line goes slack. Not today.",
        "You wait. The water gives nothing back.",
        "Something bumped the line, but it's gone.",
        "The hook comes back empty.",
        "Not even a nibble. The fish aren't interested."
    };

    private final Random       rng           = new Random();
    private final GameClock    clock         = new GameClock(6, 0);
    private final WeatherSystem weatherSystem = new WeatherSystem(Weather.CLEAR);
    private final GameState    state         = new GameState();
    private final Scanner      scanner       = new Scanner(System.in);

    public void run() {
        printWelcome();

        TimeOfDay lastPeriod  = null;
        Weather   lastWeather = null;
        int       castCount   = 0;

        while (true) {
            TimeOfDay time    = clock.getTimeOfDay();
            Weather   weather = weatherSystem.getCurrent();

            if (time != lastPeriod) {
                System.out.println();
                System.out.println("  --- " + time.displayName + " ---");
                System.out.println("  " + time.flavor);
                lastPeriod = time;
            }
            if (weather != lastWeather) {
                System.out.println("  [" + weather.displayName + "] " + weather.flavor);
                lastWeather = weather;
            }

            printStatus(time, weather);

            System.out.print("  > ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("q") || input.equals("quit")) break;

            System.out.println();
            resolveCast(time, weather);
            castCount++;

            clock.advance(MINUTES_PER_CAST);
            if (castCount % CASTS_PER_WEATHER_CHANGE == 0) {
                weatherSystem.transition(rng);
            }

            System.out.println();
        }

        printSummary();
        scanner.close();
    }

    private void printWelcome() {
        System.out.println("============================================");
        System.out.println("            F I S H I N G   V I L L A G E");
        System.out.println("============================================");
        System.out.println("  The dock creaks underfoot. Fog hangs low.");
        System.out.println("  Your bucket is empty. The sea is waiting.");
        System.out.println();
        System.out.println("  [Enter] Cast line    [Q] Go home");
        System.out.println("============================================");
    }

    private void printStatus(TimeOfDay time, Weather weather) {
        System.out.println();
        System.out.printf("  %s  |  %s  |  %s  |  Coins: %d  |  Catches: %d%n",
            clock.getFormattedTime(),
            time.displayName,
            weather.displayName,
            state.getCoins(),
            state.getTotalCatches()
        );
        System.out.println("  [Enter] Cast  |  [Q] Quit");
    }

    private void resolveCast(TimeOfDay time, Weather weather) {
        System.out.println("  You cast your line...");
        System.out.println();

        if (rng.nextDouble() > BASE_CATCH_CHANCE * weather.catchRateModifier) {
            state.addMiss();
            System.out.println("  " + MISS_MESSAGES[rng.nextInt(MISS_MESSAGES.length)]);
            return;
        }

        var result = FishRegistry.rollCatch(rng, time, weather);
        if (result instanceof org.example.fish.CatchResult.JunkCatch jc) {
            state.addCatch(jc.junk().value);
            System.out.println("  You pulled up: " + jc.junk().displayName);
            System.out.println("  \"" + jc.junk().description + "\"");
            return;
        }
        org.example.fish.Fish fish = ((org.example.fish.CatchResult.FishCatch) result).fish();
        boolean shiny = fish.rollShiny(rng);
        double weight = fish.rollWeight(rng);
        int    value  = fish.sellValue(weight, shiny);
        int    before = state.getCoins();

        state.addCatch(value);

        if (shiny) System.out.println("  ** SHINY **");
        System.out.printf("  %s  [%s]%n", fish.name(), fish.rarity().displayName);
        System.out.printf("  %.2f kg  —  worth %d coins%n", weight, value);
        System.out.println("  \"" + fish.description() + "\"");
        System.out.printf("  Coins: %d -> %d%n", before, state.getCoins());
    }

    private void printSummary() {
        System.out.println();
        System.out.println("============================================");
        System.out.println("  You head home for the day.");
        System.out.println("============================================");
        System.out.printf("  Catches : %d%n",  state.getTotalCatches());
        System.out.printf("  Misses  : %d%n",  state.getTotalMisses());
        System.out.printf("  Coins   : %d%n",  state.getCoins());
        System.out.println("============================================");
    }
}
