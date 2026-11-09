package com.exportturtle.snapshot;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreSkill;
import org.junit.Test;

public class BossMessagesTest
{
    private final Gson gson = new Gson();

    @Test public void everyOsrsHiscoreBossHasItsOwnDistinctLine()
    {
        Map<String, String> lines = BossMessages.catalog(gson);
        assertEquals(74, lines.size());
        assertEquals(lines.size(), new HashSet<>(lines.values()).size());
        for (HiscoreSkill skill : HiscoreSkill.values())
            if (skill.getType().name().equals("BOSS"))
                assertNotNull(skill.getName(), BossMessages.canonical(skill.getName(), gson));
        assertNotNull(BossMessages.canonical("Tormented Demons", gson));
        assertEquals("Demonic Gorilla", BossMessages.canonical("Demonic Gorillas", gson));
        assertNull(BossMessages.canonical("Bork", gson));
        assertNull(BossMessages.canonical("Dungeoneering", gson));
        assertNull(BossMessages.canonical("Summoning", gson));
    }

    @Test public void gameNotificationFormatsAreParsedWithoutGuessingCounts()
    {
        assertArrayEquals(
            new String[] {"Vorkath", "1200"}, BossMessages.parse("Your Vorkath kill count is: 1,200."));
        assertArrayEquals(new String[] {"Chambers of Xeric", "23"},
            BossMessages.parse("Your completed Chambers of Xeric count is: 23."));
        assertArrayEquals(
            new String[] {"Wintertodt", "5"}, BossMessages.parse("Your subdued Wintertodt count is: 5."));
        assertEquals("Barrows Chests",
            BossMessages.canonical(BossMessages.parse("Your Barrows chest count is: 17.")[0], gson));
        assertNull(BossMessages.parse("I killed Vorkath 500 times"));
        assertNull(BossMessages.parse("Your Vorkath kill count is: -2."));
        assertNull(BossMessages.parse("Your Vorkath kill count is: 999999999999999."));
    }

    @Test public void modesAndAliasesResolveToTheIntendedOsrsBoss()
    {
        assertEquals("Nightmare", BossMessages.canonical("The Nightmare", gson));
        assertEquals("The Corrupted Gauntlet", BossMessages.canonical("Corrupted Hunllef", gson));
        assertEquals("Chambers of Xeric: Challenge Mode",
            BossMessages.canonical("Chambers of Xeric Challenge Mode", gson));
        assertNotEquals(BossMessages.catalog(gson).get("The Gauntlet"),
            BossMessages.catalog(gson).get("The Corrupted Gauntlet"));
    }
}
