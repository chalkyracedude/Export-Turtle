package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.util.Filepath;

/** Durable account-local milestones derived from successive exported snapshots. */
public final class RecentEventsStore
{
    private static volatile int retentionDays;
    public static void setRetentionDays(int days)
    {
        retentionDays = Math.max(0, Math.min(3650, days));
    }

    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private RecentEventsStore()
    {
    }

    public static void record(Filepath directory, AccountSnapshot snapshot, String sessionId, Gson gson)
        throws IOException
    {
        Journal journal = load(directory, snapshot.getAccountName(), gson);
        if (!sessionId.equals(journal.sessionId))
        {
            finalizeSession(journal);
            journal.sessionId = sessionId;
            journal.sessionBossCounts.clear();
        }
        JsonObject current = gson.toJsonTree(snapshot).getAsJsonObject();
        if (journal.baseline != null)
            collect(journal, current);
        journal.baseline = current;
        save(directory, journal, gson);
    }

    public static void recordCollection(Filepath directory, String account, String item, String timestamp,
        String sessionId, Gson gson) throws IOException
    {
        Journal journal = load(directory, account, gson);
        String id = "Logs:" + item.toLowerCase(java.util.Locale.ROOT);
        for (Event existing : journal.events)
            if (id.equals(existing.id))
                return;
        if (!journal.completedIds.add(id))
            return;
        if (!sessionId.equals(journal.sessionId))
        {
            finalizeSession(journal);
            journal.sessionId = sessionId;
            journal.sessionBossCounts.clear();
        }
        Event event = grouped(journal, "Logs", "slots", timestamp);
        JsonObject entry = new JsonObject();
        entry.addProperty("title", item);
        event.entries.add(entry);
        event.count = event.entries.size();
        event.title = "I completed " + event.count + " Collection Log slot" + (event.count == 1 ? "." : "s.");
        event.detail = "One less empty slot. One less excuse to leave.";
        save(directory, journal, gson);
    }

    public static void finishSession(Filepath directory, String account, String sessionId, Gson gson)
        throws IOException
    {
        Journal journal = load(directory, account, gson);
        if (sessionId.equals(journal.sessionId))
        {
            finalizeSession(journal);
            journal.sessionId = null;
            save(directory, journal, gson);
        }
    }

    private static void finalizeSession(Journal journal)
    {
        for (Event event : journal.events)
            if (event.sessionId != null && event.sessionId.equals(journal.sessionId))
                event.finalized = true;
    }

    public static void recordValuableDrops(Filepath directory, String account, String lootId, String source,
        List<ValuableDrops.Drop> drops, String timestamp, Gson gson) throws IOException
    {
        if (drops.isEmpty())
            return;
        Journal journal = load(directory, account, gson);
        Set<String> known = new HashSet<>();
        for (Event existing : journal.events)
            known.add(existing.id);
        for (ValuableDrops.Drop drop : drops)
        {
            String id = "Drops:" + lootId + ":" + drop.itemId;
            if (drop.itemId < 0 || drop.quantity <= 0 || drop.unitPrice <= 0 ||
                ValuableDrops.tier(drop.totalValue) == null || !known.add(id))
                continue;
            Event event = new Event();
            event.id = id;
            event.type = "Drops";
            event.title = drop.title();
            event.taskId = -1;
            event.observedAt = timestamp;
            event.itemId = drop.itemId;
            event.quantity = drop.quantity;
            event.unitPrice = drop.unitPrice;
            event.valuationBasis = drop.valuationBasis;
            event.totalValue = drop.totalValue;
            event.source = source;
            event.valueTier = ValuableDrops.tier(drop.totalValue);
            event.detail = "Source: " + source +
                           "\nQuantity: " + String.format(java.util.Locale.ENGLISH, "%,d", drop.quantity) +
                           "\nEstimated GE value when received: " +
                           String.format(java.util.Locale.ENGLISH, "%,d", drop.totalValue) + " gp (" +
                           String.format(java.util.Locale.ENGLISH, "%,d", drop.unitPrice) + " gp each).\n"
                           + "Valuation: " + drop.valuationBasis + ".\n" +
                           ValuableDrops.message(drop.totalValue);
            journal.events.add(event);
        }
        save(directory, journal, gson);
    }

