package com.example.quests.core;

import java.nio.file.*;
import java.util.*;

public final class PendingEventRewardQueueTest {
    public static void main(String[] args) throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID event = UUID.fromString("00000000-0000-0000-0000-000000000102");
        UUID rewardId = UUID.fromString("00000000-0000-0000-0000-000000000103");
        var plan = new RewardPlanner.RewardPlan(0, RewardPlanner.Armor.IRON,
                RewardPlanner.Tool.DIAMOND_PICKAXE, "BOW", List.of("POWER", "UNBREAKING"));
        var reward = new PendingEventRewardQueue.PendingReward(rewardId, event, 12, plan);
        PendingEventRewardQueue queue = new PendingEventRewardQueue();
        assert queue.enqueue(player, reward);
        assert !queue.enqueue(player, reward); // same settlement replay cannot duplicate
        assert queue.pendingCount() == 1;

        Path dir = Files.createTempDirectory("pending-event-rewards");
        Path file = dir.resolve("pending.properties");
        queue.save(file);
        PendingEventRewardQueue restored = PendingEventRewardQueue.load(file);
        assert restored.pendingCount() == 1;
        var loaded = restored.pending(player).get(0);
        assert loaded.rewardId().equals(rewardId);
        assert loaded.eventId().equals(event);
        assert loaded.bonusDiamonds() == 12;
        assert loaded.itemReward().armor() == RewardPlanner.Armor.IRON;
        assert loaded.itemReward().tool() == RewardPlanner.Tool.DIAMOND_PICKAXE;
        assert loaded.itemReward().enchantedItem().equals("BOW");
        assert loaded.itemReward().enchantments().equals(List.of("POWER", "UNBREAKING"));

        assert !restored.acknowledge(player, UUID.randomUUID());
        assert restored.acknowledge(player, rewardId);
        assert restored.pendingCount() == 0;
        restored.save(file);
        PendingEventRewardQueue afterDelivery = PendingEventRewardQueue.load(file);
        assert afterDelivery.pendingCount() == 0;
        assert !afterDelivery.enqueue(player, reward); // delivered ID remains idempotent after restart

        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        System.out.println("Pending event reward queue tests: OK");
    }
}
