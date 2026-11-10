package com.example.quests.core;

import java.util.EnumSet;
import java.util.Set;

/** Per-player difficulty progression. Only successful completions advance it. */
public enum Difficulty {
    EASY, MEDIUM, HARD;

    public static Set<Difficulty> availableAt(int completed) {
        if (completed < 5) return EnumSet.of(EASY);
        if (completed < 10) return EnumSet.of(EASY, MEDIUM);
        if (completed < 20) return EnumSet.allOf(Difficulty.class);
        if (completed < 30) return EnumSet.of(MEDIUM, HARD);
        return EnumSet.of(HARD);
    }
}
