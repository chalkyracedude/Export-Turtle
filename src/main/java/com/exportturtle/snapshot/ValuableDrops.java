package com.exportturtle.snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;

/** Capture cached GE prices on the client thread; persist immutable values on the writer. */
public final class ValuableDrops
{
    private ValuableDrops()
    {
    }

    public static boolean acceptsAdditionalLoot(LootRecordType type)
    {
        // NPC loot already arrives through the core ServerNpcLoot feed.
        return type == LootRecordType.EVENT || type == LootRecordType.PICKPOCKET;
    }

    public static List<Drop> capture(Collection<ItemStack> items, ItemManager manager)
    {
        Map<Integer, Long> quantities = new LinkedHashMap<>();
        if (items != null)
            for (ItemStack item : items)
            {
                if (item == null || item.getId() < 0 || item.getQuantity() <= 0)
                    continue;
                int id = manager.canonicalize(item.getId());
                if (id >= 0)
                    quantities.merge(id, (long)item.getQuantity(), Long::sum);
            }
        List<Drop> drops = new ArrayList<>();
        quantities.forEach((id, quantity) -> {
            long price = DropValues.price(id, manager);
            if (price <= 0 || price > Long.MAX_VALUE / quantity)
                return;
            long total = price * quantity;
            if (total < 1_000_000L)
                return;
            ItemComposition definition = manager.getItemComposition(id);
            if (definition == null || definition.getName() == null)
                return;
            String name = Text.removeTags(definition.getName());
            if (!name.isEmpty() && name.length() <= 90 && !"null".equals(name))
                drops.add(new Drop(id, name, quantity, price));
        });
        return drops;
    }

    public static String tier(long value)
    {
        if (value >= 1_000_000_000L)
            return "1b+";
        if (value >= 100_000_000L)
            return "100m+";
        if (value >= 10_000_000L)
            return "10m+";
        return value >= 1_000_000L ? "1m+" : null;
    }

    public static String message(long value)
    {
        String tier = tier(value);
        if ("1b+".equals(tier))
            return "I checked the loot twice. Then checked my pulse.";
        if ("100m+".equals(tier))
            return "My bank just moved into a better neighborhood.";
        if ("10m+".equals(tier))
            return "Suddenly, all those supply costs feel reasonable.";
        return "1m+".equals(tier) ? "Finally, a drop worth making room for." : null;
    }

    public static final class Drop
    {
        public final int itemId;
        public final String name;
        public final String valuationBasis;
        public final long quantity, unitPrice, totalValue;
        public Drop(int itemId, String name, long quantity, long unitPrice)
        {
            this.itemId = itemId;
            this.name = name;
            this.quantity = quantity;
            this.valuationBasis = DropValues.basis(itemId);
            this.unitPrice = unitPrice;
            this.totalValue = Math.multiplyExact(quantity, unitPrice);
        }
        public String title()
        {
            return "I received " + name +
                (quantity == 1 ? "" : " (x" + String.format(Locale.ENGLISH, "%,d", quantity) + ")") + " - " +
                String.format(Locale.ENGLISH, "%,d", totalValue) + " gp.";
        }
    }
}