    /** Server-confirmed kills observed by Export Turtle; never presented as an unknown lifetime count. */
    public static void recordEncounterKill(Filepath directory, String account, String boss, String timestamp,
        String sessionId, Gson gson) throws IOException
    {
        if (!BossMessages.isTrackedEncounter(boss))
            return;
        Journal journal = load(directory, account, gson);
        if (!sessionId.equals(journal.sessionId))
        {
            finalizeSession(journal);
            journal.sessionId = sessionId;
            journal.sessionBossCounts.clear();
        }
        int previous = journal.trackedEncounterKills.getOrDefault(boss, 0);
        if (previous == Integer.MAX_VALUE)
            return;
        int total = previous + 1;
        journal.trackedEncounterKills.put(boss, total);
        Event event = grouped(journal, "Combat", "boss:" + boss, timestamp);
        event.count++;
        event.title = BossMessages.sessionTitle(boss, event.count);
        event.detail = "Kills recorded by Export Turtle this session: " + event.count + ".\n" +
                       BossMessages.catalog(gson).get(boss);
        if (total % 100 == 0)
        {
            Set<String> known = new HashSet<>();
            for (Event existing : journal.events)
                known.add(existing.id);
            add(journal, known, "Milestones", "tracked-encounter:" + boss + ":" + total,
                BossMessages.trackedMilestoneTitle(boss, total),
                "Kills recorded while Export Turtle was running; not your account's lifetime total.\n" +
                    BossMessages.catalog(gson).get(boss),
                timestamp, -1);
        }
        save(directory, journal, gson);
    }

    public static void recordBossCount(Filepath directory, String account, String boss, int count,
        String timestamp, String sessionId, Gson gson) throws IOException
    {
        if (count < 1 || boss.isEmpty() || boss.length() > 100)
            return;
        boss = BossMessages.canonical(boss, gson);
        if (boss == null)
            return;
        Journal journal = load(directory, account, gson);
        if (!sessionId.equals(journal.sessionId))
        {
            finalizeSession(journal);
            journal.sessionId = sessionId;
            journal.sessionBossCounts.clear();
        }
        int before = journal.bossCounts.getOrDefault(boss, count - 1);
        int sessionBefore = journal.sessionBossCounts.getOrDefault(boss, count - 1);
        journal.bossCounts.put(boss, Math.max(before, count));
        journal.sessionBossCounts.put(boss, Math.max(sessionBefore, count));
        String flavour = BossMessages.catalog(gson).get(boss);
        if (count > sessionBefore)
        {
            Event event = grouped(journal, "Combat", "boss:" + boss, timestamp);
            if (event.count == 0)
                event.fromCount = sessionBefore;
            event.count += count - sessionBefore;
            event.title = BossMessages.sessionTitle(boss, event.count);
            event.detail = "Recorded count: " + event.fromCount + " to " + count + ".\n" + flavour;
        }
        Set<String> known = new HashSet<>();
        for (Event event : journal.events)
            known.add(event.id);
        for (int milestone = (before / 100 + 1) * 100; milestone <= count; milestone += 100)
            add(journal, known, "Milestones", "boss:" + boss + ":" + milestone,
                BossMessages.milestoneTitle(boss, milestone), flavour, timestamp, -1);
        save(directory, journal, gson);
    }

