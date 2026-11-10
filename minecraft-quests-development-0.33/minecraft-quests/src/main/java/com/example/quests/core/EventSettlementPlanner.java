package com.example.quests.core;

import java.util.*;

/** Pure settlement rules for completed server events; the Fabric adapter delivers these rewards. */
public final class EventSettlementPlanner {
    public record WinnerReward(UUID playerId, int rank, int bonusDiamonds,
                               RewardPlanner.RewardPlan itemReward) {
        public WinnerReward {
            Objects.requireNonNull(playerId);
            Objects.requireNonNull(itemReward);
            if (rank != 1 || bonusDiamonds < 5 || bonusDiamonds > 20)
                throw new IllegalArgumentException("Invalid event winner reward");
        }
    }

    public record Settlement(ServerEventManager.EventOutcome outcome,
                             List<WinnerReward> rewards, String announcement) {
        public Settlement {
            Objects.requireNonNull(outcome);
            rewards = List.copyOf(rewards);
            Objects.requireNonNull(announcement);
        }
    }

    private EventSettlementPlanner() {}

    /** Every player tied for first receives the same class of reward. Empty events have no winners. */
    public static Settlement plan(ServerEventManager.EventOutcome outcome, Random random) {
        Objects.requireNonNull(outcome);
        Objects.requireNonNull(random);
        List<WinnerReward> rewards = new ArrayList<>();
        for (UUID winner : outcome.winners()) {
            int diamonds = 5 + random.nextInt(16);
            rewards.add(new WinnerReward(winner, 1, diamonds, RewardPlanner.roll(random)));
        }
        String announcement;
        if (outcome.standings().isEmpty()) {
            announcement = "Событие " + label(outcome.type()) + " завершилось без участников.";
        } else if (outcome.winners().size() == 1) {
            announcement = "Событие " + label(outcome.type()) + " завершено! Победитель: "
                    + shortId(outcome.winners().get(0)) + " — " + outcome.winningScore() + " очков.";
        } else {
            announcement = "Событие " + label(outcome.type()) + " завершено! Победителей: "
                    + outcome.winners().size() + " (ничья), по " + outcome.winningScore() + " очков.";
        }
        return new Settlement(outcome, rewards, announcement);
    }

    private static String label(ServerEventManager.Type type) { return Localization.event(type); }

    private static String shortId(UUID id) { return id.toString().substring(0, 8); }
}
