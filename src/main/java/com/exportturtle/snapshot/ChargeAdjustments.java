package com.exportturtle.snapshot;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Named, game-reported changes; a delta never establishes an unknown balance. */
final class ChargeAdjustments
{
    private static final Pattern SAVED = Pattern.compile(
        "^Ghommal's luck saves a charge on your (.{1,100})\\.$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANKER = Pattern.compile(
        "^The banker charges your (.{1,100}?) using (.{1,300})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INGREDIENT = Pattern.compile(
        "([0-9,]+)x ([A-Za-z' ]+)", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> RESOURCES = Map.ofEntries(
        Map.entry("shadow", "soul rune"), Map.entry("trident_seas", "death rune"),
        Map.entry("trident_swamp", "death rune"), Map.entry("sanguinesti", "blood rune"),
        Map.entry("scythe", "blood rune"), Map.entry("blowpipe", "zulrah's scales"),
        Map.entry("serpentine", "zulrah's scales"), Map.entry("venator", "ancient essence"),
        Map.entry("quiver", "sunfire splinters"), Map.entry("arclight", "ancient shard"),
        Map.entry("bowfa", "crystal shard"), Map.entry("saeldor", "crystal shard"),
        Map.entry("crystal_helm", "crystal shard"), Map.entry("crystal_body", "crystal shard"),
        Map.entry("crystal_legs", "crystal shard"), Map.entry("crystal_shield", "crystal shard"),
        Map.entry("tome_fire", "burnt page"), Map.entry("tome_water", "soaked page"),
        Map.entry("tome_earth", "soiled page"), Map.entry("suffering", "ring of recoil"),
        Map.entry("craws_bow", "revenant ether"), Map.entry("webweaver", "revenant ether"),
        Map.entry("viggora", "revenant ether"), Map.entry("ursine", "revenant ether"),
        Map.entry("thammaron", "revenant ether"), Map.entry("accursed", "revenant ether"));

    private ChargeAdjustments()
    {
    }

    static Change parse(String message)
    {
        if (message == null || message.length() > 500)
            return null;
        String text = message.replaceAll("<[^>]*>", "").replace('\u2019', '\'').replace('\u00a0', ' ').trim();
        Matcher match = SAVED.matcher(text);
        if (match.matches())
        {
            String family = ChargeChecks.family(match.group(1));
            return family == null ? null : new Change(family, 1, true);
        }
        match = BANKER.matcher(text);
        if (!match.matches())
            return null;
        String family = ChargeChecks.family(match.group(1));
        String resource = family == null ? null : RESOURCES.get(family);
        if (resource == null)
            return null;
        Matcher ingredient = INGREDIENT.matcher(match.group(2));
        while (ingredient.find())
        {
            if (!resource.equals(ingredient.group(2).trim().toLowerCase(Locale.ROOT)))
                continue;
            try
            {
                long amount = Long.parseLong(ingredient.group(1).replace(",", ""));
                long charges = amount;
                if (family.equals("shadow") || family.equals("sanguinesti"))
                    charges = amount / 2;
                else if (family.equals("scythe"))
                    charges = amount / 3;
                else if (family.startsWith("tome_"))
                    charges = Math.multiplyExact(amount, 20);
                else if (family.equals("suffering"))
                    charges = Math.multiplyExact(amount, 40);
                else if (resource.equals("crystal shard"))
                    charges = Math.multiplyExact(amount, 100);
                else if (family.equals("arclight"))
                    charges = Math.addExact(Math.multiplyExact(amount / 3, 1000), amount % 3 * 333);
                return charges > 0 && charges <= 200_000 ? new Change(family, (int) charges, false) : null;
            }
            catch (ArithmeticException | NumberFormatException invalid)
            {
                return null;
            }
        }
        return null;
    }

    static final class Change
    {
        final String family;
        final int quantity;
        final boolean saved;

        Change(String family, int quantity, boolean saved)
        {
            this.family = family;
            this.quantity = quantity;
            this.saved = saved;
        }
    }
}
