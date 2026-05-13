package org.example.weather;

import java.util.EnumMap;
import java.util.Random;

public class WeatherSystem {

    // Transition probabilities: for each current weather, the chance of moving to each next weather
    private static final EnumMap<Weather, double[]> TRANSITIONS = new EnumMap<>(Weather.class);

    static {
        //                                      CLEAR  OVERCAST  RAIN  STORM   FOG
        TRANSITIONS.put(Weather.CLEAR,    new double[]{ 0.65,  0.20,  0.05,  0.00,  0.10 });
        TRANSITIONS.put(Weather.OVERCAST, new double[]{ 0.25,  0.35,  0.30,  0.05,  0.05 });
        TRANSITIONS.put(Weather.RAIN,     new double[]{ 0.10,  0.30,  0.35,  0.20,  0.05 });
        TRANSITIONS.put(Weather.STORM,    new double[]{ 0.05,  0.20,  0.50,  0.25,  0.00 });
        TRANSITIONS.put(Weather.FOG,      new double[]{ 0.40,  0.15,  0.05,  0.00,  0.40 });
    }

    private static final Weather[] ORDER = Weather.values();

    private Weather current;

    public WeatherSystem(Weather initial) {
        this.current = initial;
    }

    public Weather getCurrent() {
        return current;
    }

    public void setCurrent(Weather weather) {
        this.current = weather;
    }

    /** Transition to the next weather state based on current weather's probabilities. */
    public Weather transition(Random rng) {
        double[] probs = TRANSITIONS.get(current);
        double roll = rng.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (roll < cumulative) {
                current = ORDER[i];
                return current;
            }
        }
        return current;
    }
}
