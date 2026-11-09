package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class DiaryCaptureTest
{
    @Test public void capturesAllTiersAndUsesKaramjasDedicatedFlags()
    {
        Client client = mock(Client.class);
        when(client.getVarbitValue(VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.ATJUN_MED_DONE)).thenReturn(2);
        Map<String, Map<String, Boolean>> diaries = DiaryCapture.capture(client);
        assertEquals(12, diaries.size());
        assertEquals(48, diaries.values().stream().mapToInt(Map::size).sum());
        assertTrue(diaries.get("Ardougne").get("Easy"));
        assertFalse(diaries.get("Ardougne").get("Elite"));
        assertTrue(diaries.get("Karamja").get("Medium"));
        verify(client).getVarbitValue(VarbitID.ATJUN_EASY_DONE);
        verify(client).getVarbitValue(VarbitID.ATJUN_HARD_DONE);
    }

    @Test public void karamjaStartedIsNotCompleteAndClaimedRemainsComplete()
    {
        Client client = mock(Client.class);
        when(client.getVarbitValue(VarbitID.ATJUN_EASY_DONE)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.ATJUN_MED_DONE)).thenReturn(2);
        when(client.getVarbitValue(VarbitID.ATJUN_HARD_DONE)).thenReturn(3);
        var tiers = DiaryCapture.capture(client).get("Karamja");
        assertFalse(tiers.get("Easy"));
        assertTrue(tiers.get("Medium"));
        assertTrue(tiers.get("Hard"));
    }

    @Test public void readsTaskCountsFromSummaryProcedures()
    {
        Client client = mock(Client.class);
        when(client.getIntStack()).thenReturn(new int[] {492}, new int[] {492});
        var counts = new com.google.gson.Gson().toJsonTree(DiaryCapture.taskCounts(client)).getAsJsonObject();
        assertEquals(492, counts.get("completed").getAsInt());
        assertEquals(492, counts.get("total").getAsInt());
        verify(client).runScript(3971);
        verify(client).runScript(3980);
    }
}
