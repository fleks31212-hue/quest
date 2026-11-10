package com.example.quests.core;
import java.util.*;
/** Run with assertions enabled after compiling core + this file. */
public final class QuestEngineSmokeTest {
  public static void main(String[] args) {
    var id=UUID.randomUUID();
    var engine=new QuestEngine((player, allowed)->new QuestEngine.Quest("test", allowed.iterator().next(), 60_000), new Random(7));
    assert engine.assign(id, 1_000).difficulty()==Difficulty.EASY;
    engine.skip(id); assert engine.state(id).skipped==1; assert engine.state(id).failed==0;
    assert engine.state(id).completed==0; assert engine.state(id).winStreak==0;
    engine.assign(id, 2_000); long next=engine.complete(id, 3_000, 7);
    assert engine.state(id).completed==1; assert engine.state(id).diamondsEarned==7;
    assert next>=13_000 && next<=123_000;
    assert Difficulty.availableAt(4).equals(Set.of(Difficulty.EASY));
    assert Difficulty.availableAt(5).contains(Difficulty.MEDIUM);
    assert Difficulty.availableAt(30).equals(Set.of(Difficulty.HARD));
    System.out.println("QuestEngine smoke test: OK");
  }
}
