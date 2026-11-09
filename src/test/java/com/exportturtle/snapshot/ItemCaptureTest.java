package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.EnumSet;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;

public class ItemCaptureTest
{
    private Client client;
    private Player player;
    private ItemCapture capture;
    private Widget bank;
    private net.runelite.client.game.ItemManager itemManager;

    @Before public void setup()
    {
        client = mock(Client.class);
        player = mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(player.getName()).thenReturn("Test Player");
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        bank = mock(Widget.class);
        itemManager = mock(net.runelite.client.game.ItemManager.class);
        capture = new ItemCapture(client, itemManager);
    }

    private void container(int id, Item... items)
    {
        ItemContainer container = mock(ItemContainer.class);
        when(container.getItems()).thenReturn(items);
        when(client.getItemContainer(id)).thenReturn(container);
        for (Item item : items)
        {
            if (item.getId() < 0)
                continue;
            ItemComposition definition = mock(ItemComposition.class);
            when(definition.getNote()).thenReturn(-1);
            when(definition.getPlaceholderTemplateId()).thenReturn(-1);
            when(client.getItemDefinition(item.getId())).thenReturn(definition);
        }
    }

    @Test public void ambiguousChecksExpireAndNeverCrossAccounts()
    {
        ItemComposition definition = mock(ItemComposition.class);
        when(definition.getName()).thenReturn("Iban's staff (u)");
        when(client.getItemDefinition(123)).thenReturn(definition);
        String message = "You have 123 charges left on the staff.";
        capture.onItemAction(123, "Check");
        when(client.getTickCount()).thenReturn(6);
        capture.onMessage(message);
        assertFalse(capture.poll(true).charges.containsKey("ibans"));
        capture.onItemAction(123, "Check");
        capture.onMessage(message);
        assertEquals(Integer.valueOf(123), capture.poll(true).charges.get("ibans").quantity);
        capture.onMessage("You have 456 charges left on the staff.");
        assertEquals(Integer.valueOf(123), capture.poll(true).charges.get("ibans").quantity);
        capture.onItemAction(123, "Check");
        when(player.getName()).thenReturn("Other Player");
        capture.onMessage(message);
        assertFalse(capture.poll(true).charges.containsKey("ibans"));
        capture.onItemAction(123, "Check");
        capture.onItemAction(456, "Wield");
        capture.onMessage(message);
        assertFalse(capture.poll(true).charges.containsKey("ibans"));
    }

    @Test public void seedVaultRequiresOpeningAndKeepsFinalContainerChanges()
    {
        container(InventoryID.SEED_VAULT, new Item(10, 20));
        assertNull(capture.poll(false).observationTime("seedVault"));
        Widget vault = mock(Widget.class);
        when(client.getWidget(InterfaceID.SeedVault.CONTENT)).thenReturn(vault);
        ItemSnapshot opened = capture.poll(false);
        assertEquals(Long.valueOf(20), opened.containers.get("seedVault").items.get(10));
        assertFalse(opened.bankObservedThisSession);
        when(client.getWidget(InterfaceID.SeedVault.CONTENT)).thenReturn(null);
        container(InventoryID.SEED_VAULT, new Item(10, 5));
        capture.onContainerChanged(InventoryID.SEED_VAULT);
        assertEquals(Long.valueOf(5), capture.poll(false).containers.get("seedVault").items.get(10));
        clearInvocations(client);
        assertNull(capture.poll(false));
        verify(client, never()).getItemContainer(anyInt());
        when(player.getName()).thenReturn("Different Player");
        assertNull(capture.poll(false).observationTime("seedVault"));
    }

    @Test public void emptySeedVaultIsObservedButHiddenVaultIsNot()
    {
        Widget vault = mock(Widget.class);
        when(client.getWidget(InterfaceID.SeedVault.CONTENT)).thenReturn(vault);
        when(vault.isHidden()).thenReturn(true);
        container(InventoryID.SEED_VAULT);
        assertNull(capture.poll(false).observationTime("seedVault"));
        when(vault.isHidden()).thenReturn(false);
        ItemSnapshot empty = capture.poll(false);
        assertNotNull(empty.observationTime("seedVault"));
        assertTrue(empty.containers.get("seedVault").items.isEmpty());
    }

