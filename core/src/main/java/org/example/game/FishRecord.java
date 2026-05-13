package org.example.game;

public class FishRecord {
    public boolean discovered;
    public int     totalCaught;
    public double  personalBest; // kg
    public boolean shinyUnlocked;

    public void record(double weight, boolean shiny) {
        discovered = true;
        totalCaught++;
        if (weight > personalBest) personalBest = weight;
        if (shiny) shinyUnlocked = true;
    }
}
