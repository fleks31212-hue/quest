# Implementation status — 0.16-dev

## Added in this iteration
- Added `/quests give <player> <easy|medium|hard>` syntax.
- Explicitly requested tiers are checked against the player's personal progression gate.
- Early-game players cannot be assigned medium/hard quests; progression remains based only on completed quests.
- Added tab suggestions for the three tier names.
- Existing random `/quests give <player>` behavior is unchanged.

## Verification
- Compiled the standalone Java core with Java 21.
- Ran a focused smoke test for progression gating, ordinary assignment, and skip accounting.
- Fabric Loom build and in-game 1.21.1 test have not yet been run in this environment.

## Still pending
- Full implementation of all punishment handlers, including protected temporary terrain restoration.
- More complete quest objective verification (structures, building, fish, breeding, trades, raid, boss damage constraints).
- Full server event system and leaderboard UI.
- Full Fabric build and multiplayer testing.

# Iteration 0.17-dev

## Added
- Added a deterministic leaderboard projection with rankings for wins, diamonds, best streak and fewest failures.
- Added a server-wide event lifecycle model for boss hunt, diamond rush, building contest, last survivor and happy hour.
- Event lifecycle supports start, expiry, remaining-time queries and administrator stop; it prevents overlapping events.
- Added focused tests for leaderboard ordering and event lifecycle.

## Verification
- Compiled the standalone Java core and focused tests with Java 21.
- Ran leaderboard/event assertions successfully.
- Fabric integration, actual world effects, persistence of active events and in-game UI remain pending.


# Iteration 0.19-dev

## Added
- Added event settlement that produces an immutable final result containing event type, start/finish times, final standings and winners.
- Supports joint winners when multiple participants tie for the highest score.
- Settlement clears the active event and prevents duplicate settlement; already expired events do not produce a payout result.
- Updated project version to 0.19.0-dev.

## Verification
- Compiled all standalone core classes and core tests using Java 21.
- Ran six test programs successfully: objective rules, punishment selection, quest catalog, quest engine, rewards/punishments, and leaderboard/event scoring/settlement.
- Quest catalog test confirms 100 entries; reward/punishment test confirms 69 entries.
- Fabric Loom build and in-game Minecraft 1.21.1 verification remain pending.

## Still pending
- Fabric event hooks that award points for actual Minecraft actions.
- Distribution of event rewards to winners and HUD/leaderboard display.
- Persistence of active event sessions across server restarts.
- Full Fabric build and multiplayer testing.

# Iteration 0.20-dev

## Added
- Added `EventScoringPolicy`, a server-side core policy that maps verified action categories to event points.
- Scoring is event-specific: boss damage, diamond ore mined, building contest votes, survival checkpoints and happy-hour quest completions.
- Mismatched actions and non-positive quantities are rejected; per-action quantity is capped to prevent accidental oversized awards.
- Added regression tests for accepted/rejected actions, scoring amounts, caps and leaderboard ordering.

## Verification
- Standalone Java 21 compilation and core test execution are run for this iteration.
- This policy is not yet connected to Fabric world/game callbacks. Callers must verify actions on the server; client-reported actions must not be trusted.
- Winner reward delivery, active event persistence, HUD display, Fabric build and in-game multiplayer tests remain pending.

# Iteration 0.21-dev

## Added
- Added versioned persistence for active server-event sessions, including event type, start/end timestamps and participant scores.
- Saves use a temporary file and atomic replacement where supported to reduce the chance of a partially written event state.
- Restore rejects expired, malformed, unsupported-version and unreasonable participant-count data rather than resuming invalid state.
- Added restart/expiry regression coverage for event persistence.

## Verification
- Compiled all standalone core classes and tests with Java 21.
- Ran all six core test programs successfully; catalog and punishment counts remain 100 and 69.
- This persistence API is currently tested as a core component; it is not yet wired to Fabric server lifecycle callbacks or event commands.
- Fabric Loom build and in-game Minecraft 1.21.1 testing remain pending.

