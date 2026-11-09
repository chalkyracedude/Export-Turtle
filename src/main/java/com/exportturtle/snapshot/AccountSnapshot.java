package com.exportturtle.snapshot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.WorldType;

/** Versioned interchange data. Contains no login or session identifiers. */
public final class AccountSnapshot
{
    private final int schemaVersion = 1;
    private final String accountName;
    private final String capturedAt;
    private final Map<String, SkillProgress> skills;
    private final Map<String, QuestProgress> quests;
    private final Map<String, Map<String, Boolean>> diaries;
    private final CombatProgress combatAchievements;
    private final DiaryTasks diaryTasks;
    private final ClanProgress clan;
    private Integer questPoints;
    private Map<String, Object> character;
    private Map<String, Object> taskCatalog;
    private Map<String, Map<String, List<Map<String, Object>>>> diaryTaskDetails;
    private Map<String, Boolean> unlocks;
    private Map<String, String> exportInfo;

    void withTaskDetails(JsonObject catalog)
    {
        taskCatalog = new LinkedHashMap<>();
        for (String key : new String[] {"source", "revision", "license", "diarySource"})
            taskCatalog.put(key, catalog.get(key).getAsString());
        if (combatAchievements != null && combatAchievements.taskBits != null)
        {
            List<Map<String, Object>> tasks = new ArrayList<>();
            for (JsonElement element : catalog.getAsJsonArray("combat"))
            {
                JsonObject task = element.getAsJsonObject();
                int id = task.get("id").getAsInt();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", id);
                for (String key : new String[] {"name", "boss", "tier"})
                    row.put(key, task.get(key).getAsString());
                row.put("points", task.get("points").getAsInt());
                if (id >= 0 && id / 32 < combatAchievements.taskBits.length)
                    row.put("completed", (combatAchievements.taskBits[id / 32] & (1 << (id % 32))) != 0);
                tasks.add(Collections.unmodifiableMap(row));
            }
            combatAchievements.tasks = Collections.unmodifiableList(tasks);
        }
        if (diaryTaskStates == null)
            return;
        diaryTaskDetails = new LinkedHashMap<>();
        catalog.getAsJsonObject("diaries").entrySet().forEach(region -> {
            Map<String, List<Map<String, Object>>> tiers = new LinkedHashMap<>();
            region.getValue().getAsJsonObject().entrySet().forEach(tier -> {
                JsonArray names = tier.getValue().getAsJsonArray();
                List<Boolean> states = diaryTaskStates
                    .getOrDefault(region.getKey(), Collections.emptyMap()).get(tier.getKey());
                List<Map<String, Object>> rows = new ArrayList<>();
                for (int i = 0; i < names.size(); i++)
                {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("index", i);
                    row.put("name", names.get(i).getAsString());
                    if (states != null && states.size() == names.size())
                        row.put("completed", states.get(i));
                    rows.add(Collections.unmodifiableMap(row));
                }
                tiers.put(tier.getKey(), Collections.unmodifiableList(rows));
            });
            diaryTaskDetails.put(region.getKey(), Collections.unmodifiableMap(tiers));
        });
    }

    public AccountSnapshot withCharacter(int world, Set<WorldType> worldTypes,
        int combatLevel, String accountType)
    {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("world", world);
        details.put("worldTypes",
            worldTypes.stream().map(Enum::name).sorted().collect(Collectors.toList()));
        details.put("combatLevel", combatLevel);
        details.put("accountType", accountType);
        details.put("totalLevel", skills.values().stream().mapToInt(skill -> skill.level).sum());
        details.put("totalXp", skills.values().stream().mapToLong(skill -> skill.xp).sum());
        character = Collections.unmodifiableMap(details);
        return this;
    }

    public AccountSnapshot withMetadata(Map<String, Boolean> unlocks, Map<String, String> info)
    {
        this.unlocks = Collections.unmodifiableMap(new LinkedHashMap<>(unlocks));
        this.exportInfo = Collections.unmodifiableMap(new LinkedHashMap<>(info));
        return this;
    }

    public AccountSnapshot withQuestPoints(int value)
    {
        this.questPoints = value;
        return this;
    }
    private final Map<String, Map<String, List<Boolean>>> diaryTaskStates;

    public AccountSnapshot(String accountName, String capturedAt, Map<String, SkillProgress> skills,
        Map<String, QuestProgress> quests)
    {
        this(accountName, capturedAt, skills, quests, Collections.emptyMap());
    }

    public AccountSnapshot(String accountName, String capturedAt, Map<String, SkillProgress> skills,
        Map<String, QuestProgress> quests, Map<String, Map<String, Boolean>> diaries)
    {
        this(accountName, capturedAt, skills, quests, diaries, null);
    }

    public AccountSnapshot(String accountName, String capturedAt, Map<String, SkillProgress> skills,
        Map<String, QuestProgress> quests, Map<String, Map<String, Boolean>> diaries,
        CombatProgress combatAchievements)
    {
        this(accountName, capturedAt, skills, quests, diaries, combatAchievements, null);
    }

