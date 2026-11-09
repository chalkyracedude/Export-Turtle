package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class CollectionItemIdsTest
{
    @Test public void shopVariantsShareSlotsWithoutCollapsingOtherItems()
    {
        assertEquals(
            ItemID.MOTHERLODE_REWARD_HAT, CollectionItemIds.canonical(ItemID.FOSSIL_MOTHERLODE_REWARD_HAT));
        assertEquals(
            ItemID.MOTHERLODE_REWARD_TOP, CollectionItemIds.canonical(ItemID.FOSSIL_MOTHERLODE_REWARD_TOP));
        assertEquals(
            ItemID.MOTHERLODE_REWARD_LEGS, CollectionItemIds.canonical(ItemID.FOSSIL_MOTHERLODE_REWARD_LEGS));
        assertEquals(ItemID.MOTHERLODE_REWARD_BOOTS,
            CollectionItemIds.canonical(ItemID.FOSSIL_MOTHERLODE_REWARD_BOOTS));
        assertEquals(11341, CollectionItemIds.canonical(11341));
        assertEquals(11342, CollectionItemIds.canonical(11342));
    }
}
