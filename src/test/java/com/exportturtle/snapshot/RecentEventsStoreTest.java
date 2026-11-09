package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RecentEventsStoreTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final Gson gson = new Gson();
    private AccountSnapshot snapshot(int level, String quest)
    {
        return new AccountSnapshot("Test", "2026-01-01T12:00:00Z",
            Collections.singletonMap("Attack", new AccountSnapshot.SkillProgress(level, 100)),
            Collections.singletonMap("quest", new AccountSnapshot.QuestProgress("Test quest", quest)));
    }
    private JsonObject journal(Filepath directory) throws Exception
    {
        try (var reader = new InputStreamReader(
                 directory.joinSegment(SnapshotStore.fileName("Test")).openInputStream(),
                 StandardCharsets.UTF_8))
        {
            return gson.fromJson(reader, JsonObject.class);
        }
    }
    @Test public void baselineDoesNotAnnounceOldProgressAndChangesAreDeduplicated() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        RecentEventsStore.record(directory, snapshot(1, "NOT_STARTED"), "test-session", gson);
        assertEquals(0, journal(directory).getAsJsonArray("events").size());
        RecentEventsStore.record(directory, snapshot(2, "FINISHED"), "test-session", gson);
        assertEquals(2, journal(directory).getAsJsonArray("events").size());
        RecentEventsStore.record(directory, snapshot(2, "FINISHED"), "test-session", gson);
        assertEquals(2, journal(directory).getAsJsonArray("events").size());
        RecentEventsStore.recordCollection(directory, "Test", "Dragon axe", "2026-01-01T12:01:00Z", "test-session", gson);
        RecentEventsStore.recordCollection(directory, "Test", "Dragon axe", "2026-01-01T12:01:01Z", "test-session", gson);
        assertEquals(3, journal(directory).getAsJsonArray("events").size());
        assertEquals("Logs",
            journal(directory).getAsJsonArray("events").get(2).getAsJsonObject().get("type").getAsString());
    }

    @Test public void collectionSlotsGroupActualNamesAndDeduplicateAcrossSessions() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        RecentEventsStore.recordCollection(
            directory, "Test", "Dragon axe", "2026-01-01T12:01:00Z", "one", gson);
        RecentEventsStore.recordCollection(
            directory, "Test", "Dragon axe", "2026-01-01T12:02:00Z", "one", gson);
        assertEquals("I completed 1 Collection Log slot.",
            journal(directory).getAsJsonArray("events").get(0).getAsJsonObject().get("title").getAsString());
        RecentEventsStore.recordCollection(
            directory, "Test", "Dragon pickaxe", "2026-01-01T12:03:00Z", "one", gson);
        JsonObject event = journal(directory).getAsJsonArray("events").get(0).getAsJsonObject();
        assertEquals("I completed 2 Collection Log slots.", event.get("title").getAsString());
        assertEquals("Dragon axe",
            event.getAsJsonArray("entries").get(0).getAsJsonObject().get("title").getAsString());
        assertEquals(2, event.getAsJsonArray("entries").size());
        RecentEventsStore.finishSession(directory, "Test", "one", gson);
        RecentEventsStore.recordCollection(
            directory, "Test", "Dragon axe", "2026-01-01T13:01:00Z", "two", gson);
        RecentEventsStore.recordCollection(
            directory, "Test", "Jar of dirt", "2026-01-01T13:02:00Z", "two", gson);
        assertEquals(2, journal(directory).getAsJsonArray("events").size());
        assertTrue(journal(directory)
                .getAsJsonArray("events")
                .get(0)
                .getAsJsonObject()
                .get("finalized")
                .getAsBoolean());
    }

    @Test public void automaticEncounterKillsPersistWithoutInventingLifetimeTotals() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        String time = "2026-01-01T12:01:00Z";
        for (int i = 0; i < 99; i++)
            RecentEventsStore.recordEncounterKill(directory, "Test", "Demonic Gorilla", time, "one", gson);
        RecentEventsStore.recordEncounterKill(directory, "Test", "Tormented Demon", time, "one", gson);
        var events = journal(directory).getAsJsonArray("events");
        assertEquals(2, events.size());
        assertEquals("I killed 99 demonic gorillas this session.",
            events.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals("I killed 1 tormented demon this session.",
            events.get(1).getAsJsonObject().get("title").getAsString());
        RecentEventsStore.finishSession(directory, "Test", "one", gson);
        RecentEventsStore.recordEncounterKill(directory, "Test", "Demonic Gorilla", time, "two", gson);
        events = journal(directory).getAsJsonArray("events");
        assertEquals(4, events.size());
        assertTrue(events.get(0).getAsJsonObject().get("finalized").getAsBoolean());
        assertEquals("I killed 1 demonic gorilla this session.",
            events.get(2).getAsJsonObject().get("title").getAsString());
        assertEquals("I recorded 100 demonic gorilla kills with Export Turtle.",
            events.get(3).getAsJsonObject().get("title").getAsString());
        RecentEventsStore.recordEncounterKill(directory, "Test", "Demonic Gorilla", time, "two", gson);
        assertEquals(4, journal(directory).getAsJsonArray("events").size());
        RecentEventsStore.recordEncounterKill(directory, "Other", "Demonic Gorilla", time, "two", gson);
        assertEquals(101,
            journal(directory).getAsJsonObject("trackedEncounterKills").get("Demonic Gorilla").getAsInt());
        RecentEventsStore.recordEncounterKill(directory, "Test", "Tortured Gorilla", time, "two", gson);
        assertEquals(4, journal(directory).getAsJsonArray("events").size());
    }

    private JsonObject skillJournal(Filepath directory, int level, int xp, String session) throws Exception
    {
        AccountSnapshot value = new AccountSnapshot("Test", "2026-01-01T12:00:00Z",
            Collections.singletonMap("Farming", new AccountSnapshot.SkillProgress(level, xp)),
            Collections.emptyMap());
        RecentEventsStore.record(directory, value, session, gson);
        return journal(directory);
    }

    @Test public void levelsAccumulatePerSkillAndLogoutSealsTheSession() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        skillJournal(directory, 10, 100, "one");
        skillJournal(directory, 11, 200, "one");
        JsonObject first =
            skillJournal(directory, 16, 300, "one").getAsJsonArray("events").get(0).getAsJsonObject();
        assertEquals("I gained 6 Farming levels.", first.get("title").getAsString());
        assertFalse(first.get("finalized").getAsBoolean());
        RecentEventsStore.finishSession(directory, "Test", "one", gson);
        assertTrue(journal(directory)
                .getAsJsonArray("events")
                .get(0)
                .getAsJsonObject()
                .get("finalized")
                .getAsBoolean());
        JsonObject second = skillJournal(directory, 17, 400, "two");
        assertEquals(2, second.getAsJsonArray("events").size());
        assertEquals("I gained 1 Farming level.",
            second.getAsJsonArray("events").get(1).getAsJsonObject().get("title").getAsString());
    }

    @Test public void interruptedSessionIsFinalizedWithoutReplayingProgress() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        skillJournal(directory, 10, 100, "one");
        skillJournal(directory, 12, 200, "one");
        JsonObject result = skillJournal(directory, 12, 200, "restart");
        assertEquals(1, result.getAsJsonArray("events").size());
        assertTrue(result.getAsJsonArray("events").get(0).getAsJsonObject().get("finalized").getAsBoolean());
    }

    @Test public void everyCrossedXpMilestoneIsSeparateAndDeduplicated() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        skillJournal(directory, 98, 900000, "one");
        JsonObject result = skillJournal(directory, 99, 3100000, "one");
        assertEquals(5, result.getAsJsonArray("events").size()); // skill, 99, three XP milestones
        assertEquals(5, skillJournal(directory, 99, 3100000, "one").getAsJsonArray("events").size());
    }

    @Test public void bossMilestonesUseActualKillCountMessagesWithoutBackfillingHistory() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        RecentEventsStore.recordBossCount(directory, "Test", "Vorkath", 501, "2026-01-01T12:00:00Z", "test-session", gson);
        assertEquals(1, journal(directory).getAsJsonArray("events").size());
        RecentEventsStore.recordBossCount(directory, "Test", "Vorkath", 600, "2026-01-01T12:01:00Z", "test-session", gson);
        RecentEventsStore.recordBossCount(directory, "Test", "Vorkath", 600, "2026-01-01T12:01:00Z", "test-session", gson);
        assertEquals(2, journal(directory).getAsJsonArray("events").size());
    }

    @Test public void diaryTaskDetailsAndQuestPointThresholdsSurviveRepeatedSnapshots() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        Map<String, Map<String, List<Boolean>>> before = new java.util.LinkedHashMap<>();
        before.put("Desert", Collections.singletonMap("Easy", java.util.Arrays.asList(false, false, false)));
        AccountSnapshot initial = new AccountSnapshot("Test", "2026-01-01T12:00:00Z", Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), null, null, before)
                                      .withQuestPoints(49);
        RecentEventsStore.record(directory, initial, "one", gson);
        before.put("Desert", Collections.singletonMap("Easy", java.util.Arrays.asList(true, true, true)));
        AccountSnapshot after = new AccountSnapshot("Test", "2026-01-01T12:01:00Z", Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), null, null, before)
                                    .withQuestPoints(101);
        RecentEventsStore.record(directory, after, "one", gson);
        RecentEventsStore.record(directory, after, "one", gson);
        JsonObject result = journal(directory);
        assertEquals(3, result.getAsJsonArray("events").size());
        JsonObject diary = result.getAsJsonArray("events").get(0).getAsJsonObject();
        assertEquals("I completed 3 Achievement Diary tasks.", diary.get("title").getAsString());
        assertEquals(3, diary.getAsJsonArray("entries").size());
    }

    @Test public void skillsStaySeparateAndTotalMilestonesAreEveryHundred() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        Map<String, AccountSnapshot.SkillProgress> skills = new java.util.LinkedHashMap<>();
        skills.put("Farming", new AccountSnapshot.SkillProgress(40, 100));
        skills.put("Sailing", new AccountSnapshot.SkillProgress(47, 100));
        RecentEventsStore.record(directory,
            new AccountSnapshot("Test", "2026-01-01T12:00:00Z", skills, Collections.emptyMap()), "one", gson);
        skills.put("Farming", new AccountSnapshot.SkillProgress(46, 200));
        skills.put("Sailing", new AccountSnapshot.SkillProgress(54, 200));
        RecentEventsStore.record(directory,
            new AccountSnapshot("Test", "2026-01-01T12:01:00Z", skills, Collections.emptyMap()), "one", gson);
        var events = journal(directory).getAsJsonArray("events");
        assertEquals(3, events.size());
        assertEquals(
            "I gained 6 Farming levels.", events.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals(
            "I gained 7 Sailing levels.", events.get(1).getAsJsonObject().get("title").getAsString());
        assertEquals(
            "I reached 100 total level.", events.get(2).getAsJsonObject().get("title").getAsString());
    }

    @Test public void wrongSessionCannotFinalizeAnActiveGroupAndAccountsStaySeparate() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        skillJournal(directory, 10, 100, "one");
        skillJournal(directory, 11, 200, "one");
        RecentEventsStore.finishSession(directory, "Test", "stale", gson);
        assertFalse(journal(directory)
                .getAsJsonArray("events")
                .get(0)
                .getAsJsonObject()
                .get("finalized")
                .getAsBoolean());
        RecentEventsStore.record(directory,
            new AccountSnapshot(
                "Other", "2026-01-01T12:00:00Z", Collections.emptyMap(), Collections.emptyMap()),
            "one", gson);
        assertEquals(1, journal(directory).getAsJsonArray("events").size());
    }

    @Test public void questsAndCombatTasksGroupSeparatelyWithTheirIndividualDetails() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        Map<String, AccountSnapshot.QuestProgress> quests = new java.util.LinkedHashMap<>();
        quests.put("A", new AccountSnapshot.QuestProgress("Quest A", "NOT_STARTED"));
        quests.put("B", new AccountSnapshot.QuestProgress("Quest B", "NOT_STARTED"));
        RecentEventsStore.record(directory,
            new AccountSnapshot("Test", "2026-01-01T12:00:00Z", Collections.emptyMap(), quests,
                Collections.emptyMap(),
                new AccountSnapshot.CombatProgress(0, Collections.emptyMap(), new int[] {0})),
            "one", gson);
        quests.put("A", new AccountSnapshot.QuestProgress("Quest A", "FINISHED"));
        quests.put("B", new AccountSnapshot.QuestProgress("Quest B", "FINISHED"));
        AccountSnapshot after = new AccountSnapshot("Test", "2026-01-01T12:01:00Z", Collections.emptyMap(),
            quests, Collections.emptyMap(),
            new AccountSnapshot.CombatProgress(2, Collections.emptyMap(), new int[] {3}));
        RecentEventsStore.record(directory, after, "one", gson);
        RecentEventsStore.record(directory, after, "one", gson);
        var events = journal(directory).getAsJsonArray("events");
        assertEquals(2, events.size());
        assertEquals("I completed 2 quests.", events.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals(2, events.get(0).getAsJsonObject().getAsJsonArray("entries").size());
        assertEquals("I completed 2 Combat Achievement tasks.",
            events.get(1).getAsJsonObject().get("title").getAsString());
        assertEquals(1, events.get(1)
                            .getAsJsonObject()
                            .getAsJsonArray("entries")
                            .get(1)
                            .getAsJsonObject()
                            .get("taskId")
                            .getAsInt());
    }

    @Test public void bossSessionSummariesAccumulateWithoutCrossSessionBackfill() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        RecentEventsStore.recordBossCount(
            directory, "Test", "Tormented Demon", 30, "2026-01-01T12:00:00Z", "one", gson);
        RecentEventsStore.recordBossCount(
            directory, "Test", "Tormented Demon", 31, "2026-01-01T12:01:00Z", "one", gson);
        RecentEventsStore.recordBossCount(
            directory, "Test", "Tormented Demon", 31, "2026-01-01T12:01:00Z", "one", gson);
        var first = journal(directory).getAsJsonArray("events").get(0).getAsJsonObject();
        assertEquals("I killed 2 tormented demons this session.", first.get("title").getAsString());
        assertTrue(first.get("detail").getAsString().contains(
            "They were already tormented. I felt a little unnecessary."));
        RecentEventsStore.finishSession(directory, "Test", "one", gson);
        RecentEventsStore.recordBossCount(
            directory, "Test", "Tormented Demon", 50, "2026-01-01T13:00:00Z", "two", gson);
        var events = journal(directory).getAsJsonArray("events");
        assertEquals(2, events.size());
        assertTrue(events.get(0).getAsJsonObject().get("finalized").getAsBoolean());
        assertEquals("I killed 1 tormented demon this session.",
            events.get(1).getAsJsonObject().get("title").getAsString());
    }
}
