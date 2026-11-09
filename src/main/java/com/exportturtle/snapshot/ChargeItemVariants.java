package com.exportturtle.snapshot;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Charge counts encoded by the game's item definition, including teleport jewelry. */
final class ChargeItemVariants
{
    private static final Map<String, String> FAMILIES = Map.ofEntries(
        Map.entry("amulet of glory", "glory"), Map.entry("amulet of glory (t)", "glory"),
        Map.entry("ring of dueling", "dueling"), Map.entry("games necklace", "games_necklace"),
        Map.entry("skills necklace", "skills_necklace"), Map.entry("combat bracelet", "combat_bracelet"),
        Map.entry("ring of wealth", "wealth"), Map.entry("digsite pendant", "digsite_pendant"),
        Map.entry("burning amulet", "burning_amulet"), Map.entry("necklace of passage", "passage"),
        Map.entry("slayer ring", "slayer_ring"), Map.entry("ring of returning", "returning"),
        Map.entry("teleport crystal", "teleport_crystal"), Map.entry("pharaoh's sceptre", "pharaoh"));
    private static final Pattern NUMBER = Pattern.compile("^(.+?)\\s*(?:\\(([0-9]{1,3})\\)| ([0-9]{1,3}))$");

    private ChargeItemVariants()
    {
    }

    static ChargeMessages.Result read(String itemName, String timestamp)
    {
        if (itemName == null || itemName.length() > 150)
            return null;
        String name = itemName.toLowerCase(Locale.ROOT).replace('\u2019', '\'');
        if (name.equals("amulet of eternal glory") || name.equals("slayer ring (eternal)"))
            return new ChargeMessages.Result(name.startsWith("amulet") ? "glory" : "slayer_ring",
                new ItemSnapshot.Charge(null, null, true, timestamp, "item-variant"), name);
        Matcher match = NUMBER.matcher(name);
        String base = match.matches() ? match.group(1).trim() : name;
        String family = FAMILIES.get(base);
        if (family == null)
            return null;
        int count = match.matches() ? Integer.parseInt(match.group(2) == null ? match.group(3) : match.group(2)) : 0;
        int maximum = family.equals("pharaoh") ? 100 : 8;
        return count > maximum ? null : new ChargeMessages.Result(family,
            new ItemSnapshot.Charge(count, null, false, timestamp, "item-variant"), name);
    }
}
