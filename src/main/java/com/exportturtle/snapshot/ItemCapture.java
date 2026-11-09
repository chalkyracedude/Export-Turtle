package com.exportturtle.snapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

/** Event-driven, client-thread copies of personal bank, inventory and equipment. */
public final class ItemCapture
{
    private static final Set<Integer> CORRUPTED_BOWS =
        Set.of(ItemID.BOW_OF_FAERDHINEN_INFINITE, ItemID.BOW_OF_FAERDHINEN_INFINITE_ITHELL,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_IORWERTH, ItemID.BOW_OF_FAERDHINEN_INFINITE_TRAHAEARN,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_CADARN, ItemID.BOW_OF_FAERDHINEN_INFINITE_CRWYS,
            ItemID.BOW_OF_FAERDHINEN_INFINITE_MEILYR, ItemID.BOW_OF_FAERDHINEN_INFINITE_AMLODD);
    private static final Set<Integer> CORRUPTED_BLADES =
        Set.of(ItemID.BLADE_OF_SAELDOR_INFINITE, ItemID.BLADE_OF_SAELDOR_INFINITE_ITHELL,
            ItemID.BLADE_OF_SAELDOR_INFINITE_IORWERTH, ItemID.BLADE_OF_SAELDOR_INFINITE_TRAHAEARN,
            ItemID.BLADE_OF_SAELDOR_INFINITE_CADARN, ItemID.BLADE_OF_SAELDOR_INFINITE_CRWYS,
            ItemID.BLADE_OF_SAELDOR_INFINITE_MEILYR, ItemID.BLADE_OF_SAELDOR_INFINITE_AMLODD);
    private final Client client;
    private final ItemManager itemManager;
    private final Map<String, ItemSnapshot.Container> containers = new LinkedHashMap<>();
    private final Map<String, ItemSnapshot.Charge> charges = new LinkedHashMap<>();
    private final Map<String, ItemSnapshot.Charge> chargeVariants = new LinkedHashMap<>();
    private final Map<Integer, Integer> graphicCycles = new LinkedHashMap<>();
    private final Map<String, ChargeUsage.Baseline> usage = new LinkedHashMap<>();
    private String account;
    private String checkedItem;
    private int checkedAt;
    private int combatAt = Integer.MIN_VALUE;
    private int pickupAt = Integer.MIN_VALUE;
    private int pickupX, pickupY, pickupQuantity, scalesBeforePickup;
    private boolean dirty = true;
    private boolean bankOpen;
    private boolean bankDirty;
    private boolean bankSeen;
    private boolean vaultOpen;
    private boolean vaultSeen;
    private boolean vaultDirty;
    private boolean changed;

    public ItemCapture(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
    }

    public void reset()
    {
        account = null;
        checkedItem = null;
        containers.clear();
        charges.clear();
        chargeVariants.clear();
        usage.clear();
        graphicCycles.clear();
        combatAt = Integer.MIN_VALUE;
        pickupAt = Integer.MIN_VALUE;
        pickupQuantity = 0;
        bankSeen = false;
        vaultOpen = false;
        vaultSeen = false;
        vaultDirty = false;
        bankOpen = false;
        bankDirty = false;
        dirty = true;
        changed = false;
    }

    public void onContainerChanged(int id)
    {
        if (id == InventoryID.WORN)
            for (Map.Entry<String, ChargeUsage.Baseline> entry : usage.entrySet())
                if (entry.getValue().itemId != equippedId(ChargeUsage.slot(entry.getKey())))
                    entry.getValue().away = true;
        if (id == InventoryID.INV || id == InventoryID.WORN || id == InventoryID.BANK ||
            id == InventoryID.SEED_VAULT)
            dirty = true;
        if (id == InventoryID.BANK)
            bankDirty = true;
        if (id == InventoryID.SEED_VAULT)
            vaultDirty = true;
    }

