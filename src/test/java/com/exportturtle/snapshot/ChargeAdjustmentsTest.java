package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import org.junit.Test;

public class ChargeAdjustmentsTest
{
    @Test public void recognisesNamedSavingsAndBankerResourceRatios()
    {
        var saved = ChargeAdjustments.parse("Ghommal's luck saves a charge on your Tumeken's shadow.");
        assertTrue(saved.saved);
        assertEquals("shadow", saved.family);
        assertEquals(100, ChargeAdjustments.parse(
            "The banker charges your Tumeken's shadow using 200x Soul rune, 500x Chaos rune.").quantity);
        assertEquals(40, ChargeAdjustments.parse(
            "The banker charges your Tome of water using 2x Soaked page.").quantity);
        assertEquals(1000, ChargeAdjustments.parse(
            "The banker charges your Arclight using 3x Ancient shard.").quantity);
        assertNull(ChargeAdjustments.parse("The banker charges your unknown sword using 10x Coins."));
        assertNull(ChargeAdjustments.parse(
            "The banker charges your Arclight using 999999999999999999999999x Ancient shard."));
    }

    @Test public void readsTeleportVariantsWithoutInventingPerCopyIdentity()
    {
        assertEquals(Integer.valueOf(6), ChargeItemVariants.read("Amulet of glory(6)", "now").charge.quantity);
        assertEquals(Integer.valueOf(3), ChargeItemVariants.read("Amulet of glory (t)(3)", "now").charge.quantity);
        assertEquals(Integer.valueOf(7), ChargeItemVariants.read("Ring of dueling(7)", "now").charge.quantity);
        assertEquals(Integer.valueOf(0), ChargeItemVariants.read("Skills necklace", "now").charge.quantity);
        assertTrue(ChargeItemVariants.read("Amulet of eternal glory", "now").charge.permanent);
        assertNull(ChargeItemVariants.read("Unknown ring(5)", "now"));
        assertNull(ChargeItemVariants.read("Games necklace(99)", "now"));
    }
}
