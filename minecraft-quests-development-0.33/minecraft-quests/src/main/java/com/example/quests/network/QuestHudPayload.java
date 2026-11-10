package com.example.quests.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Compact server-authoritative HUD snapshot. */
public record QuestHudPayload(String title, int progress, int target, int remainingSeconds, boolean active)
        implements CustomPayload {
    public static final Id<QuestHudPayload> ID = new Id<>(Identifier.of("minecraft_quests", "quest_hud"));
    public static final PacketCodec<RegistryByteBuf, QuestHudPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, QuestHudPayload::title,
            PacketCodecs.VAR_INT, QuestHudPayload::progress,
            PacketCodecs.VAR_INT, QuestHudPayload::target,
            PacketCodecs.VAR_INT, QuestHudPayload::remainingSeconds,
            PacketCodecs.BOOL, QuestHudPayload::active,
            QuestHudPayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