    private boolean ready()
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null ||
            !SnapshotCapture.isMainGame(client))
            return false;
        String name = client.getLocalPlayer().getName();
        if (name == null || name.trim().isEmpty())
            return false;
        if (!name.equals(account))
        {
            reset();
            account = name;
            changed = true;
        }
        return true;
    }

    public void onItemAction(int itemId, String option)
    {
        checkedItem = null;
        if (!ready() || itemId < 0 || option == null)
            return;
        String action = option.replaceAll("<[^>]*>", "").toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("check", "charges", "check-charges", "check charges").contains(action))
            return;
        ItemComposition definition = client.getItemDefinition(itemId);
        if (definition != null && ChargeChecks.family(definition.getName()) != null)
        {
            checkedItem = definition.getName();
            checkedAt = client.getTickCount();
        }
    }

    public void onMessage(String message)
    {
        if (message == null || !ready())
            return;
        if (message.equals("You disable the recoil effect of your ring.") ||
            message.equals("You enable the recoil effect of your ring."))
        {
            ItemSnapshot.Charge ring = charges.get("suffering");
            if (ring != null)
                putCharge("suffering", ring.withEffect(message.startsWith("You enable")));
            refreshUsage();
            return;
        }
        ChargeAdjustments.Change adjustment = ChargeAdjustments.parse(message);
        if (adjustment != null)
        {
            applyAdjustment(adjustment);
            return;
        }
        ChargeMessages.Result result;
        try
        {
            int age = client.getTickCount() - checkedAt;
            String context = age >= 0 && age <= 5 ? checkedItem : null;
            result = ChargeMessages.parse(message, Instant.now().toString(), context);
        }
        catch (NumberFormatException invalid)
        {
            return;
        }
        if (result == null)
        {
            String text = message.replaceAll("<[^>]*>", "").toLowerCase(java.util.Locale.ROOT);
            // An unparsed refill or uncharge makes the previous baseline unsafe to extend.
            if (text.contains("charg") && (text.startsWith("you ") ||
                text.contains("automatically")))
                for (String family : usage.keySet())
                    invalidate(family, "Unrecognised charge change; check the item");
        }
        if (result != null)
        {
            checkedItem = null;
            for (Map.Entry<String, ItemSnapshot.Charge> entry : chargeVariants.entrySet())
                if (entry.getKey().startsWith(result.key + ":") &&
                    "active".equals(entry.getValue().trackingState))
                    entry.setValue(entry.getValue().withStatus("paused", "A new charge reading replaced this baseline",
                        Instant.now().toString()));
            charges.put(result.key, result.charge);
            chargeVariants.put(result.key + ":" + result.variant, result.charge);
            establishUsage(result);
            changed = true;
        }
    }

    private void applyAdjustment(ChargeAdjustments.Change adjustment)
    {
        String family = adjustment.family;
        ItemSnapshot.Charge charge = charges.get(family);
        ChargeUsage.Baseline baseline = usage.get(family);
        int tick = client.getTickCount();
        if (adjustment.saved)
        {
            if (charge == null || charge.permanent || baseline == null || baseline.invalid || baseline.away)
                return;
            int remaining = charge.trackedQuantity == null ? charge.quantity : charge.trackedQuantity;
            int budget = charge.quantity + (charge.trackedAddedQuantity == null ? 0 : charge.trackedAddedQuantity);
            if ((long) tick - baseline.lastTick <= 1)
            {
                if (remaining < budget)
                    putCharge(family, charge.adjust(1, false, Instant.now().toString()));
                else
                {
                    baseline.chargeFraction -= 1;
                    putCharge(family, charge.adjust(0, false, Instant.now().toString()));
                }
            }
            else
            {
                baseline.pendingSavings++;
                baseline.savingsAt = tick;
            }
            return;
        }
        if (charge == null || charge.quantity == null || charge.permanent)
        {
            if (charge == null)
                charge = new ItemSnapshot.Charge(null, null, false, Instant.now().toString(), "charge-message");
            if (!charge.permanent)
                putCharge(family, charge.withStatus("needs-check", "Refill observed; remaining balance is unknown",
                    Instant.now().toString()));
            return;
        }
        putCharge(family, charge.adjust(adjustment.quantity, true, Instant.now().toString()));
        if (baseline == null || baseline.invalid)
            status(family, "needs-check", "Refill observed; check which copy was charged");
    }

    private int equippedId(int slot)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        Item weapon = equipment == null ? null : equipment.getItem(slot);
        return weapon == null ? -1 : weapon.getId();
    }

    private void establishUsage(ChargeMessages.Result result)
    {
        usage.remove(result.key);
        if (!ChargeUsage.supported(result.key) || result.charge.permanent ||
            result.charge.quantity == null)
            return;
        int itemId = equippedId(ChargeUsage.slot(result.key));
        ItemComposition item = itemId < 0 ? null : client.getItemDefinition(itemId);
        if (item == null || item.getName() == null)
            return;
        String name = item.getName().toLowerCase(java.util.Locale.ROOT).replace('\u2019', '\'');
        // A generic family name is used by the older bowfa check parser.
        boolean matches = result.variant.equalsIgnoreCase(name) ||
            result.key.equals("bowfa") && name.equals("bow of faerdhinen") ||
            result.key.equals("arclight") && name.equals("arclight") ||
            result.key.equals("saeldor") && name.equals("blade of saeldor") ||
            result.key.equals("quiver") && name.startsWith("dizana's quiver") ||
            result.key.equals("serpentine") && result.variant.startsWith("unidentified ") &&
                "serpentine".equals(ChargeChecks.family(name)) ||
            result.key.equals("blowpipe") && result.variant.startsWith("unidentified ") &&
                "blowpipe".equals(ChargeChecks.family(name));
        if (matches)
        {
            usage.put(result.key, new ChargeUsage.Baseline(itemId, result.variant));
            refreshUsage();
        }
        else
            putCharge(result.key, result.charge.withStatus("needs-check", "Check this item while equipped",
                Instant.now().toString()));
    }

    private int inventoryCopies(int itemId)
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null || inventory.getItems() == null)
            return -1;
        int total = 0;
        for (Item item : inventory.getItems())
            if (item != null && item.getId() == itemId)
                total += Math.max(0, item.getQuantity());
        return total;
    }

    private void refreshUsage()
    {
        for (Map.Entry<String, ChargeUsage.Baseline> entry : usage.entrySet())
        {
            String family = entry.getKey();
            ChargeUsage.Baseline baseline = entry.getValue();
            if (baseline.invalid)
                continue;
            boolean equipped = baseline.itemId == equippedId(ChargeUsage.slot(family));
            int copies = inventoryCopies(baseline.itemId);
            if (equipped && copies > 0 || !equipped && copies > 1)
            {
                baseline.invalid = true;
                status(family, "needs-check", "Multiple matching copies; check the equipped item");
            }
            else if (!equipped)
            {
                baseline.away = true;
                if (copies == 1)
                    status(family, "paused", "Unequipped; resumes when this copy is equipped");
                else
                {
                    baseline.invalid = true;
                    status(family, "needs-check", "Item left inventory; check again after retrieving it");
                }
            }
            else if (baseline.away && copies < 0)
            {
                baseline.invalid = true;
                status(family, "needs-check", "Cannot identify this copy safely");
            }
            else
            {
                baseline.away = false;
                if (family.equals("suffering") && Boolean.FALSE.equals(charges.get(family).effectEnabled))
                    status(family, "paused", "Recoil effect disabled");
                else
                    status(family, "active", "Tracking observed use");
            }
        }
    }

    private void putCharge(String family, ItemSnapshot.Charge charge)
    {
        charges.put(family, charge);
        ChargeUsage.Baseline baseline = usage.get(family);
        if (baseline != null)
            chargeVariants.put(family + ":" + baseline.variant, charge);
        changed = true;
    }

    private void status(String family, String state, String reason)
    {
        ItemSnapshot.Charge charge = charges.get(family);
        if (charge != null && (!state.equals(charge.trackingState) || !reason.equals(charge.trackingReason)))
            putCharge(family, charge.withStatus(state, reason, Instant.now().toString()));
    }

    private void invalidate(String family, String reason)
    {
        ChargeUsage.Baseline baseline = usage.get(family);
        if (baseline != null)
            baseline.invalid = true;
        status(family, "needs-check", reason);
    }

    public void onGraphic(int graphic, int startCycle)
    {
        String family = ChargeUsage.graphicFamily(graphic);
        if (family == null || !ready())
            return;
        Integer previous = graphicCycles.put(graphic, startCycle);
        if (previous == null || previous != startCycle)
        {
            combatAt = client.getTickCount();
            consume(family, "attack-graphic");
        }
    }

    public void onMeleeHit(int animation)
    {
        onMeleeHit(animation, 1);
    }

    public void onMeleeHit(int animation, int damage)
    {
        if (!ready())
            return;
        combatAt = client.getTickCount();
        if (damage > 0)
            consume(ChargeUsage.meleeFamily(animation), "damage-event");
        if (animation == net.runelite.api.gameval.AnimationID.HUMAN_SWORD_SLASH ||
            animation == net.runelite.api.gameval.AnimationID.HUMAN_SWORD_STAB)
            consume("arclight", "damage-event");
        if (isMeleeAttack(animation))
        {
            consume("blood_fury", "damage-event", 1, null, false);
            consume("saeldor", "damage-event");
        }
    }

    private boolean isMeleeAttack(int animation)
    {
        if (animation < 0)
            return false;
        if (ChargeUsage.knownMelee(animation))
            return true;
        net.runelite.api.EnumComposition categories = client.getEnum(net.runelite.api.EnumID.WEAPON_STYLES);
        if (categories == null)
            return false;
        int category = client.getVarbitValue(net.runelite.api.gameval.VarbitID.COMBAT_WEAPON_CATEGORY);
        int stylesId = categories.getIntValue(category);
        net.runelite.api.EnumComposition styles = stylesId <= 0 ? null : client.getEnum(stylesId);
        int index = client.getVarpValue(net.runelite.api.gameval.VarPlayerID.COM_MODE);
        int[] ids = styles == null ? null : styles.getIntVals();
        if (ids == null || index < 0 || index >= ids.length || index >= 4)
            return false;
        net.runelite.api.StructComposition style = client.getStructComposition(ids[index]);
        String name = style == null ? null : style.getStringValue(net.runelite.api.ParamID.ATTACK_STYLE_NAME);
        return name != null && Set.of("accurate", "aggressive", "controlled", "defensive")
            .contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    public void onAnimation(int animation)
    {
        if (!ready())
            return;
        if (ChargeUsage.blowpipeAttack(animation) || ChargeUsage.quiverAttack(animation) ||
            ChargeUsage.knownMelee(animation))
            combatAt = client.getTickCount();
        if (animation == net.runelite.api.gameval.AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK)
            consume("tentacle", "attack-animation");
        if (animation == net.runelite.api.gameval.AnimationID.HUMAN_SPEAR_SPIKE ||
            animation == net.runelite.api.gameval.AnimationID.HUMAN_SCYTHE_SWEEP ||
            animation == net.runelite.api.gameval.AnimationID.DRAGON_HALBERD_SPECIAL_ATTACK)
            consume("crystal_halberd", "attack-animation");
        if (animation == net.runelite.api.gameval.AnimationID.HUMAN_BOW)
        {
            consume("craws_bow", "attack-animation");
            consume("webweaver", "attack-animation");
            consume("crystal_bow", "attack-animation");
        }
        if (animation == net.runelite.api.gameval.AnimationID.HUMAN_SPECIAL01_WEBWEAVER)
            consume("webweaver", "attack-animation", 4, null, true);
        if (animation == net.runelite.api.gameval.AnimationID.WILD_CAVE_CHAINMACE_CRUSH ||
            animation == net.runelite.api.gameval.AnimationID.WILD_CAVE_CHAINMACE_STAB)
        {
            consume("viggora", "attack-animation");
            consume("ursine", "attack-animation");
        }
        if (ChargeUsage.blowpipeAttack(animation))
            consume("blowpipe", "expected-consumption", 2.0 / 3.0, dartLossRate(), true);
        if (ChargeUsage.quiverAttack(animation) && eligibleQuiverWeapon())
            consume("quiver", "expected-consumption", 1.0 / 3.0, null, true);
    }

    private boolean eligibleQuiverWeapon()
    {
        int id = equippedId(net.runelite.api.EquipmentInventorySlot.WEAPON.getSlotIdx());
        ItemComposition item = id < 0 ? null : client.getItemDefinition(id);
        if (item == null || item.getName() == null)
            return false;
        String name = item.getName().toLowerCase(java.util.Locale.ROOT);
        // These weapons use their own charges or ammunition and never consume quiver charges.
        return (name.contains("bow") && !name.contains("venator") && !name.contains("crystal") &&
            !name.contains("faerdhinen") && !name.contains("craw") && !name.contains("webweaver") &&
            !name.contains("karil"));
    }

    private double dartLossRate()
    {
        int id = equippedId(net.runelite.api.EquipmentInventorySlot.CAPE.getSlotIdx());
        ItemComposition cape = id < 0 ? null : client.getItemDefinition(id);
        String name = cape == null || cape.getName() == null ? "" :
            cape.getName().toLowerCase(java.util.Locale.ROOT);
        if (name.contains("assembler") || name.contains("quiver") || name.contains("dizana") ||
            name.contains("ranging cape") || name.contains("ranging max cape"))
            return 0.2;
        if (name.contains("accumulator"))
            return 0.28;
        return name.contains("attractor") ? 0.4 : 1.0;
    }

    public void onCombatActivity()
    {
        if (ready())
            combatAt = client.getTickCount();
    }

    public void onReceivedDamage()
    {
        if (!ready())
            return;
        combatAt = client.getTickCount();
        ItemSnapshot.Charge recoil = charges.get("suffering");
        if (recoil != null && Boolean.TRUE.equals(recoil.effectEnabled))
            consume("suffering", "damage-event", 1, null, false);
        double rate = 1.0;
        int ringId = equippedId(net.runelite.api.EquipmentInventorySlot.RING.getSlotIdx());
        ItemComposition ring = ringId < 0 ? null : client.getItemDefinition(ringId);
        if (ring != null && ring.getName() != null &&
            (ring.getName().equalsIgnoreCase("Elven signet") ||
             ring.getName().equalsIgnoreCase("Celestial signet")))
            rate = 0.9;
        for (String family : new String[] {"crystal_helm", "crystal_body", "crystal_legs", "crystal_shield"})
            consume(family, "expected-consumption", rate, null, false);
    }

    public void onScalePickup(int x, int y)
    {
        if (!ready())
            return;
        pickupAt = client.getTickCount();
        pickupX = x;
        pickupY = y;
        pickupQuantity = 0;
        scalesBeforePickup = Math.max(0, inventoryCopies(ItemID.SNAKEBOSS_SCALE));
    }

    public void onScalesRemoved(int x, int y, int quantity)
    {
        if (x == pickupX && y == pickupY && quantity > 0 && quantity <= 200_000 &&
            (long) client.getTickCount() - pickupAt <= 5)
            pickupQuantity = quantity;
    }

    private void finishScalePickup()
    {
        long age = (long) client.getTickCount() - pickupAt;
        if (pickupQuantity == 0 || age < 1 || age > 5)
            return;
        net.runelite.api.coords.LocalPoint location = client.getLocalPlayer().getLocalLocation();
        if (location == null || location.getSceneX() != pickupX || location.getSceneY() != pickupY)
            return;
        int quantity = pickupQuantity;
        pickupQuantity = 0;
        int inventoryScales = inventoryCopies(ItemID.SNAKEBOSS_SCALE);
        if (inventoryScales < 0)
            return;
        quantity -= Math.max(0, inventoryScales - scalesBeforePickup);
        if (quantity <= 0)
            return; // The scales entered inventory, rather than a charged item.
        java.util.Set<String> recipients = new java.util.HashSet<>();
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory != null && inventory.getItems() != null)
            for (Item item : inventory.getItems())
            {
                if (item == null || item.getId() < 0)
                    continue;
                ItemComposition definition = client.getItemDefinition(item.getId());
                String family = definition == null ? null : ChargeChecks.family(definition.getName());
                if ("blowpipe".equals(family) || "serpentine".equals(family))
                    recipients.add(family);
            }
        for (String family : new String[] {"blowpipe", "serpentine"})
            if (equippedId(ChargeUsage.slot(family)) >= 0)
            {
                ItemComposition definition = client.getItemDefinition(equippedId(ChargeUsage.slot(family)));
                if (definition != null && family.equals(ChargeChecks.family(definition.getName())))
                    recipients.add(family);
            }
        if (recipients.size() == 1)
        {
            String family = recipients.iterator().next();
            ChargeUsage.Baseline baseline = usage.get(family);
            ItemSnapshot.Charge charge = charges.get(family);
            if (baseline != null && !baseline.invalid && charge != null)
            {
                putCharge(family, charge.adjust(quantity, true, Instant.now().toString()));
                return;
            }
        }
        for (String family : recipients)
            invalidate(family, "Scale pickup observed; check which item received it");
    }

    public void onTick()
    {
        if (!ready())
            return;
        refreshUsage();
        finishScalePickup();
        if (!usage.isEmpty() && client.getTickCount() % 50 == 0)
            changed = true; // A lightweight heartbeat lets readers recognise stale exported status.
        long elapsed = (long) client.getTickCount() - combatAt;
        if (elapsed >= 0 && elapsed <= 8)
            consume("serpentine", "expected-consumption", 10.0 / 90.0, null, true);
    }

    private void consume(String family, String method)
    {
        consume(family, method, 1, null, true);
    }

    private void consume(String family, String method, double rate, Double ammoRate, boolean oncePerTick)
    {
        if (family == null || !ready())
            return;
        refreshUsage();
        ChargeUsage.Baseline baseline = usage.get(family);
        if (baseline == null || baseline.invalid || baseline.away)
            return;
        int tick = client.getTickCount();
        if (oncePerTick && baseline.lastTick == tick)
            return;
        ItemSnapshot.Charge confirmed = charges.get(family);
        if (confirmed == null || confirmed.quantity == null || confirmed.permanent)
            return;
        baseline.lastTick = tick;
        baseline.chargeFraction += rate;
        if (baseline.pendingSavings > 0)
        {
            if ((long) tick - baseline.savingsAt <= 1)
                baseline.chargeFraction -= baseline.pendingSavings;
            baseline.pendingSavings = 0;
        }
        int spent = Math.max(0, (int) Math.floor(baseline.chargeFraction + 1e-9));
        baseline.chargeFraction -= spent;
        Integer ammoSpent = null;
        if (ammoRate != null)
        {
            baseline.ammoFraction += ammoRate;
            ammoSpent = (int) Math.floor(baseline.ammoFraction + 1e-9);
            baseline.ammoFraction -= ammoSpent;
        }
        ItemSnapshot.Charge tracked = confirmed.consume(Instant.now().toString(), method, spent, ammoSpent);
        putCharge(family, tracked);
    }

    public ItemSnapshot poll(boolean force)
    {
        if (!ready())
            return null;
        Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
        boolean open = bank != null && !bank.isHidden();
        boolean opened = open && !bankOpen;
        bankOpen = open;
        Widget vault = client.getWidget(InterfaceID.SeedVault.CONTENT);
        boolean vaultVisible = vault != null && !vault.isHidden();
        boolean vaultOpened = vaultVisible && !vaultOpen;
        vaultOpen = vaultVisible;
        refreshUsage();
        if (dirty || opened || vaultOpened || force)
        {
            String timestamp = Instant.now().toString();
            copy("inventory", InventoryID.INV, timestamp, false);
            copy("equipment", InventoryID.WORN, timestamp, false);
            // Unopened storage is unknown, not empty.
            if ((open || bankSeen && bankDirty) && copy("bank", InventoryID.BANK, timestamp, opened))
                bankSeen = true;
            if ((vaultVisible || vaultSeen && vaultDirty) &&
                copy("seedVault", InventoryID.SEED_VAULT, timestamp, vaultOpened))
                vaultSeen = true;
            bankDirty = false;
            vaultDirty = false;
            dirty = false;
        }
        if (!changed && !force)
            return null;
        changed = false;
        return new ItemSnapshot(
            account, Instant.now().toString(), bankSeen, containers, charges, chargeVariants);
    }

    private boolean copy(String name, int id, String timestamp, boolean fresh)
    {
        ItemContainer container = client.getItemContainer(id);
        if (container == null)
            return false;
        Map<Integer, Long> items = new LinkedHashMap<>();
        Map<Integer, String> names = new LinkedHashMap<>();
        long geValue = 0, alchemyValue = 0;
        int occupiedSlots = 0, placeholderSlots = 0;
        for (Item item : container.getItems())
        {
            if (item == null || item.getId() < 0)
                continue;
            ItemComposition definition = client.getItemDefinition(item.getId());
            if (definition == null)
                continue;
            if (definition.getPlaceholderTemplateId() != -1)
            {
                occupiedSlots++;
                placeholderSlots++;
                continue;
            }
            if (item.getQuantity() <= 0)
                continue;
            occupiedSlots++;
            // Match RuneLite BankPlugin.calculate, including mapped item variants.
            geValue += (long)itemManager.getItemPrice(item.getId()) * item.getQuantity();
            int alchemy = item.getId() == ItemID.COINS      ? 1
                          : item.getId() == ItemID.PLATINUM ? 1000
                                                            : definition.getHaPrice();
            alchemyValue += (long)alchemy * item.getQuantity();
            int canonical = definition.getNote() != -1 ? definition.getLinkedNoteId() : item.getId();
            if (canonical < 0)
                continue;
            items.merge(canonical, (long)item.getQuantity(), Long::sum);
            String itemName = definition.getName();
            if (itemName != null && !itemName.isEmpty() && itemName.length() <= 150)
                names.put(canonical, itemName);
            ChargeMessages.Result variant = ChargeItemVariants.read(itemName, timestamp);
            if (variant != null)
            {
                chargeVariants.put(variant.key + ":" + variant.variant, variant.charge);
                if (id == InventoryID.WORN)
                    putCharge(variant.key, variant.charge);
            }
            observePermanent(canonical, timestamp);
        }
        ItemSnapshot.Container previous = containers.get(name);
        if (fresh || previous == null || !previous.items.equals(items) || previous.geValue == null ||
            previous.placeholderSlots != placeholderSlots)
        {
            containers.put(name, new ItemSnapshot.Container(timestamp, items, names, geValue, alchemyValue,
                                     occupiedSlots, placeholderSlots));
            changed = true;
        }
        return true;
    }

    private void observePermanent(int id, String timestamp)
    {
        String key = id == ItemID.EMBERLIGHT         ? "emberlight"
                     : CORRUPTED_BOWS.contains(id)   ? "bowfa"
                     : CORRUPTED_BLADES.contains(id) ? "saeldor"
                     : id == ItemID.SKILLCAPE_MAX_DIZANAS || id == ItemID.SKILLCAPE_MAX_DIZANAS_TROUVER ||
                             id == ItemID.DIZANAS_QUIVER_INFINITE ||
                             id == ItemID.DIZANAS_QUIVER_INFINITE_TROUVER
                         ? "quiver"
                         : null;
        if (key != null && (!charges.containsKey(key) || !charges.get(key).permanent))
        {
            charges.put(key, new ItemSnapshot.Charge(null, 100.0, true, timestamp, "item-variant"));
            changed = true;
        }
    }
}