# Iteration 0.22-dev

## Added
- Event scoring now requires a server-generated unique action ID for each scored action.
- `ServerEventManager.contributeOnce` rejects duplicate contribution IDs, preventing duplicate callback delivery from awarding points more than once.
- Processed contribution IDs are persisted with active event sessions and restored after restart.
- Persistence format is version 2; version 1 active-event files remain readable (without historical de-duplication IDs).
- Event scoring policy now routes accepted awards through the idempotent contribution API.

## Verification
- Compiled all standalone core classes and tests with Java 21.
- Ran all six core test programs successfully.
- Regression test confirms duplicate scoring is rejected both before and after an event save/restore cycle.
- Fabric Loom build, real server callback wiring and in-game Minecraft 1.21.1 verification remain pending.

# Iteration 0.23-dev

## Added
- Connected the active event manager to Fabric server startup/shutdown persistence using a separate world-save file.
- Added administrator commands: `/quests event start <type> <seconds>`, `/quests event stop`, and `/quests event top`.
- Wired server-side diamond ore and deepslate diamond ore block-break callbacks into Diamond Rush scoring. Each mined ore block receives a server-generated unique contribution ID and is scored once.
- Event expiry is checked on the server tick, announced to players and persisted.

## Verification
- Standalone Java 21 core compilation and all six core test programs are run for this iteration.
- The Fabric adapter source was updated but cannot be compiled in this environment because Gradle/Gradle Wrapper and resolved Fabric dependencies are not available. The exact Fabric API callback signatures and in-game behavior therefore remain unverified.
- Diamond Rush is the only event currently connected to a real gameplay callback. Other event scoring actions still need their respective server-side hooks; event winner rewards and HUD display remain pending.


# Iteration 0.24-dev

## Added
- Added a boss-hunt award for player-attributed final blows against the Ender Dragon, Wither and Warden. Each defeat awards 100 points; this implementation tracks boss defeats, not continuous damage contribution.
- Added Happy Hour scoring after successful quest completion through the automatic objective path.
- Added Last Survivor checkpoint awards when exactly one non-spectator player remains online; checkpoints run every 200 server ticks and award 5 points.
- Added the administrator command `/quests event vote <player> <1..100>` for Building Contest.
- Diamond ore contribution IDs now derive from dimension, block position, world tick and player UUID, improving duplicate-callback protection.
- Accepted event contributions are persisted promptly.

## Verification
- Compiled all standalone core classes and test classes using Java 21.
- Ran all six test programs successfully: quest catalog, objective rules, quest engine, leaderboard/event scoring/settlement, punishment selection, and reward/punishment checks.
- Verified 100 quest definitions and 69 punishment entries through the existing tests.
- Fabric-specific `QuestsMod` compilation and Minecraft runtime testing were not possible: no Gradle executable/wrapper or resolved Fabric dependencies are present. Fabric callback signatures and runtime behavior are therefore not certified.

## Remaining
- Continuous boss damage contribution (currently final-blow scoring only).
- Player-voted/automated building criteria and richer event rules.
- Winner rewards and final result announcement.
- HUD event timer and leaderboard display.
- Actual Fabric build and multiplayer verification.

# Iteration 0.25-dev

## Added
- Added `EventSettlementPlanner`, a pure core settlement layer for completed events.
- Every player tied for first place receives an event reward plan: 5–20 bonus diamonds plus a randomized compatible enchanted-item reward plan.
- Added concise Russian final-announcement text for single winners, tied winners and events with no participants.
- Added tests for tied-winner reward parity, diamond bounds and empty-event settlement.

## Verification
- Standalone Java 21 compilation and core test execution are required for this iteration.
- This planner only creates reward plans; it does not yet deliver Minecraft ItemStacks or announce results through Fabric. Event expiry/stop settlement wiring, inventory overflow handling for event rewards, HUD leaderboard and full Fabric build remain pending.


