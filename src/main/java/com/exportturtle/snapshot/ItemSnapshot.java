package com.exportturtle.snapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable local ownership data; omitted containers are unknown, not empty. */
public final class ItemSnapshot
{
    final int schemaVersion = 1;
    final String accountName;
    final String capturedAt;
    final boolean bankObservedThisSession;
    final Map<String, Container> containers;
    final Map<String, Charge> charges;
    final Map<String, Charge> chargeVariants;
    final java.util.Set<String> chargeObservationsThisSession;

    ItemSnapshot(String accountName, String capturedAt, boolean bankSeen, Map<String, Container> containers,
        Map<String, Charge> charges)
    {
        this(accountName, capturedAt, bankSeen, containers, charges, Collections.emptyMap());
    }

    ItemSnapshot(String accountName, String capturedAt, boolean bankSeen, Map<String, Container> containers,
        Map<String, Charge> charges, Map<String, Charge> variants)
    {
        this.chargeVariants = Collections.unmodifiableMap(new LinkedHashMap<>(variants));
        this.chargeObservationsThisSession =
            Collections.unmodifiableSet(new java.util.LinkedHashSet<>(charges.keySet()));
        this.accountName = accountName;
        this.capturedAt = capturedAt;
        this.bankObservedThisSession = bankSeen;
        this.containers = Collections.unmodifiableMap(new LinkedHashMap<>(containers));
        this.charges = Collections.unmodifiableMap(new LinkedHashMap<>(charges));
    }

    public String bankObservationKey()
    {
        Container bank = containers.get("bank");
        return bankObservedThisSession && bank != null ? accountName + "|" + bank.observedAt : null;
    }

    public String accountName()
    {
        return accountName;
    }

    public String observationTime(String container)
    {
        Container observed = containers.get(container);
        return observed == null ? null : observed.observedAt;
    }

    static final class Container
    {
        final String observedAt;
        final Map<Integer, Long> items;
        final Map<Integer, String> names;
        final Long geValue;
        final Long alchemyValue;
        final int occupiedSlots;
        final int placeholderSlots;

        Container(String observedAt, Map<Integer, Long> items)
        {
            this(observedAt, items, Collections.emptyMap());
        }

        Container(String observedAt, Map<Integer, Long> items, Map<Integer, String> names)
        {
            this(observedAt, items, names, null, null, items.size(), 0);
        }

        Container(String observedAt, Map<Integer, Long> items, Map<Integer, String> names, Long geValue,
            Long alchemyValue, int occupiedSlots, int placeholderSlots)
        {
            this.geValue = geValue;
            this.alchemyValue = alchemyValue;
            this.occupiedSlots = occupiedSlots;
            this.placeholderSlots = placeholderSlots;
            this.observedAt = observedAt;
            this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
            this.names = Collections.unmodifiableMap(new LinkedHashMap<>(names));
        }
    }

    static final class Charge
    {
        final Integer quantity;
        final Double progressPercent;
        final boolean permanent;
        final String observedAt;
        final String source;
        final Double remainingPercent;
        final String ammoName;
        final Integer ammoQuantity;
        final Boolean effectEnabled;
        final Integer trackedQuantity;
        final Integer trackedAmmoQuantity;
        final String trackedAt;
        final String trackingMethod;
        final Integer trackedAddedQuantity;
        final String trackingState;
        final String trackingReason;
        final String trackingStatusAt;

        Charge(Integer quantity, Double progressPercent, boolean permanent, String observedAt, String source)
        {
            this(quantity, progressPercent, permanent, observedAt, source, null, null, null, null);
        }

        Charge(Integer quantity, Double progressPercent, boolean permanent, String observedAt, String source,
            Double remainingPercent, String ammoName, Integer ammoQuantity, Boolean effectEnabled)
        {
            this.quantity = quantity;
            this.progressPercent = progressPercent;
            this.permanent = permanent;
            this.observedAt = observedAt;
            this.source = source;
            this.remainingPercent = remainingPercent;
            this.ammoName = ammoName;
            this.ammoQuantity = ammoQuantity;
            this.effectEnabled = effectEnabled;
            trackedQuantity = null;
            trackedAmmoQuantity = null;
            trackedAt = null;
            trackingMethod = null;
            trackedAddedQuantity = null;
            trackingState = null;
            trackingReason = null;
            trackingStatusAt = null;
        }

        private Charge(Charge previous, Integer remaining, Integer ammo, String timestamp, String method,
            Integer added, String state, String reason, String statusAt)
        {
            quantity = previous.quantity;
            progressPercent = previous.progressPercent;
            permanent = previous.permanent;
            observedAt = previous.observedAt;
            source = previous.source;
            remainingPercent = previous.remainingPercent;
            ammoName = previous.ammoName;
            ammoQuantity = previous.ammoQuantity;
            effectEnabled = previous.effectEnabled;
            trackedQuantity = remaining;
            trackedAmmoQuantity = ammo;
            trackedAt = timestamp;
            trackingMethod = method;
            trackedAddedQuantity = added;
            trackingState = state;
            trackingReason = reason;
            trackingStatusAt = statusAt;
        }

        Charge withEffect(boolean enabled)
        {
            Charge updated = new Charge(quantity, progressPercent, permanent, observedAt, source,
                remainingPercent, ammoName, ammoQuantity, enabled);
            return new Charge(updated, trackedQuantity, trackedAmmoQuantity, trackedAt, trackingMethod,
                trackedAddedQuantity, trackingState, trackingReason, trackingStatusAt);
        }

        Charge withStatus(String state, String reason, String timestamp)
        {
            return new Charge(this, trackedQuantity, trackedAmmoQuantity, trackedAt, trackingMethod,
                trackedAddedQuantity, state, reason, timestamp);
        }

        Charge adjust(int delta, boolean refill, String timestamp)
        {
            if (quantity == null || permanent)
                return this;
            int added = (trackedAddedQuantity == null ? 0 : trackedAddedQuantity) + (refill ? delta : 0);
            added = Math.max(0, Math.min(200_000, added));
            int remaining = trackedQuantity == null ? quantity : trackedQuantity;
            remaining = Math.max(0, Math.min(Math.min(200_000, quantity + added), remaining + delta));
            return new Charge(this, remaining, trackedAmmoQuantity, timestamp,
                refill ? "refill-message" : "charge-saved-message", added,
                trackingState, trackingReason, trackingStatusAt);
        }

        Charge consume(String timestamp, String method, int charges, Integer darts)
        {
            int remaining = trackedQuantity == null ? quantity : trackedQuantity;
            Integer ammo = trackedAmmoQuantity == null ? ammoQuantity : trackedAmmoQuantity;
            Integer trackedAmmo = darts == null || ammo == null ? null : Math.max(0, ammo - darts);
            return new Charge(this, Math.max(0, remaining - charges), trackedAmmo, timestamp, method,
                trackedAddedQuantity, trackingState, trackingReason, trackingStatusAt);
        }
    }
}
