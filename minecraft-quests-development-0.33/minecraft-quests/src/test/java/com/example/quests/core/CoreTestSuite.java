package com.example.quests.core;

/** Runs the repository's dependency-free regression programs from Gradle/CI. */
public final class CoreTestSuite {
    private CoreTestSuite() {}

    public static void main(String[] args) throws Exception {
        QuestCatalogTest.main(args);
        ObjectiveRulesTest.main(args);
        QuestEngineSmokeTest.main(args);
        LeaderboardAndEventsTest.main(args);
        PunishmentSelectorTest.main(args);
        RewardAndPunishmentTest.main(args);
        PendingEventRewardQueueTest.main(args);
        LocalizationTest.main(args);
        System.out.println("Minecraft Quests core verification: all 8 test programs passed.");
    }
}
