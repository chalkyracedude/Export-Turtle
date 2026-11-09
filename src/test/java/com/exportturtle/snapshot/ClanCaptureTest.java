package com.exportturtle.snapshot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.game.ChatIconManager;
import org.junit.Test;

public class ClanCaptureTest
{
    @Test public void capturesOnlyOwnMembershipAndItsActualRankSprite()
    {
        Client client = mock(Client.class);
        ClanSettings settings = mock(ClanSettings.class);
        ClanMember member = mock(ClanMember.class);
        ChatIconManager icons = mock(ChatIconManager.class);
        ClanTitle title = new ClanTitle(-4, "Owner");
        when(client.getClanSettings()).thenReturn(settings);
        when(settings.getName()).thenReturn("ExportTurtle");
        when(settings.findMember("Test Player")).thenReturn(member);
        when(member.getRank()).thenReturn(ClanRank.OWNER);
        when(settings.titleForRank(ClanRank.OWNER)).thenReturn(title);
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffd8bc78);
        when(icons.getRankImage(title)).thenReturn(image);
        JsonObject data =
            new Gson().toJsonTree(ClanCapture.capture(client, icons, "Test Player")).getAsJsonObject();
        assertEquals("ExportTurtle", data.get("name").getAsString());
        assertEquals("Owner", data.get("rank").getAsString());
        assertEquals(0xffd8bc78, data.getAsJsonObject("icon").getAsJsonArray("pixels").get(0).getAsInt());
        verify(settings, never()).getMembers();
        verify(client, never()).getGuestClanSettings();
    }

    @Test public void missingPrimaryClanNeverUsesGuestMembership()
    {
        Client client = mock(Client.class);
        JsonObject data =
            new Gson().toJsonTree(ClanCapture.capture(client, null, "Test Player")).getAsJsonObject();
        assertEquals(0, data.size());
        verify(client, never()).getGuestClanSettings();
    }
}
