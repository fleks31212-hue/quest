# Minecraft Quests — Fabric 1.21.1

Development source project for the server-authoritative quest mod.

## Commands currently wired in source
- `/quests give <player>` — random quest from the player's unlocked difficulty tiers.
- `/quests give <player> easy|medium|hard` — admin-selected tier, only if unlocked by that player's progress.
- `/quests cancel <player>`
- `/quests skip <player>`
- `/quests complete <player>`
- `/quests sounds on|off|status`
- `/quests pause`, `/quests resume`, `/quests list`, `/quests stats <player>`, `/quests top`

## Build
Requires JDK 21 and Gradle/Fabric Loom dependency access. Run `gradle build` (or `./gradlew build` if a Gradle wrapper is added).

This ZIP contains source code, not a compiled mod JAR. Fabric compilation and in-game verification remain pending; see `docs/IMPLEMENTATION_STATUS.md`.

## 0.19-dev
- Added immutable event settlement results with final standings and participant count.
- All players tied for the highest score are included as joint winners.
- Settlement closes and clears the active event; a second settlement cannot pay out again.
- Expired events are not settled as successful results.
- Core regression tests pass with Java 21.

Note: this release still requires Fabric adapter work for in-world event scoring, winner rewards and HUD/leaderboard display. It has not been verified as a built Minecraft 1.21.1 JAR.

## 0.20-dev
- Added event-specific server-side scoring rules for the five event types.
- Scoring policy rejects mismatched action types and caps quantity per action.
- The scoring policy is core logic only; Fabric callbacks still need to verify and forward actual server-side actions.

### 0.21-dev event persistence
The standalone event manager can now save and restore an active event, including participant scores. Expired or malformed saved sessions are discarded. The persistence API is covered by core tests but is not yet wired into Fabric server lifecycle callbacks.

## 0.22-dev
- Event scoring uses unique server-generated action IDs to prevent duplicate callback deliveries from awarding points twice.
- Processed action IDs are included in active event persistence and survive server restarts.
- The event save format is versioned; existing version 1 saves remain readable.
- Java 21 standalone core compilation and all six core test programs pass.

## 0.23-dev server event commands
- `/quests event start <type> <seconds>` — start one of `boss_hunt`, `diamond_rush`, `building_contest`, `last_survivor`, `happy_hour` (10–86400 seconds).
- `/quests event stop` — stop the active event without payout.
- `/quests event top` — show the active event standings.
- `/quests event vote <player> <1..100>` — administrator awards a building-contest score.
- Active event data is loaded/saved with the world. Diamond Rush currently awards points for server-observed diamond ore and deepslate diamond ore breaks.
- Fabric adapter compilation and in-game verification are still pending.


## 0.24-dev
- Added a boss-hunt completion award for player-attributed kills of the Ender Dragon, Wither and Warden (100 points per verified defeat; this is a final-blow award, not damage contribution).
- Added Happy Hour points when a quest is successfully completed through the automatic objective-completion path.
- Added Last Survivor checkpoint scoring when exactly one non-spectator player remains online (5 points every 10 seconds).
- Added administrator-only `/quests event vote <player> <1..100>` for Building Contest; the entered score is added directly.
- Diamond block-break contribution IDs are now deterministic for a player, dimension, block position and world tick, making duplicate callback delivery idempotent.
- Accepted event contributions are saved promptly.
- All six standalone Java core test programs pass with Java 21. Fabric adapter compilation and in-game behavior remain unverified because Gradle and the Fabric dependency cache are unavailable in this environment.


## 0.26-dev
- Scheduled event expiry now settles the final standings rather than discarding them.
- The Fabric server tick broadcasts the result and delivers winner rewards: 5–20 bonus diamonds plus armor, a tool and an enchanted item.
- All tied first-place players receive rewards. Empty events produce an announcement without rewards.
- Administrator `/quests event stop` intentionally continues to stop without payout.
- Offline winners are currently not queued for later delivery; they are logged and announced.
- Core tests pass with Java 21. The Fabric adapter is still uncompiled in this environment, so this remains a development source ZIP, not a ready-to-install JAR.


