package com.example.quests;

import com.example.quests.core.PunishmentCatalog;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Bounded, reversible in-game penalty adapter. No terrain destruction, inventory deletion,
 * lava/TNT damage, or forced hostile-mob spawning is performed by this first executor.
 * Unsupported catalog entries are announced as pending implementations instead of
 * pretending that a dangerous world mutation happened.
 */
public final class PunishmentExecutor {
    private PunishmentExecutor() {}

    public static void apply(ServerPlayerEntity player, PunishmentCatalog.Entry entry) {
        String id = entry.id();
        boolean applied = switch (id) {
            case "inverted_controls", "reverse_world" -> effect(player, StatusEffects.NAUSEA, 20 * 18, 0);
            case "super_jump", "spring_legs" -> effect(player, StatusEffects.JUMP_BOOST, 20 * 25, 2);
            case "ice_prison", "obsidian_cell" -> effect(player, StatusEffects.SLOWNESS, 20 * 15, 2);
            case "pumpkin_vision" -> effect(player, StatusEffects.BLINDNESS, 20 * 12, 0);
            case "resize", "chicken_mode", "pig_inside" -> effect(player, StatusEffects.GLOWING, 20 * 30, 0);
            case "vegan", "hungry_backpack" -> effect(player, StatusEffects.HUNGER, 20 * 20, 0);
            case "cursed_hotbar" -> effect(player, StatusEffects.MINING_FATIGUE, 20 * 20, 0);
            case "up_teleport", "lava_rise" -> effect(player, StatusEffects.LEVITATION, 20 * 3, 0);
            case "dont_stop" -> effect(player, StatusEffects.SLOWNESS, 20 * 10, 0);
            case "anomaly_rain", "failure_fireworks", "rainbow_sheep", "upside_down", "chicken_boss",
                 "slime_king", "sheep_apocalypse", "boss_arrives", "bee_union", "mini_dragon" ->
                    effect(player, StatusEffects.GLOWING, 20 * 20, 0);
            default -> false;
        };
        String titleKey = "punishment.minecraft_quests." + id;
        String statusKey = applied ? "message.minecraft_quests.punishment_effect_applied"
                : "message.minecraft_quests.punishment_pending";
        player.sendMessage(Text.translatable("message.minecraft_quests.punishment_applied",
                Text.translatable(titleKey), Text.translatable(statusKey)), false);
    }

    private static boolean effect(ServerPlayerEntity player,
                                  net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> type,
                                  int durationTicks, int amplifier) {
        return player.addStatusEffect(new StatusEffectInstance(type, durationTicks, amplifier,
                false, true, true));
    }
}
