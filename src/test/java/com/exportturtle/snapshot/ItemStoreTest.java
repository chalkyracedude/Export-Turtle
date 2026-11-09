package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Map;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ItemStoreTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final Gson gson = new Gson();

    private Filepath directory()
    {
        return Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
    }

    private JsonObject read(String name) throws IOException
    {
        try (var input = directory().joinSegment(SnapshotStore.fileName(name)).openInputStream())
        {
            return gson.fromJson(
                new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), JsonObject.class);
        }
    }

    @Test public void preservesLastKnownBankAcrossSessionsAndReplacesKnownEmptyBank() throws Exception
    {
        ItemSnapshot.Container bank = new ItemSnapshot.Container("2026-11-02T00:00:00Z", Map.of(28924, 150L));
        ItemSnapshot.Charge charge =
            new ItemSnapshot.Charge(20, 1.0, false, "2026-11-02T00:00:00Z", "check-message");
        ItemStore.write(directory(),
            new ItemSnapshot("Test", "first", true, Map.of("bank", bank), Map.of("quiver", charge)), gson);
        ItemStore.write(directory(), new ItemSnapshot("Test", "second", false, Map.of(), Map.of()), gson);
        JsonObject json = read("Test");
        assertEquals(150, json.getAsJsonObject("containers")
                              .getAsJsonObject("bank")
                              .getAsJsonObject("items")
                              .get("28924")
                              .getAsInt());
        assertFalse(json.get("bankObservedThisSession").getAsBoolean());
        assertEquals(
            20, json.getAsJsonObject("charges").getAsJsonObject("quiver").get("quantity").getAsInt());
        assertEquals(0, json.getAsJsonArray("chargeObservationsThisSession").size());
        assertEquals("2026-11-02T00:00:00Z",
            json.getAsJsonObject("containers").getAsJsonObject("bank").get("observedAt").getAsString());
        ItemStore.write(directory(),
            new ItemSnapshot("Test", "third", true,
                Map.of("bank", new ItemSnapshot.Container("later", Map.of())), Map.of()),
            gson);
        assertEquals(0, read("Test")
                            .getAsJsonObject("containers")
                            .getAsJsonObject("bank")
                            .getAsJsonObject("items")
                            .size());
    }

    @Test public void retainsVariantReadingsAndReplacesOnlyTheUpdatedVariant() throws Exception
    {
        var first = new ItemSnapshot.Charge(100, null, false, "first", "check-message");
        var updated = new ItemSnapshot.Charge(200, null, false, "later", "check-message");
        ItemStore.write(directory(), new ItemSnapshot("Test", "first", false, Map.of(),
            Map.of("venator", first), Map.of("venator:echo venator bow", first)), gson);
        ItemStore.write(directory(), new ItemSnapshot("Test", "empty", false, Map.of(), Map.of()), gson);
        assertEquals(100, read("Test").getAsJsonObject("chargeVariants")
            .getAsJsonObject("venator:echo venator bow").get("quantity").getAsInt());
        ItemStore.write(directory(), new ItemSnapshot("Test", "later", false, Map.of(),
            Map.of("venator", updated), Map.of("venator:echo venator bow", updated)), gson);
        assertEquals(200, read("Test").getAsJsonObject("chargeVariants")
            .getAsJsonObject("venator:echo venator bow").get("quantity").getAsInt());
    }

    @Test public void retainedEstimatesNeverAppearActiveInANewSession() throws Exception
    {
        var charge = new ItemSnapshot.Charge(100, null, false, "checked", "check-message")
            .withStatus("active", "Tracking observed use", "status")
            .consume("used", "attack-graphic", 1, null);
        ItemStore.write(directory(), new ItemSnapshot("Test", "first", false, Map.of(),
            Map.of("shadow", charge), Map.of("shadow:tumeken's shadow", charge)), gson);
        ItemStore.write(directory(), new ItemSnapshot("Test", "next-session", false,
            Map.of(), Map.of()), gson);
        var saved = read("Test").getAsJsonObject("charges").getAsJsonObject("shadow");
        assertEquals("paused", saved.get("trackingState").getAsString());
        assertEquals(99, saved.get("trackedQuantity").getAsInt());
        assertEquals("paused", read("Test").getAsJsonObject("chargeVariants")
            .getAsJsonObject("shadow:tumeken's shadow").get("trackingState").getAsString());
    }

    @Test public void retainsVaultWhenUnavailableAndReplacesAnObservedEmptyVault() throws Exception
    {
        ItemStore.write(directory(), new ItemSnapshot("Test", "first", false,
            Map.of("seedVault", new ItemSnapshot.Container("observed", Map.of(123, 20L))), Map.of()), gson);
        ItemStore.write(directory(), new ItemSnapshot("Test", "later", false, Map.of(), Map.of()), gson);
        JsonObject vault = read("Test").getAsJsonObject("containers").getAsJsonObject("seedVault");
        assertEquals("observed", vault.get("observedAt").getAsString());
        assertEquals(20, vault.getAsJsonObject("items").get("123").getAsInt());
        ItemStore.write(directory(), new ItemSnapshot("Test", "empty", false,
            Map.of("seedVault", new ItemSnapshot.Container("empty", Map.of())), Map.of()), gson);
        assertEquals(0, read("Test").getAsJsonObject("containers").getAsJsonObject("seedVault").getAsJsonObject("items").size());
    }

    @Test public void accountsStaySeparateAndFilesDoNotAccumulate() throws Exception
    {
        for (String account : new String[] {"Test", "Other", "Test"})
            ItemStore.write(directory(), new ItemSnapshot(account, "now", false, Map.of(), Map.of()), gson);
        assertEquals("Test", read("Test").get("accountName").getAsString());
        assertEquals("Other", read("Other").get("accountName").getAsString());
        try (var files = directory().walk(1))
        {
            assertEquals(2, files.filter(file -> file.getFileName().endsWith(".json")).count());
        }
    }

    @Test public void rejectsInvalidExistingLedgerWithoutReplacingIt() throws Exception
    {
        var target = directory().joinSegment(SnapshotStore.fileName("Test"));
        target.write("{\"schemaVersion\":99,\"accountName\":\"Test\"}");
        assertThrows(IOException.class,
            ()
                -> ItemStore.write(
                    directory(), new ItemSnapshot("Test", "now", false, Map.of(), Map.of()), gson));
        assertEquals(99, read("Test").get("schemaVersion").getAsInt());
    }
}