    public AccountSnapshot(String accountName, String capturedAt, Map<String, SkillProgress> skills,
        Map<String, QuestProgress> quests, Map<String, Map<String, Boolean>> diaries,
        CombatProgress combatAchievements, DiaryTasks diaryTasks)
    {
        this(accountName, capturedAt, skills, quests, diaries, combatAchievements, diaryTasks, null);
    }

    public AccountSnapshot(String accountName, String capturedAt, Map<String, SkillProgress> skills,
        Map<String, QuestProgress> quests, Map<String, Map<String, Boolean>> diaries,
        CombatProgress combatAchievements, DiaryTasks diaryTasks,
        Map<String, Map<String, List<Boolean>>> taskStates)
    {
        this(accountName, capturedAt, skills, quests, diaries, combatAchievements, diaryTasks, taskStates,
            null);
    }

    public AccountSnapshot(String accountName, String capturedAt, Map<String, SkillProgress> skills,
        Map<String, QuestProgress> quests, Map<String, Map<String, Boolean>> diaries,
        CombatProgress combatAchievements, DiaryTasks diaryTasks,
        Map<String, Map<String, List<Boolean>>> taskStates, ClanProgress clan)
    {
        this.clan = clan;
        if (taskStates == null)
        {
            this.diaryTaskStates = null;
        }
        else
        {
            Map<String, Map<String, List<Boolean>>> regions = new LinkedHashMap<>();
            taskStates.forEach((region, tiers) -> {
                Map<String, List<Boolean>> copy = new LinkedHashMap<>();
                tiers.forEach(
                    (tier, states) -> copy.put(tier, Collections.unmodifiableList(new ArrayList<>(states))));
                regions.put(region, Collections.unmodifiableMap(copy));
            });
            this.diaryTaskStates = Collections.unmodifiableMap(regions);
        }
        this.diaryTasks = diaryTasks;
        this.accountName = accountName;
        this.capturedAt = capturedAt;
        this.skills = Collections.unmodifiableMap(new LinkedHashMap<>(skills));
        this.quests = Collections.unmodifiableMap(new LinkedHashMap<>(quests));
        Map<String, Map<String, Boolean>> diaryCopy = new LinkedHashMap<>();
        diaries.forEach(
            (region,
                tiers) -> diaryCopy.put(region, Collections.unmodifiableMap(new LinkedHashMap<>(tiers))));
        this.diaries = Collections.unmodifiableMap(diaryCopy);
        this.combatAchievements = combatAchievements;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public boolean hasClan()
    {
        return clan != null && clan.name != null && !clan.name.trim().isEmpty();
    }

    public static final class ClanProgress
    {
        private final String name;
        private final String rank;
        private final RankIcon icon;

        ClanProgress(String name, String rank, java.awt.image.BufferedImage image)
        {
            this.name = name;
            this.rank = rank;
            this.icon = image == null ? null : new RankIcon(image);
        }
    }

    private static final class RankIcon
    {
        private final int width, height;
        private final int[] pixels;
        RankIcon(java.awt.image.BufferedImage image)
        {
            width = image.getWidth();
            height = image.getHeight();
            pixels = image.getRGB(0, 0, width, height, null, 0, width);
        }
    }

    public static final class DiaryTasks
    {
        private final int completed;
        private final int total;

        public DiaryTasks(int completed, int total)
        {
            this.completed = completed;
            this.total = total;
        }
    }

    public static final class CombatProgress
    {
        private final int points;
        private final int[] taskBits;
        private final Map<String, CombatTier> tiers;
        private List<Map<String, Object>> tasks;

        public CombatProgress(int points, Map<String, CombatTier> tiers)
        {
            this(points, tiers, null);
        }

        public CombatProgress(int points, Map<String, CombatTier> tiers, int[] taskBits)
        {
            this.taskBits = taskBits == null ? null : taskBits.clone();
            this.points = points;
            this.tiers = Collections.unmodifiableMap(new LinkedHashMap<>(tiers));
        }
    }

    public static final class CombatTier
    {
        private final int completedTasks;
        private final int pointsRequired;

        public CombatTier(int completedTasks, int pointsRequired)
        {
            this.completedTasks = completedTasks;
            this.pointsRequired = pointsRequired;
        }
    }

    public static final class SkillProgress
    {
        private final int level;
        private final int xp;
        private final int boostedLevel;

        public SkillProgress(int level, int xp)
        {
            this(level, xp, level);
        }

        public SkillProgress(int level, int xp, int boostedLevel)
        {
            this.level = level;
            this.xp = xp;
            this.boostedLevel = boostedLevel;
        }
    }

    public static final class QuestProgress
    {
        private final String name;
        private final String state;

        public QuestProgress(String name, String state)
        {
            this.name = name;
            this.state = state;
        }
    }
}
