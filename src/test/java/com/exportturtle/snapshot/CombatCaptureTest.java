package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class CombatCaptureTest
{
    @Test public void omitsCombatUntilThresholdsLoad()
    {
        assertNull(CombatCapture.capture(mock(Client.class)));
    }

    @Test public void capturesCountsAndServerThresholds()
    {
        Client client = mock(Client.class);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_EASY)).thenReturn(33);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_MEDIUM)).thenReturn(115);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_HARD)).thenReturn(304);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_ELITE)).thenReturn(820);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_MASTER)).thenReturn(1465);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_GRANDMASTER)).thenReturn(2281);
        when(client.getVarbitValue(VarbitID.CA_POINTS)).thenReturn(200);
        when(client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_HARD)).thenReturn(12);
        JsonObject json = new Gson().toJsonTree(CombatCapture.capture(client)).getAsJsonObject();
        assertEquals(200, json.get("points").getAsInt());
        JsonObject tiers = json.getAsJsonObject("tiers");
        assertEquals(6, tiers.size());
        assertEquals(12, tiers.getAsJsonObject("Hard").get("completedTasks").getAsInt());
        assertEquals(2281, tiers.getAsJsonObject("Grandmaster").get("pointsRequired").getAsInt());
    }
}
