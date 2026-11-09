package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CollectionStoreTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void keepsSectionsAndUpdatesExistingItems() throws Exception
    {
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        Gson gson = new Gson();
        CollectionPage page = new CollectionPage();
        page.accountName = "Test";
        page.section = "Barrows";
        CollectionPage.Entry entry = new CollectionPage.Entry();
        entry.id = 100;
        entry.name = "Test item";
        entry.obtained = true;
        entry.quantity = 1;
        page.items = Collections.singletonList(entry);
        CollectionStore.writeAll(directory, Collections.singletonList(page), gson);
        page.section = "All Pets";
        CollectionStore.writeAll(directory, Collections.singletonList(page), gson);
        page.section = "Barrows";
        entry.quantity = 2;
        CollectionStore.writeAll(directory, Collections.singletonList(page), gson);
        try (var reader = new InputStreamReader(
                 directory.joinSegment(SnapshotStore.fileName("Test")).openInputStream(),
                 StandardCharsets.UTF_8))
        {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            assertEquals(2, data.getAsJsonObject("sections").size());
            assertEquals(2, data.getAsJsonObject("sections")
                                .getAsJsonArray("Barrows")
                                .get(0)
                                .getAsJsonObject()
                                .get("quantity")
                                .getAsInt());
        }
    }
}
