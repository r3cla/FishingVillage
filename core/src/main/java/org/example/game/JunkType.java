package org.example.game;

public enum JunkType {
    BOOT("Old Boot",    "A waterlogged boot. Smells terrible.", 1),
    KELP("Kelp Clump",  "A stringy tangle of ocean weed.",     1);

    public final String displayName;
    public final String description;
    public final int    value;

    JunkType(String displayName, String description, int value) {
        this.displayName = displayName;
        this.description = description;
        this.value       = value;
    }
}
