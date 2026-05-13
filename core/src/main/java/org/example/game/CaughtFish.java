package org.example.game;

import org.example.fish.Fish;

public record CaughtFish(Fish fish, JunkType junk, double weight, boolean shiny, int value) {

    public CaughtFish(Fish fish, double weight, boolean shiny, int value) {
        this(fish, null, weight, shiny, value);
    }

    public boolean isJunk()      { return junk != null; }
    public String  displayName() { return junk != null ? junk.displayName : fish.name(); }
}
