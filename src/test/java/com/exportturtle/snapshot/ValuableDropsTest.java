package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Filepath;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ValuableDropsTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private ItemManager manager()
    {
        ItemManager manager = mock(ItemManager.class);
        when(manager.canonicalize(anyInt())).thenAnswer(call -> call.getArgument(0));
        when(manager.getItemComposition(anyInt())).thenAnswer(call -> {
            ItemComposition item = mock(ItemComposition.class);
            when(item.getName()).thenReturn("Drop " + call.getArgument(0));
            return item;
        });
        return manager;
    }

    @Test public void thresholdsUseLongStackValuesAndCombineDuplicateStacks()
    {
        ItemManager manager = manager();
        when(manager.getItemPriceWithSource(1, false)).thenReturn(500000L);
        when(manager.getItemPriceWithSource(2, false)).thenReturn(3000000000L);
        var drops = ValuableDrops.capture(
            Arrays.asList(new ItemStack(1, 1), new ItemStack(1, 1), new ItemStack(2, 2)), manager);
        assertEquals(2, drops.size());
        assertEquals(1000000L, drops.get(0).totalValue);
        assertEquals(6000000000L, drops.get(1).totalValue);
        assertTrue(ValuableDrops.capture(Collections.singletonList(new ItemStack(1, 1)), manager).isEmpty());
        assertNull(ValuableDrops.tier(999999));
        assertEquals("1m+", ValuableDrops.tier(1000000));
        assertEquals("10m+", ValuableDrops.tier(10000000));
        assertEquals("100m+", ValuableDrops.tier(100000000));
        assertEquals("1b+", ValuableDrops.tier(1000000000));
        assertEquals(
            4, new java.util
                   .HashSet<>(Arrays.asList(ValuableDrops.message(1000000), ValuableDrops.message(10000000),
                       ValuableDrops.message(100000000), ValuableDrops.message(1000000000)))
                   .size());
        when(manager.getItemPriceWithSource(2, false)).thenReturn(Long.MAX_VALUE);
        assertTrue(ValuableDrops.capture(Collections.singletonList(new ItemStack(2, 2)), manager).isEmpty());
    }

    @Test public void untradeableComponentEstimatesUseExplicitFractionsAndRequiredEquivalents()
    {
        ItemManager manager = manager();
        when(manager.getItemPriceWithSource(ItemID.NOXIOUS_HALBERD, false)).thenReturn(90000000L);
        when(manager.getItemPriceWithSource(ItemID.SOULREAPER, false)).thenReturn(400000000L);
        when(manager.getItemPriceWithSource(ItemID.ABYSSAL_BLUDGEON, false)).thenReturn(30000000L);
        when(manager.getItemPriceWithSource(ItemID.AMULET_OF_RANCOUR, false)).thenReturn(80000000L);
        when(manager.getItemPriceWithSource(ItemID.ETCHED_ELDER_VENATOR_FANG, false)).thenReturn(120000000L);
        when(manager.getItemPriceWithSource(ItemID.BRIMSTONE_RING, false)).thenReturn(1500000L);
        for (int id : new int[] {
                 ItemID.NOXIOUS_HALBERD_PART_1, ItemID.NOXIOUS_HALBERD_PART_2, ItemID.NOXIOUS_HALBERD_PART_3})
            assertEquals(30000000L, DropValues.price(id, manager));
        for (int id : new int[] {ItemID.SOULREAPER_AXE_HEAD, ItemID.SOULREAPER_AXE_EYE,
                 ItemID.SOULREAPER_AXE_STAFF, ItemID.SOULREAPER_AXE_LURE})
            assertEquals(100000000L, DropValues.price(id, manager));
        for (int id :
            new int[] {ItemID.ABYSSAL_BLUDGEON_1, ItemID.ABYSSAL_BLUDGEON_2, ItemID.ABYSSAL_BLUDGEON_3})
            assertEquals(10000000L, DropValues.price(id, manager));
        assertEquals(80000000L, DropValues.price(ItemID.ARAXYTE_FANG, manager));
        assertEquals(120000000L, DropValues.price(ItemID.ELDER_VENATOR_FANG, manager));
        assertEquals(500000L, DropValues.price(ItemID.HYDRA_EYE, manager));
        assertTrue(
            ValuableDrops.capture(Collections.singletonList(new ItemStack(ItemID.HYDRA_EYE, 1)), manager)
                .isEmpty());
        assertEquals(0, DropValues.price(ItemID.ULTOR_VESTIGE, manager));
    }

    @Test public void doomClothEstimateSubtractsOtherInputsAndNeverUsesMissingOrNegativePrices()
    {
        ItemManager manager = manager();
        when(manager.getItemPriceWithSource(ItemID.CONFLICTION_GAUNTLETS, false)).thenReturn(100000000L);
        when(manager.getItemPriceWithSource(ItemID.ZENYTE_BRACELET_ENCHANTED, false)).thenReturn(20000000L);
        assertEquals(0, DropValues.price(ItemID.MOKHAIOTL_CLOTH, manager));
        when(manager.getItemPriceWithSource(ItemID.DEMON_TEAR, false)).thenReturn(1000L);
        assertEquals(70000000L, DropValues.price(ItemID.MOKHAIOTL_CLOTH, manager));
        when(manager.getItemPriceWithSource(ItemID.CONFLICTION_GAUNTLETS, false)).thenReturn(1000L);
        assertEquals(0, DropValues.price(ItemID.MOKHAIOTL_CLOTH, manager));
    }

    @Test public void ignoresDuplicateNpcFeedAndPlayerLootButAcceptsRaidsAndUnsiredRewards()
    {
        assertFalse(ValuableDrops.acceptsAdditionalLoot(LootRecordType.NPC));
        assertFalse(ValuableDrops.acceptsAdditionalLoot(LootRecordType.PLAYER));
        assertTrue(ValuableDrops.acceptsAdditionalLoot(LootRecordType.EVENT));
        assertTrue(ValuableDrops.acceptsAdditionalLoot(LootRecordType.PICKPOCKET));
    }

    @Test public void persistedDropsKeepPriceAtReceiptAndDeduplicateOnlySameReward() throws Exception
    {
        Gson gson = new Gson();
        Filepath dir = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        var drops = Collections.singletonList(
            new ValuableDrops.Drop(ItemID.ARAXYTE_FANG, "Araxyte fang", 1, 100000000));
        RecentEventsStore.recordValuableDrops(
            dir, "Test", "reward-one", "Araxxor", drops, "2026-11-04T12:00:00Z", gson);
        RecentEventsStore.recordValuableDrops(
            dir, "Test", "reward-one", "Araxxor", drops, "2026-11-04T12:00:00Z", gson);
        RecentEventsStore.recordValuableDrops(
            dir, "Test", "reward-two", "Araxxor", drops, "2026-11-04T12:01:00Z", gson);
        try (var reader = new InputStreamReader(
                 dir.joinSegment(SnapshotStore.fileName("Test")).openInputStream(), StandardCharsets.UTF_8))
        {
            var events = gson.fromJson(reader, JsonObject.class).getAsJsonArray("events");
            assertEquals(2, events.size());
            var event = events.get(0).getAsJsonObject();
            assertEquals("Drops", event.get("type").getAsString());
            assertEquals(100000000L, event.get("totalValue").getAsLong());
            assertEquals("100m+", event.get("valueTier").getAsString());
            assertTrue(event.get("detail").getAsString().contains("Amulet of rancor"));
        }
    }
}
