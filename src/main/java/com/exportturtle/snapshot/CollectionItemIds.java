package com.exportturtle.snapshot;

import net.runelite.api.gameval.ItemID;

/** Item aliases which represent the same collection-log unlock. */
final class CollectionItemIds
{
    private CollectionItemIds()
    {
    }

    static int canonical(int id)
    {
        switch (id)
        {
        case ItemID.FOSSIL_MOTHERLODE_REWARD_HAT:
            return ItemID.MOTHERLODE_REWARD_HAT;
        case ItemID.FOSSIL_MOTHERLODE_REWARD_TOP:
            return ItemID.MOTHERLODE_REWARD_TOP;
        case ItemID.FOSSIL_MOTHERLODE_REWARD_LEGS:
            return ItemID.MOTHERLODE_REWARD_LEGS;
        case ItemID.FOSSIL_MOTHERLODE_REWARD_BOOTS:
            return ItemID.MOTHERLODE_REWARD_BOOTS;
        default:
            return id;
        }
    }
}
