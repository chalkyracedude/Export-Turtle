package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import net.runelite.client.util.Filepath;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ExportRecoveryTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final Gson gson = new Gson();

    @After public void resetRetention() { RecentEventsStore.setRetentionDays(0); }

    @Test public void failedSnapshotReplacementCleansTemporaryFileAndCanRetry() throws Exception
    {
        var path = temporary.getRoot().toPath();
        var root = Filepath.Unchecked.getRooted(path);
        var target = path.resolve(SnapshotStore.fileName("Test"));
        Files.createDirectory(target);
        Files.writeString(target.resolve("blocker"), "synthetic test data");
        var snapshot = new AccountSnapshot("Test", Instant.now().toString(), Collections.emptyMap(), Collections.emptyMap());
        try
        {
            SnapshotStore.write(root, snapshot, gson);
            fail("Replacing a nonempty directory must fail");
        }
        catch (IOException expected) { }
        try (var files = Files.list(path))
        {
            assertFalse(files.anyMatch(file -> file.toString().endsWith(".tmp")));
        }
        Files.delete(target.resolve("blocker"));
        Files.delete(target);
        SnapshotStore.write(root, snapshot, gson);
        assertEquals("Test", gson.fromJson(Files.readString(target), JsonObject.class).get("accountName").getAsString());
    }

    @Test public void retentionPrunesOldEventsButKeepsRecentOnesAndBaseline() throws Exception
    {
        var root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        RecentEventsStore.setRetentionDays(0);
        RecentEventsStore.recordCollection(root, "Test", "Old item", Instant.now().minus(90, ChronoUnit.DAYS).toString(), "old", gson);
        RecentEventsStore.recordCollection(root, "Test", "Recent item", Instant.now().toString(), "new", gson);
        assertEquals(2, readJournal().getAsJsonArray("events").size());
        RecentEventsStore.setRetentionDays(30);
        RecentEventsStore.record(root, new AccountSnapshot("Test", Instant.now().toString(), Collections.emptyMap(), Collections.emptyMap()), "new", gson);
        JsonObject journal = readJournal();
        assertEquals(1, journal.getAsJsonArray("events").size());
        assertTrue(journal.has("baseline"));
        assertTrue(journal.getAsJsonArray("events").get(0).toString().contains("Recent item"));
    }

    @Test public void diagnosticsFailureReturnsFalseAndRecovers() throws Exception
    {
        var path = temporary.getRoot().toPath().resolve("blocked");
        Files.writeString(path, "synthetic test data");
        var root = Filepath.Unchecked.getRooted(path);
        assertFalse(ExportDiagnostics.record(root, "Test", "snapshot", true, gson));
        Files.delete(path);
        assertTrue(ExportDiagnostics.record(root, "Test", "snapshot", true, gson));
    }

    private JsonObject readJournal() throws Exception
    {
        return gson.fromJson(Files.readString(temporary.getRoot().toPath().resolve(SnapshotStore.fileName("Test"))), JsonObject.class);
    }
}
