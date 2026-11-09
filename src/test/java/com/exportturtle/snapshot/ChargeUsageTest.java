package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.EnumSet;
import net.runelite.api.*;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;

public class ChargeUsageTest
{
    private Client client;
    private Player player;
    private ItemContainer equipment;
    private ItemCapture capture;

    @Before public void setup()
    {
        client = mock(Client.class);
        player = mock(Player.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test Player");
        equipment = mock(ItemContainer.class);
        when(equipment.getItems()).thenReturn(new Item[0]);
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
        capture = new ItemCapture(client, mock(ItemManager.class));
    }

    private void equip(int id, String name)
    {
        equipSlot(EquipmentInventorySlot.WEAPON, id, name);
    }

    private void equipSlot(EquipmentInventorySlot slot, int id, String name)
    {
        when(equipment.getItem(slot.getSlotIdx())).thenReturn(new Item(id, 1));
        ItemComposition definition = mock(ItemComposition.class);
        when(definition.getName()).thenReturn(name);
        when(client.getItemDefinition(id)).thenReturn(definition);
    }

    @Test public void blowpipeUsesExpectedScalesAndAvaDartSavings()
    {
        equip(1, "Blazing blowpipe");
        equipSlot(EquipmentInventorySlot.CAPE, 2, "Ava's assembler");
        capture.onMessage("Darts: Dragon dart x 100. Scales: 100 (100%)");
        for (int tick = 1; tick <= 15; tick++)
        {
            when(client.getTickCount()).thenReturn(tick);
            capture.onAnimation(AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK_ORNAMENT);
            capture.onAnimation(AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK_ORNAMENT);
        }
        var reading = capture.poll(true).charges.get("blowpipe");
        assertEquals(Integer.valueOf(90), reading.trackedQuantity);
        assertEquals(Integer.valueOf(97), reading.trackedAmmoQuantity);
        assertEquals(Integer.valueOf(100), reading.quantity);
        assertEquals(Integer.valueOf(100), reading.ammoQuantity);
        assertEquals("expected-consumption", reading.trackingMethod);
    }

    @Test public void quiverCountsOnlyEligibleShotsAndArclightExcludesSpecials()
    {
        equip(1, "Twisted bow");
        equipSlot(EquipmentInventorySlot.CAPE, 2, "Dizana's quiver");
        capture.onMessage("Dizana's quiver has 100 charges remaining.");
        for (int tick = 1; tick <= 3; tick++)
        {
            when(client.getTickCount()).thenReturn(tick);
            capture.onAnimation(AnimationID.HUMAN_BOW);
        }
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("quiver").trackedQuantity);
        equip(3, "Echo venator bow");
        when(client.getTickCount()).thenReturn(4);
        capture.onAnimation(AnimationID.HUMAN_BOW);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("quiver").trackedQuantity);
        equip(4, "Arclight");
        capture.onMessage("Your arclight has 100 charges left. It is 25% infused.");
        capture.onMeleeHit(AnimationID.HUMAN_SWORD_STAB, 0);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("arclight").trackedQuantity);
        when(client.getTickCount()).thenReturn(5);
        capture.onMeleeHit(-1, 10);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("arclight").trackedQuantity);
    }

    @Test public void bloodFuryCountsEveryMeleeHitAndArmorUsesItsOwnSlots()
    {
        equip(1, "Scythe of vitur");
        equipSlot(EquipmentInventorySlot.AMULET, 2, "Amulet of blood fury");
        equipSlot(EquipmentInventorySlot.BODY, 3, "Crystal body");
        equipSlot(EquipmentInventorySlot.LEGS, 4, "Crystal legs");
        equipSlot(EquipmentInventorySlot.HEAD, 5, "Crystal helm");
        capture.onMessage("Your Amulet of blood fury will work for 100 more hits.");
        for (String name : new String[] {"body", "legs", "helm"})
            capture.onMessage("Your crystal " + name + " has 100 charges remaining.");
        for (int hit = 0; hit < 3; hit++)
            capture.onMeleeHit(AnimationID.SCYTHE_OF_VITUR_ATTACK);
        assertEquals(Integer.valueOf(97), capture.poll(true).charges.get("blood_fury").trackedQuantity);
        capture.onMeleeHit(AnimationID.HUMAN_BOW);
        assertEquals(Integer.valueOf(97), capture.poll(true).charges.get("blood_fury").trackedQuantity);
        capture.onReceivedDamage();
        capture.onReceivedDamage();
        for (String family : new String[] {"crystal_body", "crystal_legs", "crystal_helm"})
            assertEquals(Integer.valueOf(98), capture.poll(true).charges.get(family).trackedQuantity);
        equip(6, "Other weapon");
        capture.onContainerChanged(InventoryID.WORN);
        capture.onReceivedDamage();
        assertEquals(Integer.valueOf(97), capture.poll(true).charges.get("crystal_body").trackedQuantity);
    }

    @Test public void signetSavingsAndSerpentineCombatTimerRemainEstimates()
    {
        equipSlot(EquipmentInventorySlot.BODY, 3, "Crystal body");
        equipSlot(EquipmentInventorySlot.RING, 4, "Celestial signet");
        equipSlot(EquipmentInventorySlot.HEAD, 5, "Magma helm");
        capture.onMessage("Your crystal body has 100 charges remaining.");
        capture.onMessage("Scales: 100 (100%)");
        for (int tick = 1; tick <= 90; tick++)
        {
            when(client.getTickCount()).thenReturn(tick);
            capture.onCombatActivity();
            capture.onTick();
            capture.onTick();
        }
        assertEquals(Integer.valueOf(90), capture.poll(true).charges.get("serpentine").trackedQuantity);
        when(client.getTickCount()).thenReturn(100);
        capture.onTick();
        assertEquals(Integer.valueOf(90), capture.poll(true).charges.get("serpentine").trackedQuantity);
        for (int hit = 0; hit < 10; hit++)
            capture.onReceivedDamage();
        assertEquals(Integer.valueOf(91), capture.poll(true).charges.get("crystal_body").trackedQuantity);
    }

    @Test public void additionalStavesTrackAndPermanentQuiversDoNot()
    {
        equip(1, "Holy sanguinesti staff");
        capture.onMessage("Your Holy sanguinesti staff has 100 charges remaining.");
        capture.onGraphic(SpotanimID.SANGUINESTI_STAFF_CASTING_JUSTICIAR, 10);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("sanguinesti").trackedQuantity);
        equip(2, "Eye of Ayak");
        capture.onMessage("Your Eye of Ayak has 100 charges remaining.");
        capture.onGraphic(SpotanimID.VFX_AYAK_PLAYER_NORMAL_SPOTANIM, 20);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("eye_ayak").trackedQuantity);
        equip(3, "Twisted bow");
        equipSlot(EquipmentInventorySlot.CAPE, 4, "Dizana's max cape");
        capture.onMessage("Dizana's max cape has 100 charges remaining.");
        capture.onAnimation(AnimationID.HUMAN_BOW);
        assertNull(capture.poll(true).charges.get("quiver").trackedQuantity);
    }

    private ItemSnapshot.Charge shadow()
    {
        return capture.poll(true).charges.get("shadow");
    }

    private void inventory(Item... items)
    {
        ItemContainer container = mock(ItemContainer.class);
        when(container.getItems()).thenReturn(items);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(container);
    }

    @Test public void resumesUniqueInventorySwapsButStopsForDuplicatesAndBanking()
    {
        inventory();
        equip(1, "Tumeken's shadow");
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        equip(2, "Other weapon");
        inventory(new Item(1, 1));
        capture.onContainerChanged(InventoryID.WORN);
        assertEquals("paused", shadow().trackingState);
        equip(1, "Tumeken's shadow");
        inventory();
        capture.onContainerChanged(InventoryID.WORN);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 10);
        assertEquals("active", shadow().trackingState);
        assertEquals(Integer.valueOf(99), shadow().trackedQuantity);
        inventory(new Item(1, 1));
        capture.onContainerChanged(InventoryID.INV);
        assertEquals("needs-check", shadow().trackingState);
        when(client.getTickCount()).thenReturn(1);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        assertEquals(Integer.valueOf(99), shadow().trackedQuantity);
    }

    @Test public void reconcilesSavingsBeforeAndAfterUsageAndRefillsKeepConfirmedBalance()
    {
        equip(1, "Tumeken's shadow");
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        capture.onMessage("Ghommal's luck saves a charge on your Tumeken's shadow.");
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 10);
        assertEquals(Integer.valueOf(100), shadow().trackedQuantity);
        when(client.getTickCount()).thenReturn(1);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        assertEquals(Integer.valueOf(99), shadow().trackedQuantity);
        capture.onMessage("Ghommal's luck saves a charge on your Tumeken's shadow.");
        assertEquals(Integer.valueOf(100), shadow().trackedQuantity);
        capture.onMessage("The banker charges your Tumeken's shadow using 100x Soul rune.");
        assertEquals(Integer.valueOf(150), shadow().trackedQuantity);
        assertEquals(Integer.valueOf(100), shadow().quantity);
        assertEquals(Integer.valueOf(50), shadow().trackedAddedQuantity);
        capture.reset();
        capture.onMessage("The banker charges your Tumeken's shadow using 100x Soul rune.");
        assertNull(shadow().quantity);
        assertEquals("needs-check", shadow().trackingState);
    }

    @Test public void tracksTomesWildernessBowsAndAdditionalDegradableWeapons()
    {
        equipSlot(EquipmentInventorySlot.SHIELD, 1, "Tome of fire");
        capture.onItemAction(1, "Check");
        capture.onMessage("Your tome has been charged with Burnt Pages. It currently holds 100 charges.");
        capture.onGraphic(SpotanimID.FIRESTRIKE_CASTING, 10);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("tome_fire").trackedQuantity);
        equip(2, "Webweaver bow");
        capture.onItemAction(2, "Check");
        capture.onMessage("Your bow has 100 charges left powering it.");
        capture.onAnimation(AnimationID.HUMAN_SPECIAL01_WEBWEAVER);
        assertEquals(Integer.valueOf(96), capture.poll(true).charges.get("webweaver").trackedQuantity);
        equip(3, "Abyssal tentacle");
        capture.onMessage("Your abyssal tentacle can perform 100 more attacks.");
        capture.onAnimation(AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK);
        assertEquals(Integer.valueOf(99), capture.poll(true).charges.get("tentacle").trackedQuantity);
    }

    @Test public void correlatesScalePickupsAndRejectsAmbiguousRecipients()
    {
        inventory();
        equip(1, "Toxic blowpipe");
        when(player.getLocalLocation()).thenReturn(new net.runelite.api.coords.LocalPoint(1344, 1344));
        capture.onMessage("Darts: Dragon dart x 100. Scales: 100 (100%)");
        capture.onScalePickup(10, 10);
        capture.onScalesRemoved(10, 10, 50);
        when(client.getTickCount()).thenReturn(1);
        capture.onTick();
        assertEquals(Integer.valueOf(150), capture.poll(true).charges.get("blowpipe").trackedQuantity);
        equipSlot(EquipmentInventorySlot.HEAD, 2, "Serpentine helm");
        capture.onScalePickup(10, 10);
        capture.onScalesRemoved(10, 10, 50);
        when(client.getTickCount()).thenReturn(2);
        capture.onTick();
        assertEquals("needs-check", capture.poll(true).charges.get("blowpipe").trackingState);
        assertEquals(Integer.valueOf(150), capture.poll(true).charges.get("blowpipe").trackedQuantity);
    }

    @Test public void countsEachCastOnceAndPreservesConfirmedReading()
    {
        equip(1, "Tumeken's shadow");
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 10);
        assertNull(shadow());
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        when(client.getTickCount()).thenReturn(1);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        assertEquals(Integer.valueOf(100), shadow().quantity);
        assertEquals(Integer.valueOf(99), shadow().trackedQuantity);
        when(client.getTickCount()).thenReturn(2);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        assertEquals(Integer.valueOf(99), shadow().trackedQuantity);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 30);
        assertEquals(Integer.valueOf(98), shadow().trackedQuantity);
        capture.onMessage("Tumeken's shadow has 75 charges remaining.");
        assertEquals(Integer.valueOf(75), shadow().quantity);
        assertNull(shadow().trackedQuantity);
    }

    @Test public void stopsOnEquipmentChangesLogoutAndAccountSwitch()
    {
        equip(1, "Tumeken's shadow");
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        equip(2, "Other weapon");
        capture.onContainerChanged(InventoryID.WORN);
        equip(1, "Tumeken's shadow");
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 10);
        assertNull(shadow().trackedQuantity);
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(player.getName()).thenReturn("Other Player");
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 30);
        assertNull(shadow());
    }

    @Test public void multiHitWeaponsConsumeOncePerTickAndClampAtZero()
    {
        equip(3, "Scythe of vitur");
        capture.onMessage("Your Scythe of vitur has 1 charges remaining.");
        capture.onMeleeHit(AnimationID.SCYTHE_OF_VITUR_ATTACK);
        capture.onMeleeHit(AnimationID.SCYTHE_OF_VITUR_ATTACK);
        var first = capture.poll(true).charges.get("scythe");
        assertEquals(Integer.valueOf(1), first.quantity);
        assertEquals(Integer.valueOf(0), first.trackedQuantity);
        assertEquals("damage-event", first.trackingMethod);
        when(client.getTickCount()).thenReturn(1);
        capture.onMeleeHit(AnimationID.SCYTHE_OF_VITUR_ATTACK);
        assertEquals(Integer.valueOf(0), capture.poll(true).charges.get("scythe").trackedQuantity);
    }

    @Test public void ignoresWrongGraphicsVariantsAndTemporaryWorlds()
    {
        equip(1, "Tumeken's shadow");
        capture.onMessage("Your echo venator bow has 100 charges remaining.");
        capture.onGraphic(SpotanimID.ARROW_VENATOR01_LAUNCH01, 10);
        assertNull(capture.poll(true).charges.get("venator").trackedQuantity);
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        capture.onGraphic(SpotanimID.TOXIC_TOTS_CASTING, 20);
        assertNull(shadow().trackedQuantity);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.QUEST_SPEEDRUNNING));
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 30);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        assertNull(shadow().trackedQuantity);
    }

    @Test public void supportsEachGraphicWeaponAndReconcilesRefills()
    {
        Object[][] weapons = {
            {"Trident of the seas (e)", "trident_seas", SpotanimID.SLAYER_TOTS_CASTING},
            {"Trident of the swamp (e) (o)", "trident_swamp", SpotanimID.TOXIC_TOTS_CASTING},
            {"Echo venator bow", "venator", SpotanimID.ARROW_VENATOR01_LAUNCH01},
            {"Warped sceptre", "warped_sceptre", SpotanimID.VFX_WARPED_SCEPTRE_CAST},
            {"Bow of faerdhinen", "bowfa", SpotanimID.SP_ATTACK_ARROW_LAUNCH_FAERDHINEN}
        };
        for (Object[] weapon : weapons)
        {
            capture.reset();
            equip(1, (String) weapon[0]);
            capture.onMessage("Your " + weapon[0] + " has 100 charges remaining.");
            capture.onGraphic((Integer) weapon[2], 10);
            assertEquals(weapon[0].toString(), Integer.valueOf(99),
                capture.poll(true).charges.get(weapon[1]).trackedQuantity);
        }
        capture.reset();
        equip(2, "Tumeken's shadow");
        capture.onMessage("Tumeken's shadow has 100 charges remaining.");
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 20);
        capture.onMessage("You apply an additional 50 charges to your Tumeken's shadow. "
            + "It now has 149 charges in total.");
        assertEquals(Integer.valueOf(149), shadow().quantity);
        assertNull(shadow().trackedQuantity);
        when(client.getTickCount()).thenReturn(1);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 30);
        assertEquals(Integer.valueOf(148), shadow().trackedQuantity);
        capture.onMessage("You charge an item in a way this parser does not recognise.");
        when(client.getTickCount()).thenReturn(2);
        capture.onGraphic(SpotanimID.TUMEKENS_SHADOW_CASTING, 40);
        assertEquals(Integer.valueOf(148), shadow().trackedQuantity);
    }
}
