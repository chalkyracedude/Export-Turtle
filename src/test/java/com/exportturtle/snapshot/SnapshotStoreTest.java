package com.exportturtle.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Collections;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SnapshotStoreTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void replacesACompleteSnapshotWithoutLeavingTemporaryFiles() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        Gson gson = new Gson();
        AccountSnapshot first = new AccountSnapshot(
            "Test Player", "2026-01-01T00:00:00Z", Collections.emptyMap(), Collections.emptyMap());
        AccountSnapshot second = new AccountSnapshot(
            "test_player", "2026-01-02T00:00:00Z", Collections.emptyMap(), Collections.emptyMap());
        SnapshotStore.write(directory, first, gson);
        SnapshotStore.write(directory, second, gson);
        try (var reader = directory.joinSegment(SnapshotStore.fileName("Test Player")).openReader())
        {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            assertEquals("2026-01-02T00:00:00Z", json.get("capturedAt").getAsString());
        }
        try (var files = directory.walk(1))
        {
            assertFalse(files.anyMatch(file -> file.getFileName().endsWith(".tmp")));
        }
    }
}
