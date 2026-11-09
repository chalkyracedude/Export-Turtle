package com.exportturtle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(ExportTurtleConfig.GROUP)
public interface ExportTurtleConfig extends Config
{
    String GROUP = "turtle-data-export";

    @ConfigItem(keyName = "turtleCursor", name = "Soup Cursor", position = -1,
        description = "Use an animated Soup cursor over the game and Export Turtle panel")
    default boolean turtleCursor()
    {
        return false;
    }

    @ConfigItem(keyName = "localSnapshots", name = "Enable local exports",
        description = "Save account data and portraits to local files")
    default boolean localSnapshots()
    {
        return true;
    }
    @ConfigSection(name = "Data exports", description = "Choose which data is saved locally", position = 1)
    String exports = "exports";

    @ConfigItem(keyName = "itemExports", name = "Items and charges",
        description = "Export bank, seed vault, inventory, equipment and charges", section = exports, position = 2)
    default boolean itemExports()
    {
        return true;
    }

    @ConfigItem(keyName = "collectionExports", name = "Collection log", description = "Export collection log",
        section = exports, position = 3)
    default boolean collectionExports()
    {
        return true;
    }

    @ConfigItem(keyName = "eventHistory", name = "Recent events",
        description = "Record local account milestones and drops", section = exports, position = 4)
    default boolean eventHistory()
    {
        return true;
    }

    @ConfigItem(keyName = "portraitExports", name = "Portraits and selfies",
        description = "Allow character portrait and pose capture", section = exports, position = 5)
    default boolean portraitExports()
    {
        return true;
    }

    @ConfigItem(keyName = "clanExports", name = "Clan details", description = "Include clan name and rank",
        section = exports, position = 6)
    default boolean clanExports()
    {
        return true;
    }

    @ConfigItem(keyName = "diagnostics", name = "Export diagnostics",
        description = "Save local export health records", section = exports, position = 7)
    default boolean diagnostics()
    {
        return true;
    }

    @Range(min = 0, max = 3650)
    @ConfigItem(keyName = "eventRetentionDays", name = "Keep events (d)",
        description = "0 keeps all history; older events are removed on the next event export", position = 11)
    default int eventRetentionDays()
    {
        return 0;
    }

    @Range(min = 5, max = 600)
    @ConfigItem(keyName = "syncSeconds", name = "Sync interval (s)",
        description = "Seconds between progress exports", position = 10)
    default int syncSeconds()
    {
        return 30;
    }
}