# Iteration 0.26-dev

## Added
- Scheduled event expiry now produces a final immutable outcome instead of clearing participant scores before settlement.
- The server tick settles expired events, broadcasts the final result, and delivers winner rewards through the Minecraft item adapter.
- Event winners receive the planned 5–20 bonus diamonds plus the planned armor, tool and enchanted item bundle; the event payout does not accidentally add the ordinary quest-diamond amount from the reward plan.
- Tied first-place winners are all paid. Empty events announce completion without issuing rewards.
- Explicit administrator stop retains its previous behavior: stop without winner payout.
- Expired sessions remain available for settlement until the server tick finalizes them; new contributions are rejected after the scheduled end time.

## Verification
- Standalone Java 21 core compilation and all six core test programs are run for this iteration.
- Added regression checks that scheduled expiry settles once, retains final scores, and does not settle before the deadline.
- Fabric adapter source now includes actual reward delivery and announcements, but Fabric Loom compilation and Minecraft runtime behavior remain unverified because Gradle and resolved Fabric dependencies are unavailable in this environment.
- Offline winners are currently logged and announced but are not queued for later delivery; they must be online at settlement to receive items.


# Iteration 0.27-dev

## Added
- Added `PendingEventRewardQueue`, a persistent queue that stores the exact rolled event reward for each offline winner (bonus diamonds, armor, tool, enchanted item and enchantments).
- Queue entries have deterministic reward IDs derived from event identity and player UUID, preventing duplicate enqueue if settlement is replayed.
- Delivered reward IDs are persisted, so an already acknowledged payout cannot be queued again after restart.
- Queue files use versioned properties, temporary-file writes and atomic replacement where supported.
- Fabric adapter now loads/saves the queue with the world, attempts immediate delivery for online winners, and retries queued rewards when a player joins. Entries are acknowledged only after the reward adapter returns successfully.

## Verification
- Compiled all standalone core classes and tests with Java 21.
- Ran seven test programs successfully, including persistence, reload, acknowledgement and duplicate-replay checks for pending rewards.
- Catalog and punishment tests still confirm 100 quests and 69 punishment entries.
- Fabric join callback compilation and Minecraft runtime delivery remain unverified because Gradle and resolved Fabric dependencies are unavailable in this environment.
- Delivery is retryable, but inventory insertion and file acknowledgement cannot be made one atomic transaction; an abrupt crash between those steps can result in a duplicate payout on restart.

## Remaining
- Build with Fabric Loom and validate callback signatures against the resolved 1.21.1 API.
- Test offline winner login, full inventory behavior, and server restart in a real dedicated server.
- Finish HUD timer/leaderboard and remaining objective/punishment integrations.

# Iteration 0.28-dev

## Added
- Added a separate server-authoritative event HUD payload carrying the active event title, each player's score/rank and remaining time.
- The server sends event HUD snapshots alongside quest HUD snapshots on the existing periodic sync interval.
- The client HUD now displays the quest panel and event panel together, stacking them without overlapping; each panel disappears when its server reports no active item.
- Event HUD values are derived from the server's active event and standings; clients do not submit or calculate scores.

## Verification
- Standalone Java 21 core compilation and all seven core test programs pass.
- The Fabric-specific packet registration, callback signatures and client rendering still require a Loom build and in-game test. Network access is unavailable in this environment, and neither Gradle nor a Gradle wrapper is installed, so a `.jar` cannot be honestly certified here.

# Iteration 0.29-dev

## Added
- Added a GitHub Actions workflow that provisions Java 21 and Gradle 8.10.2, resolves Fabric Loom dependencies, runs `clean build`, and uploads build JAR artifacts.
- Added `CoreTestSuite`, which runs all seven dependency-free core regression programs.
- Wired the core suite into Gradle's `check` lifecycle, so `gradle build` cannot pass without those regression checks passing.
- Added `.gitignore` entries for Gradle and build outputs and documented local/CI build steps.

