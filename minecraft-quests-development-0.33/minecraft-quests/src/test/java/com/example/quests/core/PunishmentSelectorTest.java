package com.example.quests.core;

import java.util.Random;

public final class PunishmentSelectorTest {
    public static void main(String[] args) {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int seed = 0; seed < 2_000; seed++) {
                var entry = PunishmentSelector.roll(difficulty, new Random(seed));
                if (entry == null) throw new AssertionError("null penalty");
                if (entry.tier() != PunishmentCatalog.Tier.CHAOS) {
                    PunishmentCatalog.Tier expected = switch (difficulty) {
                        case EASY -> PunishmentCatalog.Tier.EASY;
                        case MEDIUM -> PunishmentCatalog.Tier.MEDIUM;
                        case HARD -> PunishmentCatalog.Tier.HARD;
                    };
                    if (entry.tier() != expected) throw new AssertionError("wrong tier for " + difficulty);
                }
            }
        }
        System.out.println("PunishmentSelectorTest OK");
    }
}
