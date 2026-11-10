package com.example.quests.core;

import java.util.*;
import java.io.*;
import java.nio.file.*;

/** Server-wide event lifecycle and per-player contribution tracking. */
public final class ServerEventManager {
    public enum Type { BOSS_HUNT, DIAMOND_RUSH, BUILDING_CONTEST, LAST_SURVIVOR, HAPPY_HOUR }
    public record ActiveEvent(Type type, long startedAt, long endsAt, Map<UUID, Integer> scores) {
        public ActiveEvent {
            Objects.requireNonNull(type);
            if (endsAt <= startedAt) throw new IllegalArgumentException("Event end must be after start");
            scores = Collections.unmodifiableMap(new HashMap<>(scores));
        }
        public long remainingMs(long now) { return Math.max(0, endsAt - now); }
        public boolean isExpired(long now) { return now >= endsAt; }
    }
    public record Standing(UUID playerId, int score, int rank) {}
    /** Immutable event result. All players tied for the highest score are winners. */
    public record EventOutcome(Type type, long startedAt, long finishedAt,
                               List<Standing> standings, List<UUID> winners) {
        public EventOutcome {
            Objects.requireNonNull(type);
            standings = List.copyOf(standings);
            winners = List.copyOf(winners);
        }
        public int participantCount() { return standings.size(); }
        public int winningScore() { return standings.isEmpty() ? 0 : standings.get(0).score(); }
    }
    private Type type;
    private long startedAt, endsAt;
    private final Map<UUID, Integer> scores = new HashMap<>();
    /** Unique server-generated action IDs prevent duplicate callback delivery from scoring twice. */
    private final Set<UUID> processedContributions = new HashSet<>();

    public Optional<ActiveEvent> active(long now) {
        return type == null ? Optional.empty() : Optional.of(snapshot());
    }
    public ActiveEvent start(Type newType, long now, long durationMs) {
        Objects.requireNonNull(newType);
        if (durationMs < 1_000) throw new IllegalArgumentException("Event must last at least one second");
        if (type != null) throw new IllegalStateException("Another server event is already active or awaiting settlement");
        type = newType; startedAt = now; endsAt = Math.addExact(now, durationMs); scores.clear();
        return snapshot();
    }
    /** Adds positive event points. Zero/negative contributions are ignored. */
    public boolean contribute(UUID playerId, int points, long now) {
        Objects.requireNonNull(playerId);
        if (type == null || now >= endsAt || points <= 0) return false;
        scores.merge(playerId, points, (a, b) -> (int)Math.min(Integer.MAX_VALUE, (long)a + b));
        return true;
    }

