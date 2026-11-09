package com.exportturtle.snapshot;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Absolute readings only. Ambiguous wording needs a recent, user-initiated item check. */
final class ChargeChecks
{
    private static final Map<String, String> FAMILIES = Map.ofEntries(
        Map.entry("bow of faerdhinen", "bowfa"), Map.entry("blade of saeldor", "saeldor"),
        Map.entry("dizana's quiver", "quiver"), Map.entry("dizana's max cape", "quiver"),
        Map.entry("amulet of blood fury", "blood_fury"), Map.entry("eye of ayak", "eye_ayak"),
        Map.entry("sanguinesti staff", "sanguinesti"), Map.entry("holy sanguinesti staff", "sanguinesti"),
        Map.entry("viggora's chainmace", "viggora"), Map.entry("ursine chainmace", "ursine"),
        Map.entry("thammaron's sceptre", "thammaron"), Map.entry("accursed sceptre", "accursed"),
        Map.entry("tumeken's shadow", "shadow"),
        Map.entry("trident of the seas", "trident_seas"), Map.entry("trident of the swamp", "trident_swamp"),
        Map.entry("scythe of vitur", "scythe"), Map.entry("holy scythe of vitur", "scythe"),
        Map.entry("sanguine scythe of vitur", "scythe"), Map.entry("obligator", "obligator"),
        Map.entry("warped sceptre", "warped_sceptre"),
        Map.entry("tonalztics of ralos", "tonalztics"),
        Map.entry("bryophyta's staff", "bryophyta"), Map.entry("nature staff", "bryophyta"),
        Map.entry("abyssal tentacle", "tentacle"), Map.entry("arclight", "arclight"),
        Map.entry("crystal body", "crystal_body"), Map.entry("crystal helm", "crystal_helm"),
        Map.entry("crystal legs", "crystal_legs"), Map.entry("crystal bow", "crystal_bow"),
        Map.entry("crystal shield", "crystal_shield"), Map.entry("crystal halberd", "crystal_halberd"),
        Map.entry("ring of suffering", "suffering"), Map.entry("ring of endurance", "endurance"),
        Map.entry("celestial ring", "celestial"), Map.entry("celestial signet", "celestial"),
        Map.entry("tome of fire", "tome_fire"), Map.entry("tome of water", "tome_water"),
        Map.entry("tome of earth", "tome_earth"), Map.entry("iban's staff", "ibans"),
        Map.entry("craw's bow", "craws_bow"), Map.entry("webweaver bow", "webweaver"),
        Map.entry("venator bow", "venator"), Map.entry("echo venator bow", "venator"),
        Map.entry("toxic blowpipe", "blowpipe"), Map.entry("blazing blowpipe", "blowpipe"),
        Map.entry("serpentine helm", "serpentine"), Map.entry("magma helm", "serpentine"),
        Map.entry("tanzanite helm", "serpentine"));
    private static final Pattern SUFFIX = Pattern.compile("(?: \\([a-z]{1,3}\\))+$");
    private static final Pattern NAMED = pattern(
        "^(?:Your |The )?(.{1,90}?) (?:only )?(?:has|now has|contains) ([0-9,]+) charges?(?: (?:remaining|left))?\\.?$");
    private static final Pattern ATTACKS = pattern(
        "^Your (abyssal tentacle|arclight) can perform ([0-9,]+) more attacks?\\.$");
    private static final Pattern BLOWPIPE = pattern(
        "^Darts: (None|(.{1,60}?) x ([0-9,]+))\\. Scales: ([0-9,]+) \\(([0-9]+(?:\\.[0-9]+)?)%\\)\\.?$");
    private static final Pattern FURY_REFILL = pattern(
        "^You have successfully added [0-9,]+ hits? to your Amulet of blood fury\\. It will now work for ([0-9,]+) more hits?\\.$");
    private static final Pattern NAMED_REFILL = pattern(
        "^You charge your (.{1,90}?) with .{1,200}? It now has ([0-9,]+) charges?\\.$");
    private static final Pattern SCALES = pattern("^Scales: ([0-9,]+) \\(([0-9]+(?:\\.[0-9]+)?)%\\)\\.?$");
    private static final Pattern ENDURANCE = pattern("^Your Ring of endurance is charged with ([0-9,]+) stamina doses?\\.$");
    private static final Pattern RECOIL = pattern(
        "^Your ring currently has ([0-9,]+) recoil charges? remaining\\. The recoil effect is currently (enabled|disabled)\\.$");
    private static final Pattern RECOIL_TOPUP = pattern(
        "^You load your ring with [0-9,]+ rings? of recoil\\. It now has ([0-9,]+) recoil charges\\.$");
    private static final Pattern STAFF = pattern("^You have ([0-9,]+) charges left on the staff\\.$");
    private static final Pattern TOME = pattern("^Your tome currently holds ([0-9,]+) charges?\\.$");
    private static final Pattern FIRE_TOME = pattern(
        "^Your tome has been charged with (?:Burnt|Searing) Pages\\. It currently holds ([0-9,]+) charges?\\.$");
    private static final Pattern ETHER_BOW = pattern("^Your (?:bow|mace|sceptre|weapon) has ([0-9,]+) charges? left powering it\\.$");
    private static final Pattern TOPUP = pattern(
        "^You (?:add|apply) an additional [0-9,]+ charges? to your (.{1,90}?)\\. It now has ([0-9,]+) charges? in total\\.$");
    private static final Pattern INITIAL = pattern(
        "^You (?:add|apply) ([0-9,]+) charges? to your (.{1,90}?)\\.$");
    private static final Pattern TRIDENT_TOPUP = pattern(
        "^You add [0-9,]+ charges? to the (Trident of the (?:seas|swamp)(?: \\([a-z]+\\))*)\\. New total: ([0-9,]+)\\.?$");
    private static final Pattern CELESTIAL_TOPUP = pattern(
        "^You add [0-9,]+ charges? to your (Celestial ring|Celestial signet)\\. It now has ([0-9,]+) charges?\\.$");
    private static final Pattern VENATOR_TOPUP = pattern(
        "^You use [0-9,]+ ancient essence to charge your ((?:echo )?venator bow)\\. It now has ([0-9,]+) charges\\.$");
    private static final Pattern EMPTY = pattern(
        "^Your (warped sceptre|abyssal tentacle|arclight|Celestial ring|Celestial signet) (?:has run out of charges[.!]|has degraded\\.)$");
    private static final Pattern UNCHARGE = pattern(
        "^You (?:fully )?uncharge your (warped sceptre|(?:echo )?venator bow|Tonalztics of Ralos)(?:\\.|, regaining [0-9,]+ (?:ancient essence|sunfire splinters) in the process\\.)?$");

