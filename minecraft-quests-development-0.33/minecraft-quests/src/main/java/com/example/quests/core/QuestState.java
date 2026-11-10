package com.example.quests.core;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/** Persistent player statistics and current quest state. Times are epoch milliseconds. */
public final class QuestState {
    public final UUID playerId;
    public int completed, failed, skipped, diamondsEarned, winStreak, bestStreak;
    public String activeQuestId = "";
    public Difficulty activeDifficulty;
    public long deadlineEpochMs;
    public long nextQuestEpochMs;
    public String lastResult = "NONE";
    /** Objective counters survive server restarts. */
    public final Map<String, Integer> progress = new HashMap<>();

    public QuestState(UUID playerId) { this.playerId = playerId; }
    public boolean hasActiveQuest() { return !activeQuestId.isBlank(); }
    public int getCompleted() { return completed; }
    public Difficulty[] allowedDifficulties() {
        return Difficulty.availableAt(completed).toArray(Difficulty[]::new);
    }
}
