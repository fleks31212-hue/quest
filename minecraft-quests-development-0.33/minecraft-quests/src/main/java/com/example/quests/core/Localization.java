package com.example.quests.core;

/** Russian-first names for values shown in server messages and commands.
 * Internal enum identifiers remain stable for saves, commands and networking.
 */
public final class Localization {
    private Localization() {}

    public static String difficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "Лёгкая";
            case MEDIUM -> "Средняя";
            case HARD -> "Сложная";
        };
    }

    public static String event(ServerEventManager.Type type) {
        return switch (type) {
            case BOSS_HUNT -> "Охота на босса";
            case DIAMOND_RUSH -> "Алмазная лихорадка";
            case BUILDING_CONTEST -> "Строительный конкурс";
            case LAST_SURVIVOR -> "Последний выживший";
            case HAPPY_HOUR -> "Счастливый час";
        };
    }

    public static String punishmentTier(PunishmentCatalog.Tier tier) {
        return switch (tier) {
            case EASY -> "Лёгкое";
            case MEDIUM -> "Среднее";
            case HARD -> "Сложное";
            case CHAOS -> "Хаос и безумие";
        };
    }
}
