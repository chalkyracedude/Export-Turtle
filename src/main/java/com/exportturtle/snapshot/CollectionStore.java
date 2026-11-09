package com.exportturtle.snapshot;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.util.Filepath;

/** Atomically stores collection sections without creating historical unlock events. */
public final class CollectionStore
{
    public static void writeAll(Filepath directory, List<CollectionPage> pages, Gson gson) throws IOException
    {
        if (pages.isEmpty())
            return;
        CollectionPage page = pages.get(0);
        directory.createDirectories();
        Filepath target = directory.joinSegment(SnapshotStore.fileName(page.accountName));
        Ledger ledger;
        try (InputStream input = target.openInputStream())
        {
            byte[] bytes = input.readNBytes(4 * 1024 * 1024 + 1);
            if (bytes.length > 4 * 1024 * 1024)
                throw new IOException("Collection ledger is too large");
            ledger = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), Ledger.class);
            if (ledger == null || ledger.schemaVersion != 1 || !page.accountName.equals(ledger.accountName))
                throw new IOException("Invalid collection ledger");
        }
        catch (NoSuchFileException missing)
        {
            ledger = new Ledger();
            ledger.accountName = page.accountName;
        }
        for (CollectionPage section : pages)
        {
            if (!page.accountName.equals(section.accountName))
                throw new IOException("Mixed collection accounts");
            ledger.sections.put(section.section, section.items);
        }
        ledger.capturedAt = java.time.Instant.now().toString();
        Filepath temporary = directory.createTempFile("collection-", ".tmp");
        try
        {
            temporary.write(gson.toJson(ledger));
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
    private static final class Ledger
    {
        int schemaVersion = 1;
        String accountName, capturedAt;
        Map<String, List<CollectionPage.Entry>> sections = new LinkedHashMap<>();
    }
}
