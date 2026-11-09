package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import net.runelite.api.TileItem;
import org.junit.Test;

public class PlunderSceptreTest
{
    @Test public void existingItemsAndEquipmentOrChargeChangesAreNotNewRewards()
    {
        PlunderSceptre tracker = new PlunderSceptre();
        assertFalse(tracker.inventory(100, 1));
        tracker.arm(100, 1);
        assertFalse(tracker.inventory(101, 1));
        assertTrue(tracker.inventory(102, 2));
        assertFalse(tracker.inventory(103, 2));
        assertFalse(tracker.ground(103, TileItem.OWNERSHIP_SELF, true));
    }
    @Test public void fullInventoryGroundRewardRequiresLocalOwnershipAndPlayersTile()
    {
        PlunderSceptre tracker = new PlunderSceptre();
        tracker.arm(100, 1);
        assertFalse(tracker.ground(101, TileItem.OWNERSHIP_OTHER, true));
        assertFalse(tracker.ground(101, TileItem.OWNERSHIP_SELF, false));
        assertTrue(tracker.ground(102, TileItem.OWNERSHIP_SELF, true));
        assertFalse(tracker.inventory(103, 2));
    }
    @Test public void cancellationExpiryAndLoginResetPreventFalseRewards()
    {
        PlunderSceptre tracker = new PlunderSceptre();
        tracker.arm(100, 0);
        assertFalse(tracker.inventory(116, 1));
        tracker.arm(120, 0);
        tracker.reset();
        assertFalse(tracker.inventory(121, 1));
        assertFalse(tracker.ground(121, TileItem.OWNERSHIP_SELF, true));
    }
}
