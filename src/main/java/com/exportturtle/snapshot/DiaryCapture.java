package com.exportturtle.snapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/** Reads task completion independently of reward claims. */
final class DiaryCapture
{
    private DiaryCapture()
    {
    }

    static Map<String, Map<String, Boolean>> capture(Client client)
    {
        Map<String, Map<String, Boolean>> result = new LinkedHashMap<>();
        add(result, client, "Ardougne", VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE);
        add(result, client, "Desert", VarbitID.DESERT_DIARY_EASY_COMPLETE,
            VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, VarbitID.DESERT_DIARY_HARD_COMPLETE,
            VarbitID.DESERT_DIARY_ELITE_COMPLETE);
        add(result, client, "Falador", VarbitID.FALADOR_DIARY_EASY_COMPLETE,
            VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, VarbitID.FALADOR_DIARY_HARD_COMPLETE,
            VarbitID.FALADOR_DIARY_ELITE_COMPLETE);
        add(result, client, "Fremennik", VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,
            VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,
            VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE);
        add(result, client, "Kandarin", VarbitID.KANDARIN_DIARY_EASY_COMPLETE,
            VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, VarbitID.KANDARIN_DIARY_HARD_COMPLETE,
            VarbitID.KANDARIN_DIARY_ELITE_COMPLETE);
        add(result, client, "Karamja", VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
            VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        add(result, client, "Kourend & Kebos", VarbitID.KOUREND_DIARY_EASY_COMPLETE,
            VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, VarbitID.KOUREND_DIARY_HARD_COMPLETE,
            VarbitID.KOUREND_DIARY_ELITE_COMPLETE);
        add(result, client, "Lumbridge & Draynor", VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE);
        add(result, client, "Morytania", VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,
            VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,
            VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE);
        add(result, client, "Varrock", VarbitID.VARROCK_DIARY_EASY_COMPLETE,
            VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, VarbitID.VARROCK_DIARY_HARD_COMPLETE,
            VarbitID.VARROCK_DIARY_ELITE_COMPLETE);
        add(result, client, "Western Provinces", VarbitID.WESTERN_DIARY_EASY_COMPLETE,
            VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, VarbitID.WESTERN_DIARY_HARD_COMPLETE,
            VarbitID.WESTERN_DIARY_ELITE_COMPLETE);
        add(result, client, "Wilderness", VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,
            VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,
            VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE);
        return result;
    }

    private static void add(Map<String, Map<String, Boolean>> result, Client client, String region, int easy,
        int medium, int hard, int elite)
    {
        Map<String, Boolean> tiers = new LinkedHashMap<>();
        tiers.put("Easy", completed(client, easy));
        tiers.put("Medium", completed(client, medium));
        tiers.put("Hard", completed(client, hard));
        tiers.put("Elite", completed(client, elite));
        result.put(region, tiers);
    }

    private static boolean completed(Client client, int varbit)
    {
        int value = client.getVarbitValue(varbit);
        if (varbit == VarbitID.ATJUN_EASY_DONE || varbit == VarbitID.ATJUN_MED_DONE ||
            varbit == VarbitID.ATJUN_HARD_DONE)
        {
            return value >= 2; // Karamja uses 1 for started, 2 for completed.
        }
        return value > 0;
    }

    static AccountSnapshot.DiaryTasks taskCounts(Client client)
    {
        // Read-only summary procedures also used by Dink's diary notifier.
        // No named API constants are available for these scripts in 1.13.4.
        client.runScript(3971);
        int[] stack = client.getIntStack();
        if (stack == null || stack.length == 0)
            return null;
        int completed = stack[0];
        client.runScript(3980);
        stack = client.getIntStack();
        if (stack == null || stack.length == 0)
            return null;
        int total = stack[0];
        if (total <= 0 || total > 10000 || completed < 0 || completed > total)
            return null;
        return new AccountSnapshot.DiaryTasks(completed, total);
    }
}
