package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import net.runelite.client.util.Filepath;

/** One replaceable ownership ledger per account; never discards an unknown bank. */
public final class ItemStore
{
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    public static String write(Filepath directory, ItemSnapshot snapshot, Gson gson) throws IOException
    {
        directory.createDirectories();
        Filepath target = directory.joinSegment(SnapshotStore.fileName(snapshot.accountName));
        JsonObject previous = new JsonObject();
        try (InputStream input = target.openInputStream())
        {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES)
                throw new IOException("Item ledger exceeds size limit");
            previous = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
            if (previous == null || !previous.has("schemaVersion") ||
                previous.get("schemaVersion").getAsInt() != 1 || !previous.has("accountName") ||
                !SnapshotStore.fileName(snapshot.accountName)
                    .equals(SnapshotStore.fileName(previous.get("accountName").getAsString())))
                throw new IOException("Invalid item ledger");
        }
        catch (NoSuchFileException missing)
        { /* First observation for this account. */
        }
        JsonObject current = gson.toJsonTree(snapshot).getAsJsonObject();
        for (String field : new String[] {"containers", "charges", "chargeVariants"})
        {
            JsonObject merged =
                previous.has(field) ? previous.getAsJsonObject(field).deepCopy() : new JsonObject();
            current.getAsJsonObject(field).entrySet().forEach(
                entry -> merged.add(entry.getKey(), entry.getValue()));
            current.add(field, merged);
        }
        for (String field : new String[] {"charges", "chargeVariants"})
            current.getAsJsonObject(field).entrySet().forEach(entry -> {
                String family = entry.getKey().split(":", 2)[0];
                JsonObject charge = entry.getValue().getAsJsonObject();
                boolean observed = field.equals("charges") ? snapshot.charges.containsKey(family) :
                    snapshot.chargeVariants.containsKey(entry.getKey());
                if (!observed && charge.has("trackingState"))
                {
                    charge.addProperty("trackingState", "paused");
                    charge.addProperty("trackingReason", "Not observed this session");
                    charge.addProperty("trackingStatusAt", snapshot.capturedAt);
                }
            });
        current.add("chargeObservationsThisSession", gson.toJsonTree(snapshot.charges.keySet()));
        Filepath temporary = directory.createTempFile("items-", ".tmp");
        try
        {
            temporary.write(gson.toJson(current));
            try
            {
                temporary.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException unsupported)
            {
                temporary.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            temporary.deleteIfExists();
        }
        JsonObject bank = current.getAsJsonObject("containers").getAsJsonObject("bank");
        String summary = bank == null ? "Bank: not opened"
                                      : "Bank: " + bank.getAsJsonObject("items").size() + " stacks" +
                                            (snapshot.bankObservedThisSession ? "" : " (last known)");
        return summary + "; " + current.getAsJsonObject("charges").size() + " charge records";
    }
}
