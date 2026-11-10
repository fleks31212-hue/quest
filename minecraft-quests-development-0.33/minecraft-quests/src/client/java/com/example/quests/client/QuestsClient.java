package com.example.quests.client;

import com.example.quests.network.EventHudPayload;
import com.example.quests.network.QuestHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Client HUD. All displayed quest/event values arrive from the authoritative server. */
public final class QuestsClient implements ClientModInitializer {
    private static volatile QuestHudPayload currentQuest = new QuestHudPayload("", 0, 0, 0, false);
    private static volatile EventHudPayload currentEvent = new EventHudPayload("", 0, 0, 0, false);

    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(QuestHudPayload.ID, (payload, context) ->
                context.client().execute(() -> currentQuest = payload));
        ClientPlayNetworking.registerGlobalReceiver(EventHudPayload.ID, (payload, context) ->
                context.client().execute(() -> currentEvent = payload));
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void renderHud(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        int y = 10;
        QuestHudPayload quest = currentQuest;
        if (quest.active()) {
            String title = Text.translatable("hud.minecraft_quests.quest", quest.title()).getString();
            String progress = Text.translatable("hud.minecraft_quests.progress", quest.progress(), Math.max(1, quest.target())).getString();
            String timer = Text.translatable("hud.minecraft_quests.time_left", formatTime(quest.remainingSeconds())).getString();
            int width = Math.max(client.textRenderer.getWidth(title),
                    Math.max(client.textRenderer.getWidth(progress), client.textRenderer.getWidth(timer))) + 12;
            context.fill(5, y - 5, 5 + width, y + 43, 0x99000000);
            context.drawTextWithShadow(client.textRenderer, title, 10, y, 0xFFFFFF);
            context.drawTextWithShadow(client.textRenderer, progress, 10, y + 13, 0x55FF55);
            context.drawTextWithShadow(client.textRenderer, timer, 10, y + 26, 0xFFFF55);
            y += 53;
        }
        EventHudPayload event = currentEvent;
        if (event.active()) {
            String title = Text.translatable("hud.minecraft_quests.event", event.title()).getString();
            String score = event.rank() > 0
                    ? Text.translatable("hud.minecraft_quests.score_rank", event.score(), event.rank()).getString()
                    : Text.translatable("hud.minecraft_quests.score_unranked", event.score()).getString();
            String timer = Text.translatable("hud.minecraft_quests.event_time_left", formatTime(event.remainingSeconds())).getString();
            int width = Math.max(client.textRenderer.getWidth(title),
                    Math.max(client.textRenderer.getWidth(score), client.textRenderer.getWidth(timer))) + 12;
            context.fill(5, y - 5, 5 + width, y + 43, 0x99000000);
            context.drawTextWithShadow(client.textRenderer, title, 10, y, 0xFFFFFF);
            context.drawTextWithShadow(client.textRenderer, score, 10, y + 13, 0x55DDFF);
            context.drawTextWithShadow(client.textRenderer, timer, 10, y + 26, 0xFFFF55);
        }
    }

    private static String formatTime(int seconds) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d", Math.max(0, seconds) / 60, Math.max(0, seconds) % 60);
    }
}