## 0.27-dev offline event rewards
- Event winner rewards are now stored in a persistent world-save queue, including the already-rolled item and enchantment choices.
- Offline winners receive their queued reward on their next server join; online winners are processed immediately through the same delivery path.
- Reward IDs are deterministic per event/player, and delivered IDs are persisted so replayed settlement cannot enqueue the same reward again.
- Queue writes use a temporary file and atomic replacement where supported.
- Core queue persistence and duplicate-replay tests pass with Java 21.
- The Fabric join callback and item delivery adapter are source changes only; Fabric compilation and in-game verification remain pending. Delivery is retryable if the adapter throws, with a small crash window between item insertion and queue acknowledgement that can cause a duplicate after abrupt process termination.

## 0.28-dev
- Added a server-authoritative event HUD panel showing the event name, the local player's score/rank and remaining event time.
- Quest and event HUD panels stack vertically and are independently hidden when inactive.
- The event HUD receives snapshots from the server; client-side score reporting is not used.
- Fabric packet compilation and in-game rendering still require verification with Gradle/Fabric Loom.

## Build from GitHub (0.29-dev)

A GitHub Actions workflow is included at `.github/workflows/build.yml`. After pushing this project to a GitHub repository, open **Actions → Build Fabric mod**. The workflow installs Java 21 and Gradle 8.10.2, resolves Fabric Loom/Minecraft dependencies, runs `clean build` (including all seven standalone core regression programs), and uploads the JAR files as the artifact `minecraft-quests-fabric-1.21.1`.

For a local build, install JDK 21 and Gradle 8.10.2, then run:

```bash
gradle --no-daemon clean build
```

The regular mod JAR is in `build/libs/`; the `-sources.jar` is source code only. A successful GitHub workflow build still needs an in-game smoke test on a Minecraft 1.21.1 Fabric server and client before calling the mod release-ready.

## Языки / Languages

- Основной язык мода — русский (`ru_ru`). Названия всех 100 заданий и 69 наказаний, серверные сообщения, команды и объявления подготовлены на русском.
- HUD использует языковые ключи Minecraft: русский (`ru_ru`) и английский (`en_us`).
- Внутренние идентификаторы команд и событий (`easy`, `boss_hunt` и т. п.) намеренно остаются техническими и не зависят от языка клиента.
- Языковые файлы находятся в `src/main/resources/assets/minecraft_quests/lang/`.



## 0.30-dev — русская локализация
- Русское имя и описание мода в метаданных Fabric.
- Добавлены `ru_ru.json` и `en_us.json`; подписи HUD используют языковые ключи Minecraft.
- Сложности и названия событий в серверных сообщениях показываются по-русски.
- Названия заданий и наказаний остаются русскими; внутренние ID и аргументы команд не менялись.
- Добавлены проверки локализации; агрегированный набор тестов запускается с включёнными Java assertions.

### Localization

The mod ships `ru_ru` (Russian) and `en_us` (English) language resources. Common quest and event notifications use Minecraft translatable text components, so each player receives those messages in their selected client language. Some administrator command responses and catalog descriptions are still Russian-first and are not yet fully localized.

### Languages

The mod includes Russian (`ru_ru`) and English (`en_us`) language resources. HUD labels, event notices, quest messages, and all 69 punishment names have matching translation keys. Punishment names and status messages use Minecraft translatable text, so the client language determines the displayed text where those messages are sent as translatable components.


## Build version 0.33-dev

The Gradle build declares Fabric Maven explicitly (`https://maven.fabricmc.net/`) in addition to Maven Central. CI builds the remapped artifact `build/libs/minecraft-quests-0.33.0-dev.jar` with Java 21 and Gradle 8.10.2. The source archive itself is not an installable mod; use the JAR produced by a successful Loom build.
