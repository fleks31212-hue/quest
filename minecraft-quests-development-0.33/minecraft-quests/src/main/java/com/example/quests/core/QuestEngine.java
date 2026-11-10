package com.example.quests.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Server-authoritative quest rules, independent of Minecraft classes so they can be tested.
 * Minecraft adapter code must call these methods on the server thread.
 */
public final class QuestEngine {
    public interface QuestPicker { Quest pick(UUID player, Set<Difficulty> allowed); }
    public record Quest(String id, Difficulty difficulty, long durationMs) {
        public Quest {
            Objects.requireNonNull(id); Objects.requireNonNull(difficulty);
            if (durationMs < 1_000) throw new IllegalArgumentException("durationMs must be >= 1000");
        }
    }
    public enum Result { COMPLETED, FAILED, SKIPPED, CANCELLED }
    private final Map<UUID, QuestState> players = new HashMap<>();
    private final Random random;
    private final QuestPicker picker;
    private boolean paused;
    private boolean soundsEnabled = true;

    public QuestEngine(QuestPicker picker) { this(picker, new Random()); }
    public QuestEngine(QuestPicker picker, Random random) {
        this.picker = Objects.requireNonNull(picker); this.random = Objects.requireNonNull(random);
    }
    public QuestState state(UUID id) { return players.computeIfAbsent(id, QuestState::new); }
    public Collection<QuestState> allStates() { return Collections.unmodifiableCollection(players.values()); }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean value) { paused = value; }
    public boolean soundsEnabled() { return soundsEnabled; }
    public void setSoundsEnabled(boolean value) { soundsEnabled = value; }

    public Quest assign(UUID player, long now) {
        if (paused) throw new IllegalStateException("Quest system is paused");
        QuestState s = state(player);
        Quest q = Objects.requireNonNull(picker.pick(player, Difficulty.availableAt(s.completed)));
        if (!Difficulty.availableAt(s.completed).contains(q.difficulty()))
            throw new IllegalStateException("Picker returned a difficulty not allowed by player progression");
        s.activeQuestId = q.id(); s.activeDifficulty = q.difficulty(); s.progress.clear();
        s.deadlineEpochMs = now + q.durationMs(); s.nextQuestEpochMs = 0; s.lastResult = "ASSIGNED";
        return q;
    }
    /** Assign a requested tier without allowing admins to bypass progression gates. */
    public Quest assign(UUID player, long now, Difficulty requested) {
        if (paused) throw new IllegalStateException("Quest system is paused");
        QuestState s = state(player);
        Set<Difficulty> allowed = Difficulty.availableAt(s.completed);
        if (!allowed.contains(requested))
            throw new IllegalStateException("Сложность «" + Localization.difficulty(requested) + "» ещё не открыта: выполнено " + s.completed + " заданий.");
        Quest q = null;
        // The catalog picker is random across unlocked tiers; retry until the requested tier is drawn.
        for (int attempt = 0; attempt < 10_000; attempt++) {
            Quest candidate = Objects.requireNonNull(picker.pick(player, allowed));
            if (candidate.difficulty() == requested) { q = candidate; break; }
        }
        if (q == null) throw new IllegalStateException("Не удалось подобрать задание выбранной сложности.");
        s.activeQuestId = q.id(); s.activeDifficulty = q.difficulty(); s.progress.clear();
        s.deadlineEpochMs = now + q.durationMs(); s.nextQuestEpochMs = 0; s.lastResult = "ASSIGNED";
        return q;
    }

    /** Successful completion schedules the next quest after a random 10–120 seconds. */
    public long complete(UUID player, long now, int diamondsAwarded) {
        QuestState s = requireActive(player);
        s.completed++; s.winStreak++; s.bestStreak = Math.max(s.bestStreak, s.winStreak);
        s.diamondsEarned += Math.max(0, diamondsAwarded); clearActive(s);
        s.lastResult = Result.COMPLETED.name();
        s.nextQuestEpochMs = now + (10_000L + random.nextInt(110_001));
        return s.nextQuestEpochMs;
    }

    /** A timeout is a failure. The next quest can be assigned immediately by the adapter. */
    public void fail(UUID player) {
        QuestState s = requireActive(player); s.failed++; s.winStreak = 0;
        clearActive(s); s.lastResult = Result.FAILED.name(); s.nextQuestEpochMs = 0;
    }

    /** Admin skip: no failure, penalty, reward, streak change or progression. */
    public void skip(UUID player) {
        QuestState s = requireActive(player); s.skipped++; clearActive(s);
        s.lastResult = Result.SKIPPED.name(); s.nextQuestEpochMs = 0;
    }
    public void cancel(UUID player) {
        QuestState s = state(player); clearActive(s); s.lastResult = Result.CANCELLED.name(); s.nextQuestEpochMs = 0;
    }
    public boolean shouldAssignNext(UUID player, long now) {
        QuestState s = state(player); return !paused && !s.hasActiveQuest() && s.nextQuestEpochMs > 0 && now >= s.nextQuestEpochMs;
    }
    public long remainingMs(UUID player, long now) {
        QuestState s = state(player); return s.hasActiveQuest() ? Math.max(0, s.deadlineEpochMs - now) : 0;
    }
    private QuestState requireActive(UUID id) {
        QuestState s = state(id); if (!s.hasActiveQuest()) throw new IllegalStateException("Player has no active quest"); return s;
    }
    private static void clearActive(QuestState s) { s.progress.clear(); s.activeQuestId = ""; s.activeDifficulty = null; s.deadlineEpochMs = 0; }

    /** Simple versioned properties persistence; safe to call on server shutdown/save. */
    public void save(Path file) throws IOException {
        Properties p = new Properties(); p.setProperty("version", "1"); p.setProperty("paused", Boolean.toString(paused));
        p.setProperty("sounds", Boolean.toString(soundsEnabled)); p.setProperty("players", Integer.toString(players.size()));
        int i = 0;
        for (QuestState s : players.values()) {
            String k = "p." + i++ + ".";
            p.setProperty(k+"uuid", s.playerId.toString()); p.setProperty(k+"completed", ""+s.completed);
            p.setProperty(k+"failed", ""+s.failed); p.setProperty(k+"skipped", ""+s.skipped);
            p.setProperty(k+"diamonds", ""+s.diamondsEarned); p.setProperty(k+"streak", ""+s.winStreak);
            p.setProperty(k+"best", ""+s.bestStreak); p.setProperty(k+"quest", s.activeQuestId);
            p.setProperty(k+"difficulty", s.activeDifficulty == null ? "" : s.activeDifficulty.name());
            p.setProperty(k+"deadline", ""+s.deadlineEpochMs); p.setProperty(k+"next", ""+s.nextQuestEpochMs);
            p.setProperty(k+"result", s.lastResult);
            p.setProperty(k+"progress", encodeProgress(s.progress));
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        try (OutputStream out = Files.newOutputStream(file)) { p.store(out, "Minecraft Quests state"); }
    }
    public static QuestEngine load(Path file, QuestPicker picker) throws IOException {
        QuestEngine e = new QuestEngine(picker); if (!Files.exists(file)) return e;
        Properties p = new Properties(); try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        e.paused = Boolean.parseBoolean(p.getProperty("paused", "false"));
        e.soundsEnabled = Boolean.parseBoolean(p.getProperty("sounds", "true"));
        int count = Integer.parseInt(p.getProperty("players", "0"));
        for (int i=0; i<count; i++) {
            String k="p."+i+"."; UUID id=UUID.fromString(p.getProperty(k+"uuid")); QuestState s=e.state(id);
            s.completed=num(p,k+"completed"); s.failed=num(p,k+"failed"); s.skipped=num(p,k+"skipped");
            s.diamondsEarned=num(p,k+"diamonds"); s.winStreak=num(p,k+"streak"); s.bestStreak=num(p,k+"best");
            s.activeQuestId=p.getProperty(k+"quest", ""); String d=p.getProperty(k+"difficulty", "");
            s.activeDifficulty=d.isBlank()?null:Difficulty.valueOf(d); s.deadlineEpochMs=lng(p,k+"deadline");
            s.nextQuestEpochMs=lng(p,k+"next"); s.lastResult=p.getProperty(k+"result", "NONE");
            decodeProgress(p.getProperty(k+"progress", ""), s.progress);
        }
        return e;
    }
    private static String encodeProgress(Map<String,Integer> progress) {
        return progress.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).reduce((a,b)->a+";"+b).orElse("");
    }
    private static void decodeProgress(String raw, Map<String,Integer> out) {
        if(raw==null || raw.isBlank()) return;
        for(String pair:raw.split(";")){ int at=pair.lastIndexOf('='); if(at>0) try { out.put(pair.substring(0,at),Integer.parseInt(pair.substring(at+1))); } catch(NumberFormatException ignored){} }
    }
    private static int num(Properties p,String k){return Integer.parseInt(p.getProperty(k,"0"));}
    private static long lng(Properties p,String k){return Long.parseLong(p.getProperty(k,"0"));}
}
