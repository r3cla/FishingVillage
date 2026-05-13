package org.example.weather;

public enum Weather {
    CLEAR   (1.0, "Clear",    "The sky is blue. A perfect day to fish."),
    OVERCAST(1.0, "Overcast", "Grey clouds roll in. Fish are rising to the surface."),
    RAIN    (1.1, "Rainy",    "Rain drums on the surface. The fish are restless and feeding."),
    STORM   (1.2, "Stormy",   "Waves churn and the depths stir. Something big is moving."),
    FOG     (1.0, "Foggy",    "Thick mist hugs the water. Strange things lurk below.");

    /** Multiplier applied to the overall chance of catching anything this cast. */
    public final double catchRateModifier;
    public final String displayName;
    public final String flavor;

    Weather(double catchRateModifier, String displayName, String flavor) {
        this.catchRateModifier = catchRateModifier;
        this.displayName = displayName;
        this.flavor = flavor;
    }
}