    private static Journal load(Filepath directory, String account, Gson gson) throws IOException
    {
        directory.createDirectories();
        Filepath target = directory.joinSegment(SnapshotStore.fileName(account));
        try (InputStream input = target.openInputStream())
        {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES)
                throw new IOException("Event journal is too large");
            Journal journal = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), Journal.class);
            if (journal == null || journal.schemaVersion != 1 || !account.equals(journal.accountName))
                throw new IOException("Invalid event journal");
            return journal;
        }
        catch (NoSuchFileException missing)
        {
            Journal journal = new Journal();
            journal.accountName = account;
            return journal;
        }
    }

    private static void save(Filepath directory, Journal journal, Gson gson) throws IOException
    {
        if (retentionDays > 0)
        {
            java.time.Instant cutoff =
                java.time.Instant.now().minus(retentionDays, java.time.temporal.ChronoUnit.DAYS);
            journal.events.removeIf(event -> {
                try
                {
                    return event.observedAt != null &&
                        java.time.Instant.parse(event.observedAt).isBefore(cutoff);
                }
                catch (java.time.format.DateTimeParseException invalid)
                {
                    return false;
                }
            });
        }
        Filepath target = directory.joinSegment(SnapshotStore.fileName(journal.accountName));
        Filepath temporary = directory.createTempFile("events-", ".tmp");
        try
        {
            temporary.write(gson.toJson(journal));
            try
            {
                temporary.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException error)
            {
                temporary.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            temporary.deleteIfExists();
        }
    }

    private static void collect(Journal journal, JsonObject current)
    {
        JsonObject previous = journal.baseline;
        String timestamp = current.get("capturedAt").getAsString();
        Set<String> known = new HashSet<>();
        for (Event event : journal.events)
            known.add(event.id);
        collectSkills(journal, known, previous, current, timestamp);
        collectDiaryTasks(journal, previous, current, timestamp);
        if (previous.has("questPoints") && current.has("questPoints"))
            milestones(journal, known, "quest-points", integer(previous, "questPoints"),
                integer(current, "questPoints"), 50, "quest points", timestamp);
        JsonObject beforeQuests = object(previous, "quests");
        for (Map.Entry<String, JsonElement> entry : object(current, "quests").entrySet())
        {
            JsonObject after = entry.getValue().getAsJsonObject(), before =
                                                                       object(beforeQuests, entry.getKey());
            if (before.has("state") && !"FINISHED".equals(before.get("state").getAsString()) &&
                "FINISHED".equals(after.get("state").getAsString()))
                completion(
                    journal, known, "Quests", entry.getKey(), after.get("name").getAsString(), timestamp, -1);
        }
        JsonObject beforeDiaries = object(previous, "diaries");
        for (Map.Entry<String, JsonElement> region : object(current, "diaries").entrySet())
        {
            JsonObject oldRegion = object(beforeDiaries, region.getKey());
            for (Map.Entry<String, JsonElement> tier : region.getValue().getAsJsonObject().entrySet())
                if (oldRegion.has(tier.getKey()) && !oldRegion.get(tier.getKey()).getAsBoolean() &&
                    tier.getValue().getAsBoolean())
                    add(journal, known, "Diaries", region.getKey() + ":" + tier.getKey(),
                        region.getKey() + " · " + tier.getKey(), "Diary tier completed", timestamp, -1);
        }
        JsonObject beforeCombat = object(previous, "combatAchievements"),
                   afterCombat = object(current, "combatAchievements");
        if (beforeCombat.has("taskBits") && afterCombat.has("taskBits"))
        {
            int count = Math.min(beforeCombat.getAsJsonArray("taskBits").size(),
                afterCombat.getAsJsonArray("taskBits").size());
            for (int word = 0; word < count; word++)
            {
                int oldBits = beforeCombat.getAsJsonArray("taskBits").get(word).getAsInt();
                int bits = afterCombat.getAsJsonArray("taskBits").get(word).getAsInt() & ~oldBits;
                for (int bit = 0; bit < 32; bit++)
                    if ((bits & (1 << bit)) != 0)
                        completion(journal, known, "Combat", String.valueOf(word * 32 + bit),
                            "Combat achievement completed", timestamp, word * 32 + bit);
            }
        }
    }

    private static void completion(Journal journal, Set<String> known, String type, String key, String title,
        String timestamp, int taskId)
    {
        String id = type + ":" + key;
        if (known.contains(id) || !journal.completedIds.add(id))
            return;
        Event event = grouped(journal, type, "completed", timestamp);
        JsonObject detail = new JsonObject();
        detail.addProperty("title", title);
        detail.addProperty("taskId", taskId);
        event.entries.add(detail);
        event.count = event.entries.size();
        event.title = "I completed " + event.count +
                      ("Quests".equals(type) ? (event.count == 1 ? " quest." : " quests.")
                                               : (event.count == 1 ? " Combat Achievement task."
                                                                   : " Combat Achievement tasks."));
        event.detail = "Quests".equals(type)
                           ? "A few more problems solved for people who could have done it themselves."
                           : "Proof that hitting things can require a little finesse.";
    }

    private static void collectSkills(
        Journal journal, Set<String> known, JsonObject previous, JsonObject current, String timestamp)
    {
        JsonObject beforeSkills = object(previous, "skills"), afterSkills = object(current, "skills");
        int beforeTotal = 0, afterTotal = 0;
        for (Map.Entry<String, JsonElement> entry : afterSkills.entrySet())
        {
            String skill = entry.getKey();
            JsonObject before = object(beforeSkills, skill), after = entry.getValue().getAsJsonObject();
            int oldLevel = integer(before, "level"), level = integer(after, "level");
            beforeTotal += oldLevel;
            afterTotal += level;
            if (oldLevel > 0 && level > oldLevel)
            {
                Event event = grouped(journal, "Skills", skill, timestamp);
                if (event.fromLevel == 0)
                    event.fromLevel = oldLevel;
                event.count += level - oldLevel;
                event.title =
                    "I gained " + event.count + " " + skill + " level" + (event.count == 1 ? "." : "s.");
                event.detail = skill + " increased from level " + event.fromLevel + " to " + level + ".";
                if (oldLevel < 99 && level >= 99)
                    add(journal, known, "Milestones", "99:" + skill, "I reached 99 " + skill + ".",
                        "Level 99 skill milestone", timestamp, -1);
            }
            if (before.has("xp") && after.has("xp"))
                for (int million = integer(before, "xp") / 1000000 + 1;
                    million <= integer(after, "xp") / 1000000; million++)
                    add(journal, known, "Milestones", "xp:" + skill + ":" + million,
                        "I reached " + million + " million " + skill + " XP.", "Skill XP milestone",
                        timestamp, -1);
        }
        if (beforeTotal > 0 && beforeSkills.keySet().equals(afterSkills.keySet()))
            milestones(journal, known, "total", beforeTotal, afterTotal, 100, "total level", timestamp);
    }

    private static void milestones(Journal journal, Set<String> known, String key, int before, int after,
        int interval, String label, String timestamp)
    {
        for (int value = (before / interval + 1) * interval; value <= after; value += interval)
            add(journal, known, "Milestones", key + ":" + value, "I reached " + value + " " + label + ".",
                "Progress milestone", timestamp, -1);
    }

    private static Event grouped(Journal journal, String type, String key, String timestamp)
    {
        String id = type + ":session:" + journal.sessionId + ":" + key;
        for (Event event : journal.events)
            if (id.equals(event.id) && !event.finalized)
            {
                event.observedAt = timestamp;
                return event;
            }
        Event event = new Event();
        event.id = id;
        event.type = type;
        event.taskId = -1;
        event.sessionId = journal.sessionId;
        event.observedAt = timestamp;
        journal.events.add(event);
        return event;
    }

    private static void collectDiaryTasks(
        Journal journal, JsonObject previous, JsonObject current, String timestamp)
    {
        JsonObject before = object(previous, "diaryTaskStates"), after = object(current, "diaryTaskStates");
        for (Map.Entry<String, JsonElement> region : after.entrySet())
            for (Map.Entry<String, JsonElement> tier : region.getValue().getAsJsonObject().entrySet())
            {
                JsonElement old = object(before, region.getKey()).get(tier.getKey());
                if (old == null || !old.isJsonArray() ||
                    old.getAsJsonArray().size() != tier.getValue().getAsJsonArray().size())
                    continue;
                for (int index = 0; index < old.getAsJsonArray().size(); index++)
                    if (!old.getAsJsonArray().get(index).getAsBoolean() &&
                        tier.getValue().getAsJsonArray().get(index).getAsBoolean())
                    {
                        Event event = grouped(journal, "Diaries", "tasks", timestamp);
                        JsonObject detail = new JsonObject();
                        detail.addProperty("region", region.getKey());
                        detail.addProperty("tier", tier.getKey());
                        detail.addProperty("taskIndex", index);
                        if (!event.entries.contains(detail))
                            event.entries.add(detail);
                        event.count = event.entries.size();
                        event.title = "I completed " + event.count + " Achievement Diary task" +
                                      (event.count == 1 ? "." : "s.");
                        event.detail = "Gielinor's to-do list just got a little shorter.";
                    }
            }
    }

    private static void add(Journal journal, Set<String> known, String type, String key, String title,
        String detail, String timestamp, int task)
    {
        String id = type + ":" + key;
        if (!known.add(id))
            return;
        Event event = new Event();
        event.id = id;
        event.type = type;
        event.title = title;
        event.detail = detail;
        event.observedAt = timestamp;
        event.taskId = task;
        journal.events.add(event);
    }
    private static JsonObject object(JsonObject source, String key)
    {
        JsonElement value = source.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }
    private static int integer(JsonObject source, String key)
    {
        return source.has(key) ? source.get(key).getAsInt() : 0;
    }
    private static final class Journal
    {
        int schemaVersion = 1;
        String accountName;
        JsonObject baseline;
        String sessionId;
        Set<String> completedIds = new HashSet<>();
        Map<String, Integer> sessionBossCounts = new LinkedHashMap<>();
        Map<String, Integer> trackedEncounterKills = new LinkedHashMap<>();
        Map<String, Integer> bossCounts = new LinkedHashMap<>();
        List<Event> events = new ArrayList<>();
    }
    private static final class Event
    {
        String id, type, title, detail, observedAt;
        Integer itemId;
        Long quantity, unitPrice, totalValue;
        String source, valueTier, valuationBasis;
        int taskId;
        String sessionId;
        boolean finalized;
        int count, fromLevel, fromCount;
        List<JsonObject> entries = new ArrayList<>();
    }
}
