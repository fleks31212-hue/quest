package com.example.quests.core;
import java.util.*;
import java.nio.file.*;
public final class LeaderboardAndEventsTest {
 public static void main(String[] args) {
  QuestState a=new QuestState(UUID.fromString("00000000-0000-0000-0000-000000000001")); a.completed=8; a.diamondsEarned=40; a.bestStreak=5;
  QuestState b=new QuestState(UUID.fromString("00000000-0000-0000-0000-000000000002")); b.completed=3; b.diamondsEarned=90; b.bestStreak=3;
  assert Leaderboard.top(List.of(a,b), Leaderboard.Metric.WINS, 1).get(0).playerId().equals(a.playerId);
  assert Leaderboard.top(List.of(a,b), Leaderboard.Metric.DIAMONDS, 1).get(0).playerId().equals(b.playerId);
  assert Leaderboard.top(List.of(a,b), Leaderboard.Metric.BEST_STREAK, 10).size()==2;
  ServerEventManager events=new ServerEventManager();
  var event=events.start(ServerEventManager.Type.DIAMOND_RUSH, 1000, 5000);
  assert event.remainingMs(2000)==4000;
  boolean rejected=false; try { events.start(ServerEventManager.Type.HAPPY_HOUR, 2000, 5000); } catch(IllegalStateException expected) { rejected=true; }
  assert rejected;
  UUID p1=UUID.fromString("00000000-0000-0000-0000-000000000011"), p2=UUID.fromString("00000000-0000-0000-0000-000000000012");
  assert events.contribute(p1, 3, 2000); assert events.contribute(p2, 7, 2000); assert !events.contribute(p1, -2, 2000);
  var standings=events.standings(2000, 10); assert standings.size()==2 && standings.get(0).playerId().equals(p2) && standings.get(0).rank()==1;
  assert !events.tick(5999); assert events.tick(6000); assert events.active(6000).isEmpty();
  events.start(ServerEventManager.Type.BUILDING_CONTEST, 7000, 3000);
  UUID p3=UUID.fromString("00000000-0000-0000-0000-000000000013");
  assert events.contribute(p1, 10, 7100); assert events.contribute(p3, 10, 7100); assert events.contribute(p2, 4, 7100);
  var outcome=events.finish(8000).orElseThrow();
  assert outcome.participantCount()==3 && outcome.winningScore()==10;
  assert outcome.winners().size()==2 && outcome.winners().containsAll(List.of(p1,p3));
  assert events.active(8000).isEmpty() && events.finish(8001).isEmpty();
  var settlement = EventSettlementPlanner.plan(outcome, new Random(42));
  assert settlement.rewards().size() == 2; // tied first place: both players are rewarded
  assert settlement.rewards().stream().allMatch(r -> r.rank() == 1 && r.bonusDiamonds() >= 5 && r.bonusDiamonds() <= 20);
  assert settlement.announcement().contains("ничья");
  var emptyOutcome = new ServerEventManager.EventOutcome(ServerEventManager.Type.HAPPY_HOUR, 1, 2, List.of(), List.of());
  var emptySettlement = EventSettlementPlanner.plan(emptyOutcome, new Random(1));
  assert emptySettlement.rewards().isEmpty() && emptySettlement.announcement().contains("без участников");
  // Event scoring policy accepts only the action associated with the active event.
  events.start(ServerEventManager.Type.DIAMOND_RUSH, 11000, 5000);
  UUID action1 = UUID.fromString("00000000-0000-0000-0000-000000000021");
  UUID action2 = UUID.fromString("00000000-0000-0000-0000-000000000022");
  UUID action3 = UUID.fromString("00000000-0000-0000-0000-000000000023");
  var validAward = EventScoringPolicy.award(events, p1, action1, EventScoringPolicy.Action.DIAMOND_ORE_MINED, 2, 11100);
  assert validAward.accepted() && validAward.points() == 4;
  var duplicateAward = EventScoringPolicy.award(events, p1, action1, EventScoringPolicy.Action.DIAMOND_ORE_MINED, 2, 11100);
  assert !duplicateAward.accepted(); // duplicate callback is idempotently rejected
  var wrongAction = EventScoringPolicy.award(events, p1, action2, EventScoringPolicy.Action.BOSS_DAMAGE, 50, 11100);
  assert !wrongAction.accepted() && wrongAction.points() == 0;
  var cappedAward = EventScoringPolicy.award(events, p2, action3, EventScoringPolicy.Action.DIAMOND_ORE_MINED, 5000, 11100);
  assert cappedAward.accepted() && cappedAward.points() == 2000;
  assert events.standings(11100, 10).get(0).playerId().equals(p2);
  // Active event state and participant scores survive a restart.
  try {
   Path saved = Files.createTempDirectory("quests-event-test").resolve("event.properties");
   events.save(saved);
   ServerEventManager restored = ServerEventManager.load(saved, 12000);
   assert restored.active(12000).orElseThrow().type() == ServerEventManager.Type.DIAMOND_RUSH;
   assert restored.standings(12000, 10).get(0).playerId().equals(p2);
   assert !restored.contributeOnce(p1, action1, 4, 12000); // dedupe survives restart
   assert restored.contributeOnce(p1, UUID.fromString("00000000-0000-0000-0000-000000000024"), 4, 12000);
   assert ServerEventManager.load(saved, 20000).active(20000).isEmpty();
   Files.deleteIfExists(saved);
   Files.deleteIfExists(saved.getParent());
  } catch (java.io.IOException ex) { throw new AssertionError(ex); }
  // Every event type accepts only its matching verified action.
  events.stop();
  events.start(ServerEventManager.Type.BOSS_HUNT, 21000, 5000);
  assert EventScoringPolicy.award(events, p1, UUID.randomUUID(), EventScoringPolicy.Action.BOSS_DEFEATED, 1, 21100).points() == 100;
  assert !EventScoringPolicy.award(events, p1, UUID.randomUUID(), EventScoringPolicy.Action.DIAMOND_ORE_MINED, 1, 21100).accepted();
  events.stop();
  events.start(ServerEventManager.Type.BUILDING_CONTEST, 22000, 5000);
  assert EventScoringPolicy.award(events, p1, UUID.randomUUID(), EventScoringPolicy.Action.BUILD_VOTE_RECEIVED, 17, 22100).points() == 17;
  events.stop();
  events.start(ServerEventManager.Type.LAST_SURVIVOR, 23000, 5000);
  assert EventScoringPolicy.award(events, p1, UUID.randomUUID(), EventScoringPolicy.Action.SURVIVAL_CHECKPOINT, 1, 23100).points() == 5;
  events.stop();
  events.start(ServerEventManager.Type.HAPPY_HOUR, 24000, 5000);
  assert EventScoringPolicy.award(events, p1, UUID.randomUUID(), EventScoringPolicy.Action.QUEST_COMPLETED_DURING_HAPPY_HOUR, 1, 24100).points() == 3;
  events.stop();
  events.start(ServerEventManager.Type.HAPPY_HOUR, 9000, 1000);
  assert events.finish(10000).isEmpty(); // manual early-settlement API refuses an already expired event
  var expiryManager = new ServerEventManager();
  expiryManager.start(ServerEventManager.Type.DIAMOND_RUSH, 30000, 1000);
  assert expiryManager.contribute(p1, 12, 30500);
  assert expiryManager.finishIfExpired(30999).isEmpty();
  var expiredOutcome = expiryManager.finishIfExpired(31000).orElseThrow();
  assert expiredOutcome.winners().equals(List.of(p1)) && expiredOutcome.winningScore() == 12;
  assert expiryManager.finishIfExpired(31001).isEmpty(); // scheduled settlement can only happen once
  System.out.println("Leaderboard, event scoring and settlement tests: OK");
 }
}
