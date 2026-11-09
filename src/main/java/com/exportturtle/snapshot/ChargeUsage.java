package com.exportturtle.snapshot;

import java.util.Map;
import java.util.Set;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpotanimID;

/** Item-specific usage signals; counts remain distinct from confirmed check readings. */
final class ChargeUsage
{
    private static final Map<Integer, String> CASTS = Map.ofEntries(
        Map.entry(SpotanimID.FIRESTRIKE_CASTING, "tome_fire"),
        Map.entry(SpotanimID.FIREBOLT_CASTING, "tome_fire"),
        Map.entry(SpotanimID.FIREBLAST_CASTING, "tome_fire"),
        Map.entry(SpotanimID.FIREWAVE_CASTING, "tome_fire"),
        Map.entry(SpotanimID.FIRESURGE_CASTING, "tome_fire"),
        Map.entry(SpotanimID.WATERSTRIKE_CASTING, "tome_water"),
        Map.entry(SpotanimID.WATERBOLT_CASTING, "tome_water"),
        Map.entry(SpotanimID.WATERBLAST_CASTING, "tome_water"),
        Map.entry(SpotanimID.WATERWAVE_CASTING, "tome_water"),
        Map.entry(SpotanimID.WATERSURGE_CASTING, "tome_water"),
        Map.entry(SpotanimID.EARTHSTRIKE_CASTING, "tome_earth"),
        Map.entry(SpotanimID.EARTHBOLT_CASTING, "tome_earth"),
        Map.entry(SpotanimID.EARTHBLAST_CASTING, "tome_earth"),
        Map.entry(SpotanimID.EARTHWAVE_CASTING, "tome_earth"),
        Map.entry(SpotanimID.EARTHSURGE_CASTING, "tome_earth"),
        Map.entry(SpotanimID.IBANBLAST_CASTING, "ibans"),
        Map.entry(SpotanimID.IBANBLAST_CASTING_ORN, "ibans"),
        Map.entry(SpotanimID.TUMEKENS_SHADOW_CASTING, "shadow"),
        Map.entry(SpotanimID.SLAYER_TOTS_CASTING, "trident_seas"),
        Map.entry(SpotanimID.TOXIC_TOTS_CASTING, "trident_swamp"),
        Map.entry(SpotanimID.ARROW_VENATOR01_LAUNCH01, "venator"),
        Map.entry(SpotanimID.VFX_WARPED_SCEPTRE_CAST, "warped_sceptre"),
        Map.entry(SpotanimID.SP_ATTACK_ARROW_LAUNCH_FAERDHINEN, "bowfa"),
        Map.entry(SpotanimID.SANGUINESTI_STAFF_CASTING, "sanguinesti"),
        Map.entry(SpotanimID.SANGUINESTI_STAFF_CASTING_JUSTICIAR, "sanguinesti"),
        Map.entry(SpotanimID.VFX_AYAK_PLAYER_NORMAL_SPOTANIM, "eye_ayak"),
        Map.entry(SpotanimID.VFX_AYAK_PLAYER_SPECIAL_SPOTANIM, "eye_ayak"));

    static int slot(String family)
    {
        switch (family)
        {
            case "quiver": return EquipmentInventorySlot.CAPE.getSlotIdx();
            case "blood_fury": return EquipmentInventorySlot.AMULET.getSlotIdx();
            case "crystal_helm":
            case "serpentine": return EquipmentInventorySlot.HEAD.getSlotIdx();
            case "crystal_body": return EquipmentInventorySlot.BODY.getSlotIdx();
            case "crystal_legs": return EquipmentInventorySlot.LEGS.getSlotIdx();
            case "crystal_shield":
            case "tome_fire":
            case "tome_water":
            case "tome_earth": return EquipmentInventorySlot.SHIELD.getSlotIdx();
            case "suffering": return EquipmentInventorySlot.RING.getSlotIdx();
            default: return EquipmentInventorySlot.WEAPON.getSlotIdx();
        }
    }

    static boolean supported(String family)
    {
        return Set.of("shadow", "trident_seas", "trident_swamp", "venator", "warped_sceptre",
            "bowfa", "scythe", "obligator", "blowpipe", "blood_fury", "crystal_helm",
            "crystal_body", "crystal_legs", "crystal_shield", "serpentine", "quiver", "arclight",
            "sanguinesti", "eye_ayak", "tome_fire", "tome_water", "tome_earth", "ibans",
            "craws_bow", "webweaver", "viggora", "ursine", "tentacle", "crystal_bow",
            "crystal_halberd", "saeldor", "suffering").contains(family);
    }

    static boolean blowpipeAttack(int animation)
    {
        return animation == AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK ||
            animation == AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK_ORNAMENT ||
            animation == AnimationID.TOXIC_BLOWPIPE_SPECIAL_UPDATED;
    }

    static boolean quiverAttack(int animation)
    {
        return animation == AnimationID.HUMAN_BOW || animation == AnimationID.HUMAN_CROSSBOW ||
            animation == AnimationID.DTTD_PLAYER_FIRE_BONE_CROSSBOW ||
            animation == AnimationID.DTTD_PLAYER_FIRE_BONE_CROSSBOW_PVN ||
            animation == AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD ||
            animation == AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD_PVN;
    }

    static boolean knownMelee(int animation)
    {
        return meleeFamily(animation) != null || Set.of(AnimationID.HUMAN_SWORD_STAB,
            AnimationID.HUMAN_SWORD_SLASH, AnimationID.HUMAN_SWORD_LUNGE,
            AnimationID.HUMAN_AXE_CHOP, AnimationID.HUMAN_AXE_HACK, AnimationID.HUMAN_AXE_SMASH,
            AnimationID.HUMAN_SPEAR_SPIKE, AnimationID.HUMAN_SPEAR_LUNGE,
            AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK, AnimationID.GHRAZI_RAPIER_ATTACK,
            AnimationID.HUMAN_OSMUMTENS_FANG, AnimationID.HUMAN_2H_AXE_ATTACK,
            AnimationID.HUMAN_HALBERD_VIRULENCE_01).contains(animation);
    }

    private ChargeUsage()
    {
    }

    static String graphicFamily(int graphic)
    {
        return CASTS.get(graphic);
    }

    static String meleeFamily(int animation)
    {
        switch (animation)
        {
            case AnimationID.SCYTHE_OF_VITUR_ATTACK:
                return "scythe";
            case AnimationID.TFA_HUMAN_OBLIGATOR_ATTACK:
            case AnimationID.TFA_HUMAN_OBLIGATOR_MULTIHIT:
            case AnimationID.TFA_HUMAN_OBLIGATOR_ATTACK_FEMALE:
            case AnimationID.TFA_HUMAN_OBLIGATOR_MULTIHIT_FEMALE:
                return "obligator";
            default:
                return null;
        }
    }

    static final class Baseline
    {
        final int itemId;
        final String variant;
        int lastTick = Integer.MIN_VALUE;
        double chargeFraction;
        double ammoFraction;
        boolean away;
        boolean invalid;
        int pendingSavings;
        int savingsAt = Integer.MIN_VALUE;

        Baseline(int itemId, String variant)
        {
            this.itemId = itemId;
            this.variant = variant;
        }
    }
}
