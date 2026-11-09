package com.exportturtle.snapshot;

import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.util.Text;

/** One server NPC-loot event represents one credited kill, regardless of drop-stack count. */
public final class EncounterKills
{
    private EncounterKills()
    {
    }

    public static String encounter(ServerNpcLoot event)
    {
        if (event == null || event.getComposition() == null || event.getItems() == null ||
            event.getItems().isEmpty())
            return null;
        String raw = event.getComposition().getName();
        if (raw == null)
            return null;
        String name = Text.removeTags(raw);
        if ("Tormented demon".equalsIgnoreCase(name))
            return "Tormented Demon";
        if ("Demonic gorilla".equalsIgnoreCase(name))
            return "Demonic Gorilla";
        return null;
    }
}
