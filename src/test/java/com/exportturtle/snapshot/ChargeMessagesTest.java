package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChargeMessagesTest
{
    @Test public void readsVerifiedObligatorCheckMessage()
    {
        var result = ChargeMessages.parse("Your Obligator has 8,376 charges remaining.", "now");
        assertNotNull(result);
        assertEquals("obligator", result.key);
        assertEquals(Integer.valueOf(8376), result.charge.quantity);
    }

    @Test public void readsCountsProgressAndTrueZeroWithoutGuessing()
    {
        var quiver = ChargeMessages.parse(
            "Dizana's quiver has 1,234 charges remaining. It has made 25% progress towards being blessed.",
            "now");
        assertEquals("quiver", quiver.key);
        assertEquals(Integer.valueOf(1234), quiver.charge.quantity);
        assertEquals(25.0, quiver.charge.progressPercent, 0);
        var bow = ChargeMessages.parse("Your bow of Faerdhinen has 5,000 charges remaining. It has made "
                                           + "20% progress towards being corrupted.",
            "now");
        assertEquals("bowfa", bow.key);
        assertEquals(20.0, bow.charge.progressPercent, 0);
        var sword = ChargeMessages.parse("Your arclight has 0 charges left. It is 42% infused.", "now");
        assertEquals("arclight", sword.key);
        assertEquals(Integer.valueOf(0), sword.charge.quantity);
        assertEquals(42.0, sword.charge.progressPercent, 0);
        assertNull(ChargeMessages.parse("Your arclight has 50 charges left.", "now").charge.progressPercent);
    }

    @Test public void readsQuiverTopupsWithoutInventingBlessingProgress()
    {
        var result = ChargeMessages.parse(
            "You use 500 Sunfire splinters to charge Dizana's quiver. It now has 1,000 charges.", "now");
        assertEquals(Integer.valueOf(1000), result.charge.quantity);
        assertNull(result.charge.progressPercent);
    }

    @Test public void readsObservedGameMessages()
    {
        String[][] cases = {{"The Eye of Ayak has 50,000 charges remaining.", "eye_ayak", "50000"},
            {"The Eye of Ayak has been charged with runes. It currently has 1 charge.", "eye_ayak", "1"},
            {"The Eye of Ayak has been charged with demon tears. It currently has 123 charges.", "eye_ayak",
                "123"},
            {"You uncharge the Eye of Ayak.", "eye_ayak", "0"},
            {"Darts: Adamant dart x 16,350. Scales: 16,000 (97.6%)", "blowpipe", "16000"},
            {"Darts: None. Scales: 0 (0.0%)", "blowpipe", "0"},
            {"Your echo venator bow has 50,000 charges remaining.", "venator", "50000"},
            {"Your Scythe of Vitur has 7,284 charges remaining.", "scythe", "7284"},
            {"Tumeken's shadow has 10,000 charges remaining.", "shadow", "10000"},
            {"Your Trident of the Swamp (e) (o) has 8,517 charges.", "trident_swamp", "8517"},
            {"Your Sanguinesti staff has 15,625 charges remaining.", "sanguinesti", "15625"},
            {"Scales: 9,740 (88.5%)", "serpentine", "9740"},
            {"Your Amulet of blood fury will work for 21,973 more hits.", "blood_fury", "21973"}};
        for (String[] row : cases)
        {
            var result = ChargeMessages.parse(row[0], "now");
            assertNotNull(row[0], result);
            assertEquals(row[1], result.key);
            assertEquals(Integer.valueOf(row[2]), result.charge.quantity);
            assertNull(result.charge.progressPercent);
            assertNull(ChargeMessages.parse("Friend says " + row[0], "now"));
        }
    }

    @Test public void recognizesCosmeticAndEnhancedVariants()
    {
        String[][] variants = {{"Blazing blowpipe", "blowpipe"}, {"Holy sanguinesti staff", "sanguinesti"},
            {"Holy scythe of vitur", "scythe"}, {"Sanguine scythe of vitur", "scythe"},
            {"Magma helm", "serpentine"}, {"Tanzanite helm", "serpentine"},
            {"Trident of the seas (e)", "trident_seas"}, {"Trident of the seas (o)", "trident_seas"},
            {"Trident of the seas (eo)", "trident_seas"}, {"Trident of the swamp (e)", "trident_swamp"},
            {"Trident of the swamp (o)", "trident_swamp"}, {"Trident of the swamp (eo)", "trident_swamp"}};
        for (String[] variant : variants)
        {
            var result = ChargeMessages.parse("Your " + variant[0] + " has 1,234 charges left.", "now");
            assertNotNull(variant[0], result);
            assertEquals(variant[1], result.key);
            assertEquals(Integer.valueOf(1234), result.charge.quantity);
            assertNull(result.charge.progressPercent);
            assertNull(
                ChargeMessages.parse("Friend says Your " + variant[0] + " has 123 charges left.", "now"));
        }
    }

    @Test public void rejectsUnrelatedMessagesAndInvalidValues()
    {
        assertNull(ChargeMessages.parse("Friend says Your arclight has 5 charges left.", "now"));
        assertNull(ChargeMessages.parse("Your arclight has 999,999 charges left.", "now"));
        assertNull(ChargeMessages.parse(
            "Dizana's quiver has 10 charges remaining. It has made 101% progress towards being blessed.",
            "now"));
        assertNull(ChargeMessages.parse("Set withdrawal charges to 5000", "now"));
    }
    @Test public void readsExplicitEquipmentCountsWithoutInventingInfusion()
    {
        String[][] cases = {{"Toxic blowpipe", "blowpipe"}, {"Trident of the seas", "trident_seas"},
            {"Trident of the swamp", "trident_swamp"}, {"Serpentine helm", "serpentine"},
            {"Amulet of blood fury", "blood_fury"}, {"Tumeken's shadow", "shadow"},
            {"Sanguinesti staff", "sanguinesti"}, {"Scythe of vitur", "scythe"}};
        for (String[] entry : cases)
        {
            var result = ChargeMessages.parse("Your " + entry[0] + " has 1,234 charges left.", "now");
            assertEquals(entry[1], result.key);
            assertEquals(Integer.valueOf(1234), result.charge.quantity);
            assertNull(result.charge.progressPercent);
        }
        assertNull(ChargeMessages.parse("Your weapon has 123 charges left.", "now"));
        assertNull(ChargeMessages.parse("Friend says Your Toxic blowpipe has 123 charges left.", "now"));
    }
}
