package com.exportturtle.snapshot;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads game-produced check messages; no combat-use estimates or charge settings. */
final class ChargeMessages
{
    private static final Pattern CHECK =
        Pattern.compile("^(?:Your )?(Dizana's quiver|Dizana's max cape|Bow of faerdhinen|Blade of "
                            + "Saeldor|Arclight) has ([\\d,]+) charges?(?: (?:remaining|left))?\\.(.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRESS = Pattern.compile(
        "([\\d.]+)% (?:progress towards being (?:blessed|corrupted)|infused)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUIVER_CHARGED = Pattern.compile(
        "^You use [\\d,]+ Sunfire splinters to charge Dizana's quiver\\. It now has ([\\d,]+) charges\\.$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern EQUIPMENT_CHECK = Pattern.compile(
        "^(?:Your )?(Obligator|(?:Echo )?Venator bow|Toxic blowpipe|Blazing blowpipe|Trident of the "
            + "seas(?: \\((?:e|o|eo)\\))*|Trident of the swamp(?: \\((?:e|o|eo)\\))*|Serpentine helm|Magma "
            + "helm|Tanzanite helm|Amulet of blood fury|Tumeken's shadow|(?:Holy )?Sanguinesti "
            +
            "staff|(?:(?:Holy|Sanguine) )?Scythe of vitur) (?:has|contains) ([\\d,]+) (?:charges?|scales)(?: "
            + "(?:remaining|left))?\\.(?:.*)$",
        Pattern.CASE_INSENSITIVE);

    static Result parse(String message, String timestamp)
    {
        return parse(message, timestamp, null);
    }

    static Result parse(String message, String timestamp, String checkedItem)
    {
        if (message == null || message.length() > 500)
            return null;
        String normalized = message.replace('\u2019', '\'');
        normalized = normalized.replaceAll("<[^>]*>", "").replace('\u00a0', ' ').trim();
        Result additional = ChargeChecks.parse(normalized, timestamp, checkedItem);
        if (additional != null)
            return additional;
        Matcher ayak = Pattern
                           .compile("^(?:The|Your) Eye of Ayak has (?:been charged with (?:runes|demon tears)\\. It "
                                        + "currently has )?([0-9,]+) charges?(?: remaining)?\\.$",
                               Pattern.CASE_INSENSITIVE)
                           .matcher(normalized);
        if (ayak.matches())
            return count("eye_ayak", "eye of ayak", ayak.group(1), timestamp);
        if (normalized.equalsIgnoreCase("You uncharge the Eye of Ayak."))
            return count("eye_ayak", "eye of ayak", "0", timestamp);
        Matcher fury =
            Pattern
                .compile("^Your Amulet of blood fury (?:will work for|can perform) ([0-9,]+) more hits?\\.$",
                    Pattern.CASE_INSENSITIVE)
                .matcher(normalized);
        if (fury.matches())
            return count("blood_fury", "amulet of blood fury", fury.group(1), timestamp);
        Matcher equipment = EQUIPMENT_CHECK.matcher(normalized);
        if (equipment.matches())
        {
            String key;
            switch (equipment.group(1).toLowerCase(Locale.ROOT).replaceAll("(?: \\((?:e|o|eo)\\))+$", ""))
            {
            case "obligator":
                key = "obligator";
                break;
            case "venator bow":
            case "echo venator bow":
                key = "venator";
                break;
            case "blazing blowpipe":
            case "toxic blowpipe":
                key = "blowpipe";
                break;
            case "trident of the seas":
                key = "trident_seas";
                break;
            case "trident of the swamp":
                key = "trident_swamp";
                break;
            case "magma helm":
            case "tanzanite helm":
            case "serpentine helm":
                key = "serpentine";
                break;
            case "amulet of blood fury":
                key = "blood_fury";
                break;
            case "tumeken's shadow":
                key = "shadow";
                break;
            case "holy sanguinesti staff":
            case "sanguinesti staff":
                key = "sanguinesti";
                break;
            case "holy scythe of vitur":
            case "sanguine scythe of vitur":
            case "scythe of vitur":
                key = "scythe";
                break;
            default:
                return null;
            }
            int quantity = Integer.parseInt(equipment.group(2).replace(",", ""));
            if (quantity > 200_000)
                return null;
            return new Result(key, new ItemSnapshot.Charge(quantity, null, false, timestamp, "check-message"),
                equipment.group(1).toLowerCase(Locale.ROOT));
        }
        Matcher check = CHECK.matcher(normalized);
        if (check.matches())
        {
            String name = check.group(1).toLowerCase(Locale.ROOT);
            String key = name.startsWith("dizana")  ? "quiver"
                         : name.startsWith("bow")   ? "bowfa"
                         : name.startsWith("blade") ? "saeldor"
                                                    : "arclight";
            int quantity = Integer.parseInt(check.group(2).replace(",", ""));
            if (quantity < 0 || quantity > 200_000)
                return null;
            Matcher progress = PROGRESS.matcher(check.group(3));
            Double percent = progress.find() ? Double.valueOf(progress.group(1)) : null;
            if (percent != null && (!Double.isFinite(percent) || percent < 0 || percent > 100))
                return null;
            return new Result(
                key, new ItemSnapshot.Charge(quantity, percent, false, timestamp, "check-message"));
        }
        Matcher charge = QUIVER_CHARGED.matcher(normalized);
        if (charge.matches())
        {
            int quantity = Integer.parseInt(charge.group(1).replace(",", ""));
            if (quantity <= 200_000)
                return new Result(
                    "quiver", new ItemSnapshot.Charge(quantity, null, false, timestamp, "charge-message"));
        }
        return null;
    }

    static Result count(String family, String variant, String quantity, String timestamp)
    {
        int value;
        try
        {
            value = Integer.parseInt(quantity.replace(",", ""));
        }
        catch (NumberFormatException invalid)
        {
            return null;
        }
        if (value < 0 || value > 200000)
            return null;
        return new Result(
            family, new ItemSnapshot.Charge(value, null, false, timestamp, "check-message"), variant);
    }

    static final class Result
    {
        final String key;
        final ItemSnapshot.Charge charge;
        final String variant;
        Result(String key, ItemSnapshot.Charge charge)
        {
            this(key, charge, key);
        }
        Result(String key, ItemSnapshot.Charge charge, String variant)
        {
            this.key = key;
            this.charge = charge;
            this.variant = variant;
        }
    }
}
