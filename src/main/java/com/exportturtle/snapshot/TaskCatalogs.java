package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Adds names on the writer thread using our bundled task catalog. */
final class TaskCatalogs
{
    private static JsonObject catalog;

    private TaskCatalogs()
    {
    }

    static synchronized void attach(AccountSnapshot snapshot, Gson gson) throws IOException
    {
        if (catalog == null)
        {
            try (InputStream input = TaskCatalogs.class.getResourceAsStream("task-catalog.json"))
            {
                if (input == null)
                    throw new IOException("Task catalog is missing");
                catalog = gson.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
            }
        }
        snapshot.withTaskDetails(catalog);
    }
}