## Verification
- Compiled all core and test Java sources with `javac --release 21`.
- Ran all seven core test programs successfully, including the aggregate `CoreTestSuite`.
- This environment cannot resolve external hosts, so Gradle distribution and Fabric Maven dependencies cannot be downloaded here. The Fabric Loom build is therefore still unexecuted; the workflow is prepared but has not been run on GitHub. The artifact is still a source ZIP, not a verified game JAR.


# Iteration 0.30-dev — Russian-first localization

## Added
- Set the Fabric metadata display name and description to Russian.
- Added Minecraft language resources `ru_ru.json` and `en_us.json` for HUD labels, score/rank strings, timers, event labels and mod metadata translation keys.
- Switched the client HUD labels to Minecraft translation keys, so the panel follows the player's selected language.
- Centralized Russian display names for quest difficulty, event types and punishment tiers; server announcements and catalog output no longer expose English enum constants.
- Kept command tokens and persisted enum identifiers stable in English to avoid breaking server commands and existing world saves.
- Added localization regression checks and enabled Java assertions in the Gradle core verification task.

## Verification
- Parsed `fabric.mod.json`, `ru_ru.json` and `en_us.json` successfully as UTF-8 JSON.
- Compiled all dependency-free core and test Java sources with Java 21.
- Ran the aggregate core suite with assertions enabled: all eight test programs passed.
- Fabric Loom build and in-game locale switching remain unverified until the CI build runs with network access.

## 0.31-dev — player-facing localization wiring

- Added matching Russian (`ru_ru`) and English (`en_us`) translation keys for common quest/event chat notifications.
- Replaced the corresponding hard-coded server chat strings with Minecraft translatable text components, so each player sees the message in their selected client language.
- Expanded localization regression checks: both language files must have identical keys, non-empty Russian values, and matching format placeholders.
- Verification: Java 21 core compile and all 8 standalone regression test programs pass.
- Limitation: this does not yet translate every administrator command response or every punishment title/description; Fabric/Loom compilation and an in-game language switch test remain outstanding.

## 0.32-dev — punishment localization

- Added matching Russian and English translation keys for all 69 punishment catalog entries.
- Punishment execution messages now use Minecraft translatable text keys instead of embedding Russian-only strings.
- Timeout broadcasts resolve punishment names through the receiving player's language.
- Expanded `LocalizationTest` to assert all 69 catalog entries exist in both language files and Russian labels match the catalog.
- Validation: Java 21 core compilation and all 8 standalone test programs passed; both language JSON files parse and contain the same 111 keys.
- Fabric Loom build and in-game language switching remain unverified in this environment.


# Iteration 0.33-dev — build configuration and command-tree repair

## Added / fixed
- Added the Fabric Maven repository explicitly so Loom can resolve Yarn mappings, Fabric Loader and Fabric API dependencies in a clean CI environment.
- Aligned the Gradle project version with the delivered source iteration (`0.33.0-dev`).
- Fixed mismatched parentheses in the root `/quests` command registration and the nested `/quests event` command tree. A Java parser pass now reports no syntax errors in `QuestsMod.java`; unresolved Minecraft/Fabric symbols still require Loom dependency resolution.
- Narrowed the CI artifact upload to the remapped mod JAR rather than also uploading the sources JAR.

## Verification
- Compiled all dependency-free core and test sources with Java 21.
- Ran `CoreTestSuite`: all eight test programs passed, including 100 quest catalog entries and 69 punishment entries.
- Parsed `fabric.mod.json`, both language files, punishment data and sound data as JSON; Russian and English files have the same 111 keys.
- A full Fabric Loom build was not run in this environment because Gradle is not installed and external dependency downloads are unavailable. The GitHub Actions workflow is configured to perform that build; it has not yet been executed.
