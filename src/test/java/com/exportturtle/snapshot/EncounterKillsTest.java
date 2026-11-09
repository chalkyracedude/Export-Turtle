package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.NPCComposition;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemStack;
import org.junit.Test;

public class EncounterKillsTest
{
    @Test public void multipleDropStacksStillRepresentOneMatchingEncounter()
    {
        NPCComposition npc = mock(NPCComposition.class);
        when(npc.getName()).thenReturn("<col=ffffff>Demonic gorilla</col>");
        ServerNpcLoot event =
            new ServerNpcLoot(npc, Arrays.asList(new ItemStack(1, 5), new ItemStack(2, 100)));
        assertEquals("Demonic Gorilla", EncounterKills.encounter(event));
        when(npc.getName()).thenReturn("Tormented demon");
        assertEquals("Tormented Demon", EncounterKills.encounter(event));
        when(npc.getName()).thenReturn("Tortured gorilla");
        assertNull(EncounterKills.encounter(event));
        when(npc.getName()).thenReturn("Vorkath");
        assertNull(EncounterKills.encounter(event));
    }

    @Test public void malformedOrEmptyLootCannotRecordKills()
    {
        NPCComposition npc = mock(NPCComposition.class);
        when(npc.getName()).thenReturn("Demonic gorilla");
        assertNull(EncounterKills.encounter(null));
        assertNull(EncounterKills.encounter(
            new ServerNpcLoot(null, Collections.singletonList(new ItemStack(1, 1)))));
        assertNull(EncounterKills.encounter(new ServerNpcLoot(npc, Collections.emptyList())));
        when(npc.getName()).thenReturn(null);
        assertNull(
            EncounterKills.encounter(new ServerNpcLoot(npc, Collections.singletonList(new ItemStack(1, 1)))));
    }
}
