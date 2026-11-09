package com.exportturtle;

import com.exportturtle.ui.ExportTheme;
import com.exportturtle.ui.SidebarLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;
import net.runelite.client.ui.PluginPanel;

/** Swing-only view; game-state reads and file writes belong to other components. */
final class ExportPanel extends PluginPanel
{
    private final JTextArea status = SidebarLayout.status("Waiting...");
    private final JLabel portraitState = new JLabel("Portraits enabled");
    private final JLabel accountName = new JLabel("Waiting for account");
    private final JPopupMenu files = new JPopupMenu();
    private final BasicArrowButton copyFile = new BasicArrowButton(SwingConstants.SOUTH);
    private final JButton export = new JButton("Sync now");
    private final javax.swing.JCheckBox turtleCursor = new javax.swing.JCheckBox("Soup Cursor");
    private long bankNoticeUntil;
    private boolean saveError;
    private final JPanel coverage = SidebarLayout.column();
    private final Map<String, JLabel> results = new LinkedHashMap<>();
    private final Map<String, Boolean> features = new LinkedHashMap<>();

    ExportPanel(Runnable capture, JPanel portrait, Runnable copyDirectory, Consumer<ExportFile> copyExport)
    {
        setOpaque(true);
        setBackground(ExportTheme.BACKGROUND);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        JPanel body = SidebarLayout.column();
        JLabel title = new JLabel("Export Turtle");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        SidebarLayout.addRow(body, title, 0);
        accountName.setFont(accountName.getFont().deriveFont(13f));
        SidebarLayout.addRow(body, accountName, 4);
        JPanel cursorRow = new JPanel(new BorderLayout(6, 0));
        cursorRow.setOpaque(false);
        cursorRow.add(turtleCursor, BorderLayout.WEST);
        JLabel cursorPreview = new JLabel(new javax.swing.ImageIcon(com.exportturtle.ui.TurtleCursor.preview()));
        cursorPreview.setToolTipText("Soup cursor preview");
        cursorPreview.setHorizontalAlignment(SwingConstants.LEFT);
        cursorRow.add(cursorPreview, BorderLayout.CENTER);
        SidebarLayout.addRow(body, cursorRow, 6);
        JPanel selfieSection = utilitySection("Selfie");
        SidebarLayout.addRow(selfieSection, portraitState, 0);
        SidebarLayout.addRow(selfieSection, portrait, 4);
        SidebarLayout.addRow(body, selfieSection, 8);
        JPanel snapshotSection = utilitySection("Sync status");
        export.addActionListener(event -> {
            saveError = false;
            capture.run();
        });

        SidebarLayout.addRow(snapshotSection, export, 4);
        SidebarLayout.addRow(snapshotSection, status, 4);

        for (String name :
            new String[] {"Progress", "Bank", "Seed vault", "Items", "Collection", "Events", "Clan", "Diagnostics"})
        {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            JLabel titleLabel = new JLabel(name);
            JLabel value = new JLabel("Waiting");
            titleLabel.setFont(titleLabel.getFont().deriveFont(13f));
            value.setFont(value.getFont().deriveFont(Font.BOLD, 13f));
            row.add(titleLabel, BorderLayout.WEST);
            row.add(value, BorderLayout.EAST);
            results.put(name, value);
            SidebarLayout.addRow(coverage, row, 3);
        }
        SidebarLayout.addRow(snapshotSection, coverage, 2);
        JButton copyFolder = new JButton("Copy export path");
        copyFolder.setToolTipText("Copy the export folder path, then paste it into your file manager");
        copyFolder.addActionListener(event -> copyDirectory.run());
        for (ExportFile file : ExportFile.values())
        {
            JMenuItem item = new JMenuItem(file.toString());
            item.addActionListener(event -> copyExport.accept(file));
            files.add(item);
        }
        copyFile.setToolTipText("Copy a saved export path for the current account");
        copyFile.addActionListener(event -> files.show(copyFile, 0, copyFile.getHeight()));
        JPanel fileActions = new JPanel(new BorderLayout(4, 0));
        fileActions.setOpaque(false);
        fileActions.add(copyFolder, BorderLayout.CENTER);
        fileActions.add(copyFile, BorderLayout.EAST);
        setAccount(null);
        SidebarLayout.addRow(snapshotSection, fileActions, 8);
        SidebarLayout.addRow(body, snapshotSection, 8);
        add(body, BorderLayout.NORTH);
        ExportTheme.apply(body);
        turtleCursor.setPreferredSize(new java.awt.Dimension(
            turtleCursor.getFontMetrics(turtleCursor.getFont()).stringWidth(turtleCursor.getText())
                + turtleCursor.getInsets().left + turtleCursor.getInsets().right + 24,
            turtleCursor.getPreferredSize().height));
        title.setForeground(ExportTheme.GOLD);
        getWrappedPanel().setBackground(ExportTheme.BACKGROUND);
        getWrappedPanel().setOpaque(true);
        if (getScrollPane() != null)
        {
            getScrollPane().setBackground(ExportTheme.BACKGROUND);
            getScrollPane().getViewport().setBackground(ExportTheme.BACKGROUND);
            // PluginPanel adds an inner north-aligned wrapper below the viewport.
            java.awt.Component viewportContent = getScrollPane().getViewport().getView();
            if (viewportContent != null)
                viewportContent.setBackground(ExportTheme.BACKGROUND);
            getScrollPane().getVerticalScrollBar().setBackground(ExportTheme.BACKGROUND);
        }
    }

