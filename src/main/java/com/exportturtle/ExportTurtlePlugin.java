package com.exportturtle;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.exportturtle.selfie.SelfiePanel;
import com.exportturtle.snapshot.AccountSnapshot;
import com.exportturtle.snapshot.CollectionPage;
import com.exportturtle.snapshot.CollectionStore;
import com.exportturtle.snapshot.FullCollectionCapture;
import com.exportturtle.snapshot.ItemCapture;
import com.exportturtle.snapshot.ItemSnapshot;
import com.exportturtle.snapshot.ItemStore;
import com.exportturtle.snapshot.RecentEventsStore;
import com.exportturtle.snapshot.SnapshotCapture;
import com.exportturtle.snapshot.SnapshotStore;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(name = "Export Turtle", internalName = "export-turtle",
    description = "Export account data, character portraits and saved poses to local files",
    tags = {"turtle", "export", "json", "portrait", "selfie"})
public class ExportTurtlePlugin extends Plugin
{
    private static final DateTimeFormatter SAVED_TIME =
        DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);
    private final Map<net.runelite.api.Skill, Integer> eventLevels =
        new java.util.EnumMap<>(net.runelite.api.Skill.class);
    private final Map<net.runelite.api.Skill, Integer> eventXp =
        new java.util.EnumMap<>(net.runelite.api.Skill.class);
    private static final String COLLECTION_UNLOCK_PREFIX = "New item added to your collection log: ";

    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private ExportTurtleConfig config;
    @Inject private SnapshotCapture capture;
    @Inject private Gson gson;
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    private volatile long exportGeneration;
    private SelfiePanel selfie;
    private FullCollectionCapture fullCollection;
    private ItemCapture items;
    private int itemTicks;
    private ItemSnapshot pendingItems;
    private AtomicBoolean itemWriting;
    private volatile boolean retryItems;
    @Inject private ItemManager itemManager;
    private volatile String bankNotice;

    private AtomicBoolean writing;
    private volatile boolean requested;
    private volatile boolean running;
    private int ticks;
    private String eventSession = java.util.UUID.randomUUID().toString();
    private String eventAccount;
    private final com.exportturtle.snapshot.PlunderSceptre plunderSceptre =
        new com.exportturtle.snapshot.PlunderSceptre();
    private net.runelite.api.coords.WorldPoint plunderGround;
    private int plunderGroundTick;
    private ExecutorService writer;
    private volatile ExportPanel panel;
    private volatile String visibleAccount;
    private NavigationButton navigation;
    @Inject private net.runelite.client.input.MouseManager mouseManager;
    private com.exportturtle.ui.TurtleCursor turtleCursor;

    @Provides ExportTurtleConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(ExportTurtleConfig.class);
    }

    @Override protected void startUp() throws IOException
    {
        eventSession = java.util.UUID.randomUUID().toString();
        eventAccount = null;
        plunderSceptre.reset();
        plunderGround = null;
        writing = new AtomicBoolean();
        running = true;
        requested = true;
        ticks = 0;
        writer = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "exportturtle-snapshot-writer");
            thread.setDaemon(true);
            return thread;
        });
        fullCollection = new FullCollectionCapture(client);
        items = new ItemCapture(client, itemManager);
        itemTicks = 0;
        pendingItems = null;
        itemWriting = new AtomicBoolean();
        retryItems = false;
        bankNotice = null;
        selfie = new SelfiePanel(client, clientThread, getPluginDirectory().join("selfies"));
        selfie.enablePoseStorage(gson);
        selfie.setExportAllowed(() -> running && config.localSnapshots() && config.portraitExports());
        panel = new ExportPanel(() -> requested = true, selfie, this::copyExportFolder, this::copyExportFile);
        panel.setExportEnabled(config.localSnapshots());
        panel.setCoverage(config);
        turtleCursor = new com.exportturtle.ui.TurtleCursor(client.getCanvas(), mouseManager);
        panel.onTurtleCursorChanged(value -> configManager.setConfiguration(
            ExportTurtleConfig.GROUP, "turtleCursor", value));
        SwingUtilities.invokeLater(this::updateTurtleCursor);
        navigation = NavigationButton.builder()
                         .tooltip("Export Turtle")
                         .icon(ImageUtil.loadImageResource(getClass(), "turtle.png"))
                         .priority(7)
                         .panel(panel)
                         .build();
        NavigationButton added = navigation;
        SwingUtilities.invokeLater(() -> clientToolbar.addNavigation(added));
    }

    private void copyExportFile(ExportFile file)
    {
        final String account = visibleAccount;
        final long generation = exportGeneration;
        if (account == null || file == null || !running)
            return;
        try
        {
            writer.execute(() -> {
                if (!running || generation != exportGeneration)
                    return;
                try
                {
                    net.runelite.client.util.Filepath path = file.forAccount(getPluginDirectory(), account);
                    if (path.isFile())
                        copyExportPath(path, generation);
                    else
                        fileMessage("No saved " + file.toString().toLowerCase(Locale.ROOT) + " file for this account yet.", false);
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not copy export path", error);
                    fileMessage("Could not copy export path.", true);
                }
            });
        }
        catch (RejectedExecutionException error)
        {
            log.debug("File request ignored while stopping", error);
        }
    }

    private void fileMessage(String message, boolean failure)
    {
        ExportPanel recipient = panel;
        long generation = exportGeneration;
        SwingUtilities.invokeLater(() -> {
            if (running && recipient == panel && generation == exportGeneration)
                recipient.showFileMessage(message, failure);
        });
    }

    private void updateAccountName()
    {
        String name = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null &&
            SnapshotCapture.isMainGame(client) ? client.getLocalPlayer().getName() : null;
        if (java.util.Objects.equals(name, visibleAccount))
            return;
        visibleAccount = name;
        ExportPanel recipient = panel;
        long generation = exportGeneration;
        SwingUtilities.invokeLater(() -> {
            if (running && recipient == panel && generation == exportGeneration)
                recipient.setAccount(name);
        });
    }

    private void copyExportPath(net.runelite.client.util.Filepath path, long generation)
    {
        ExportPanel recipient = panel;
        SwingUtilities.invokeLater(() -> {
            if (!running || generation != exportGeneration || recipient != panel)
                return;
            try
            {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path.toString()), null);
                recipient.showFileMessage("Export path copied.", false);
            }
            catch (RuntimeException error)
            {
                log.debug("Could not copy export path", error);
                recipient.showFileMessage("Could not copy export path. Try again.", true);
            }
        });
    }

    private void copyExportFolder()
    {
        final long generation = exportGeneration;
        try
        {
            writer.execute(() -> {
                if (!running || generation != exportGeneration)
                    return;
                try
                {
                    net.runelite.client.util.Filepath directory = getPluginDirectory();
                    directory.createDirectories();
                    copyExportPath(directory, generation);
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not copy export folder path", error);
                    fileMessage("Could not copy export folder path.", true);
                }
            });
        }
        catch (RejectedExecutionException error)
        {
            log.debug("Export folder request ignored while stopping", error);
        }
    }

    @Subscribe public void onStatChanged(net.runelite.api.events.StatChanged event)
    {
        if (!running || !config.localSnapshots() || !config.eventHistory() ||
            client.getGameState() != GameState.LOGGED_IN || !SnapshotCapture.isMainGame(client))
            return;
        int level = client.getRealSkillLevel(event.getSkill());
        Integer beforeLevel = eventLevels.put(event.getSkill(), level);
        Integer beforeXp = eventXp.put(event.getSkill(), event.getXp());
        if (beforeLevel == null || level != beforeLevel || beforeXp == null ||
            event.getXp() / 1000000 > beforeXp / 1000000)
            requested = true;
    }

    @Subscribe public void onVarbitChanged(net.runelite.api.events.VarbitChanged event)
    {
        if (running && config.localSnapshots() &&
            event.getVarpId() == net.runelite.api.gameval.VarPlayerID.QP)
            requested = true;
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
            requested = true;
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            plunderSceptre.reset();
            plunderGround = null;
            final String account = eventAccount, session = eventSession;
            eventAccount = null;
            eventLevels.clear();
            eventXp.clear();
            eventSession = java.util.UUID.randomUUID().toString();
            if (account != null && writer != null && !writer.isShutdown())
                try
                {
                    writer.execute(() -> {
                        try
                        {
                            if (running && config.localSnapshots() && config.eventHistory())
                                RecentEventsStore.finishSession(
                                    getPluginDirectory().join("events"), account, session, gson);
                        }
                        catch (IOException | RuntimeException error)
                        {
                            log.debug("Could not finalize event session", error);
                        }
                    });
                }
                catch (RejectedExecutionException error)
                {
                    log.debug("Event writer stopped", error);
                }
        }
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING ||
            event.getGameState() == GameState.LOGGING_IN)
        {
            exportGeneration++;
            visibleAccount = null;
            if (fullCollection != null)
            {
                fullCollection.resetSession();
            }
            if (items != null)
                items.reset();
            pendingItems = null;
            SelfiePanel current = selfie;
            SwingUtilities.invokeLater(() -> {
                if (current != null)
                    current.clear();
                if (panel != null)
                    panel.resetSyncStatus(config);
            });
        }
    }

    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if (ExportTurtleConfig.GROUP.equals(event.getGroup()) && "turtleCursor".equals(event.getKey()))
        {
            SwingUtilities.invokeLater(this::updateTurtleCursor);
            return;
        }
        if (ExportTurtleConfig.GROUP.equals(event.getGroup()))
        {
            exportGeneration++;
            requested = true;
            clientThread.invoke(() -> {
                if (running && fullCollection != null)
                {
                    fullCollection.resetSession();
                }
                if (items != null)
                    items.reset();
                pendingItems = null;
                itemTicks = 0;
            });
            ExportPanel current = panel;
            SwingUtilities.invokeLater(() -> {
                if (running && current == panel)
                {
                    current.setExportEnabled(config.localSnapshots());
                    current.setCoverage(config);
                    current.setStatus(config.localSnapshots() ? "Waiting for your next game tick."
                                                              : "Local snapshots disabled.");
                }
            });
        }
    }

    @Subscribe public void onGameTick(GameTick event)
    {
        if (!running)
            return;
        updateAccountName();
        RecentEventsStore.setRetentionDays(config.eventRetentionDays());
        syncPlunderSceptre();
        syncCollectionLog();
        if (canTrackCharges())
            items.onTick();
        syncItems();

        if (!running || !config.localSnapshots() || writing.get())
        {
            return;
        }
        if (!requested && ++ticks < Math.max(8, config.syncSeconds() * 5 / 3))
        {
            return;
        }
        if (requested && config.collectionExports() && fullCollection != null)
        {
            try
            {
                fullCollection.start();
            }
            catch (RuntimeException error)
            {
                log.debug("Could not request full collection log", error);
                fullCollection.reset();
            }
        }
        requested = false;
        ticks = 0;
        final AccountSnapshot snapshot;
        try
        {
            snapshot = capture.capture(config.clanExports());
        }
        catch (IllegalStateException exception)
        {
            setStatus(exception.getMessage());
            return;
        }
        catch (RuntimeException exception)
        {
            log.debug("Game data is not ready for a snapshot", exception);
            setStatus("Game data is not ready yet. Try exporting again shortly.");
            return;
        }
        final String capturedSession = eventSession;
        eventAccount = snapshot.getAccountName();
        AtomicBoolean inFlight = writing;
        inFlight.set(true);
        ExportPanel recipient = panel;
        try
        {
            final long generation = exportGeneration;
            writer.execute(() -> {
                try
                {
                    if (generation != exportGeneration) return;
                    if (running && recipient == panel && config.localSnapshots())
                    {
                        SnapshotStore.write(getPluginDirectory().join("snapshots"), snapshot, gson);
                        if (config.eventHistory())
                            RecentEventsStore.record(
                                getPluginDirectory().join("events"), snapshot, capturedSession, gson);
                        if (config.diagnostics())
                        {
                            boolean diagnosticSaved = com.exportturtle.snapshot.ExportDiagnostics.record(
                                getPluginDirectory().join("diagnostics"), snapshot.getAccountName(),
                                "snapshot", true, gson);
                            SwingUtilities.invokeLater(() -> {
                                if (generation == exportGeneration && running && recipient == panel)
                                    recipient.setFeatureStatus("Diagnostics",
                                        diagnosticSaved ? "Synced" : "Failed", !diagnosticSaved);
                            });
                        }
                        SwingUtilities.invokeLater(() -> {
                            if (generation != exportGeneration || !running || recipient != panel)
                                return;
                            if (config.eventHistory())
                                recipient.setFeatureStatus("Events", "Synced", false);
                            if (config.clanExports())
                                recipient.setFeatureStatus(
                                    "Clan", snapshot.hasClan() ? "Synced" : "Not in a clan", false);
                        });
                        setStatus(recipient, generation, "Saved at " + LocalDateTime.now().format(SAVED_TIME));
                    }
                }
                catch (IOException | RuntimeException exception)
                {
                    try
                    {
                        if (config.diagnostics())
                            com.exportturtle.snapshot.ExportDiagnostics.record(
                                getPluginDirectory().join("diagnostics"), snapshot.getAccountName(),
                                "snapshot", false, gson);
                    }
                    catch (IOException diagnosticError)
                    {
                        log.debug("Could not save export diagnostics", diagnosticError);
                    }
                    log.debug("Could not write account snapshot", exception);
                    setStatus(recipient, generation, "Could not save snapshot. Check the RuneLite log.");
                }
                finally
                {
                    inFlight.set(false);
                }
            });
        }
        catch (RejectedExecutionException exception)
        {
            inFlight.set(false);
        }
    }

    @Subscribe public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (running && config.localSnapshots() && items != null)
            items.onContainerChanged(event.getContainerId());
    }

    private void syncItems()
    {
        if (!running || !config.localSnapshots() || !config.itemExports() || items == null)
            return;
        try
        {
            ItemSnapshot observed = items.poll(requested || retryItems);
            retryItems = false;
            if (observed != null)
                pendingItems = observed;
            if (pendingItems == null || (!requested && ++itemTicks < 5) || itemWriting.get())
                return;
            ItemSnapshot snapshot = pendingItems;
            pendingItems = null;
            itemTicks = 0;
            ExportPanel recipient = panel;
            AtomicBoolean inFlight = itemWriting;
            inFlight.set(true);
            final long generation = exportGeneration;
            writer.execute(() -> {
                try
                {
                    if (generation != exportGeneration) return;
                    if (running && config.localSnapshots() && config.itemExports() && recipient == panel)
                    {
                        String summary = ItemStore.write(getPluginDirectory().join("items"), snapshot, gson);
                        recordHealth(snapshot.accountName(), "items", true, generation);
                        String observation = snapshot.bankObservationKey();
                        boolean bankUpdated = observation != null && !observation.equals(bankNotice);
                        if (bankUpdated)
                            bankNotice = observation;
                        SwingUtilities.invokeLater(() -> {
                            if (generation == exportGeneration && running && recipient == panel)
                            {
                                recipient.setItemsStatus(summary, bankUpdated);
                                recipient.setObservation("Bank", snapshot.observationTime("bank"));
                                recipient.setObservation("Seed vault", snapshot.observationTime("seedVault"));
                            }
                        });
                    }
                }
                catch (IOException | RuntimeException error)
                {
                    if (generation == exportGeneration && running && recipient == panel)
                        retryItems = true;
                    recordHealth(snapshot.accountName(), "items", false, generation);
                    log.debug("Could not save bank and charges", error);
                    setStatus(recipient, generation, "Could not save item data. Check the RuneLite log.");
                }
                finally
                {
                    inFlight.set(false);
                }
            });
        }
        catch (RuntimeException error)
        {
            itemWriting.set(false);
            log.debug("Item data is not ready", error);
        }
    }

    // Ensure RuneLite's core server-loot event producer is active even with its Loot Tracker panel disabled.
    @Inject private net.runelite.client.game.LootManager lootManager;

    @Subscribe public void onServerNpcLoot(net.runelite.client.events.ServerNpcLoot event)
    {
        if (!running || !config.localSnapshots() || !config.eventHistory() ||
            client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null ||
            !SnapshotCapture.isMainGame(client))
            return;
        if (event.getComposition() != null)
            queueValuableDrops(event.getComposition().getName(), event.getItems());
        String boss = com.exportturtle.snapshot.EncounterKills.encounter(event);
        if (boss == null)
            return;
        String account = client.getLocalPlayer().getName();
        eventAccount = account;
        String session = eventSession;
        String timestamp = Instant.now().toString();
        ExportPanel recipient = panel;
        try
        {
            final long generation = exportGeneration;
            writer.execute(() -> {
                if (generation != exportGeneration || !running || !config.localSnapshots()) return;
                if (!running || !config.localSnapshots() || !config.eventHistory() || recipient != panel)
                    return;
                try
                {
                    RecentEventsStore.recordEncounterKill(
                        getPluginDirectory().join("events"), account, boss, timestamp, session, gson);
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not record encounter kill", error);
                }
            });
        }
        catch (RejectedExecutionException error)
        {
            log.debug("Encounter writer stopped", error);
        }
    }

    @Subscribe public void onLootReceived(net.runelite.client.plugins.loottracker.LootReceived event)
    {
        if (com.exportturtle.snapshot.ValuableDrops.acceptsAdditionalLoot(event.getType()))
            queueValuableDrops(event.getName(), event.getItems());
    }

    private void queueValuableDrops(
        String rawSource, java.util.Collection<net.runelite.client.game.ItemStack> items)
    {
        queueValuableDrops(rawSource, items, false);
    }

    private void queueValuableDrops(String rawSource,
        java.util.Collection<net.runelite.client.game.ItemStack> items, boolean plunderReward)
    {
        if (!running || !config.localSnapshots() || !config.eventHistory() ||
            client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null ||
            !SnapshotCapture.isMainGame(client))
            return;
        java.util.Collection<net.runelite.client.game.ItemStack> capturedItems = items;
        if (!plunderReward && items != null)
            capturedItems =
                items.stream()
                    .filter(item
                        -> item != null && !com.exportturtle.snapshot.PlunderSceptre.isSceptre(item.getId()))
                    .collect(java.util.stream.Collectors.toList());
        List<com.exportturtle.snapshot.ValuableDrops.Drop> drops =
            com.exportturtle.snapshot.ValuableDrops.capture(capturedItems, itemManager);
        if (drops.isEmpty())
            return;
        String source = rawSource == null ? "Unknown source" : Text.removeTags(rawSource);
        String capturedSource = source.substring(0, Math.min(160, source.length()));
        String account = client.getLocalPlayer().getName();
        String timestamp = Instant.now().toString();
        String lootId = java.util.UUID.randomUUID().toString();
        ExportPanel recipient = panel;
        try
        {
            final long generation = exportGeneration;
            writer.execute(() -> {
                if (generation != exportGeneration || !running || !config.localSnapshots()) return;
                if (!running || !config.localSnapshots() || !config.eventHistory() || recipient != panel)
                    return;
                try
                {
                    RecentEventsStore.recordValuableDrops(getPluginDirectory().join("events"), account,
                        lootId, capturedSource, drops, timestamp, gson);
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not record valuable drops", error);
                }
            });
        }
        catch (RejectedExecutionException error)
        {
            log.debug("Valuable drop writer stopped", error);
        }
    }

    private boolean canCaptureLoot()
    {
        return running && config.localSnapshots() && config.eventHistory() &&
            client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null &&
            SnapshotCapture.isMainGame(client);
    }

    private int heldSceptres()
    {
        int total = 0;
        for (int inventory :
            new int[] {net.runelite.api.gameval.InventoryID.INV, net.runelite.api.gameval.InventoryID.WORN})
        {
            net.runelite.api.ItemContainer container = client.getItemContainer(inventory);
            if (container != null)
                for (net.runelite.api.Item item : container.getItems())
                    if (com.exportturtle.snapshot.PlunderSceptre.isSceptre(item.getId()))
                        total += item.getQuantity();
        }
        return total;
    }

    private boolean canTrackCharges()
    {
        return running && config.localSnapshots() && config.itemExports() && items != null;
    }

    @Subscribe public void onGraphicChanged(net.runelite.api.events.GraphicChanged event)
    {
        if (!canTrackCharges() || event.getActor() == null || event.getActor() != client.getLocalPlayer())
            return;
        for (net.runelite.api.ActorSpotAnim graphic : event.getActor().getSpotAnims())
            items.onGraphic(graphic.getId(), graphic.getStartCycle());
    }

    @Subscribe public void onAnimationChanged(net.runelite.api.events.AnimationChanged event)
    {
        if (canTrackCharges() && event.getActor() != null && event.getActor() == client.getLocalPlayer())
            items.onAnimation(event.getActor().getAnimation());
    }

    @Subscribe public void onHitsplatApplied(net.runelite.api.events.HitsplatApplied event)
    {
        net.runelite.api.Player player = client.getLocalPlayer();
        net.runelite.api.Hitsplat hit = event.getHitsplat();
        if (!canTrackCharges() || player == null || hit == null)
            return;
        int type = hit.getHitsplatType();
        if (event.getActor() == player && type == net.runelite.api.HitsplatID.BLOCK_ME)
            items.onCombatActivity();
        if (java.util.Set.of(net.runelite.api.HitsplatID.DAMAGE_ME,
                net.runelite.api.HitsplatID.DAMAGE_ME_CYAN, net.runelite.api.HitsplatID.DAMAGE_ME_ORANGE,
                net.runelite.api.HitsplatID.DAMAGE_ME_YELLOW, net.runelite.api.HitsplatID.DAMAGE_ME_WHITE,
                net.runelite.api.HitsplatID.DAMAGE_MAX_ME, net.runelite.api.HitsplatID.DAMAGE_MAX_ME_CYAN,
                net.runelite.api.HitsplatID.DAMAGE_MAX_ME_ORANGE, net.runelite.api.HitsplatID.DAMAGE_MAX_ME_YELLOW,
                net.runelite.api.HitsplatID.DAMAGE_MAX_ME_WHITE).contains(type))
        {
            if (event.getActor() == player)
            {
                if (hit.getAmount() > 0)
                    items.onReceivedDamage();
            }
            else if (hit.isMine())
                items.onMeleeHit(player.getAnimation(), hit.getAmount());
        }
    }

    @Subscribe public void onItemDespawned(net.runelite.api.events.ItemDespawned event)
    {
        if (canTrackCharges() && event.getItem().getId() == net.runelite.api.gameval.ItemID.SNAKEBOSS_SCALE)
        {
            net.runelite.api.Point location = event.getTile().getSceneLocation();
            items.onScalesRemoved(location.getX(), location.getY(), event.getItem().getQuantity());
        }
    }

    @Subscribe public void onMenuOptionClicked(net.runelite.api.events.MenuOptionClicked event)
    {
        if (canTrackCharges())
        {
            items.onItemAction(event.getItemId(), event.getMenuOption());
            if (event.getId() == net.runelite.api.gameval.ItemID.SNAKEBOSS_SCALE &&
                Text.removeTags(event.getMenuOption()).equalsIgnoreCase("Take"))
                items.onScalePickup(event.getParam0(), event.getParam1());
        }
        if (!canCaptureLoot())
        {
            plunderSceptre.reset();
            return;
        }
        String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
        int id = event.getId();
        boolean plunderObject = id == net.runelite.api.gameval.ObjectID.NTK_GOLDEN_CHEST_MULTI_1 ||
                                id == net.runelite.api.gameval.ObjectID.NTK_SARCOPHAGUS_MULTI_1 ||
                                id == net.runelite.api.gameval.ObjectID.NTK_SARCOPHAGUS_DECAY_MULTI ||
                                id == net.runelite.api.gameval.ObjectID.NTK_GOLDEN_CHEST_MULTI ||
                                id == net.runelite.api.gameval.ObjectID.NTK_GOLDEN_CHEST_CLOSED ||
                                id == net.runelite.api.gameval.ObjectID.NTK_SARCOPHAGUS_MULTI ||
                                id == net.runelite.api.gameval.ObjectID.NTK_SARCOPHAGUS;
        if (plunderObject &&
            client.getLocalPlayer().getWorldLocation().getRegionID() ==
                7749 /* Pyramid Plunder; no gameval region constant */
            && client.getVarbitValue(net.runelite.api.gameval.VarbitID.NTK_PLAYER_TIMER_COUNT) > 0 &&
            (option.equals("search") || option.equals("open")))
        {
            plunderGround = null;
            plunderSceptre.arm(client.getTickCount(), heldSceptres());
        }
        else if (!option.equals("walk here") && !option.equals("examine"))
            plunderSceptre.reset();
    }

    private void syncPlunderSceptre()
    {
        if (!canCaptureLoot())
            return;
        if (!plunderSceptre.active(client.getTickCount()))
        {
            plunderGround = null;
            return;
        }
        if (plunderGround != null && client.getTickCount() - plunderGroundTick <= 15 &&
            plunderSceptre.ground(client.getTickCount(), net.runelite.api.TileItem.OWNERSHIP_SELF,
                plunderGround.equals(client.getLocalPlayer().getWorldLocation())))
        {
            plunderGround = null;
            recordPlunderSceptre();
        }
        if (plunderGround != null && client.getTickCount() - plunderGroundTick > 15)
            plunderGround = null;
        if (plunderSceptre.inventory(client.getTickCount(), heldSceptres()))
            recordPlunderSceptre();
    }

    @Subscribe public void onItemSpawned(net.runelite.api.events.ItemSpawned event)
    {
        // Loading can deliver the ground spawn before the ejected player's position updates.
        if (!running || !config.localSnapshots() || !SnapshotCapture.isMainGame(client) ||
            !com.exportturtle.snapshot.PlunderSceptre.isSceptre(event.getItem().getId()) ||
            event.getItem().getOwnership() != net.runelite.api.TileItem.OWNERSHIP_SELF)
            return;
        plunderGround = event.getTile().getWorldLocation();
        plunderGroundTick = client.getTickCount();
    }

    private void recordPlunderSceptre()
    {
        queueValuableDrops("Pyramid Plunder",
            java.util.Collections.singletonList(
                new net.runelite.client.game.ItemStack(net.runelite.api.gameval.ItemID.PHARAOHS_SCEPTRE, 1)),
            true);
    }

    private void syncCollectionLog()
    {
        if (!running || !config.localSnapshots() || !config.collectionExports() || fullCollection == null)
        {
            return;
        }
        try
        {
            if (fullCollection.syncWhenOpened())
            {
                setStatus("Syncing Collection Log...");
            }
            List<CollectionPage> pages = fullCollection.finish();
            if (pages == null)
            {
                return;
            }
            ExportPanel recipient = panel;
            final long generation = exportGeneration;
            writer.execute(() -> {
                if (generation != exportGeneration || !running || !config.localSnapshots()) return;
                if (!running || !config.localSnapshots() || !config.collectionExports() || recipient != panel)
                {
                    return;
                }
                try
                {
                    CollectionStore.writeAll(getPluginDirectory().join("collections"), pages, gson);
                    recordHealth(pages.isEmpty() ? null : pages.get(0).accountName, "collection", true, generation);
                    setStatus(recipient, generation, "Collection Log synced.");
                }
                catch (IOException | RuntimeException error)
                {
                    recordHealth(pages.isEmpty() ? null : pages.get(0).accountName, "collection", false, generation);
                    log.debug("Could not save full collection log", error);
                    setStatus(recipient, generation, "Could not sync Collection Log.");
                }
            });
        }
        catch (RejectedExecutionException error)
        {
            log.debug("Collection writer stopped", error);
        }
        catch (RuntimeException error)
        {
            log.debug("Collection fetch unavailable", error);
            fullCollection.reset();
        }
    }

    @Subscribe public void onScriptPreFired(ScriptPreFired event)
    {
        if (running && config.localSnapshots() && config.collectionExports() && fullCollection != null)
        {
            fullCollection.onScript(event);
        }
    }

    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (!running || !config.localSnapshots() ||
            (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) ||
            client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
        {
            return;
        }
        String message = Text.removeTags(event.getMessage());
        if (config.itemExports() && items != null)
            items.onMessage(message);
        if (!config.eventHistory())
            return;
        if (SnapshotCapture.isMainGame(client))
        {
            String[] kill = com.exportturtle.snapshot.BossMessages.parse(message);
            if (kill != null)
            {
                final String account = client.getLocalPlayer().getName(), boss = kill[0];
                final String session = eventSession;
                final String timestamp = Instant.now().toString();
                try
                {
                    final int count = Integer.parseInt(kill[1]);
                    final long generation = exportGeneration;
                    writer.execute(() -> {
                        if (generation != exportGeneration || !running || !config.localSnapshots()) return;
                        try
                        {
                            if (!config.eventHistory()) return;
                            String canonical = com.exportturtle.snapshot.BossMessages.canonical(boss, gson);
                            if (!com.exportturtle.snapshot.BossMessages.isTrackedEncounter(canonical))
                                RecentEventsStore.recordBossCount(getPluginDirectory().join("events"),
                                    account, boss, count, timestamp, session, gson);
                        }
                        catch (IOException | RuntimeException error)
                        {
                            log.debug("Could not record boss milestone", error);
                        }
                    });
                }
                catch (IllegalArgumentException | RejectedExecutionException error)
                {
                    log.debug("Could not queue boss milestone", error);
                }
            }
        }
        if (!message.startsWith(COLLECTION_UNLOCK_PREFIX))
        {
            return;
        }
        String item = message.substring(COLLECTION_UNLOCK_PREFIX.length()).trim();
        if (item.isEmpty() || item.length() > 120)
        {
            return;
        }
        String account = client.getLocalPlayer().getName();
        String timestamp = Instant.now().toString();
        String collectionSession = eventSession;
        ExportPanel recipient = panel;
        try
        {
            final long generation = exportGeneration;
            writer.execute(() -> {
                if (generation != exportGeneration || !running || !config.localSnapshots()) return;
                if (!running || !config.localSnapshots() || !config.eventHistory() || recipient != panel)
                {
                    return;
                }
                try
                {
                    RecentEventsStore.recordCollection(getPluginDirectory().join("events"), account, item,
                        timestamp, collectionSession, gson);
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not record collection unlock", error);
                }
            });
        }
        catch (RejectedExecutionException error)
        {
            log.debug("Event writer stopped", error);
        }
    }

    private void recordHealth(String account, String channel, boolean success, long generation)
    {
        if (account == null || !running || generation != exportGeneration || !config.localSnapshots() || !config.diagnostics())
            return;
        try
        {
            boolean saved = com.exportturtle.snapshot.ExportDiagnostics.record(
                getPluginDirectory().join("diagnostics"), account, channel, success, gson);
            ExportPanel recipient = panel;
            SwingUtilities.invokeLater(() -> {
                if (running && recipient == panel && generation == exportGeneration)
                    recipient.setFeatureStatus("Diagnostics", saved ? "Synced" : "Failed", !saved);
            });
        }
        catch (IOException error)
        {
            log.debug("Could not write export health", error);
        }
    }

    private void setStatus(String message)
    {
        setStatus(panel, message);
    }

    private void setStatus(ExportPanel recipient, String message)
    {
        setStatus(recipient, exportGeneration, message);
    }

    private void setStatus(ExportPanel recipient, long generation, String message)
    {
        SwingUtilities.invokeLater(() -> {
            if (generation == exportGeneration && running && recipient != null && recipient == panel)
            {
                recipient.setStatus(message);
            }
        });
    }

    @Override protected void shutDown()
    {
        com.exportturtle.ui.TurtleCursor removedCursor = turtleCursor;
        turtleCursor = null;
        if (removedCursor != null)
            SwingUtilities.invokeLater(removedCursor::close);
        running = false;
        requested = false;
        if (writer != null)
        {
            writer.shutdownNow();
            writer = null;
        }
        if (selfie != null)
        {
            selfie.close();
            selfie = null;
        }
        fullCollection = null;
        items = null;
        pendingItems = null;
        NavigationButton removed = navigation;
        navigation = null;
        panel = null;
        if (removed != null)
        {
            SwingUtilities.invokeLater(() -> clientToolbar.removeNavigation(removed));
        }
    }

    private void updateTurtleCursor()
    {
        if (!running || panel == null || turtleCursor == null)
            return;
        panel.setTurtleCursorSelected(config.turtleCursor());
        try
        {
            turtleCursor.setEnabled(config.turtleCursor(), panel);
        }
        catch (RuntimeException error)
        {
            log.debug("Custom cursor is unavailable", error);
            configManager.setConfiguration(ExportTurtleConfig.GROUP, "turtleCursor", false);
            panel.showFileMessage("Custom cursors are unavailable on this system.", true);
        }
    }
}
