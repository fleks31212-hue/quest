package com.example.quests.core;

import java.util.*;

/** Names and tiers for the agreed penalty pool; execution is intentionally delegated to bounded Fabric handlers. */
public final class PunishmentCatalog {
    public enum Tier { EASY, MEDIUM, HARD, CHAOS }
    public record Entry(String id, String title, Tier tier) {}
    private static final List<Entry> ENTRIES = List.of(
        e("back_1000","Телепорт назад на 1000 блоков",Tier.EASY), e("random_1000","Случайный телепорт в радиусе 1000",Tier.EASY),
        e("tool_damage","Поломка инструмента",Tier.EASY), e("inventory_shuffle","Перемешивание инвентаря",Tier.EASY),
        e("inverted_controls","Инверсия управления",Tier.EASY), e("vegan","Веганство",Tier.EASY), e("mountain_link","Горная ссылка",Tier.EASY),
        e("super_jump","Супер-прыгун",Tier.EASY), e("resize","Смена размеров",Tier.EASY), e("ice_prison","Ледяной плен",Tier.EASY),
        e("random_10000","Случайный телепорт в радиусе 10000",Tier.MEDIUM), e("armor_damage","Поломка брони",Tier.MEDIUM),
        e("up_teleport","Телепорт вверх",Tier.MEDIUM), e("baby_zombie","Мини-зомби",Tier.MEDIUM), e("angry_bees","Злые пчёлы",Tier.MEDIUM),
        e("dimension_swap","Телепорт между измерениями",Tier.MEDIUM), e("bed_teleport","Телепорт к кровати",Tier.MEDIUM),
        e("underground_trap","Подземная ловушка",Tier.MEDIUM), e("sky_island","Островок в небе",Tier.MEDIUM), e("nether_surprise","Незерский сюрприз",Tier.MEDIUM),
        e("ocean_island","Остров в океане",Tier.MEDIUM), e("silverfish","Чешуйницы",Tier.MEDIUM), e("witches","Ведьмы",Tier.MEDIUM),
        e("phantoms","Фантомы",Tier.MEDIUM), e("cursed_hotbar","Проклятый хотбар",Tier.MEDIUM), e("chicken_army","Куриная армия",Tier.MEDIUM),
        e("obsidian_cell","Обсидиановая клетка",Tier.MEDIUM), e("thunderstorm","Гроза богов",Tier.MEDIUM), e("hostile_mobs","Агрессивные мобы",Tier.MEDIUM),
        e("three_wardens","Три Вардена",Tier.HARD), e("all_gear_damage","Поломка всего снаряжения",Tier.HARD), e("drop_inventory","Выбрасывание инвентаря",Tier.HARD),
        e("five_safe_tnt","Пять моментальных динамитов",Tier.HARD), e("temporary_terrain","Уничтожение территории (с восстановлением)",Tier.HARD),
        e("two_ravagers_a","Два разорителя — вариант 1",Tier.HARD), e("two_ravagers_b","Два разорителя — вариант 2",Tier.HARD),
        e("ten_endermen","Десять эндерменов",Tier.HARD), e("diamond_tax","Алмазный налог",Tier.HARD), e("fate_trade","Обмен с судьбой",Tier.HARD),
        e("inventory_roulette","Инвентарная рулетка",Tier.HARD), e("hungry_backpack","Голодный рюкзак",Tier.HARD), e("anvil_rain","Наковальневый дождь",Tier.HARD),
        e("lava_ring","Лавовое кольцо",Tier.HARD), e("meteor_shower","Метеоритный дождь",Tier.HARD), e("mob_rain","Дождь из мобов",Tier.HARD),
        e("reverse_world","Обратный мир",Tier.HARD), e("last_chance","Последний шанс",Tier.HARD), e("lava_rise","Лавовый подъём",Tier.HARD), e("dont_stop","Не останавливайся!",Tier.HARD),
        e("pumpkin_vision","Тыквенный взгляд",Tier.CHAOS), e("chicken_mode","Куриный режим",Tier.CHAOS), e("spring_legs","Пружинные ноги",Tier.CHAOS),
        e("upside_down","Перевёртыш",Tier.CHAOS), e("rainbow_sheep","Радужная овца",Tier.CHAOS), e("pig_inside","Свинья внутри",Tier.CHAOS),
        e("chicken_drop","Куриный десант",Tier.CHAOS), e("villager_meeting","Собрание жителей",Tier.CHAOS), e("bat_party","Летучая вечеринка",Tier.CHAOS),
        e("llama_caravan","Караван без приглашения",Tier.CHAOS), e("anomaly_rain","Аномальный дождь",Tier.CHAOS), e("chicken_boss","Курица-босс",Tier.CHAOS),
        e("potion_madness","Зельевар-псих",Tier.CHAOS), e("slime_king","Слизневый король",Tier.CHAOS), e("sheep_apocalypse","Овечий апокалипсис",Tier.CHAOS),
        e("boss_arrives","Начальник пришёл",Tier.CHAOS), e("bee_union","Пчелиный профсоюз",Tier.CHAOS), e("chicken_meteor","Куриный метеорит",Tier.CHAOS),
        e("mini_dragon","Дракон на минималках",Tier.CHAOS), e("failure_fireworks","Праздник поражения",Tier.CHAOS));
    private static Entry e(String id,String title,Tier tier){return new Entry(id,title,tier);}
    public static List<Entry> all(){return ENTRIES;}
    public static List<Entry> byTier(Tier tier){return ENTRIES.stream().filter(x->x.tier()==tier).toList();}
    public static Optional<Entry> find(String id){return ENTRIES.stream().filter(x->x.id().equals(id)).findFirst();}
    private PunishmentCatalog(){}
}
