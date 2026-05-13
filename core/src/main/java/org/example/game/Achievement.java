package org.example.game;

public enum Achievement {
    FIRST_CATCH     ("First Cast",           "Catch your first fish"),
    CATCH_10        ("Hooked on Fishing",    "Catch 10 fish total"),
    CATCH_50        ("Seasoned Angler",      "Catch 50 fish total"),
    CATCH_100       ("Master Angler",        "Catch 100 fish total"),
    FIRST_SHINY     ("Golden Scales",        "Catch a shiny fish"),
    FIRST_RARE      ("Deep Pull",            "Catch a Rare or better fish"),
    FIRST_EPIC      ("Monster Catch",        "Catch an Epic or better fish"),
    FIRST_LEGENDARY ("Legend of the Lake",   "Catch a Legendary fish"),
    ALL_COMMON      ("Common Ground",        "Discover all Common fish"),
    ALL_UNCOMMON    ("Uncommon Taste",       "Discover all Uncommon fish"),
    ALL_RARE        ("Rare Find",            "Discover all Rare fish"),
    ALL_FISH        ("Completionist",        "Discover every species");

    public final String displayName;
    public final String description;

    Achievement(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
