package com.exportturtle.snapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

/** Explicit component estimates; never use an untradeable item's shop/alchemy value. */
public final class DropValues
{
    private static final Map<Integer, Rule> RULES = new LinkedHashMap<>();
    static
    {
        fraction(ItemID.NOXIOUS_HALBERD, 3, "One third of the noxious halberd GE value",
            ItemID.NOXIOUS_HALBERD_PART_1, ItemID.NOXIOUS_HALBERD_PART_2, ItemID.NOXIOUS_HALBERD_PART_3);
        fraction(ItemID.SOULREAPER, 4, "One quarter of the Soulreaper axe GE value",
            ItemID.SOULREAPER_AXE_HEAD, ItemID.SOULREAPER_AXE_EYE, ItemID.SOULREAPER_AXE_STAFF,
            ItemID.SOULREAPER_AXE_LURE);
        fraction(ItemID.ABYSSAL_BLUDGEON, 3, "One third of the abyssal bludgeon GE value",
            ItemID.ABYSSAL_BLUDGEON_1, ItemID.ABYSSAL_BLUDGEON_2, ItemID.ABYSSAL_BLUDGEON_3);
        fraction(ItemID.BRIMSTONE_RING, 3, "One third of the brimstone ring GE value", ItemID.HYDRA_EYE,
            ItemID.HYDRA_FANG, ItemID.HYDRA_HEART);
        fraction(ItemID.AMULET_OF_RANCOUR, 1, "Amulet of rancor GE value (component estimate)",
            ItemID.ARAXYTE_FANG);
        fraction(ItemID.ETCHED_ELDER_VENATOR_FANG, 1, "Etched elder venator fang GE value",
            ItemID.ELDER_VENATOR_FANG);
        fraction(ItemID.ULTOR_RING, 1, "Ultor ring GE value (component estimate)", ItemID.ULTOR_VESTIGE);
        fraction(
            ItemID.VENATOR_RING, 1, "Venator ring GE value (component estimate)", ItemID.VENATOR_VESTIGE);
        fraction(ItemID.MAGUS_RING, 1, "Magus ring GE value (component estimate)", ItemID.MAGUS_VESTIGE);
        fraction(
            ItemID.BELLATOR_RING, 1, "Bellator ring GE value (component estimate)", ItemID.BELLATOR_VESTIGE);
    }
    private DropValues()
    {
    }
    private static void fraction(int target, int divisor, String label, int... ids)
    {
        for (int id : ids)
            RULES.put(id, new Rule(target, divisor, label));
    }

    public static long price(int id, ItemManager manager)
    {
        Rule rule = RULES.get(id);
        if (rule != null)
            return Math.max(0, manager.getItemPriceWithSource(rule.target, false)) / rule.divisor;
        if (id == ItemID.MOKHAIOTL_CLOTH)
        {
            long result = manager.getItemPriceWithSource(ItemID.CONFLICTION_GAUNTLETS, false);
            long bracelet = manager.getItemPriceWithSource(ItemID.ZENYTE_BRACELET_ENCHANTED, false);
            long tears = manager.getItemPriceWithSource(ItemID.DEMON_TEAR, false);
            if (result <= 0 || bracelet <= 0 || tears <= 0 || tears > Long.MAX_VALUE / 10000)
                return 0;
            long afterBracelet = Math.max(0, result - bracelet);
            return Math.max(0, afterBracelet - tears * 10000);
        }
        return manager.getItemPriceWithSource(id, false);
    }

    public static String basis(int id)
    {
        Rule rule = RULES.get(id);
        if (rule != null)
            return rule.label;
        return id == ItemID.MOKHAIOTL_CLOTH
            ? "Confliction gauntlets GE value minus tormented bracelet and 10,000 demon tears"
            : "Grand Exchange item/variant value";
    }
    private static final class Rule
    {
        final int target, divisor;
        final String label;
        Rule(int target, int divisor, String label)
        {
            this.target = target;
            this.divisor = divisor;
            this.label = label;
        }
    }
}
