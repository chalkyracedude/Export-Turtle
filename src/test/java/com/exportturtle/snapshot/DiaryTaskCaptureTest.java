package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class DiaryTaskCaptureTest
{
    @Test public void capturesAllTasksAndHandlesSignedAndAlternativeFlags()
    {
        Client client = mock(Client.class);
        when(client.getVarpValue(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY)).thenReturn(Integer.MIN_VALUE);
        when(client.getVarpValue(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2)).thenReturn(1 << 9);
        when(client.getVarbitValue(VarbitID.ATJUN_EASY_BANANA)).thenReturn(5);
        var regions = DiaryTaskCapture.capture(client);
        assertEquals(12, regions.size());
        assertEquals(492, regions.values()
                              .stream()
                              .flatMap(tiers -> tiers.values().stream())
                              .mapToInt(java.util.List::size)
                              .sum());
        assertTrue(regions.get("Ardougne").get("Hard").get(5));
        assertFalse(regions.get("Ardougne").get("Hard").get(4));
        assertTrue(regions.get("Desert").get("Medium").get(10));
        assertTrue(regions.get("Karamja").get("Easy").get(0));
        assertFalse(regions.get("Karamja").get("Easy").get(1));
    }
}
