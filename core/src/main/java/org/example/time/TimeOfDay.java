package org.example.time;

public enum TimeOfDay {
    DAY   ( 6, 19, "Day",   "Sun climbs high. Fish stir in the shallows."),
    NIGHT (19,  6, "Night", "Moonlight ripples on the water. The deep awakens.");

    public final int startHour; // inclusive
    public final int endHour;   // exclusive
    public final String displayName;
    public final String flavor;

    TimeOfDay(int startHour, int endHour, String displayName, String flavor) {
        this.startHour = startHour;
        this.endHour = endHour;
        this.displayName = displayName;
        this.flavor = flavor;
    }

    public static TimeOfDay of(int hour) {
        for (TimeOfDay t : values()) {
            if (t.contains(hour)) return t;
        }
        return NIGHT;
    }

    private boolean contains(int hour) {
        if (startHour < endHour) return hour >= startHour && hour < endHour;
        return hour >= startHour || hour < endHour; // wraps midnight
    }
}
