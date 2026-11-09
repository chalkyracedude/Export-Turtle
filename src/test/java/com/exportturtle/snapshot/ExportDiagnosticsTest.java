package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ExportDiagnosticsTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void recordsHealthWithoutRawErrorsAndKeepsAccountsSeparate() throws Exception
    {
        var directory = temporary.getRoot().toPath();
        var root = Filepath.Unchecked.getRooted(directory);
        Gson gson = new Gson();
        assertTrue(ExportDiagnostics.record(root, "First Player", "snapshot", true, gson));
        assertTrue(ExportDiagnostics.record(root, "Second Player", "snapshot", false, gson));
        JsonObject first = gson.fromJson(Files.readString(directory.resolve(SnapshotStore.fileName("First Player"))), JsonObject.class);
        JsonObject second = gson.fromJson(Files.readString(directory.resolve(SnapshotStore.fileName("Second Player"))), JsonObject.class);
        assertTrue(first.get("success").getAsBoolean());
        assertFalse(second.get("success").getAsBoolean());
        assertEquals("Export failed; see RuneLite log", second.get("error").getAsString());
        assertFalse(second.has("stackTrace"));
        assertFalse(second.has("credentials"));
        try (var paths = Files.list(directory))
        {
            assertEquals(2, paths.count());
        }
    }
    @Test public void retainsOtherDatasetsAndLastSuccessfulTimeAfterFailure() throws Exception
    {
        var root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        Gson gson = new Gson();
        assertTrue(ExportDiagnostics.record(root, "Test Player", "snapshot", true, gson));
        assertTrue(ExportDiagnostics.record(root, "Test Player", "items", true, gson));
        String file = Files.readString(temporary.getRoot().toPath().resolve(SnapshotStore.fileName("Test Player")));
        JsonObject before = gson.fromJson(file, JsonObject.class).getAsJsonObject("channels");
        String success = before.getAsJsonObject("items").get("lastSuccessAt").getAsString();
        assertTrue(ExportDiagnostics.record(root, "Test Player", "items", false, gson));
        JsonObject after = gson.fromJson(Files.readString(temporary.getRoot().toPath().resolve(SnapshotStore.fileName("Test Player"))), JsonObject.class).getAsJsonObject("channels");
        assertTrue(after.getAsJsonObject("snapshot").get("success").getAsBoolean());
        assertFalse(after.getAsJsonObject("items").get("success").getAsBoolean());
        assertEquals(success, after.getAsJsonObject("items").get("lastSuccessAt").getAsString());
    }

}
