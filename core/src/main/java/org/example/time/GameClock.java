package org.example.time;

public class GameClock {

    private int minuteOfDay; // 0–1439

    public GameClock(int startHour, int startMinute) {
        this.minuteOfDay = startHour * 60 + startMinute;
    }

    public void advance(int minutes) {
        minuteOfDay = (minuteOfDay + minutes) % 1440;
    }

    public void setMinuteOfDay(int minute) {
        minuteOfDay = ((minute % 1440) + 1440) % 1440;
    }

    public TimeOfDay getTimeOfDay() {
        return TimeOfDay.of(minuteOfDay / 60);
    }

    public int getMinuteOfDay() {
        return minuteOfDay;
    }

    public String getFormattedTime() {
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        String ampm = hour < 12 ? "AM" : "PM";
        return String.format("%d:%02d %s", displayHour, minute, ampm);
    }
}