    private JPanel utilitySection(String name)
    {
        JPanel section = SidebarLayout.column();
        section.setOpaque(true);
        section.setBackground(ExportTheme.PANEL);
        javax.swing.border.TitledBorder border =
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ExportTheme.BORDER), name);
        border.setTitleColor(ExportTheme.GOLD);
        border.setTitleFont(getFont().deriveFont(Font.BOLD));
        section.setBorder(
            BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        return section;
    }

    void setCoverage(ExportTurtleConfig config)
    {
        turtleCursor.setSelected(config.turtleCursor());
        features.put("Progress", config.localSnapshots());
        features.put("Bank", config.localSnapshots() && config.itemExports());
        features.put("Seed vault", config.localSnapshots() && config.itemExports());
        features.put("Items", config.localSnapshots() && config.itemExports());
        features.put("Collection", config.localSnapshots() && config.collectionExports());
        features.put("Events", config.localSnapshots() && config.eventHistory());
        boolean portraitsEnabled = config.localSnapshots() && config.portraitExports();
        portraitState.setText(portraitsEnabled ? "Portraits enabled" : "Portraits disabled");
        portraitState.setFont(portraitState.getFont().deriveFont(13f));
        portraitState.setForeground(portraitsEnabled ? new Color(114, 214, 139) : new Color(180, 190, 180));
        features.put("Clan", config.localSnapshots() && config.clanExports());
        features.put("Diagnostics", config.localSnapshots() && config.diagnostics());
        features.forEach((name, active) -> setFeatureStatus(name, active ? "Waiting" : "Disabled", false));
        results.get("Bank").setToolTipText("Open your bank to refresh.");
        results.get("Seed vault").setToolTipText("Open your seed vault to refresh.");
        results.get("Collection").setToolTipText("Open the collection journal to sync.");
    }

    void onTurtleCursorChanged(java.util.function.Consumer<Boolean> listener)
    {
        turtleCursor.addActionListener(event -> listener.accept(turtleCursor.isSelected()));
    }

    void setTurtleCursorSelected(boolean selected)
    {
        turtleCursor.setSelected(selected);
    }

    void resetSyncStatus(ExportTurtleConfig config)
    {
        saveError = false;
        setAccount(null);
        bankNoticeUntil = 0;
        status.setToolTipText(null);
        setCoverage(config);
        SidebarLayout.updateStatus(status, "Waiting...", SidebarLayout.StatusTone.WAITING);
    }

    void showFileMessage(String message, boolean failure)
    {
        SidebarLayout.updateStatus(status, message,
            failure ? SidebarLayout.StatusTone.ERROR : SidebarLayout.StatusTone.WAITING);
    }

    void setAccount(String name)
    {
        accountName.setText(name == null ? "Waiting for account" : "Account: " + name);
        for (java.awt.Component item : files.getComponents())
            item.setEnabled(name != null);
        copyFile.setEnabled(name != null);
    }

    void setObservation(String name, String time)
    {
        if (time == null)
            return;
        setFeatureStatus(name, "Synced", false);
        results.get(name).setToolTipText("Observed: " + time);
    }

    void setFeatureStatus(String name, String text, boolean failure)
    {
        JLabel result = results.get(name);
        if (result == null)
            return;
        if (Boolean.FALSE.equals(features.get(name)))
        {
            text = "Disabled";
            failure = false;
        }
        if ("Synced".equals(text) || "Not in a clan".equals(text))
            result.setToolTipText("Last export: " + ZonedDateTime.now().format(
                DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a")));
        else
            result.setToolTipText(text);
        result.setText(text);
        result.setForeground(failure ? new Color(255, 100, 100)
                             : ("Synced".equals(text) || "Not in a clan".equals(text))
                                 ? new Color(114, 214, 139)
                                 : new Color(180, 190, 180));
    }

    void setItemsStatus(String message, boolean bankUpdated)
    {
        setFeatureStatus("Items", "Synced", false);
        if (bankUpdated)
            setFeatureStatus("Bank", "Synced", false);
        export.setToolTipText(message + ". Open your bank to refresh; charges are Experimental.");
        if (bankUpdated)
        {
            if (saveError)
                return;
            bankNoticeUntil = System.nanoTime() + 4_000_000_000L;
            status.setToolTipText(message);
            updateSyncTime();
        }
    }

    void setExportEnabled(boolean value)
    {
        export.setEnabled(value);
    }

    private void updateSyncTime()
    {
        String time = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
        SidebarLayout.updateStatus(status, "Last sync: " + time, SidebarLayout.StatusTone.SUCCESS);
    }

    void setStatus(String message)
    {
        if (message.startsWith("Saved at "))
            setFeatureStatus("Progress", "Synced", false);
        if (message.startsWith("Collection Log synced"))
            setFeatureStatus("Collection", "Synced", false);
        if (message.startsWith("Could not"))
            setFeatureStatus(message.contains("Collection") ? "Collection"
                             : message.contains("item")     ? "Items"
                                                            : "Progress",
                "Failed", true);
        if (saveError && !message.startsWith("Could not"))
            return;
        if (message.startsWith("Could not"))
            saveError = true;
        if (message.startsWith("Saved at ") && System.nanoTime() < bankNoticeUntil)
            return;
        SidebarLayout.StatusTone tone =
            (message.startsWith("Saved at ") || message.startsWith("Collection Log synced."))
                ? SidebarLayout.StatusTone.SUCCESS
            : message.startsWith("Could not") ? SidebarLayout.StatusTone.ERROR
                                              : SidebarLayout.StatusTone.WAITING;
        if (tone == SidebarLayout.StatusTone.SUCCESS)
            updateSyncTime();
        else
            SidebarLayout.updateStatus(status, message, tone);
    }
}
