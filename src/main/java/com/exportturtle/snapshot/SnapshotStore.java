package com.exportturtle.snapshot;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import net.runelite.client.util.Filepath;

/** Publishes complete files so readers never observe a partial JSON document. */
public final class SnapshotStore
{
    private SnapshotStore()
    {
    }

    public static String fileName(String accountName)
    {
        String normalized =
            accountName.replace('\u00a0', ' ').replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8)) + ".json";
    }

    public static void write(Filepath directory, AccountSnapshot snapshot, Gson gson) throws IOException
    {
        TaskCatalogs.attach(snapshot, gson);
        directory.createDirectories();
        Filepath target = directory.joinSegment(fileName(snapshot.getAccountName()));
        Filepath temporary = directory.createTempFile("snapshot-", ".tmp");
        try
        {
            temporary.write(gson.toJson(snapshot));
            try
            {
                temporary.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                temporary.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            temporary.deleteIfExists();
        }
    }
}
