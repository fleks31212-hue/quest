package com.example.quests.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server-authoritative snapshot for the active server-event HUD. */
public record EventHudPayload(String title, int score, int rank, int remainingSeconds, boolean active)
        implements CustomPayload {
    public static final Id<EventHudPayload> ID = new Id<>(Identifier.of("minecraft_quests", "event_hud"));
    public static final PacketCodec<RegistryByteBuf, EventHudPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, EventHudPayload::title,
            PacketCodecs.VAR_INT, EventHudPayload::score,
            PacketCodecs.VAR_INT, EventHudPayload::rank,
            PacketCodecs.VAR_INT, EventHudPayload::remainingSeconds,
            PacketCodecs.BOOL, EventHudPayload::active,
            EventHudPayload::new);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
