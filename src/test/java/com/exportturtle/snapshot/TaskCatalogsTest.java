package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TaskCatalogsTest
{
    @Test public void namedCombatTasksRespectSignedAndWordBoundaryBits()
    {
        Gson gson = new Gson();
        JsonObject catalog = gson.fromJson(
            "{\"source\":\"test\",\"revision\":1,\"license\":\"test\",\"diarySource\":\"test\","
            + "\"diaries\":{},\"combat\":["
            + "{\"id\":31,\"name\":\"One\",\"boss\":\"Boss\",\"tier\":\"Easy\",\"points\":1},"
            + "{\"id\":32,\"name\":\"Two\",\"boss\":\"Boss\",\"tier\":\"Hard\",\"points\":3},"
            + "{\"id\":64,\"name\":\"Unknown\",\"boss\":\"Boss\",\"tier\":\"Hard\",\"points\":3}]}",
            JsonObject.class);
        AccountSnapshot snapshot = new AccountSnapshot("Test", "now", Map.of(), Map.of(), Map.of(),
            new AccountSnapshot.CombatProgress(4, Map.of(), new int[] {Integer.MIN_VALUE, 1}));
        snapshot.withTaskDetails(catalog);
        var tasks = gson.toJsonTree(snapshot).getAsJsonObject().getAsJsonObject("combatAchievements").getAsJsonArray("tasks");
        assertTrue(tasks.get(0).getAsJsonObject().get("completed").getAsBoolean());
        assertTrue(tasks.get(1).getAsJsonObject().get("completed").getAsBoolean());
        assertFalse(tasks.get(2).getAsJsonObject().has("completed"));
    }

    @Test public void mismatchedDiaryCatalogNeverGuessesCompletion() throws Exception
    {
        Gson gson = new Gson();
        AccountSnapshot snapshot = new AccountSnapshot("Test", "now", Map.of(), Map.of(), Map.of(), null,
            null, Map.of("Ardougne", Map.of("Easy", List.of(true))));
        TaskCatalogs.attach(snapshot, gson);
        JsonObject json = gson.toJsonTree(snapshot).getAsJsonObject();
        var tasks = json.getAsJsonObject("diaryTaskDetails").getAsJsonObject("Ardougne").getAsJsonArray("Easy");
        assertTrue(tasks.size() > 1);
        assertTrue(tasks.get(0).getAsJsonObject().has("name"));
        assertFalse(tasks.get(0).getAsJsonObject().has("completed"));
        assertTrue(json.getAsJsonObject("taskCatalog").get("license").getAsString().contains("creativecommons.org"));
    }
}
