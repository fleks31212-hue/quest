package com.example.quests;

import com.example.quests.core.*;
import com.example.quests.network.QuestHudPayload;
import com.example.quests.network.EventHudPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.registry.Registries;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.Comparator;

public final class QuestsMod implements ModInitializer {
    public static final String MOD_ID="minecraft_quests";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    private static QuestEngine engine;
    private static Path saveFile;
    private static Path eventSaveFile;
    private static Path pendingRewardsFile;
    private static ServerEventManager eventManager = new ServerEventManager();
    private static PendingEventRewardQueue pendingEventRewards = new PendingEventRewardQueue();

    @Override public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(QuestHudPayload.ID, QuestHudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EventHudPayload.ID, EventHudPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            saveFile=server.getSavePath(WorldSavePath.ROOT).resolve("minecraft-quests.properties");
            eventSaveFile=server.getSavePath(WorldSavePath.ROOT).resolve("minecraft-quests-event.properties");
            pendingRewardsFile=server.getSavePath(WorldSavePath.ROOT).resolve("minecraft-quests-pending-rewards.properties");
            try { engine=QuestEngine.load(saveFile,QuestCatalog::pick); }
            catch(Exception e){ LOGGER.error("Could not load quest data; starting fresh",e); engine=new QuestEngine(QuestCatalog::pick); }
            try { eventManager=ServerEventManager.load(eventSaveFile,System.currentTimeMillis()); }
            catch(Exception e){ LOGGER.error("Could not load event data; starting without an active event",e); eventManager=new ServerEventManager(); }
            try { pendingEventRewards=PendingEventRewardQueue.load(pendingRewardsFile); }
            catch(Exception e){ LOGGER.error("Could not load pending event rewards; starting with an empty queue",e); pendingEventRewards=new PendingEventRewardQueue(); }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save(server));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> deliverPendingEventRewards(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(QuestsMod::tick);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity sp && engine != null) recordObjective(sp, "block", Registries.BLOCK.getId(state.getBlock()).toString());
            if (player instanceof ServerPlayerEntity sp && (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE))) {
                String actionKey = "diamond:" + world.getRegistryKey().getValue() + ":" + pos.toShortString()
                        + ":" + world.getTime() + ":" + sp.getUuid();
                var award = EventScoringPolicy.award(eventManager, sp.getUuid(),
                        java.util.UUID.nameUUIDFromBytes(actionKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        EventScoringPolicy.Action.DIAMOND_ORE_MINED, 1, System.currentTimeMillis());
                if (award.accepted()) { sp.sendMessage(Text.translatable("message.minecraft_quests.diamond_points", award.points()), true); saveEvents(); }
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getAttacker() instanceof ServerPlayerEntity sp && engine != null) {
                recordObjective(sp, "entity", Registries.ENTITY_TYPE.getId(entity.getType()).toString());
                String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                if (entityId.equals("minecraft:ender_dragon") || entityId.equals("minecraft:wither")
                        || entityId.equals("minecraft:warden")) {
                    var award = EventScoringPolicy.award(eventManager, sp.getUuid(),
                            java.util.UUID.nameUUIDFromBytes(("boss-defeat:" + entity.getUuid() + ":" + sp.getUuid())
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            EventScoringPolicy.Action.BOSS_DEFEATED, 1, System.currentTimeMillis());
                    if (award.accepted()) { sp.sendMessage(Text.translatable("message.minecraft_quests.boss_points", award.points()), true); saveEvents(); }
                }
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("quests").requires(s->s.hasPermissionLevel(2))
                .then(CommandManager.literal("give").then(CommandManager.argument("player",StringArgumentType.word())
                    .executes(c->give(c.getSource(),StringArgumentType.getString(c,"player"),null))
                    .then(CommandManager.argument("difficulty",StringArgumentType.word()).suggests((c,b)->{b.suggest("easy");b.suggest("medium");b.suggest("hard");return b.buildFuture();})
                        .executes(c->give(c.getSource(),StringArgumentType.getString(c,"player"),StringArgumentType.getString(c,"difficulty"))))))
                .then(CommandManager.literal("cancel").then(CommandManager.argument("player",StringArgumentType.word()).executes(c->cancel(c.getSource(),StringArgumentType.getString(c,"player")))))
                .then(CommandManager.literal("skip").then(CommandManager.argument("player",StringArgumentType.word()).executes(c->skip(c.getSource(),StringArgumentType.getString(c,"player")))))
                .then(CommandManager.literal("complete").then(CommandManager.argument("player",StringArgumentType.word()).executes(c->complete(c.getSource(),StringArgumentType.getString(c,"player")))))
                .then(CommandManager.literal("sounds").then(CommandManager.literal("on").executes(c->sounds(c.getSource(),true))).then(CommandManager.literal("off").executes(c->sounds(c.getSource(),false))).then(CommandManager.literal("status").executes(c->reply(c.getSource(),"Звуки мода: "+(engine!=null&&engine.soundsEnabled()?"включены":"выключены")))))
                .then(CommandManager.literal("pause").executes(c->{if(engine==null)return 0;engine.setPaused(true);return reply(c.getSource(),"Система заданий приостановлена.");}))
                .then(CommandManager.literal("resume").executes(c->{if(engine==null)return 0;engine.setPaused(false);return reply(c.getSource(),"Система заданий продолжена.");}))
                .then(CommandManager.literal("list").executes(c->{String ids=QuestCatalog.definitions().stream().map(d->d.quest().id()+" — "+d.title()+" ["+Localization.difficulty(d.quest().difficulty())+"]").reduce((a,b)->a+"; "+b).orElse("пусто");return reply(c.getSource(),"Каталог заданий ("+QuestCatalog.all().size()+"): "+ids);}))
                .then(CommandManager.literal("stats").then(CommandManager.argument("player",StringArgumentType.word()).executes(c->stats(c.getSource(),StringArgumentType.getString(c,"player")))))
                .then(CommandManager.literal("top").executes(c->top(c.getSource())))
                .then(CommandManager.literal("help").executes(c->reply(c.getSource(),"/quests give|cancel|skip|complete <игрок>, sounds on|off|status, pause, resume, list, stats <игрок>, top, event start|stop|top|vote")))
            );
            dispatcher.register(CommandManager.literal("quests").requires(s -> s.hasPermissionLevel(2))
            .then(CommandManager.literal("event")
                .then(CommandManager.literal("start")
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> { for (String name : new String[]{"boss_hunt", "diamond_rush", "building_contest", "last_survivor", "happy_hour"}) b.suggest(name); return b.buildFuture(); })
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(10, 86400))
                            .executes(c -> startEvent(c.getSource(), StringArgumentType.getString(c, "type"), IntegerArgumentType.getInteger(c, "seconds"))))))
                .then(CommandManager.literal("stop").executes(c -> stopEvent(c.getSource())))
                .then(CommandManager.literal("vote")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("points", IntegerArgumentType.integer(1, 100))
                            .executes(c -> voteBuilding(c.getSource(), StringArgumentType.getString(c, "player"), IntegerArgumentType.getInteger(c, "points"))))))
                .then(CommandManager.literal("top").executes(c -> eventTop(c.getSource())))
            ));
        });
    }
    private static void tick(MinecraftServer server){ if(engine==null)return; long now=System.currentTimeMillis();
        eventManager.finishIfExpired(now).ifPresent(outcome -> settleEvent(server, outcome));
        // Last-survivor checkpoints: award only when exactly one non-spectator player remains online.
        if (server.getTicks() % 200 == 0 && eventManager.active(now)
                .map(e -> e.type() == ServerEventManager.Type.LAST_SURVIVOR).orElse(false)) {
            var alive = server.getPlayerManager().getPlayerList().stream().filter(p -> !p.isSpectator()).toList();
            if (alive.size() == 1) {
                ServerPlayerEntity survivor = alive.get(0);
                var award = EventScoringPolicy.award(eventManager, survivor.getUuid(), java.util.UUID.randomUUID(),
                        EventScoringPolicy.Action.SURVIVAL_CHECKPOINT, 1, now);
                if (award.accepted()) survivor.sendMessage(Text.translatable("message.minecraft_quests.survivor_points", award.points()), true);
                saveEvents();
            }
        }
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
            QuestState s=engine.state(p.getUuid());
            if (server.getTicks() % 10 == 0) { syncHud(p, s, now); syncEventHud(p, now); }
            updateInventoryObjective(p, s);
            updateEnvironmentObjective(p, s);
            if(s.hasActiveQuest()&&engine.remainingMs(p.getUuid(),now)==0){
                try {
                    Difficulty failedDifficulty = s.activeDifficulty == null ? Difficulty.EASY : s.activeDifficulty;
                    PunishmentCatalog.Entry punishment = PunishmentSelector.roll(failedDifficulty, new java.util.Random());
                    engine.fail(p.getUuid());
                    PunishmentExecutor.apply(p, punishment);
                    p.sendMessage(Text.translatable("message.minecraft_quests.quest_failed"),false);
                    if(engine.soundsEnabled()) p.playSound(SoundEvents.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
                    server.getPlayerManager().broadcast(Text.translatable("message.minecraft_quests.player_failed", p.getName().getString(), Text.translatable("punishment.minecraft_quests." + punishment.id())), false);
                } catch(Exception ex){ LOGGER.warn("Could not apply timeout penalty for {}", p.getName().getString(), ex); }
            } else if(engine.shouldAssignNext(p.getUuid(),now)){
                try {
                    QuestEngine.Quest q=engine.assign(p.getUuid(),now);
                    initializeObjectiveBaseline(p, q.id());
                    String title=QuestCatalog.find(q.id()).map(QuestCatalog.Definition::title).orElse(q.id());
                    p.sendMessage(Text.translatable("message.minecraft_quests.new_quest", title, Localization.difficulty(q.difficulty())),false);
                    if(engine.soundsEnabled()) p.playSound(SoundEvents.ENTITY_COW_AMBIENT, 1.0f, 1.0f);
                    server.getPlayerManager().broadcast(Text.translatable("message.minecraft_quests.new_quest_broadcast", p.getName().getString(), title, Localization.difficulty(q.difficulty())), false);
                } catch(Exception ignored){}
            }
        }
        // Periodic checkpoint limits progress loss if the server stops unexpectedly.
        if (server.getTicks() % 1200 == 0) save(server);
    }
    private static void syncHud(ServerPlayerEntity player, QuestState state, long now) {
        if (state == null || !state.hasActiveQuest()) {
            ServerPlayNetworking.send(player, new QuestHudPayload("", 0, 0, 0, false));
            return;
        }
        QuestCatalog.Definition definition = QuestCatalog.find(state.activeQuestId).orElse(null);
        ObjectiveRules.Target target = ObjectiveRules.target(state.activeQuestId);
        int needed = target == null ? 1 : target.amount();
        int progress = state.progress.getOrDefault("objective", 0);
        if (target != null && target.kind().equals("survive")) {
            progress = progress / 20;
        }
        if (target != null && target.kind().equals("dimensions")) {
            progress = state.progress.getOrDefault("dim_overworld", 0)
                    + state.progress.getOrDefault("dim_nether", 0)
                    + state.progress.getOrDefault("dim_end", 0);
        }
        String title = definition == null ? state.activeQuestId : definition.title();
        int seconds = (int) Math.ceil(engine.remainingMs(player.getUuid(), now) / 1000.0);
        ServerPlayNetworking.send(player, new QuestHudPayload(title, progress, needed, seconds, true));
    }

    private static void syncEventHud(ServerPlayerEntity player, long now) {
        var active = eventManager.active(now);
        if (active.isEmpty() || active.get().isExpired(now)) {
            ServerPlayNetworking.send(player, new EventHudPayload("", 0, 0, 0, false));
            return;
        }
        var event = active.get();
        var standings = eventManager.standings(now, Integer.MAX_VALUE);
        int score = 0;
        int rank = 0;
        for (var standing : standings) {
            if (standing.playerId().equals(player.getUuid())) {
                score = standing.score();
                rank = standing.rank();
                break;
            }
        }
        String title = Localization.event(event.type());
        int seconds = (int) Math.ceil(event.remainingMs(now) / 1000.0);
        ServerPlayNetworking.send(player, new EventHudPayload(title, score, rank, seconds, true));
    }

    private static void updateInventoryObjective(ServerPlayerEntity player, QuestState state) {
        if (!state.hasActiveQuest()) return;
        ObjectiveRules.Target target = ObjectiveRules.target(state.activeQuestId);
        if (target == null || !target.kind().equals("item")) return;
        net.minecraft.util.Identifier itemId = net.minecraft.util.Identifier.tryParse(target.id());
        if (itemId == null) return;
        net.minecraft.item.Item item = Registries.ITEM.get(itemId);
        int total = 0;
        for (net.minecraft.item.ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) total += stack.getCount();
        }
        int baseline = state.progress.getOrDefault("baseline_item", 0);
        int collected = Math.max(0, total - baseline);
        int previous = state.progress.getOrDefault("objective", 0);
        if (collected > previous) {
            state.progress.put("objective", collected);
            player.sendMessage(Text.translatable("message.minecraft_quests.quest_progress", Math.min(collected, target.amount()), target.amount()), true);
            if (collected >= target.amount()) completeFromEvent(player);
        }
    }

    /** Server-tick objective checks for biomes, dimensions and survival conditions. */
    private static void updateEnvironmentObjective(ServerPlayerEntity player, QuestState state) {
        if (!state.hasActiveQuest()) return;
        ObjectiveRules.Target target = ObjectiveRules.target(state.activeQuestId);
        if (target == null) return;
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        String biome = player.getWorld().getBiome(player.getBlockPos()).getKey()
                .map(key -> key.getValue().toString()).orElse("");
        if (target.kind().equals("biome")) {
            if (target.id().equals(biome)) completeFromEvent(player);
            return;
        }
        if (target.kind().equals("dimensions")) {
            if (dimension.equals("minecraft:overworld")) state.progress.put("dim_overworld", 1);
            if (dimension.equals("minecraft:the_nether")) state.progress.put("dim_nether", 1);
            if (dimension.equals("minecraft:the_end")) state.progress.put("dim_end", 1);
            if (state.progress.containsKey("dim_overworld") && state.progress.containsKey("dim_nether") && state.progress.containsKey("dim_end")) completeFromEvent(player);
            return;
        }
        if (!target.kind().equals("survive")) return;
        boolean condition = switch (target.id()) {
            case "underwater" -> player.isSubmergedInWater();
            case "nether" -> dimension.equals("minecraft:the_nether");
            case "end" -> dimension.equals("minecraft:the_end");
            case "height_200" -> player.getY() >= 200.0;
            case "no_armor" -> hasNoArmor(player);
            case "no_food" -> player.getHungerManager().getFoodLevel() <= 6;
            case "desert" -> biome.contains("desert");
            case "ocean" -> biome.contains("ocean");
            case "deep_dark" -> biome.equals("minecraft:deep_dark");
            default -> false;
        };
        if (condition) {
            int ticks = state.progress.merge("objective", 1, Integer::sum);
            if (ticks >= target.amount() * 20) completeFromEvent(player);
        }
    }

    private static boolean hasNoArmor(ServerPlayerEntity player) {
        for (net.minecraft.item.ItemStack stack : player.getArmorItems()) if (!stack.isEmpty()) return false;
        return true;
    }

    private static void completeFromEvent(ServerPlayerEntity player) {
        try {
            // Validate and commit completion before issuing items: no reward exploit without an active quest.
            var plan = RewardIssuer.createPlan();
            engine.complete(player.getUuid(), System.currentTimeMillis(), plan.diamonds());
            var eventAward = EventScoringPolicy.award(eventManager, player.getUuid(), java.util.UUID.randomUUID(),
                    EventScoringPolicy.Action.QUEST_COMPLETED_DURING_HAPPY_HOUR, 1, System.currentTimeMillis());
            if (eventAward.accepted()) { player.sendMessage(Text.translatable("message.minecraft_quests.happy_hour_points", eventAward.points()), true); saveEvents(); }
            RewardIssuer.issue(player, plan);
            player.sendMessage(Text.translatable("message.minecraft_quests.quest_completed"), false);
            if (engine.soundsEnabled()) player.playSound(net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } catch (Exception ex) { LOGGER.warn("Could not complete quest for {}", player.getName().getString(), ex); }
    }

    private static void initializeObjectiveBaseline(ServerPlayerEntity player, String questId) {
        ObjectiveRules.Target target = ObjectiveRules.target(questId);
        if (target == null || !target.kind().equals("item")) return;
        net.minecraft.util.Identifier itemId = net.minecraft.util.Identifier.tryParse(target.id());
        if (itemId == null) return;
        net.minecraft.item.Item item = Registries.ITEM.get(itemId);
        int total = 0;
        for (net.minecraft.item.ItemStack stack : player.getInventory().main) if (stack.isOf(item)) total += stack.getCount();
        engine.state(player.getUuid()).progress.put("baseline_item", total);
    }
    private static void recordObjective(ServerPlayerEntity player, String kind, String registryId) {
        QuestState state = engine.state(player.getUuid());
        if (!state.hasActiveQuest()) return;
        ObjectiveRules.Target target = ObjectiveRules.target(state.activeQuestId);
        if (target == null || !target.kind().equals(kind) || !target.id().equals(registryId)) return;
        int count = state.progress.merge("objective", 1, Integer::sum);
        player.sendMessage(Text.translatable("message.minecraft_quests.quest_progress", count, target.amount()), true);
        if (count >= target.amount()) completeFromEvent(player);
    }
    private static ServerPlayerEntity player(ServerCommandSource s,String name){return s.getServer().getPlayerManager().getPlayer(name);}
    private static int give(ServerCommandSource s,String name,String requestedTier){ServerPlayerEntity p=player(s,name);if(p==null)return reply(s,"Игрок не найден или не в сети.");try{QuestEngine.Quest q;if(requestedTier==null)q=engine.assign(p.getUuid(),System.currentTimeMillis());else{Difficulty d=switch(requestedTier.toLowerCase(java.util.Locale.ROOT)){case "easy"->Difficulty.EASY;case "medium"->Difficulty.MEDIUM;case "hard"->Difficulty.HARD;default->throw new IllegalArgumentException("Используй easy, medium или hard.");};q=engine.assign(p.getUuid(),System.currentTimeMillis(),d);}initializeObjectiveBaseline(p,q.id());String title=QuestCatalog.find(q.id()).map(QuestCatalog.Definition::title).orElse(q.id());p.sendMessage(Text.translatable("message.minecraft_quests.given", title, Localization.difficulty(q.difficulty())),false);if(engine.soundsEnabled())p.playSound(SoundEvents.ENTITY_COW_AMBIENT,1.0f,1.0f);s.getServer().getPlayerManager().broadcast(Text.literal("Задание для "+name+": "+title+" ["+Localization.difficulty(q.difficulty())+"]"),false);return reply(s,"Задание выдано "+name+": "+title);}catch(Exception e){return reply(s,"Не удалось выдать задание: "+e.getMessage());}}
    private static int cancel(ServerCommandSource s,String n){ServerPlayerEntity p=player(s,n);if(p==null)return reply(s,"Игрок не найден.");engine.cancel(p.getUuid());return reply(s,"Задание игрока отменено.");}
    private static int skip(ServerCommandSource s,String n){ServerPlayerEntity p=player(s,n);if(p==null)return reply(s,"Игрок не найден.");try{engine.skip(p.getUuid());QuestEngine.Quest q=engine.assign(p.getUuid(),System.currentTimeMillis());initializeObjectiveBaseline(p,q.id());String title=QuestCatalog.find(q.id()).map(QuestCatalog.Definition::title).orElse(q.id());p.sendMessage(Text.translatable("message.minecraft_quests.skipped", title),false);if(engine.soundsEnabled())p.playSound(SoundEvents.ENTITY_COW_AMBIENT,1.0f,1.0f);return reply(s,"Задание пропущено; выдано новое.");}catch(Exception e){return reply(s,"Пропуск невозможен: "+e.getMessage());}}
    private static int complete(ServerCommandSource s,String n){ServerPlayerEntity p=player(s,n);if(p==null)return reply(s,"Игрок не найден.");try{var plan=RewardIssuer.createPlan();long next=engine.complete(p.getUuid(),System.currentTimeMillis(),plan.diamonds());RewardIssuer.issue(p,plan);return reply(s,"Задание отмечено выполненным; выдана награда ("+plan.diamonds()+" алмазов). Следующее через "+((next-System.currentTimeMillis())/1000)+" сек.");}catch(Exception e){return reply(s,"Нет активного задания.");}}
    private static int sounds(ServerCommandSource s,boolean on){engine.setSoundsEnabled(on);return reply(s,"Звуки мода "+(on?"включены":"выключены")+".");}
    private static int stats(ServerCommandSource s,String n){ServerPlayerEntity p=player(s,n);if(p==null)return reply(s,"Игрок не найден.");QuestState q=engine.state(p.getUuid());return reply(s,n+": победы "+q.completed+", поражения "+q.failed+", пропуски "+q.skipped+", серия "+q.winStreak+", алмазы "+q.diamondsEarned);}
    private static int top(ServerCommandSource s){String result=engine.allStates().stream().sorted(Comparator.comparingInt((QuestState q)->q.completed).reversed()).limit(10).map(q->q.playerId.toString().substring(0,8)+" — "+q.completed).reduce((a,b)->a+" | "+b).orElse("Пока нет результатов.");return reply(s,"Топ игроков: "+result);}
    private static void settleEvent(MinecraftServer server, ServerEventManager.EventOutcome outcome) {
        var settlement = EventSettlementPlanner.plan(outcome, new java.util.Random());
        server.getPlayerManager().broadcast(Text.literal(settlement.announcement()), false);
        java.util.UUID eventId = java.util.UUID.nameUUIDFromBytes((outcome.type().name() + ":" + outcome.startedAt() + ":" + outcome.finishedAt())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (var reward : settlement.rewards()) {
            java.util.UUID rewardId = java.util.UUID.nameUUIDFromBytes((eventId + ":" + reward.playerId())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var pending = new PendingEventRewardQueue.PendingReward(rewardId, eventId,
                    reward.bonusDiamonds(), reward.itemReward());
            pendingEventRewards.enqueue(reward.playerId(), pending);
        }
        savePendingEventRewards();
        for (var reward : settlement.rewards()) {
            ServerPlayerEntity winner = server.getPlayerManager().getPlayer(reward.playerId());
            if (winner != null) deliverPendingEventRewards(winner);
            else LOGGER.info("Queued event reward for offline winner {}", reward.playerId());
        }
        saveEvents();
    }

    /** Deliver queued rewards on join and acknowledge only after the item adapter succeeds. */
    private static void deliverPendingEventRewards(ServerPlayerEntity player) {
        for (var pending : pendingEventRewards.pending(player.getUuid())) {
            try {
                RewardIssuer.issueEventReward(player, pending.bonusDiamonds(), pending.itemReward());
                pendingEventRewards.acknowledge(player.getUuid(), pending.rewardId());
                savePendingEventRewards();
            } catch (RuntimeException ex) {
                LOGGER.error("Could not deliver queued event reward to {}", player.getName().getString(), ex);
                break;
            }
        }
    }

    private static void savePendingEventRewards() {
        if (pendingRewardsFile == null) return;
        try { pendingEventRewards.save(pendingRewardsFile); }
        catch (Exception e) { LOGGER.error("Could not save pending event rewards", e); }
    }

    private static int startEvent(ServerCommandSource source, String requestedType, int seconds) {
        ServerEventManager.Type type = switch (requestedType.toLowerCase(java.util.Locale.ROOT)) {
            case "boss_hunt" -> ServerEventManager.Type.BOSS_HUNT;
            case "diamond_rush" -> ServerEventManager.Type.DIAMOND_RUSH;
            case "building_contest" -> ServerEventManager.Type.BUILDING_CONTEST;
            case "last_survivor" -> ServerEventManager.Type.LAST_SURVIVOR;
            case "happy_hour" -> ServerEventManager.Type.HAPPY_HOUR;
            default -> null;
        };
        if (type == null) return reply(source, "Неизвестное событие. Используй boss_hunt, diamond_rush, building_contest, last_survivor или happy_hour.");
        try {
            var active = eventManager.start(type, System.currentTimeMillis(), seconds * 1000L);
            saveEvents();
            source.getServer().getPlayerManager().broadcast(Text.translatable("message.minecraft_quests.event_started", Localization.event(type), seconds), false);
            return reply(source, "Событие запущено.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return reply(source, "Не удалось запустить событие: " + e.getMessage());
        }
    }
    private static int stopEvent(ServerCommandSource source) {
        var stopped = eventManager.stop();
        if (stopped.isEmpty()) return reply(source, "Активного события нет.");
        saveEvents();
        source.getServer().getPlayerManager().broadcast(Text.translatable("message.minecraft_quests.event_stopped", Localization.event(stopped.get().type())), false);
        return reply(source, "Событие остановлено без выдачи наград.");
    }
    private static int eventTop(ServerCommandSource source) {
        var standings = eventManager.standings(System.currentTimeMillis(), 10);
        if (standings.isEmpty()) return reply(source, "Активное событие отсутствует или очков пока нет.");
        String rows = standings.stream().map(row -> row.rank() + ". " + row.playerId().toString().substring(0, 8) + " — " + row.score())
                .reduce((a, b) -> a + " | " + b).orElse("");
        return reply(source, "Рейтинг события: " + rows);
    }
    private static int voteBuilding(ServerCommandSource source, String playerName, int points) {
        ServerPlayerEntity target = player(source, playerName);
        if (target == null) return reply(source, "Игрок не найден или не в сети.");
        var award = EventScoringPolicy.award(eventManager, target.getUuid(), java.util.UUID.randomUUID(),
                EventScoringPolicy.Action.BUILD_VOTE_RECEIVED, points, System.currentTimeMillis());
        if (!award.accepted()) return reply(source, "Голоса принимаются только во время события building_contest.");
        saveEvents();
        target.sendMessage(Text.translatable("message.minecraft_quests.build_vote", award.points()), true);
        return reply(source, "Начислено " + award.points() + " очков игроку " + playerName + ".");
    }
    private static void saveEvents() {
        if (eventSaveFile == null) return;
        try { eventManager.save(eventSaveFile); }
        catch (Exception e) { LOGGER.error("Could not save server event data", e); }
    }
    private static int reply(ServerCommandSource s,String m){s.sendFeedback(()->Text.literal(m),false);return Command.SINGLE_SUCCESS;}
    private static void save(MinecraftServer server){
        eventManager.finishIfExpired(System.currentTimeMillis()).ifPresent(outcome -> settleEvent(server, outcome));
        if(engine!=null&&saveFile!=null)try{engine.save(saveFile);}catch(Exception e){LOGGER.error("Could not save quest data",e);}
        saveEvents();
        savePendingEventRewards();
    }
}
