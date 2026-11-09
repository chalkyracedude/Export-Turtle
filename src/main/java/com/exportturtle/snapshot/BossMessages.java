package com.exportturtle.snapshot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** OSRS boss notifications and individually authored journal flavor. */
public final class BossMessages
{
    private static final Pattern NOTIFICATION =
        Pattern.compile("^Your (?:completed |subdued )?(.{1,100}?) (?:kill |completion |chest )?count is: "
                        + "([0-9,]+)\\.(?:.*)$");
    private static Map<String, String> messages;
    private BossMessages()
    {
    }

    public static String[] parse(String message)
    {
        Matcher matcher = NOTIFICATION.matcher(message);
        if (!matcher.matches())
            return null;
        try
        {
            int count = Integer.parseInt(matcher.group(2).replace(",", ""));
            return count > 0 ? new String[] {matcher.group(1), Integer.toString(count)} : null;
        }
        catch (NumberFormatException error)
        {
            return null;
        }
    }

    public static synchronized Map<String, String> catalog(Gson gson)
    {
        if (messages == null)
        {
            try (InputStream input = BossMessages.class.getResourceAsStream("boss-messages.json"))
            {
                if (input == null)
                    throw new IllegalStateException("Boss message catalog is missing");
                JsonObject json =
                    gson.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
                Map<String, String> loaded = new LinkedHashMap<>();
                json.entrySet().forEach(entry -> loaded.put(entry.getKey(), entry.getValue().getAsString()));
                messages = Collections.unmodifiableMap(loaded);
            }
            catch (java.io.IOException error)
            {
                throw new IllegalStateException("Boss messages are unreadable", error);
            }
        }
        return messages;
    }

    private static String normalize(String name)
    {
        return name.toLowerCase(Locale.ROOT).replaceFirst("^the ", "").replaceAll("[^a-z0-9]", "");
    }

    public static String canonical(String name, Gson gson)
    {
        String key = normalize(name);
        if (key.equals("barrows"))
            key = "barrowschests";
        if (key.equals("lunar"))
            key = "lunarchests";
        if (key.equals("demonicgorillas"))
            key = "demonicgorilla";
        if (key.equals("tormenteddemons"))
            key = "tormenteddemon";
        if (key.equals("crystallinehunllef"))
            key = "gauntlet";
        if (key.equals("corruptedhunllef"))
            key = "corruptedgauntlet";
        if (key.equals("moonsofperil"))
            key = "lunarchests";
        for (String boss : catalog(gson).keySet())
            if (normalize(boss).equals(key))
                return boss;
        return null;
    }

    public static boolean isTrackedEncounter(String boss)
    {
        return "Tormented Demon".equals(boss) || "Demonic Gorilla".equals(boss);
    }

    public static String sessionTitle(String boss, int count)
    {
        if (isTrackedEncounter(boss))
            return "I killed " + count + " " + boss.toLowerCase(Locale.ROOT) + (count == 1 ? "" : "s") +
                " this session.";
        if (boss.contains("Chambers of Xeric") || boss.contains("Theatre of Blood") ||
            boss.contains("Tombs of Amascut") || boss.contains("Gauntlet") || boss.contains("Chests") ||
            boss.equals("The Fractured Archive"))
            return "I completed " + count + " " + boss + " run" + (count == 1 ? "" : "s") + " this session.";
        return "I defeated " + boss + " " + count + " time" + (count == 1 ? "" : "s") + " this session.";
    }

    public static String trackedMilestoneTitle(String boss, int count)
    {
        return "I recorded " + count + " " + boss.toLowerCase(Locale.ROOT) + " kills with Export Turtle.";
    }

    public static String milestoneTitle(String boss, int count)
    {
        if (isTrackedEncounter(boss))
            return "I killed " + count + " " + boss.toLowerCase(Locale.ROOT) + "s.";
        if (boss.contains("Chambers of Xeric") || boss.contains("Theatre of Blood") ||
            boss.contains("Tombs of Amascut") || boss.contains("Gauntlet") || boss.contains("Chests") ||
            boss.equals("The Fractured Archive"))
            return "I reached " + count + " " + boss + " completions.";
        return "I defeated " + boss + " " + count + " times.";
    }
}
