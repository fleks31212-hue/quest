package com.example.quests.core;

/** Dependency-free catalog checks; can be run with the project's test harness or manually. */
public final class QuestCatalogTest {
    public static void main(String[] args) {
        if (QuestCatalog.all().size() != 100) throw new AssertionError("Expected 100 quests");
        if (QuestCatalog.definitions().stream().map(QuestCatalog.Definition::category).distinct().count() != 7)
            throw new AssertionError("Expected seven quest categories");
        for (QuestCatalog.Definition d : QuestCatalog.definitions()) {
            if (d.title().isBlank() || d.quest().durationMs() < 1_000) throw new AssertionError(d.quest().id());
        }
        System.out.println("Quest catalog checks passed: 100 quests / 7 categories");
    }
}
