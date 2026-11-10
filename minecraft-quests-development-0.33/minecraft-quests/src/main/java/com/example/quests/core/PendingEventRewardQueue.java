package com.example.quests.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Persistent offline-delivery queue for already rolled event rewards. */
public final class PendingEventRewardQueue {
    public record PendingReward(UUID rewardId, UUID eventId, int bonusDiamonds,
                                RewardPlanner.RewardPlan itemReward) {
        public PendingReward {
            Objects.requireNonNull(rewardId);
            Objects.requireNonNull(eventId);
            Objects.requireNonNull(itemReward);
            if (bonusDiamonds < 5 || bonusDiamonds > 20) throw new IllegalArgumentException("Event bonus must be 5..20");
        }
    }

    private final Map<UUID, List<PendingReward>> byPlayer = new HashMap<>();
    private final Set<UUID> knownRewardIds = new HashSet<>();
    private final Set<UUID> deliveredRewardIds = new HashSet<>();

    /** Idempotent by reward ID, so replaying settlement cannot queue the same payout twice. */
    public boolean enqueue(UUID playerId, PendingReward reward) {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(reward);
        if (deliveredRewardIds.contains(reward.rewardId()) || !knownRewardIds.add(reward.rewardId())) return false;
        byPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(reward);
        return true;
    }

    public List<PendingReward> pending(UUID playerId) {
        return List.copyOf(byPlayer.getOrDefault(playerId, List.of()));
    }

    /** Remove only after the adapter reports successful delivery. */
    public boolean acknowledge(UUID playerId, UUID rewardId) {
        List<PendingReward> rewards = byPlayer.get(playerId);
        if (rewards == null) return false;
        boolean removed = rewards.removeIf(reward -> reward.rewardId().equals(rewardId));
        if (removed) { knownRewardIds.remove(rewardId); deliveredRewardIds.add(rewardId); }
        if (rewards.isEmpty()) byPlayer.remove(playerId);
        return removed;
    }

    public int pendingCount() { return knownRewardIds.size(); }

    public void save(Path file) throws IOException {
        Properties p = new Properties();
        p.setProperty("version", "2");
        int count = 0;
        for (var playerEntry : byPlayer.entrySet()) {
            for (PendingReward reward : playerEntry.getValue()) {
                String prefix = "reward." + count++ + ".";
                p.setProperty(prefix + "player", playerEntry.getKey().toString());
                p.setProperty(prefix + "id", reward.rewardId().toString());
                p.setProperty(prefix + "event", reward.eventId().toString());
                p.setProperty(prefix + "diamonds", Integer.toString(reward.bonusDiamonds()));
                RewardPlanner.RewardPlan plan = reward.itemReward();
                p.setProperty(prefix + "armor", plan.armor().name());
                p.setProperty(prefix + "tool", plan.tool().name());
                p.setProperty(prefix + "item", plan.enchantedItem());
                p.setProperty(prefix + "enchantments", String.join(",", plan.enchantments()));
            }
        }
        p.setProperty("count", Integer.toString(count));
        p.setProperty("deliveredCount", Integer.toString(deliveredRewardIds.size()));
        int deliveredIndex = 0;
        for (UUID id : deliveredRewardIds) p.setProperty("delivered." + deliveredIndex++, id.toString());
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Minecraft Quests pending event rewards"); }
        try { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
    }

    public static PendingEventRewardQueue load(Path file) throws IOException {
        PendingEventRewardQueue queue = new PendingEventRewardQueue();
        if (!Files.exists(file)) return queue;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        String version = p.getProperty("version");
        if (!"1".equals(version) && !"2".equals(version)) return queue;
        try {
            int count = Integer.parseInt(p.getProperty("count", "0"));
            if (count < 0 || count > 100_000) return queue;
            for (int i = 0; i < count; i++) {
                String prefix = "reward." + i + ".";
                UUID player = UUID.fromString(p.getProperty(prefix + "player"));
                UUID id = UUID.fromString(p.getProperty(prefix + "id"));
                UUID event = UUID.fromString(p.getProperty(prefix + "event"));
                int diamonds = Integer.parseInt(p.getProperty(prefix + "diamonds"));
                RewardPlanner.Armor armor = RewardPlanner.Armor.valueOf(p.getProperty(prefix + "armor"));
                RewardPlanner.Tool tool = RewardPlanner.Tool.valueOf(p.getProperty(prefix + "tool"));
                String item = p.getProperty(prefix + "item");
                String rawEnchants = p.getProperty(prefix + "enchantments", "");
                List<String> enchants = rawEnchants.isBlank() ? List.of() : Arrays.asList(rawEnchants.split(",", -1));
                PendingReward reward = new PendingReward(id, event, diamonds,
                        new RewardPlanner.RewardPlan(0, armor, tool, item, enchants));
                if (!queue.enqueue(player, reward)) return new PendingEventRewardQueue();
            }
            if ("2".equals(version)) {
                int deliveredCount = Integer.parseInt(p.getProperty("deliveredCount", "0"));
                if (deliveredCount < 0 || deliveredCount > 1_000_000) return new PendingEventRewardQueue();
                for (int i = 0; i < deliveredCount; i++) {
                    UUID id = UUID.fromString(p.getProperty("delivered." + i));
                    if (queue.knownRewardIds.contains(id) || !queue.deliveredRewardIds.add(id)) return new PendingEventRewardQueue();
                }
            }
            return queue;
        } catch (RuntimeException malformed) {
            return new PendingEventRewardQueue();
        }
    }
}
