package com.example.quests.core;

/** Dependency-free coverage for the newly supported exploration/survival adapters. */
public final class ObjectiveRulesTest {
    public static void main(String[] args) {
        assert ObjectiveRules.target("explore_jungle").kind().equals("biome");
        assert ObjectiveRules.target("explore_dimensions").amount() == 3;
        assert ObjectiveRules.target("survive_underwater").amount() == 300;
        assert ObjectiveRules.target("survive_nether").amount() == 600;
        assert ObjectiveRules.target("survive_end").amount() == 900;
        assert ObjectiveRules.target("survive_deep_dark").id().equals("deep_dark");
        assert ObjectiveRules.target("build_tower_10") == null; // no false-positive completion
        System.out.println("ObjectiveRules test: OK");
    }
}
