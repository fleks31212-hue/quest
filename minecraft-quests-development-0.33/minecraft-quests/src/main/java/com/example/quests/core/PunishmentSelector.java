package com.example.quests.core;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Selects a penalty after a timed-out quest. This class only chooses a catalog entry;
 * world-changing effects are deliberately handled by bounded server-side executors.
 */
public final class PunishmentSelector {
    private PunishmentSelector() {}

    public static PunishmentCatalog.Entry roll(Difficulty failedQuestDifficulty, Random random) {
        Objects.requireNonNull(failedQuestDifficulty, "failedQuestDifficulty");
        Objects.requireNonNull(random, "random");
        PunishmentCatalog.Tier tier = switch (failedQuestDifficulty) {
            case EASY -> PunishmentCatalog.Tier.EASY;
            case MEDIUM -> PunishmentCatalog.Tier.MEDIUM;
            case HARD -> PunishmentCatalog.Tier.HARD;
        };
        // The funny Chaos set is a bonus pool, not a replacement for difficulty-based penalties.
        if (random.nextInt(5) == 0) tier = PunishmentCatalog.Tier.CHAOS;
        List<PunishmentCatalog.Entry> choices = PunishmentCatalog.byTier(tier);
        if (choices.isEmpty()) throw new IllegalStateException("No punishments registered for " + tier);
        return choices.get(random.nextInt(choices.size()));
    }
}
