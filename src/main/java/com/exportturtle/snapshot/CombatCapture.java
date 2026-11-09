package com.exportturtle.snapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/** Reads the game-maintained task counts and point thresholds. */
final class CombatCapture
{
    private CombatCapture()
    {
    }

    static AccountSnapshot.CombatProgress capture(Client client)
    {
        // Thresholds are server supplied; omit this section until they are loaded.
        if (client.getVarbitValue(VarbitID.CA_THRESHOLD_EASY) <= 0 ||
            client.getVarbitValue(VarbitID.CA_THRESHOLD_MEDIUM) <= 0 ||
            client.getVarbitValue(VarbitID.CA_THRESHOLD_HARD) <= 0 ||
            client.getVarbitValue(VarbitID.CA_THRESHOLD_ELITE) <= 0 ||
            client.getVarbitValue(VarbitID.CA_THRESHOLD_MASTER) <= 0 ||
            client.getVarbitValue(VarbitID.CA_THRESHOLD_GRANDMASTER) <= 0)
        {
            return null;
        }
        Map<String, AccountSnapshot.CombatTier> tiers = new LinkedHashMap<>();
        add(tiers, client, "Easy", VarbitID.CA_TOTAL_TASKS_COMPLETED_EASY, VarbitID.CA_THRESHOLD_EASY);
        add(tiers, client, "Medium", VarbitID.CA_TOTAL_TASKS_COMPLETED_MEDIUM, VarbitID.CA_THRESHOLD_MEDIUM);
        add(tiers, client, "Hard", VarbitID.CA_TOTAL_TASKS_COMPLETED_HARD, VarbitID.CA_THRESHOLD_HARD);
        add(tiers, client, "Elite", VarbitID.CA_TOTAL_TASKS_COMPLETED_ELITE, VarbitID.CA_THRESHOLD_ELITE);
        add(tiers, client, "Master", VarbitID.CA_TOTAL_TASKS_COMPLETED_MASTER, VarbitID.CA_THRESHOLD_MASTER);
        add(tiers, client, "Grandmaster", VarbitID.CA_TOTAL_TASKS_COMPLETED_GRANDMASTER,
            VarbitID.CA_THRESHOLD_GRANDMASTER);
        int[] taskBits = new int[] {client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_0),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_1),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_2),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_3),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_4),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_5),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_6),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_7),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_8),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_9),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_10),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_11),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_12),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_13),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_14),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_15),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_16),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_17),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_18),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_19),
            client.getVarpValue(VarPlayerID.CA_TASK_COMPLETED_20)};
        return new AccountSnapshot.CombatProgress(client.getVarbitValue(VarbitID.CA_POINTS), tiers, taskBits);
    }

    private static void add(Map<String, AccountSnapshot.CombatTier> tiers, Client client, String name,
        int completedTasks, int pointsRequired)
    {
        tiers.put(name, new AccountSnapshot.CombatTier(
                            client.getVarbitValue(completedTasks), client.getVarbitValue(pointsRequired)));
    }
}
