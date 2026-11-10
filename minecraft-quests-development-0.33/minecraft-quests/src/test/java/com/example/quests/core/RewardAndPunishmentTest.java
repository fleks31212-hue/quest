package com.example.quests.core;
import java.util.Random;
public final class RewardAndPunishmentTest {
 public static void main(String[] args) {
  for(int seed=0;seed<500;seed++) { var r=RewardPlanner.roll(new Random(seed)); if(r.diamonds()<1||r.diamonds()>20||r.enchantments().isEmpty()||r.enchantments().size()>3) throw new AssertionError("bad reward roll");
   if(r.enchantedItem().equals("GOLDEN_APPLE")) throw new AssertionError("non-enchantable bonus item");
   if(r.enchantedItem().equals("CROSSBOW") && r.enchantments().contains("MULTISHOT") && r.enchantments().contains("PIERCING")) throw new AssertionError("incompatible crossbow enchants");
   if(r.enchantedItem().equals("DIAMOND_PICKAXE") && r.enchantments().contains("FORTUNE") && r.enchantments().contains("SILK_TOUCH")) throw new AssertionError("incompatible pickaxe enchants"); }
  if(PunishmentCatalog.byTier(PunishmentCatalog.Tier.EASY).size()!=10) throw new AssertionError("easy penalties");
  if(PunishmentCatalog.byTier(PunishmentCatalog.Tier.MEDIUM).size()!=19) throw new AssertionError("medium penalties");
  if(PunishmentCatalog.byTier(PunishmentCatalog.Tier.HARD).size()!=20) throw new AssertionError("hard penalties");
  if(PunishmentCatalog.byTier(PunishmentCatalog.Tier.CHAOS).size()!=20) throw new AssertionError("chaos penalties");
  long unique= PunishmentCatalog.all().stream().map(PunishmentCatalog.Entry::id).distinct().count();
  if(unique!=PunishmentCatalog.all().size()) throw new AssertionError("duplicate ids");
  System.out.println("RewardAndPunishmentTest OK; entries="+PunishmentCatalog.all().size());
 }
}
