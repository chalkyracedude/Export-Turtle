package com.exportturtle.snapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.StructComposition;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;

/** Fetches owned collection items through the collection interface's search scripts. */
public final class FullCollectionCapture
{
    // Collection Log cache/script identifiers used by the search-data fetch.
    // The search-data fetch is also used by https://github.com/weirdgloop/WikiSync.
    private static final int ITEM_SCRIPT = 4100;
    private static final int CLOSE_SEARCH_SCRIPT = 2240;
    private static final int GROUP_ENUM = 2102;
    private static final int REPLACEMENTS_ENUM = 3721;
    private static final int SECTIONS_PARAM = 683;
    private static final int LABEL_PARAM = 689;
    private static final int ITEMS_PARAM = 690;
    private static final int TIMEOUT_TICKS = 20;
    private static final int QUIET_TICKS = 2;

    private final Client client;
    private final Set<Integer> obtained = new HashSet<>();
    private final Map<String, List<Integer>> catalogue = new LinkedHashMap<>();
    private final Map<Integer, Integer> replacements = new LinkedHashMap<>();
    private String account;
    private int started;
    private int lastItem;
    private boolean wasOpen;

    public FullCollectionCapture(Client client)
    {
        this.client = client;
    }

    /** Trigger once per opening, never repeatedly while browsing collection sections. */
    public boolean syncWhenOpened()
    {
        var search = client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE);
        boolean open = search != null && !search.isHidden() && client.getLocalPlayer() != null &&
                       client.getTopLevelWorldView() != null && !client.getTopLevelWorldView().isInstance();
        boolean justOpened = open && !wasOpen;
        wasOpen = open;
        return justOpened && start();
    }

    public void resetSession()
    {
        wasOpen = false;
        reset();
    }

    public boolean start()
    {
        if (account != null || client.getLocalPlayer() == null || client.getTopLevelWorldView() == null ||
            client.getTopLevelWorldView().isInstance())
        {
            return false;
        }
        var search = client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE);
        if (search == null || search.isHidden())
        {
            return false;
        }
        reset();
        var replacementEnum = client.getEnum(REPLACEMENTS_ENUM);
        int[] oldIds = replacementEnum.getKeys();
        int[] newIds = replacementEnum.getIntVals();
        for (int i = 0; i < Math.min(oldIds.length, newIds.length); i++)
        {
            replacements.put(oldIds[i], newIds[i]);
        }
        for (int group : client.getEnum(GROUP_ENUM).getIntVals())
        {
            StructComposition top = client.getStructComposition(group);
            for (int section : client.getEnum(top.getIntValue(SECTIONS_PARAM)).getIntVals())
            {
                StructComposition entry = client.getStructComposition(section);
                List<Integer> ids = new ArrayList<>();
                for (int id : client.getEnum(entry.getIntValue(ITEMS_PARAM)).getIntVals())
                {
                    ids.add(CollectionItemIds.canonical(replacements.getOrDefault(id, id)));
                }
                catalogue.put(entry.getStringValue(LABEL_PARAM), ids);
            }
        }
        if (catalogue.isEmpty())
        {
            return false;
        }
        account = client.getLocalPlayer().getName();
        started = lastItem = client.getTickCount();
        // Addresses only the open collection interface; never sends gameplay actions.
        client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE, MenuAction.CC_OP, 1, -1, "Search", null);
        client.runScript(CLOSE_SEARCH_SCRIPT);
        return true;
    }

    public void onScript(ScriptPreFired event)
    {
        if (account == null || event.getScriptId() != ITEM_SCRIPT)
        {
            return;
        }
        Object[] args = event.getScriptEvent().getArguments();
        if (args == null || args.length < 2 || !(args[1] instanceof Integer))
        {
            return;
        }
        int id = (Integer)args[1];
        obtained.add(CollectionItemIds.canonical(replacements.getOrDefault(id, id)));
        lastItem = client.getTickCount();
    }

    /** Returns a completed batch once the client's item stream has settled. */
    public List<CollectionPage> finish()
    {
        if (account == null)
        {
            return null;
        }
        if (client.getLocalPlayer() == null || !account.equals(client.getLocalPlayer().getName()))
        {
            reset();
            return null;
        }
        int tick = client.getTickCount();
        if (tick - started > TIMEOUT_TICKS)
        {
            reset();
            return null;
        }
        // An empty response could be a failed request. Never overwrite ownership with it.
        if (obtained.isEmpty() || tick - lastItem < QUIET_TICKS)
        {
            return null;
        }
        List<CollectionPage> pages = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> section : catalogue.entrySet())
        {
            CollectionPage page = new CollectionPage();
            page.accountName = account;
            page.section = section.getKey();
            for (int id : section.getValue())
            {
                CollectionPage.Entry entry = new CollectionPage.Entry();
                entry.id = id;
                entry.name = client.getItemDefinition(id).getName();
                entry.obtained = obtained.contains(id);
                // The search reports ownership, not item quantities.
                entry.quantity = entry.obtained ? 1 : 0;
                page.items.add(entry);
            }
            pages.add(page);
        }
        reset();
        return pages;
    }

    public void reset()
    {
        account = null;
        obtained.clear();
        catalogue.clear();
        replacements.clear();
    }
}
