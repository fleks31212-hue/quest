package com.example.quests.core;

import java.util.Objects;
import java.util.UUID;

/** Server-side scoring rules. Call only after the corresponding action is verified by the server. */
public final class EventScoringPolicy {
    public enum Action {
        BOSS_DAMAGE,
        BOSS_DEFEATED,
        DIAMOND_ORE_MINED,
        BUILD_VOTE_RECEIVED,
        SURVIVAL_CHECKPOINT,
        QUEST_COMPLETED_DURING_HAPPY_HOUR
    }

    public record Award(boolean accepted, int points) {
        public static Award rejected() { return new Award(false, 0); }
    }

    private static final int MAX_UNITS_PER_ACTION = 1_000;
    private EventScoringPolicy() {}

    /**
     * Converts a verified server-side action into event points. The action must match
     * the active event type; quantity is clamped to prevent accidental oversized awards.
     */
    public static Award award(ServerEventManager manager, UUID playerId, UUID actionId, Action action,
                              int quantity, long now) {
        Objects.requireNonNull(manager);
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(actionId);
        Objects.requireNonNull(action);
        if (quantity <= 0 || manager.active(now).isEmpty()) return Award.rejected();

        ServerEventManager.Type type = manager.active(now).orElseThrow().type();
        int pointsPerUnit = switch (type) {
            case BOSS_HUNT -> action == Action.BOSS_DAMAGE ? 1 : action == Action.BOSS_DEFEATED ? 100 : 0;
            case DIAMOND_RUSH -> action == Action.DIAMOND_ORE_MINED ? 2 : 0;
            case BUILDING_CONTEST -> action == Action.BUILD_VOTE_RECEIVED ? 1 : 0;
            case LAST_SURVIVOR -> action == Action.SURVIVAL_CHECKPOINT ? 5 : 0;
            case HAPPY_HOUR -> action == Action.QUEST_COMPLETED_DURING_HAPPY_HOUR ? 3 : 0;
        };
        if (pointsPerUnit == 0) return Award.rejected();
        int units = Math.min(quantity, MAX_UNITS_PER_ACTION);
        int points = Math.multiplyExact(units, pointsPerUnit);
        return manager.contributeOnce(playerId, actionId, points, now) ? new Award(true, points) : Award.rejected();
    }
}
