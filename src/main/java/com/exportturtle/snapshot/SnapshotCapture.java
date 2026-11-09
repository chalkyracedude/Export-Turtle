package com.exportturtle.snapshot;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;

/** Reads a complete snapshot on the RuneLite client thread. */
public final class SnapshotCapture
{
    private static final EnumSet<WorldType> TEMPORARY_WORLDS = EnumSet.of(WorldType.SEASONAL,
        WorldType.DEADMAN, WorldType.BETA_WORLD, WorldType.TOURNAMENT_WORLD, WorldType.NOSAVE_MODE,
        WorldType.QUEST_SPEEDRUNNING, WorldType.PVP_ARENA, WorldType.LAST_MAN_STANDING);

    private final Client client;
    private final net.runelite.client.game.ChatIconManager chatIcons;

    public SnapshotCapture(Client client)
    {
        this(client, null);
    }

    @Inject public SnapshotCapture(Client client, net.runelite.client.game.ChatIconManager chatIcons)
    {
        this.client = client;
        this.chatIcons = chatIcons;
    }

    public static boolean isMainGame(Client client)
    {
        return client.getWorldType().stream().noneMatch(TEMPORARY_WORLDS::contains);
    }

    // RuneLite retains OVERALL as a deprecated sentinel; it is not a skill row.
    public AccountSnapshot capture()
    {
        return capture(true);
    }

    @SuppressWarnings("deprecation")
    public AccountSnapshot capture(boolean includeClan)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            throw new IllegalStateException("Log in before exporting a snapshot.");
        }
        if (client.getWorldType().stream().anyMatch(TEMPORARY_WORLDS::contains))
        {
            throw new IllegalStateException("Snapshots are available on main-game worlds only.");
        }
        String name = client.getLocalPlayer().getName();
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalStateException("Your character name is not available yet.");
        }
        Map<String, AccountSnapshot.SkillProgress> skills = new LinkedHashMap<>();
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                skills.put(skill.getName(), new AccountSnapshot.SkillProgress(client.getRealSkillLevel(skill),
                                                client.getSkillExperience(skill), client.getBoostedSkillLevel(skill)));
            }
        }
        Map<String, AccountSnapshot.QuestProgress> quests = new LinkedHashMap<>();
        for (Quest quest : Quest.values())
        {
            quests.put(quest.name(),
                new AccountSnapshot.QuestProgress(quest.getName(), quest.getState(client).name()));
        }
        Map<String, Boolean> unlocks = new LinkedHashMap<>();
        unlocks.put(
            "rigour", client.getVarbitValue(net.runelite.api.gameval.VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1);
        unlocks.put(
            "augury", client.getVarbitValue(net.runelite.api.gameval.VarbitID.PRAYER_AUGURY_UNLOCKED) == 1);
        unlocks.put("arceuusSpellbook",
            client.getVarbitValue(net.runelite.api.gameval.VarbitID.ARCEUUS_SPELLBOOK_UNLOCKED) == 1);
        unlocks.put("ancientSpellbook",
            Quest.DESERT_TREASURE_I.getState(client) == net.runelite.api.QuestState.FINISHED);
        unlocks.put(
            "lunarSpellbook", Quest.LUNAR_DIPLOMACY.getState(client) == net.runelite.api.QuestState.FINISHED);
        Map<String, String> info = new LinkedHashMap<>();
        info.put("pluginVersion", "0.1.0-dev");
        info.put("questSource", "RuneLite Quest API");
        info.put("questCount", Integer.toString(quests.size()));
        info.put("combatCatalogSource", "Bundled task mapping; game progress and thresholds");
        info.put("diaryCatalogSource", "Bundled task mapping; game progress");
        info.put("collectionCatalogSource", "Live game collection enums on journal sync");
        info.put("combatCatalogDiscovery", "mapping-required");
        info.put("diaryCatalogDiscovery", "mapping-required");
        info.put("collectionCatalogDiscovery", "automatic");
        return new AccountSnapshot(name, Instant.now().toString(), skills, quests,
            DiaryCapture.capture(client), CombatCapture.capture(client), DiaryCapture.taskCounts(client),
            DiaryTaskCapture.capture(client),
            includeClan ? ClanCapture.capture(client, chatIcons, name) : null)
            .withQuestPoints(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.QP))
            .withMetadata(unlocks, info)
            .withCharacter(client.getWorld(), client.getWorldType(), client.getLocalPlayer().getCombatLevel(),
                client.getAccountType() == null ? "UNKNOWN" : client.getAccountType().name());
    }
}