    @Test public void bankRequiresOpeningAndUnchangedPollDoesNoWork()
    {
        container(InventoryID.BANK, new Item(ItemID.SUNFIRESPLINTER, 123));
        ItemSnapshot first = capture.poll(false);
        assertFalse(first.bankObservedThisSession);
        assertFalse(first.containers.containsKey("bank"));
        assertNull(capture.poll(false));
        when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(bank);
        ItemSnapshot opened = capture.poll(false);
        assertEquals(Long.valueOf(123), opened.containers.get("bank").items.get(ItemID.SUNFIRESPLINTER));
        assertTrue(opened.bankObservedThisSession);
        clearInvocations(client);
        assertNull(capture.poll(false));
        verify(client, never()).getItemContainer(anyInt());
    }

    @Test public void emptyBankIsKnownAndAccountSwitchClearsMemory()
    {
        when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(bank);
        container(InventoryID.BANK);
        assertTrue(capture.poll(false).containers.get("bank").items.isEmpty());
        when(player.getName()).thenReturn("Other");
        when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(null);
        ItemSnapshot other = capture.poll(false);
        assertFalse(other.bankObservedThisSession);
        assertTrue(other.containers.isEmpty());
        assertTrue(other.charges.isEmpty());
    }

    @Test public void normalizesNotesAndSkipsPlaceholdersAndEmptySlots()
    {
        container(InventoryID.INV, new Item(10, 2), new Item(11, 3), new Item(12, 1), new Item(-1, 0));
        when(client.getItemDefinition(11).getNote()).thenReturn(799);
        when(client.getItemDefinition(11).getLinkedNoteId()).thenReturn(10);
        when(client.getItemDefinition(12).getPlaceholderTemplateId()).thenReturn(14401);
        assertEquals(Long.valueOf(5), capture.poll(false).containers.get("inventory").items.get(10));
        assertEquals(1, capture.poll(true).containers.get("inventory").items.size());
    }

    @Test public void keepsBankInSyncWhenFinalContainerChangeArrivesAfterClosing()
    {
        when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(bank);
        container(InventoryID.BANK, new Item(10, 5));
        capture.poll(false);
        when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(null);
        container(InventoryID.BANK, new Item(10, 2));
        capture.onContainerChanged(InventoryID.BANK);
        assertEquals(Long.valueOf(2), capture.poll(false).containers.get("bank").items.get(10));
    }

    @Test public void permanentItemsDoNotFabricateChargeQuantities()
    {
        container(
            InventoryID.WORN, new Item(ItemID.EMBERLIGHT, 1), new Item(ItemID.BOW_OF_FAERDHINEN_INFINITE, 1));
        ItemSnapshot snapshot = capture.poll(false);
        assertTrue(snapshot.charges.get("emberlight").permanent);
        assertTrue(snapshot.charges.get("bowfa").permanent);
        assertNull(snapshot.charges.get("bowfa").quantity);
        assertFalse(snapshot.charges.containsKey("quiver"));
    }

    @Test public void chargeChecksAreLastObservedAndDoNotReadWithdrawalSettings()
    {
        capture.onMessage(
            "Dizana's quiver has 2,500 charges remaining. It has made 12% progress towards being blessed.");
        ItemSnapshot snapshot = capture.poll(false);
        assertEquals(Integer.valueOf(2500), snapshot.charges.get("quiver").quantity);
        assertEquals(12.0, snapshot.charges.get("quiver").progressPercent, 0);
        verify(client, never()).getVarbitValue(anyInt());
        JsonObject json = new Gson().toJsonTree(snapshot).getAsJsonObject();
        assertFalse(json.toString().contains("Dizana's quiver has"));
    }

    @Test public void rejectsLoggedOutAndTemporaryWorlds()
    {
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        assertNull(capture.poll(true));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));
        assertNull(capture.poll(true));
        verify(client, never()).getItemContainer(anyInt());
    }
    @Test public void capturesVariantPricesAndPlaceholderSlotsFromTheSameBank()
    {
        when(client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER)).thenReturn(bank);
        container(InventoryID.BANK, new Item(ItemID.COINS, 500), new Item(12931, 1), new Item(14401, 0));
        when(client.getItemDefinition(14401).getPlaceholderTemplateId()).thenReturn(14401);
        when(itemManager.getItemPrice(ItemID.COINS)).thenReturn(1L);
        when(itemManager.getItemPrice(12931)).thenReturn(5_000_000L);
        when(client.getItemDefinition(12931).getHaPrice()).thenReturn(10_000);
        var captured = capture.poll(false).containers.get("bank");
        assertEquals(Long.valueOf(5_000_500), captured.geValue);
        assertEquals(Long.valueOf(10_500), captured.alchemyValue);
        assertEquals(3, captured.occupiedSlots);
        assertEquals(1, captured.placeholderSlots);
        assertEquals(2, captured.items.size());
    }
}
