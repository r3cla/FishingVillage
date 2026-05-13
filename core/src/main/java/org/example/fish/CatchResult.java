package org.example.fish;

import org.example.game.JunkType;

public sealed interface CatchResult permits CatchResult.FishCatch, CatchResult.JunkCatch {
    record FishCatch(Fish fish) implements CatchResult {}
    record JunkCatch(JunkType junk) implements CatchResult {}
}
