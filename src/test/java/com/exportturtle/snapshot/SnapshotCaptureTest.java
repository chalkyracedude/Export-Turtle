package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.EnumSet;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import org.junit.Test;

public class SnapshotCaptureTest
{
    @Test public void refusesLoggedOutAndTemporaryWorlds()
    {
        Client client = mock(Client.class);
        SnapshotCapture capture = new SnapshotCapture(client);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        assertThrows(IllegalStateException.class, capture::capture);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(mock(Player.class));
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));
        assertThrows(IllegalStateException.class, capture::capture);
        verify(client, never()).getSkillExperience(any());
    }

    @Test public void capturesBaseLevelsAndQuestStatesWithoutOverallDuplication()
    {
        Client client = mock(Client.class);
        Player player = mock(Player.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test Player");
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        when(client.getRealSkillLevel(any(Skill.class))).thenReturn(50);
        when(client.getSkillExperience(any(Skill.class))).thenReturn(101333);
        when(client.getBoostedSkillLevel(any(Skill.class))).thenReturn(55);
        when(client.getWorld()).thenReturn(301);
        when(player.getCombatLevel()).thenReturn(90);
        when(client.getIntStack()).thenReturn(new int[] {2});
        JsonObject json = new Gson().toJsonTree(new SnapshotCapture(client).capture()).getAsJsonObject();
        assertEquals(24, json.getAsJsonObject("skills").size());
        assertFalse(json.getAsJsonObject("skills").has("Overall"));
        assertTrue(json.getAsJsonObject("skills").has("Runecraft"));
        assertTrue(json.getAsJsonObject("skills").has("Sailing"));
        assertTrue(json.getAsJsonObject("quests").size() > 100);
        assertEquals(50, json.getAsJsonObject("skills").getAsJsonObject("Attack").get("level").getAsInt());
        assertEquals(55, json.getAsJsonObject("skills").getAsJsonObject("Attack").get("boostedLevel").getAsInt());
        JsonObject character = json.getAsJsonObject("character");
        assertEquals(301, character.get("world").getAsInt());
        assertEquals(90, character.get("combatLevel").getAsInt());
        assertEquals(1200, character.get("totalLevel").getAsInt());
        assertEquals(24L * 101333, character.get("totalXp").getAsLong());
    }
}
