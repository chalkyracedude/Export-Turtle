package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChargeChecksTest
{
    @Test public void exportsDartsAndScalePercentage()
    {
        var result = ChargeMessages.parse(
            "Darts: Adamant dart x 16,350. Scales: 16,000 (97.6%)", "now", "Blazing blowpipe");
        assertEquals("blowpipe", result.key);
        assertEquals("Blazing blowpipe", result.variant);
        assertEquals(Integer.valueOf(16000), result.charge.quantity);
        assertEquals(Integer.valueOf(16350), result.charge.ammoQuantity);
        assertEquals("Adamant dart", result.charge.ammoName);
        assertEquals(97.6, result.charge.remainingPercent, 0);
        assertNull(ChargeMessages.parse("Darts: None. Scales: 0 (101%)", "now"));
        assertEquals(Integer.valueOf(0), ChargeMessages.parse(
            "Darts: None. Scales: 0 (0.0%)", "now").charge.ammoQuantity);
    }

    @Test public void ambiguousMessagesRequireMatchingCheckContext()
    {
        String staff = "You have 1,234 charges left on the staff.";
        assertNull(ChargeMessages.parse(staff, "now"));
        assertNull(ChargeMessages.parse(staff, "now", "Tome of water"));
        assertEquals("ibans", ChargeMessages.parse(staff, "now", "Iban's staff (u)").key);
        String tome = "Your tome currently holds 300 charges.";
        assertNull(ChargeMessages.parse(tome, "now"));
        assertEquals("tome_water", ChargeMessages.parse(tome, "now", "Tome of water").key);
        String bow = "Your bow has 900 charges left powering it.";
        assertNull(ChargeMessages.parse(bow, "now"));
        assertEquals("webweaver", ChargeMessages.parse(bow, "now", "Webweaver bow").key);
        var ring = ChargeMessages.parse("Your ring currently has 500 recoil charges remaining. "
            + "The recoil effect is currently disabled.", "now", "Ring of suffering (ri)");
        assertEquals(Boolean.FALSE, ring.charge.effectEnabled);
    }

    @Test public void readsAbsoluteTotalsAndExplicitEmptyMessages()
    {
        assertEquals(Integer.valueOf(4200), ChargeMessages.parse(
            "You add an additional 200 charges to your warped sceptre. It now has 4,200 charges in total.",
            "now").charge.quantity);
        assertEquals(Integer.valueOf(0), ChargeMessages.parse(
            "Your abyssal tentacle has degraded.", "now").charge.quantity);
        assertEquals("tonalztics", ChargeMessages.parse(
            "Your Tonalztics of Ralos has 1,234 charges remaining.", "now").key);
        assertEquals("bryophyta", ChargeMessages.parse("The nature staff has 50 charges.", "now").key);
        assertEquals("crystal_body", ChargeMessages.parse(
            "Your crystal body has 500 charges remaining", "now").key);
        assertNull(ChargeMessages.parse("You load your Ring of endurance with 100 stamina doses.", "now"));
        assertEquals(Integer.valueOf(500), ChargeMessages.parse(
            "<col=ff0000>Your warped sceptre has 500\u00a0charges remaining.</col>", "now").charge.quantity);
    }
}
