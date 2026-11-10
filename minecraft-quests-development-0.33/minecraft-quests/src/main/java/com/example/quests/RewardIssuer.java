package com.example.quests;

import com.example.quests.core.RewardPlanner;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Locale;
import java.util.Random;

/** Converts the pure reward plan into real vanilla Minecraft item stacks. */
public final class RewardIssuer {
    private static final Random RANDOM = new Random();

    private RewardIssuer() {}

    public static RewardPlanner.RewardPlan createPlan() {
        return RewardPlanner.roll(RANDOM);
    }

    public static int issue(ServerPlayerEntity player, RewardPlanner.RewardPlan plan) {
        give(player, "minecraft:diamond", plan.diamonds());
        issueItemBundle(player, plan);
        player.sendMessage(
            net.minecraft.text.Text.literal(
                "Награда: " + plan.diamonds() +
                " алмазов, броня, инструмент и зачарованный предмет!"
            ), false
        );
        return plan.diamonds();
    }

    public static int issueEventReward(
        ServerPlayerEntity player,
        int bonusDiamonds,
        RewardPlanner.RewardPlan plan
    ) {
        if (bonusDiamonds < 5 || bonusDiamonds > 20) {
            throw new IllegalArgumentException(
                "Event bonus must be 5..20 diamonds"
            );
        }

        give(player, "minecraft:diamond", bonusDiamonds);
        issueItemBundle(player, plan);

        player.sendMessage(
            net.minecraft.text.Text.literal(
                "Награда за событие: " + bonusDiamonds +
                " бонусных алмазов, броня, инструмент и зачарованный предмет!"
            ), false
        );

        return bonusDiamonds;
    }

    private static void issueItemBundle(
        ServerPlayerEntity player,
        RewardPlanner.RewardPlan plan
    ) {
        String armor = plan.armor().name().toLowerCase(Locale.ROOT);
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};

        give(
            player,
            "minecraft:" + armor + "_" +
            pieces[RANDOM.nextInt(pieces.length)],
            1
        );

        give(
            player,
            "minecraft:" + plan.tool().name().toLowerCase(Locale.ROOT),
            1
        );

        ItemStack bonus = new ItemStack(item(plan.enchantedItem()));

        var enchantmentRegistry =
            player.getWorld().getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT);

        for (String enchantName : plan.enchantments()) {
            Identifier id = Identifier.of(
                "minecraft",
                enchantName.toLowerCase(Locale.ROOT)
            );

            enchantmentRegistry.getEntry(id).ifPresent(entry -> {
                Enchantment enchantment = entry.value();

                int level = 1 + RANDOM.nextInt(
                    Math.max(1, enchantment.getMaxLevel())
                );

                bonus.addEnchantment(entry, level);
            });
        }

        player.getInventory().offerOrDrop(bonus);
    }

    private static Item item(String path) {
        Identifier id = Identifier.of(
            "minecraft",
            path.toLowerCase(Locale.ROOT)
        );

        return Registries.ITEM.get(id);
    }

    private static void give(
        ServerPlayerEntity player,
        String id,
        int count
    ) {
        ItemStack stack = new ItemStack(
            Registries.ITEM.get(Identifier.of(id)),
            count
        );

        player.getInventory().offerOrDrop(stack);
    }
}
