package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.ScriptEvent;
import net.runelite.api.StructComposition;
import net.runelite.api.WorldView;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

public class FullCollectionCaptureTest
{
    @Test public void requiresOwnOpenCollectionLog()
    {
        Client client = mock(Client.class);
        FullCollectionCapture capture = new FullCollectionCapture(client);
        assertFalse(capture.start());
        verify(client, never()).runScript(anyInt());
    }

    @Test public void capturesEverySectionAndWaitsForQuietTicks()
    {
        Client client = mock(Client.class);
        Player player = mock(Player.class);
        WorldView world = mock(WorldView.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test");
        when(client.getTopLevelWorldView()).thenReturn(world);
        Widget search = mock(Widget.class);
        when(client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE)).thenReturn(search);
        enumeration(client, 3721, new int[0]);
        when(client.getEnum(3721).getKeys()).thenReturn(new int[0]);
        enumeration(client, 2102, new int[] {1});
        StructComposition group = mock(StructComposition.class);
        when(client.getStructComposition(1)).thenReturn(group);
        when(group.getIntValue(683)).thenReturn(20);
        enumeration(client, 20, new int[] {2, 3});
        for (int section = 2; section <= 3; section++)
        {
            StructComposition entry = mock(StructComposition.class);
            when(client.getStructComposition(section)).thenReturn(entry);
            when(entry.getStringValue(689)).thenReturn("Section " + section);
            when(entry.getIntValue(690)).thenReturn(100 + section);
            enumeration(client, 100 + section, new int[] {1000 + section});
            ItemComposition item = mock(ItemComposition.class);
            when(client.getItemDefinition(1000 + section)).thenReturn(item);
            when(item.getName()).thenReturn("Item " + section);
        }
        FullCollectionCapture capture = new FullCollectionCapture(client);
        assertTrue(capture.syncWhenOpened());
        assertFalse(capture.syncWhenOpened());
        ScriptEvent script = mock(ScriptEvent.class);
        when(script.getArguments()).thenReturn(new Object[] {4100, 1002});
        ScriptPreFired event = mock(ScriptPreFired.class);
        when(event.getScriptId()).thenReturn(4100);
        when(event.getScriptEvent()).thenReturn(script);
        capture.onScript(event);
        assertNull(capture.finish());
        when(client.getTickCount()).thenReturn(2);
        var pages = capture.finish();
        assertEquals(2, pages.size());
        assertTrue(pages.get(0).items.get(0).obtained);
        assertFalse(pages.get(1).items.get(0).obtained);
        assertNull(capture.finish());
        assertFalse(capture.syncWhenOpened());
        when(search.isHidden()).thenReturn(true);
        assertFalse(capture.syncWhenOpened());
        when(search.isHidden()).thenReturn(false);
        assertTrue(capture.syncWhenOpened());
        assertFalse(capture.syncWhenOpened());
        capture.resetSession();
        assertNull(capture.finish());
    }

    @Test public void neverAutoFetchesAnInstancedCollectionLog()
    {
        Client client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(mock(Player.class));
        WorldView world = mock(WorldView.class);
        when(client.getTopLevelWorldView()).thenReturn(world);
        when(world.isInstance()).thenReturn(true);
        when(client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE)).thenReturn(mock(Widget.class));
        assertFalse(new FullCollectionCapture(client).syncWhenOpened());
        verify(client, never()).getEnum(anyInt());
        verify(client, never()).runScript(anyInt());
    }

    private void enumeration(Client client, int id, int[] values)
    {
        EnumComposition entry = mock(EnumComposition.class);
        when(client.getEnum(id)).thenReturn(entry);
        when(entry.getIntVals()).thenReturn(values);
    }
}
