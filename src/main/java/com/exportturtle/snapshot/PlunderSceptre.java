package com.exportturtle.snapshot;

import net.runelite.api.TileItem;
import net.runelite.api.gameval.ItemID;

/** Short-lived reward detection armed only by the local player's actual plunder action. */
public final class PlunderSceptre
{
    private int armedTick = -100, before;
    public void reset()
    {
        armedTick = -100;
    }
    public void arm(int tick, int held)
    {
        armedTick = tick;
        before = held;
    }
    public boolean active(int tick)
    {
        return tick >= armedTick && tick - armedTick <= 15;
    }
    public boolean inventory(int tick, int held)
    {
        if (!active(tick) || held <= before)
            return false;
        reset();
        return true;
    }
    public boolean ground(int tick, int ownership, boolean atPlayer)
    {
        if (!active(tick) || ownership != TileItem.OWNERSHIP_SELF || !atPlayer)
            return false;
        reset();
        return true;
    }
    public static boolean isSceptre(int id)
    {
        return id == ItemID.PHARAOHS_SCEPTRE || id == ItemID.PHARAOHS_SCEPTRE_CHARGED ||
            id == ItemID.PHARAOHS_SCEPTRE_CHARGED_INITIAL;
    }
}
