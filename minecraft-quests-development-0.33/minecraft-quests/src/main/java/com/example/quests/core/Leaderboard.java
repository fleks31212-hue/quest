package com.example.quests.core;

import java.util.*;

/** Deterministic leaderboard projection over persistent player statistics. */
public final class Leaderboard {
    public enum Metric { WINS, DIAMONDS, BEST_STREAK, FAILURES }
    public record Entry(UUID playerId, int wins, int failures, int skips, int diamonds, int bestStreak) {}
    private Leaderboard() {}

    public static List<Entry> top(Collection<QuestState> states, Metric metric, int limit) {
        if (limit < 1) return List.of();
        Comparator<Entry> order = switch (Objects.requireNonNull(metric)) {
            case WINS -> Comparator.comparingInt(Entry::wins).reversed().thenComparing(Comparator.comparingInt(Entry::bestStreak).reversed());
            case DIAMONDS -> Comparator.comparingInt(Entry::diamonds).reversed().thenComparing(Comparator.comparingInt(Entry::wins).reversed());
            case BEST_STREAK -> Comparator.comparingInt(Entry::bestStreak).reversed().thenComparing(Comparator.comparingInt(Entry::wins).reversed());
            case FAILURES -> Comparator.comparingInt(Entry::failures).thenComparing(Comparator.comparingInt(Entry::wins).reversed());
        };
        return states.stream().map(s -> new Entry(s.playerId, s.completed, s.failed, s.skipped, s.diamondsEarned, s.bestStreak))
                .sorted(order.thenComparing(e -> e.playerId().toString()))
                .limit(limit).toList();
    }
}
