package com.exportturtle.snapshot;

import net.runelite.api.Client;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.game.ChatIconManager;

/** Copies only the local player's primary clan membership, never the roster. */
@lombok.extern.slf4j.Slf4j final class ClanCapture
{
    static AccountSnapshot.ClanProgress capture(Client client, ChatIconManager icons, String account)
    {
        ClanSettings settings = client.getClanSettings();
        if (settings == null)
            return new AccountSnapshot.ClanProgress(null, null, null);
        if (settings.getName() == null || settings.getName().trim().isEmpty())
            return new AccountSnapshot.ClanProgress(null, null, null);
        ClanMember member = settings.findMember(account);
        if (member == null)
            return new AccountSnapshot.ClanProgress(null, null, null);
        ClanTitle title = settings.titleForRank(member.getRank());
        java.awt.image.BufferedImage image = null;
        if (title != null && icons != null)
        {
            try
            {
                image = icons.getRankImage(title);
            }
            catch (RuntimeException unavailable)
            {
                log.debug("Clan rank icon is not available yet", unavailable);
            }
        }
        return new AccountSnapshot.ClanProgress(
            settings.getName(), title == null ? null : title.getName(), image);
    }
}
