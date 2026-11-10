package com.example.quests.core;

import java.util.*;

/** Curated 100-quest pool. This catalog supplies metadata; in-world objective verification is adapter work. */
public final class QuestCatalog {
    public record Definition(QuestEngine.Quest quest, String title, String category) {}
    private static final List<Definition> DEFINITIONS = List.of(
        q("mine_coal_32", "Добудь 32 угля", "mine", Difficulty.EASY, 600),
        q("mine_iron_16", "Добудь 16 железной руды", "mine", Difficulty.EASY, 900),
        q("mine_copper_32", "Добудь 32 медной руды", "mine", Difficulty.EASY, 900),
        q("mine_redstone_16", "Добудь 16 редстоуна", "mine", Difficulty.MEDIUM, 1200),
        q("mine_lapis_8", "Добудь 8 лазурита", "mine", Difficulty.EASY, 900),
        q("mine_diamonds_8", "Добудь 8 алмазов", "mine", Difficulty.HARD, 2400),
        q("mine_gold_32", "Добудь 32 золотой руды", "mine", Difficulty.MEDIUM, 1500),
        q("mine_obsidian_16", "Добудь 16 обсидиана", "mine", Difficulty.MEDIUM, 1800),
        q("mine_quartz_16", "Добудь 16 кварца", "mine", Difficulty.MEDIUM, 1500),
        q("mine_emerald_16", "Получи 16 изумрудов из руды", "mine", Difficulty.HARD, 2400),
        q("mine_debris_32", "Добудь 32 древних обломка", "mine", Difficulty.HARD, 3600),
        q("mine_diamonds_32", "Добудь 32 алмаза", "mine", Difficulty.HARD, 3600),
        q("mine_nether_gold_64", "Добудь 64 незерской золотой руды", "mine", Difficulty.MEDIUM, 1800),
        q("mine_iron_64", "Добудь 64 железной руды", "mine", Difficulty.MEDIUM, 1800),
        q("mine_emerald_ore_16", "Добудь 16 изумрудной руды", "mine", Difficulty.HARD, 2400),
        q("build_tower_10", "Построй башню высотой 10 блоков", "build", Difficulty.EASY, 900),
        q("build_bridge_15", "Построй мост длиной 15 блоков", "build", Difficulty.EASY, 900),
        q("build_wood_house", "Построй деревянный дом", "build", Difficulty.EASY, 1200),
        q("build_farm_7", "Построй ферму 7×7", "build", Difficulty.EASY, 1200),
        q("build_room_windows", "Построй комнату с 4 окнами", "build", Difficulty.EASY, 1200),
        q("build_castle_200", "Построй каменный замок из 200 блоков", "build", Difficulty.MEDIUM, 2400),
        q("build_bridge_50", "Построй мост длиной 50 блоков", "build", Difficulty.MEDIUM, 1800),
        q("build_auto_farm", "Построй автоматическую ферму", "build", Difficulty.HARD, 3600),
        q("build_beacon", "Построй маяк", "build", Difficulty.HARD, 3600),
        q("build_underwater_room", "Построй подводную комнату", "build", Difficulty.MEDIUM, 2400),
        q("build_house_15", "Построй дом размером 15×15", "build", Difficulty.MEDIUM, 2400),
        q("build_tower_50", "Построй башню высотой 50 блоков", "build", Difficulty.HARD, 3600),
        q("build_nether_portal", "Укрась портал в Незер 50 блоками", "build", Difficulty.MEDIUM, 2400),
        q("build_storage", "Создай хранилище с 20 сундуками", "build", Difficulty.MEDIUM, 2400),
        q("build_arena", "Построй арену 25×25", "build", Difficulty.HARD, 3600),
        q("hunt_zombies_10", "Победи 10 зомби", "hunt", Difficulty.EASY, 900),
        q("hunt_skeletons_10", "Победи 10 скелетов", "hunt", Difficulty.EASY, 900),
        q("hunt_spiders_5", "Победи 5 пауков", "hunt", Difficulty.EASY, 900),
        q("hunt_creepers_5", "Победи 5 криперов", "hunt", Difficulty.MEDIUM, 1200),
        q("hunt_drowned_5", "Победи 5 утопленников", "hunt", Difficulty.MEDIUM, 1200),
        q("hunt_endermen_10", "Победи 10 эндерменов", "hunt", Difficulty.HARD, 2400),
        q("hunt_witches_10", "Победи 10 ведьм", "hunt", Difficulty.HARD, 2400),
        q("hunt_piglins_15", "Победи 15 пиглинов", "hunt", Difficulty.MEDIUM, 1800),
        q("hunt_ghasts_5", "Победи 5 гастов", "hunt", Difficulty.HARD, 2400),
        q("hunt_ravagers_3", "Победи 3 разорителей", "hunt", Difficulty.HARD, 3000),
        q("hunt_wither", "Победи иссушителя", "hunt", Difficulty.HARD, 3600),
        q("hunt_dragon", "Победи дракона Края", "hunt", Difficulty.HARD, 5400),
        q("hunt_guardians_2", "Победи 2 стражей", "hunt", Difficulty.MEDIUM, 1800),
        q("hunt_blazes_20", "Победи 20 ифритов", "hunt", Difficulty.HARD, 2400),
        q("hunt_zombies_30", "Победи 30 зомби за 5 минут", "hunt", Difficulty.MEDIUM, 300),
        q("explore_village", "Найди деревню", "explore", Difficulty.EASY, 1200),
        q("explore_desert_well", "Найди пустынный колодец", "explore", Difficulty.MEDIUM, 2400),
        q("explore_jungle", "Найди джунгли", "explore", Difficulty.EASY, 1800),
        q("explore_lush_cave", "Найди пышную пещеру", "explore", Difficulty.MEDIUM, 1800),
        q("explore_ruined_portal", "Найди разрушенный портал", "explore", Difficulty.EASY, 1200),
        q("explore_mineshaft", "Найди заброшенную шахту", "explore", Difficulty.MEDIUM, 1800),
        q("explore_fortress", "Найди крепость Незера", "explore", Difficulty.MEDIUM, 2400),
        q("explore_monument", "Найди океанический монумент", "explore", Difficulty.HARD, 3600),
        q("explore_mansion", "Найди лесной особняк", "explore", Difficulty.HARD, 5400),
        q("explore_outpost", "Найди аванпост разбойников", "explore", Difficulty.MEDIUM, 2400),
        q("explore_ancient_city", "Найди древний город", "explore", Difficulty.HARD, 3600),
        q("explore_stronghold", "Найди крепость", "explore", Difficulty.HARD, 3600),
        q("explore_end_city", "Найди город Края", "explore", Difficulty.HARD, 3600),
        q("explore_bastion", "Найди бастион", "explore", Difficulty.HARD, 3000),
        q("explore_dimensions", "Посети все три измерения", "explore", Difficulty.MEDIUM, 2400),
        q("collect_wheat_32", "Собери 32 пшеницы", "collect", Difficulty.EASY, 900),
        q("collect_carrots_32", "Собери 32 моркови", "collect", Difficulty.EASY, 900),
        q("collect_apples_16", "Собери 16 яблок", "collect", Difficulty.MEDIUM, 1500),
        q("collect_bones_16", "Собери 16 костей", "collect", Difficulty.EASY, 900),
        q("collect_fish_10", "Поймай 10 рыб", "collect", Difficulty.MEDIUM, 1200),
        q("collect_pearls_16", "Собери 16 жемчугов Края", "collect", Difficulty.HARD, 2400),
        q("collect_rods_16", "Собери 16 огненных стержней", "collect", Difficulty.HARD, 2400),
        q("collect_slime_32", "Собери 32 сгустка слизи", "collect", Difficulty.MEDIUM, 1800),
        q("collect_honey_16", "Собери 16 бутылочек мёда", "collect", Difficulty.MEDIUM, 1800),
        q("collect_totems_5", "Получи 5 тотемов бессмертия", "collect", Difficulty.HARD, 5400),
        q("collect_diamonds_64", "Собери 64 алмаза", "collect", Difficulty.HARD, 5400),
        q("collect_debris_32", "Собери 32 древних обломка", "collect", Difficulty.HARD, 5400),
        q("collect_heads_16", "Собери 16 голов мобов", "collect", Difficulty.HARD, 5400),
        q("collect_golden_apples_5", "Собери 5 зачарованных золотых яблок", "collect", Difficulty.HARD, 5400),
        q("collect_pearls_64", "Собери 64 жемчуга Края", "collect", Difficulty.HARD, 5400),
        q("special_tame_horse", "Приручи лошадь", "special", Difficulty.EASY, 1200),
        q("special_breed_5", "Разведи 5 пар животных", "special", Difficulty.MEDIUM, 1800),
        q("special_trade_5", "Совершай сделки с 5 жителями", "special", Difficulty.MEDIUM, 1800),
        q("special_raid", "Переживи рейд", "special", Difficulty.HARD, 3600),
        q("special_elytra_1000", "Пролететь 1000 блоков на элитрах", "special", Difficulty.HARD, 2400),
        q("special_y300", "Доберись до высоты Y=300", "special", Difficulty.MEDIUM, 1800),
        q("special_nether_10", "Проведи 10 минут в Незере", "special", Difficulty.MEDIUM, 600),
        q("special_hero", "Получи эффект «Герой деревни»", "special", Difficulty.HARD, 3600),
        q("special_iron_dragon", "Победи дракона оружием из железа", "special", Difficulty.HARD, 5400),
        q("special_egg", "Получи яйцо дракона", "special", Difficulty.HARD, 5400),
        q("survive_no_sleep", "Не спи 5 минут", "survive", Difficulty.EASY, 300),
        q("survive_cave", "Выживи 3 минуты в пещере", "survive", Difficulty.EASY, 180),
        q("survive_underwater", "Проведи 5 минут под водой", "survive", Difficulty.MEDIUM, 300),
        q("survive_night", "Переживи ночь без укрытия", "survive", Difficulty.MEDIUM, 1200),
        q("survive_desert", "Выживи 5 минут в пустыне", "survive", Difficulty.EASY, 300),
        q("survive_nether", "Выживи 10 минут в Незере", "survive", Difficulty.MEDIUM, 600),
        q("survive_y200", "Проведи 5 минут на высоте Y=200", "survive", Difficulty.MEDIUM, 300),
        q("survive_no_armor", "Выживи 10 минут без брони", "survive", Difficulty.MEDIUM, 600),
        q("survive_deep_dark", "Выживи 5 минут в глубокой тьме", "survive", Difficulty.HARD, 300),
        q("survive_ocean", "Выживи 10 минут в океане", "survive", Difficulty.MEDIUM, 600),
        q("survive_nether_gold", "Проведи 15 минут в Незере без золотой брони", "survive", Difficulty.HARD, 900),
        q("survive_ancient_city", "Проведи 10 минут в древнем городе", "survive", Difficulty.HARD, 600),
        q("survive_half_heart", "Выживи 10 минут с половиной сердца", "survive", Difficulty.HARD, 600),
        q("survive_no_food", "Проведи 15 минут без еды", "survive", Difficulty.MEDIUM, 900),
        q("survive_end", "Выживи 15 минут в Краю", "survive", Difficulty.HARD, 900)
    );
    private static final Map<String, Definition> BY_ID;
    static { Map<String, Definition> m = new LinkedHashMap<>(); for (Definition d : DEFINITIONS) m.put(d.quest().id(), d); BY_ID = Collections.unmodifiableMap(m); }
    private QuestCatalog() {}
    private static Definition q(String id, String title, String category, Difficulty difficulty, int seconds) {
        return new Definition(new QuestEngine.Quest(id, difficulty, seconds * 1000L), title, category);
    }
    public static QuestEngine.Quest pick(UUID player, Set<Difficulty> allowed) {
        List<QuestEngine.Quest> pool = DEFINITIONS.stream().map(Definition::quest).filter(q -> allowed.contains(q.difficulty())).toList();
        if (pool.isEmpty()) throw new IllegalStateException("Нет доступных заданий для сложности: " + allowed.stream().map(Localization::difficulty).toList());
        return pool.get(new Random().nextInt(pool.size()));
    }
    /** Admin-selected difficulty still obeys each player's progression gate. */
    public static QuestEngine.Quest pick(UUID player, Set<Difficulty> allowed, Difficulty requested) {
        if (!allowed.contains(requested)) {
            throw new IllegalStateException("Эта сложность пока закрыта для игрока. Выполнено заданий: требуется открыть следующий этап прогресса.");
        }
        List<QuestEngine.Quest> pool = DEFINITIONS.stream().map(Definition::quest)
                .filter(q -> q.difficulty() == requested).toList();
        if (pool.isEmpty()) throw new IllegalStateException("Нет заданий выбранной сложности: " + requested);
        return pool.get(new Random().nextInt(pool.size()));
    }
    public static List<QuestEngine.Quest> all() { return DEFINITIONS.stream().map(Definition::quest).toList(); }
    public static List<Definition> definitions() { return DEFINITIONS; }
    public static Optional<Definition> find(String id) { return Optional.ofNullable(BY_ID.get(id)); }
}