    private ChargeChecks()
    {
    }

    static String family(String item)
    {
        return item == null ? null : FAMILIES.get(SUFFIX.matcher(item.toLowerCase(Locale.ROOT)
            .replace('\u2019', '\'').trim()).replaceAll(""));
    }

    static ChargeMessages.Result parse(String message, String timestamp, String checkedItem)
    {
        Matcher refill = FURY_REFILL.matcher(message);
        if (refill.matches())
            return known("amulet of blood fury", refill.group(1), timestamp, "charge-message");
        refill = NAMED_REFILL.matcher(message);
        if (refill.matches())
            return known(refill.group(1), refill.group(2), timestamp, "charge-message");
        Matcher match = BLOWPIPE.matcher(message);
        if (match.matches())
        {
            Double percent = percent(match.group(5));
            Integer ammo = match.group(3) == null ? 0 : number(match.group(3));
            if (percent == null || ammo == null)
                return null;
            String variant = "blowpipe".equals(family(checkedItem)) ? checkedItem : "unidentified blowpipe";
            return reading("blowpipe", variant, match.group(4), timestamp, "check-message", percent,
                ammo == 0 && match.group(2) == null ? "None" : match.group(2), ammo, null);
        }
        match = SCALES.matcher(message);
        if (match.matches())
        {
            Double percent = percent(match.group(2));
            if (percent == null)
                return null;
            String variant = "serpentine".equals(family(checkedItem)) ? checkedItem : "unidentified serpentine helm";
            return reading("serpentine", variant, match.group(1), timestamp, "check-message", percent, null, null, null);
        }
        match = ATTACKS.matcher(message);
        if (match.matches())
            return known(match.group(1), match.group(2), timestamp, "check-message");
        match = NAMED.matcher(message);
        if (match.matches() && !Set.of("arclight", "bowfa", "saeldor", "quiver").contains(
                family(match.group(1)) == null ? "" : family(match.group(1))))
            return known(match.group(1), match.group(2), timestamp, "check-message");
        match = ENDURANCE.matcher(message);
        if (match.matches())
            return known("ring of endurance", match.group(1), timestamp, "check-message");
        match = RECOIL.matcher(message);
        if (match.matches() && "suffering".equals(family(checkedItem)))
            return reading("suffering", checkedItem, match.group(1), timestamp, "check-message", null,
                null, null, match.group(2).equalsIgnoreCase("enabled"));
        match = RECOIL_TOPUP.matcher(message);
        if (match.matches())
            return known("ring of suffering", match.group(1), timestamp, "charge-message");
        match = STAFF.matcher(message);
        if (match.matches() && "ibans".equals(family(checkedItem)))
            return known(checkedItem, match.group(1), timestamp, "check-message");
        match = TOME.matcher(message);
        if (match.matches() && Set.of("tome_fire", "tome_water", "tome_earth").contains(
                family(checkedItem) == null ? "" : family(checkedItem)))
            return known(checkedItem, match.group(1), timestamp, "check-message");
        match = FIRE_TOME.matcher(message);
        if (match.matches())
            return known("tome of fire", match.group(1), timestamp, "check-message");
        match = ETHER_BOW.matcher(message);
        if (match.matches() && Set.of("craws_bow", "webweaver", "viggora", "ursine", "thammaron", "accursed")
                .contains(family(checkedItem) == null ? "" : family(checkedItem)))
            return known(checkedItem, match.group(1), timestamp, "check-message");
        for (Pattern pattern : new Pattern[] {TOPUP, CELESTIAL_TOPUP, VENATOR_TOPUP, TRIDENT_TOPUP})
        {
            match = pattern.matcher(message);
            if (match.matches())
                return known(match.group(1), match.group(2), timestamp, "charge-message");
        }
        match = INITIAL.matcher(message);
        if (match.matches())
            return known(match.group(2), match.group(1), timestamp, "charge-message");
        for (Pattern pattern : new Pattern[] {EMPTY, UNCHARGE})
        {
            match = pattern.matcher(message);
            if (match.matches())
                return known(match.group(1), "0", timestamp, "charge-message");
        }
        return null;
    }

    private static ChargeMessages.Result known(String name, String count, String timestamp, String source)
    {
        String family = family(name);
        return family == null ? null : reading(family, name.toLowerCase(Locale.ROOT), count, timestamp,
            source, null, null, null, null);
    }

    private static ChargeMessages.Result reading(String family, String variant, String count,
        String timestamp, String source, Double percent, String ammoName, Integer ammo, Boolean enabled)
    {
        Integer quantity = number(count);
        return quantity == null ? null : new ChargeMessages.Result(family,
            new ItemSnapshot.Charge(quantity, null, false, timestamp, source, percent, ammoName, ammo, enabled), variant);
    }

    private static Integer number(String value)
    {
        try
        {
            int count = Integer.parseInt(value.replace(",", ""));
            return count >= 0 && count <= 200_000 ? count : null;
        }
        catch (NumberFormatException invalid)
        {
            return null;
        }
    }

    private static Double percent(String value)
    {
        double percent = Double.parseDouble(value);
        return Double.isFinite(percent) && percent >= 0 && percent <= 100 ? percent : null;
    }

    private static Pattern pattern(String expression)
    {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
    }
}
