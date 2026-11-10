package com.example.quests.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regression checks for Russian-first player-facing names. */
public final class LocalizationTest {
    public static void main(String[] args) {
        check(Localization.difficulty(Difficulty.EASY).equals("Лёгкая"), "easy difficulty must be Russian");
        check(Localization.difficulty(Difficulty.MEDIUM).equals("Средняя"), "medium difficulty must be Russian");
        check(Localization.difficulty(Difficulty.HARD).equals("Сложная"), "hard difficulty must be Russian");
        check(Localization.event(ServerEventManager.Type.BOSS_HUNT).equals("Охота на босса"), "boss event name must be Russian");
        check(Localization.event(ServerEventManager.Type.DIAMOND_RUSH).equals("Алмазная лихорадка"), "diamond event name must be Russian");
        check(Localization.event(ServerEventManager.Type.BUILDING_CONTEST).equals("Строительный конкурс"), "build event name must be Russian");
        check(Localization.event(ServerEventManager.Type.LAST_SURVIVOR).equals("Последний выживший"), "survival event name must be Russian");
        check(Localization.event(ServerEventManager.Type.HAPPY_HOUR).equals("Счастливый час"), "happy hour name must be Russian");
        check(Localization.punishmentTier(PunishmentCatalog.Tier.CHAOS).equals("Хаос и безумие"), "chaos tier must be Russian");
        verifyLanguageFiles();
        System.out.println("LocalizationTest: OK");
    }
    private static void verifyLanguageFiles() {
        try {
            Path root = Path.of("src/main/resources/assets/minecraft_quests/lang");
            Map<String, String> ru = readEntries(Files.readString(root.resolve("ru_ru.json")));
            Map<String, String> en = readEntries(Files.readString(root.resolve("en_us.json")));
            check(!ru.isEmpty(), "Russian language file must not be empty");
            check(PunishmentCatalog.all().size() == 69, "punishment catalog must contain 69 entries");
            for (PunishmentCatalog.Entry entry : PunishmentCatalog.all()) {
                String key = "punishment.minecraft_quests." + entry.id();
                check(ru.containsKey(key), "Missing Russian punishment translation: " + key);
                check(en.containsKey(key), "Missing English punishment translation: " + key);
                check(ru.get(key).equals(entry.title()), "Russian punishment title mismatch: " + key);
            }
            check(ru.keySet().equals(en.keySet()), "ru_ru.json and en_us.json must have identical keys");
            for (String key : ru.keySet()) {
                check(placeholders(ru.get(key)).equals(placeholders(en.get(key))),
                        "Translation placeholders differ for key " + key);
                check(!ru.get(key).isBlank(), "Russian translation must not be blank: " + key);
            }
        } catch (java.io.IOException e) {
            throw new AssertionError("Could not read language resources", e);
        }
    }

    private static Map<String, String> readEntries(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\\s*\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        while (matcher.find()) result.put(matcher.group(1), matcher.group(2));
        return result;
    }

    private static String placeholders(String value) {
        Matcher matcher = Pattern.compile("%[0-9]*\\$?[sd]").matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) result.append(matcher.group()).append(';');
        return result.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
