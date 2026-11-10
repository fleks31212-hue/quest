package com.example.quests.core;

import java.util.Map;

/** Small, deterministic objective adapter for objectives that can be verified from server events. */
public final class ObjectiveRules {
    public record Target(String kind, String id, int amount) {}
    private static final Map<String, Target> TARGETS = Map.ofEntries(
        Map.entry("mine_coal_32", new Target("block", "minecraft:coal_ore", 32)),
        Map.entry("mine_diamonds_8", new Target("item", "minecraft:diamond", 8)),
        Map.entry("mine_diamonds_32", new Target("item", "minecraft:diamond", 32)),
        Map.entry("mine_emerald_16", new Target("item", "minecraft:emerald", 16)),
        Map.entry("mine_emerald_ore_16", new Target("block", "minecraft:emerald_ore", 16)),
        Map.entry("mine_iron_16", new Target("block", "minecraft:iron_ore", 16)),
        Map.entry("mine_copper_32", new Target("block", "minecraft:copper_ore", 32)),
        Map.entry("mine_redstone_16", new Target("block", "minecraft:redstone_ore", 16)),
        Map.entry("mine_lapis_8", new Target("block", "minecraft:lapis_ore", 8)),
        Map.entry("mine_gold_32", new Target("block", "minecraft:gold_ore", 32)),
        Map.entry("mine_obsidian_16", new Target("block", "minecraft:obsidian", 16)),
        Map.entry("mine_quartz_16", new Target("block", "minecraft:nether_quartz_ore", 16)),
        Map.entry("mine_debris_32", new Target("block", "minecraft:ancient_debris", 32)),
        Map.entry("mine_nether_gold_64", new Target("block", "minecraft:nether_gold_ore", 64)),
        Map.entry("hunt_zombies_10", new Target("entity", "minecraft:zombie", 10)),
        Map.entry("hunt_skeletons_10", new Target("entity", "minecraft:skeleton", 10)),
        Map.entry("hunt_spiders_5", new Target("entity", "minecraft:spider", 5)),
        Map.entry("hunt_creepers_5", new Target("entity", "minecraft:creeper", 5)),
        Map.entry("hunt_drowned_5", new Target("entity", "minecraft:drowned", 5)),
        Map.entry("hunt_endermen_10", new Target("entity", "minecraft:enderman", 10)),
        Map.entry("hunt_witches_10", new Target("entity", "minecraft:witch", 10)),
        Map.entry("hunt_piglins_15", new Target("entity", "minecraft:piglin", 15)),
        Map.entry("hunt_ghasts_5", new Target("entity", "minecraft:ghast", 5)),
        Map.entry("hunt_ravagers_3", new Target("entity", "minecraft:ravager", 3)),
        Map.entry("hunt_blazes_20", new Target("entity", "minecraft:blaze", 20)),
        Map.entry("collect_wheat_32", new Target("item", "minecraft:wheat", 32)),
        Map.entry("collect_carrots_32", new Target("item", "minecraft:carrot", 32)),
        Map.entry("collect_apples_16", new Target("item", "minecraft:apple", 16)),
        Map.entry("collect_bones_16", new Target("item", "minecraft:bone", 16)),
        Map.entry("collect_pearls_16", new Target("item", "minecraft:ender_pearl", 16)),
        Map.entry("collect_pearls_64", new Target("item", "minecraft:ender_pearl", 64)),
        Map.entry("collect_rods_16", new Target("item", "minecraft:blaze_rod", 16)),
        Map.entry("collect_slime_32", new Target("item", "minecraft:slime_ball", 32)),
        Map.entry("collect_honey_16", new Target("item", "minecraft:honey_bottle", 16)),
        Map.entry("collect_totems_5", new Target("item", "minecraft:totem_of_undying", 5)),
        Map.entry("collect_diamonds_64", new Target("item", "minecraft:diamond", 64)),
        Map.entry("collect_debris_32", new Target("item", "minecraft:ancient_debris", 32)),
        Map.entry("collect_golden_apples_5", new Target("item", "minecraft:enchanted_golden_apple", 5)),
        Map.entry("explore_jungle", new Target("biome", "minecraft:jungle", 1)),
        Map.entry("explore_dimensions", new Target("dimensions", "all", 3)),
        Map.entry("survive_underwater", new Target("survive", "underwater", 300)),
        Map.entry("survive_nether", new Target("survive", "nether", 600)),
        Map.entry("survive_y200", new Target("survive", "height_200", 300)),
        Map.entry("survive_no_armor", new Target("survive", "no_armor", 600)),
        Map.entry("survive_ocean", new Target("survive", "ocean", 600)),
        Map.entry("survive_end", new Target("survive", "end", 900)),
        Map.entry("survive_no_food", new Target("survive", "no_food", 900)),
        Map.entry("survive_desert", new Target("survive", "desert", 300)),
        Map.entry("survive_deep_dark", new Target("survive", "deep_dark", 300))
    );
    private ObjectiveRules() {}
    public static Target target(String questId) { return TARGETS.get(questId); }
    public static boolean supports(String questId) { return TARGETS.containsKey(questId); }
}