    /** Adds a contribution exactly once per server-generated action ID for this event. */
    public boolean contributeOnce(UUID playerId, UUID contributionId, int points, long now) {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(contributionId);
        if (type == null || now >= endsAt || points <= 0 || processedContributions.contains(contributionId)) return false;
        scores.merge(playerId, points, (a, b) -> (int)Math.min(Integer.MAX_VALUE, (long)a + b));
        processedContributions.add(contributionId);
        return true;
    }
    public List<Standing> standings(long now, int limit) {
        return standingsWithoutExpiry(limit);
    }
    private List<Standing> standingsWithoutExpiry(int limit) {
        if (limit < 1) return List.of();
        List<Map.Entry<UUID,Integer>> ordered = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID,Integer>comparingByValue().reversed().thenComparing(e -> e.getKey().toString()))
                .limit(limit).toList();
        List<Standing> result = new ArrayList<>();
        for (int i=0;i<ordered.size();i++) result.add(new Standing(ordered.get(i).getKey(), ordered.get(i).getValue(), i+1));
        return List.copyOf(result);
    }
    /** Finalizes an event only after its scheduled end time; safe to call on every server tick. */
    public Optional<EventOutcome> finishIfExpired(long now) {
        if (type == null || now < endsAt) return Optional.empty();
        return Optional.of(buildOutcomeAndClear(now));
    }

    private EventOutcome buildOutcomeAndClear(long now) {
        List<Standing> finalStandings = standingsWithoutExpiry(Integer.MAX_VALUE);
        List<UUID> winners = finalStandings.isEmpty() ? List.of() : finalStandings.stream()
                .filter(s -> s.score() == finalStandings.get(0).score())
                .map(Standing::playerId).toList();
        EventOutcome outcome = new EventOutcome(type, startedAt, now, finalStandings, winners);
        clear();
        return outcome;
    }

    /** Finalizes an active event early, for explicit administrative settlement. */
    public Optional<EventOutcome> finish(long now) {
        if (type == null || now >= endsAt) {
            expire(now);
            return Optional.empty();
        }
        return Optional.of(buildOutcomeAndClear(now));
    }
    public Optional<ActiveEvent> stop() {
        if (type == null) return Optional.empty();
        ActiveEvent previous = snapshot(); clear(); return Optional.of(previous);
    }
    public boolean tick(long now) { boolean had = type != null; expire(now); return had && type == null; }
    /** Persists an active event and its participant scores for server restarts. */
    public void save(Path file) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", "2");
        properties.setProperty("active", Boolean.toString(type != null));
        if (type != null) {
            properties.setProperty("type", type.name());
            properties.setProperty("startedAt", Long.toString(startedAt));
            properties.setProperty("endsAt", Long.toString(endsAt));
            properties.setProperty("participants", Integer.toString(scores.size()));
            properties.setProperty("processedContributions", Integer.toString(processedContributions.size()));
            int actionIndex = 0;
            for (UUID contributionId : processedContributions) {
                properties.setProperty("contribution." + actionIndex++, contributionId.toString());
            }
            int index = 0;
            for (var entry : scores.entrySet()) {
                String prefix = "participant." + index++ + ".";
                properties.setProperty(prefix + "uuid", entry.getKey().toString());
                properties.setProperty(prefix + "score", Integer.toString(entry.getValue()));
            }
        }
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "Minecraft Quests active event");
        }
        try {
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Loads a saved event; malformed or expired state is rejected rather than resumed. */
    public static ServerEventManager load(Path file, long now) throws IOException {
        ServerEventManager manager = new ServerEventManager();
        if (!Files.exists(file)) return manager;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
        String version = properties.getProperty("version");
        if (!("1".equals(version) || "2".equals(version))
                || !"true".equals(properties.getProperty("active", "false"))) return manager;
        try {
            Type loadedType = Type.valueOf(properties.getProperty("type"));
            long loadedStart = Long.parseLong(properties.getProperty("startedAt"));
            long loadedEnd = Long.parseLong(properties.getProperty("endsAt"));
            int count = Integer.parseInt(properties.getProperty("participants", "0"));
            int actionCount = "2".equals(version)
                    ? Integer.parseInt(properties.getProperty("processedContributions", "0")) : 0;
            if (loadedEnd <= loadedStart || loadedEnd <= now || count < 0 || count > 100_000
                    || actionCount < 0 || actionCount > 1_000_000) return manager;
            manager.type = loadedType;
            manager.startedAt = loadedStart;
            manager.endsAt = loadedEnd;
            for (int i = 0; i < actionCount; i++) {
                UUID contributionId = UUID.fromString(properties.getProperty("contribution." + i));
                if (!manager.processedContributions.add(contributionId)) return new ServerEventManager();
            }
            for (int i = 0; i < count; i++) {
                String prefix = "participant." + i + ".";
                UUID player = UUID.fromString(properties.getProperty(prefix + "uuid"));
                int score = Integer.parseInt(properties.getProperty(prefix + "score"));
                if (score < 0) return new ServerEventManager();
                manager.scores.put(player, score);
            }
            return manager;
        } catch (RuntimeException malformed) {
            return new ServerEventManager();
        }
    }

    private ActiveEvent snapshot() { return new ActiveEvent(type, startedAt, endsAt, scores); }
    private void expire(long now) { if (type != null && now >= endsAt) clear(); }
    private void clear() { type = null; startedAt = 0; endsAt = 0; scores.clear(); processedContributions.clear(); }
}
