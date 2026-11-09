package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.client.util.Filepath;

/** Small local health record; never includes credentials or exception messages. */
public final class ExportDiagnostics
{
    private ExportDiagnostics()
    {
    }

    public static synchronized boolean record(
        Filepath directory, String account, String channel, boolean success, Gson gson)
    {
        try
        {
            directory.createDirectories();
            Filepath target = directory.joinSegment(SnapshotStore.fileName(account));
            JsonObject channels = new JsonObject();
            try (InputStream input = target.openInputStream())
            {
                byte[] bytes = input.readNBytes(65_537);
                if (bytes.length > 65_536)
                    return false;
                JsonObject previous = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
                if (previous == null || !previous.has("accountName") ||
                    !SnapshotStore.fileName(account).equals(SnapshotStore.fileName(previous.get("accountName").getAsString())))
                    return false;
                if (previous.has("channels"))
                    channels = previous.getAsJsonObject("channels").deepCopy();
            }
            catch (NoSuchFileException firstExport)
            {
                // No health record has been written for this account yet.
            }
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("schemaVersion", 1);
            report.put("accountName", account);
            report.put("pluginVersion", "0.1.0-dev");
            report.put("attemptedAt", Instant.now().toString());
            report.put("channel", channel);
            report.put("success", success);
            report.put("error", success ? null : "Export failed; see RuneLite log");
            JsonObject entry = new JsonObject();
            entry.addProperty("attemptedAt", (String)report.get("attemptedAt"));
            entry.addProperty("success", success);
            if (success)
                entry.addProperty("lastSuccessAt", (String)report.get("attemptedAt"));
            else
            {
                entry.addProperty("error", "Export failed; see RuneLite log");
                if (channels.has(channel) && channels.getAsJsonObject(channel).has("lastSuccessAt"))
                    entry.add("lastSuccessAt", channels.getAsJsonObject(channel).get("lastSuccessAt").deepCopy());
            }
            channels.add(channel, entry);
            report.put("channels", channels);
            Filepath temporary = directory.createTempFile("diagnostic-", ".tmp");
            try
            {
                temporary.write(gson.toJson(report));
                temporary.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
            finally
            {
                temporary.deleteIfExists();
            }
            return true;
        }
        catch (IOException | RuntimeException ignored)
        {
            return false;
        }
    }
}
