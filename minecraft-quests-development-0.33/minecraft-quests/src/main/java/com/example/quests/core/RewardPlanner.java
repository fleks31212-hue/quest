package com.example.quests.core;

import java.util.*;

/** Pure reward rules. The Fabric adapter turns this plan into Minecraft ItemStacks. */
public final class RewardPlanner {
    public enum Armor { LEATHER, GOLDEN, CHAINMAIL, IRON, DIAMOND, NETHERITE }
    public enum Tool {
        WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE,
        WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE,
        WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL,
        WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE, DIAMOND_HOE, NETHERITE_HOE,
        WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD
    }
    public record RewardPlan(int diamonds, Armor armor, Tool tool, String enchantedItem, List<String> enchantments) {
        public RewardPlan { enchantments = List.copyOf(enchantments); }
    }
    private static final String[] ITEMS = {"BOW", "CROSSBOW", "TRIDENT", "SHIELD", "FISHING_ROD", "ELYTRA", "DIAMOND_PICKAXE", "DIAMOND_SWORD"};
    private static final Map<String, List<String>> COMPATIBLE_ENCHANTS = Map.of(
        "BOW", List.of("POWER", "PUNCH", "FLAME", "INFINITY", "UNBREAKING", "MENDING"),
        "CROSSBOW", List.of("MULTISHOT", "PIERCING", "QUICK_CHARGE", "UNBREAKING", "MENDING"),
        "TRIDENT", List.of("LOYALTY", "CHANNELING", "RIPTIDE", "IMPALING", "UNBREAKING", "MENDING"),
        "SHIELD", List.of("UNBREAKING", "MENDING"),
        "FISHING_ROD", List.of("LUCK_OF_THE_SEA", "LURE", "UNBREAKING", "MENDING"),
        "ELYTRA", List.of("UNBREAKING", "MENDING"),
        "DIAMOND_PICKAXE", List.of("EFFICIENCY", "FORTUNE", "SILK_TOUCH", "UNBREAKING", "MENDING"),
        "DIAMOND_SWORD", List.of("SHARPNESS", "SMITE", "LOOTING", "UNBREAKING", "MENDING", "FIRE_ASPECT", "KNOCKBACK")
    );
    private RewardPlanner() {}

    public static RewardPlan roll(Random random) {
        Objects.requireNonNull(random);
        Armor armor = Armor.values()[random.nextInt(Armor.values().length)];
        Tool tool = Tool.values()[random.nextInt(Tool.values().length)];
        String item = ITEMS[random.nextInt(ITEMS.length)];
        List<String> pool = new ArrayList<>(COMPATIBLE_ENCHANTS.get(item));
        Collections.shuffle(pool, random);
        List<String> enchants = new ArrayList<>();
        int desired = 1 + random.nextInt(Math.min(3, pool.size()));
        for (String enchant : pool) {
            if (enchants.size() >= desired) break;
            if ((enchant.equals("FORTUNE") && enchants.contains("SILK_TOUCH")) ||
                (enchant.equals("SILK_TOUCH") && enchants.contains("FORTUNE")) ||
                (enchant.equals("MULTISHOT") && enchants.contains("PIERCING")) ||
                (enchant.equals("PIERCING") && enchants.contains("MULTISHOT")) ||
                (enchant.equals("RIPTIDE") && (enchants.contains("LOYALTY") || enchants.contains("CHANNELING"))) ||
                ((enchant.equals("LOYALTY") || enchant.equals("CHANNELING")) && enchants.contains("RIPTIDE"))) continue;
            enchants.add(enchant);
        }
        if (enchants.isEmpty()) enchants.add("UNBREAKING");
        return new RewardPlan(1 + random.nextInt(20), armor, tool, item, enchants);
    }
}
