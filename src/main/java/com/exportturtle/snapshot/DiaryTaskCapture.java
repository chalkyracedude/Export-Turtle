package com.exportturtle.snapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Per-task bit locations, in the in-game/Wiki task order.
 * Mapping reference: weirdgloop/wikisync-api achievementDiariesSpecs.json.
 */
final class DiaryTaskCapture
{
    private static final String[] TIERS = {"Easy", "Medium", "Hard", "Elite"};

    static Map<String, Map<String, List<Boolean>>> capture(Client client)
    {
        Map<String, Map<String, List<Boolean>>> result = new LinkedHashMap<>();
        add(result, client, "Ardougne",
            new Task[][] {{// Easy
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 0),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 1),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 2),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 4),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 5),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 6),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 7),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 9),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 11),
                              p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 12)},
                {// Medium
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 24),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 25)},
                {// Hard
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 5)},
                {// Elite
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 10),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 11),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 12),
                    p(VarPlayerID.ARDOUNGE_ACHIEVEMENT_DIARY2, 13)}});
        add(result, client, "Desert",
            new Task[][] {
                {// Easy
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 1), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 2),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 3), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 4),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 5), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 6),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 7), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 8),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 9), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 10),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 11)},
                {// Medium
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 12), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 14), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 16), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 18), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 20), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 22), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 23)},
                {// Hard
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 24), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 26), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 28), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 30), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 0), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 1)},
                {// Elite
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 2), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 5), p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2, 8)}});
        add(result, client, "Falador",
            new Task[][] {
                {// Easy
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 0), p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 1),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 2), p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 3),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 4), p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 5),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 6), p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 7),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 8), p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 9),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 10)},
                {// Medium
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 11),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 12),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 24),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 25)},
                {// Hard
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 4)},
                {// Elite
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.FALADOR_ACHIEVEMENT_DIARY2, 10)}});
        add(result, client, "Fremennik",
            new Task[][] {{// Easy
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 1),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 2),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 3),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 4),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 5),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 6),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 7),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 8),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 9),
                              p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 10)},
                {// Medium
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 11),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 12),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 20)},
                {// Hard
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 24),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 30)},
                {// Elite
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.FREMENNIK_ACHIEVEMENT_DIARY2, 4)}});
        add(result, client, "Kandarin",
            new Task[][] {{// Easy
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 1),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 2),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 3),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 4),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 5),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 6),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 7),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 8),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 9),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 10),
                              p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 11)},
                {// Medium
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 12),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 24),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 25)},
                {// Hard
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 4)},
                {// Elite
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 10),
                    p(VarPlayerID.KANDARIN_ACHIEVEMENT_DIARY2, 11)}});
        add(result, client, "Karamja",
            new Task[][] {{// Easy
                              b(VarbitID.ATJUN_EASY_BANANA, 5), b(VarbitID.ATJUN_EASY_SWING, 1),
                              b(VarbitID.ATJUN_EASY_GOLD, 1), b(VarbitID.ATJUN_EASY_BOAT_SARIM, 1),
                              b(VarbitID.ATJUN_EASY_BOAT_ARDY, 1), b(VarbitID.ATJUN_EASY_CAIRN, 1),
                              b(VarbitID.ATJUN_EASY_FISHING, 1), b(VarbitID.ATJUN_EASY_SEAWEED, 5),
                              b(VarbitID.ATJUN_EASY_TZHAAR, 1), b(VarbitID.ATJUN_EASY_JOGRE, 1)},
                {// Medium
                    b(VarbitID.ATJUN_MED_AGILITY, 1), b(VarbitID.ATJUN_MED_VOLCANO, 1),
                    b(VarbitID.ATJUN_MED_CRANDOR, 1), b(VarbitID.ATJUN_MED_CART, 1),
                    b(VarbitID.ATJUN_MED_CLEANUP, 1), b(VarbitID.ATJUN_MED_SPIDER, 1),
                    b(VarbitID.ATJUN_MED_KHAZARD, 1), b(VarbitID.ATJUN_MED_TEAK, 1),
                    b(VarbitID.ATJUN_MED_MAHOGANY, 1), b(VarbitID.ATJUN_MED_KARAMBWAN, 1),
                    b(VarbitID.ATJUN_MED_MACHETTE, 1), b(VarbitID.ATJUN_MED_GLIDER, 1),
                    b(VarbitID.ATJUN_MED_FARMING, 1), b(VarbitID.ATJUN_MED_GRAAHK, 1),
                    b(VarbitID.ATJUN_MED_SHILO_VINES, 1), b(VarbitID.ATJUN_MED_SHILO_LAVA, 1),
                    b(VarbitID.ATJUN_MED_SHILO_STAIRS, 1), b(VarbitID.ATJUN_MED_CHARTER, 1),
                    b(VarbitID.ATJUN_MED_TOPAZ, 1)},
                {// Hard
                    b(VarbitID.ATJUN_HARD_FIGHTPITS, 1), b(VarbitID.ATJUN_HARD_FIGHTCAVE, 1),
                    b(VarbitID.ATJUN_HARD_OOMLIE, 1), b(VarbitID.ATJUN_HARD_NATURE, 1),
                    b(VarbitID.ATJUN_HARD_KARAMBWAN, 1), b(VarbitID.ATJUN_HARD_DEATHWING, 1),
                    b(VarbitID.ATJUN_HARD_XBOW, 1), b(VarbitID.ATJUN_HARD_PALM, 5),
                    b(VarbitID.ATJUN_HARD_DURADEL, 1), b(VarbitID.ATJUN_HARD_DRAGON, 1)},
                {// Elite
                    p(VarPlayerID.ATJUN_TASKS_4, 1), p(VarPlayerID.ATJUN_TASKS_4, 2),
                    p(VarPlayerID.ATJUN_TASKS_4, 3), p(VarPlayerID.ATJUN_TASKS_4, 4),
                    p(VarPlayerID.ATJUN_TASKS_4, 5)}});
        add(result, client, "Kourend & Kebos",
            new Task[][] {
                {// Easy
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 1), p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 2),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 3), p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 4),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 5), p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 6),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 7), p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 8),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 9), p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 10),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 11),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 12)},
                {// Medium
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 24)},
                {// Hard
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 3)},
                {// Elite
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 10),
                    p(VarPlayerID.KOUREND_ACHIEVEMENT_DIARY2, 11)}});
        add(result, client, "Lumbridge & Draynor",
            new Task[][] {{// Easy
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 1),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 2),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 3),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 4),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 5),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 6),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 7),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 8),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 9),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 10),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 11),
                              p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 12)},
                {// Medium
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 24)},
                {// Hard
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 3)},
                {// Elite
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.LUMB_DRAY_ACHIEVEMENT_DIARY2, 9)}});
        add(result, client, "Morytania",
            new Task[][] {{// Easy
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 1),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 2),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 3),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 4),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 5),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 6),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 7),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 8),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 9),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 10),
                              p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 11)},
                {// Medium
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 12),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 22)},
                {// Hard
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 24),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 2)},
                {// Elite
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.MORYTANIA_ACHIEVEMENT_DIARY2, 8)}});
        add(result, client, "Varrock",
            new Task[][] {
                {// Easy
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 1), p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 2),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 3), p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 4),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 5), p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 6),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 7), p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 8),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 9), p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 10),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 11),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 12),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 14)},
                {// Medium
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 24),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 28)},
                {// Hard
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 6)},
                {// Elite
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 10),
                    p(VarPlayerID.VARROCK_ACHIEVEMENT_DIARY2, 11)}});
        add(result, client, "Western Provinces",
            new Task[][] {
                {// Easy
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 1), p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 2),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 3), p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 4),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 5), p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 6),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 7), p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 8),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 9), p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 10),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 11)},
                {// Medium
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 12),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 17),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 24)},
                {// Hard
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 2),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 4),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 5)},
                {// Elite
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 6),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 12),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 13),
                    p(VarPlayerID.WESTERN_ACHIEVEMENT_DIARY2, 14)}});
        add(result, client, "Wilderness",
            new Task[][] {{// Easy
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 1),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 2),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 3),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 4),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 5),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 6),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 7),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 8),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 9),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 10),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 11),
                              p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 12)},
                {// Medium
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 13),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 14),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 15),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 16),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 18),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 19),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 20),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 21),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 22),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 23),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 24)},
                {// Hard
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 25),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 26),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 27),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 28),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 29),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 30),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY, 31),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 0),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 1),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 2)},
                {// Elite
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 3),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 5),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 7),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 8),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 9),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 10),
                    p(VarPlayerID.WILDERNESS_ACHIEVEMENT_DIARY2, 11)}});
        return result;
    }

    private static void add(
        Map<String, Map<String, List<Boolean>>> result, Client client, String region, Task[][] tasks)
    {
        Map<String, List<Boolean>> tiers = new LinkedHashMap<>();
        Map<Integer, Integer> varps = new LinkedHashMap<>();
        for (int tier = 0; tier < tasks.length; tier++)
        {
            List<Boolean> states = new ArrayList<>();
            for (int index = 0; index < tasks[tier].length; index++)
            {
                Task task = tasks[tier][index];
                boolean done =
                    task.varbit
                        ? client.getVarbitValue(task.id) == task.value
                        : (varps.computeIfAbsent(task.id, client::getVarpValue) & (1 << task.value)) != 0;
                // Desert medium task 11 has separate Ironman and non-Ironman flags.
                if (region.equals("Desert") && tier == 1 && index == 10)
                {
                    done |= (client.getVarpValue(VarPlayerID.DESERT_ACHIEVEMENT_DIARY2) & (1 << 9)) != 0;
                }
                states.add(done);
            }
            tiers.put(TIERS[tier], states);
        }
        result.put(region, tiers);
    }

    private static Task p(int id, int offset)
    {
        return new Task(false, id, offset);
    }
    private static Task b(int id, int value)
    {
        return new Task(true, id, value);
    }

    private static final class Task
    {
        final boolean varbit;
        final int id;
        final int value;
        Task(boolean varbit, int id, int value)
        {
            this.varbit = varbit;
            this.id = id;
            this.value = value;
        }
    }
}
