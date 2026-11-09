package com.exportturtle;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.exportturtle.snapshot.AccountSnapshot;
import com.exportturtle.snapshot.SnapshotStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class SnapshotContractTest
{
    @Test public void serializesOnlyTheVersionedAccountData()
    {
        Map<String, AccountSnapshot.SkillProgress> skills = new LinkedHashMap<>();
        skills.put("Attack", new AccountSnapshot.SkillProgress(50, 101333));
        Map<String, AccountSnapshot.QuestProgress> quests = new LinkedHashMap<>();
        quests.put("COOKS_ASSISTANT", new AccountSnapshot.QuestProgress("Cook's Assistant", "FINISHED"));
        AccountSnapshot snapshot = new AccountSnapshot("Test Player", "2026-01-01T00:00:00Z", skills, quests);
        skills.clear();
        JsonObject json = new Gson().toJsonTree(snapshot).getAsJsonObject();
        assertEquals(1, json.get("schemaVersion").getAsInt());
        assertEquals(6, json.size());
        assertEquals(101333, json.getAsJsonObject("skills").getAsJsonObject("Attack").get("xp").getAsInt());
        assertEquals("FINISHED",
            json.getAsJsonObject("quests").getAsJsonObject("COOKS_ASSISTANT").get("state").getAsString());
    }

    @Test public void fileNamesAreStableAndCannotCarryPathSegments()
    {
        assertEquals(SnapshotStore.fileName("Test_Player"), SnapshotStore.fileName("test player"));
        assertTrue(SnapshotStore.fileName("../test").matches("[a-f0-9-]{36}\\.json"));
    }
}
