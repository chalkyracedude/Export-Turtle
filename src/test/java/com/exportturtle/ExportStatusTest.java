package com.exportturtle;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.Test;

public class ExportStatusTest
{
    @Test public void turtleCursorToggleNotifiesOnlyForUserChanges() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ExportPanel panel = new ExportPanel(() -> {}, new JPanel(), () -> {}, file -> {});
            java.util.List<Boolean> changes = new java.util.ArrayList<>();
            panel.onTurtleCursorChanged(changes::add);
            panel.setTurtleCursorSelected(true);
            assertTrue(changes.isEmpty());
            find(panel, javax.swing.JCheckBox.class).doClick(0);
            assertEquals(java.util.Collections.singletonList(false), changes);
        });
    }

    @Test public void errorsStayVisibleUntilManualRetry() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ExportPanel panel = new ExportPanel(() -> {}, new JPanel(), () -> {}, file -> {});
            panel.setExportEnabled(true);
            panel.setStatus("Could not save snapshot.");
            panel.setStatus("Saved at 9:15 AM");
            panel.setItemsStatus("Bank: 10 stacks; 2 charge records", true);
            JTextArea status = find(panel, JTextArea.class);
            assertEquals("Could not save snapshot.", status.getText());
            find(panel, JButton.class).doClick(0);
            panel.setStatus("Collection Log synced.");
            assertTrue(status.getText().startsWith("Last sync: "));
        });
    }

    @Test public void sessionResetClearsPreviousAccountSuccessAndErrors() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ExportPanel panel = new ExportPanel(() -> {}, new JPanel(), () -> {}, file -> {});
            ExportTurtleConfig config = new ExportTurtleConfig() {};
            panel.setCoverage(config);
            panel.setFeatureStatus("Bank", "Synced", false);
            panel.setStatus("Could not save snapshot.");
            panel.resetSyncStatus(config);
            assertEquals("Waiting...", find(panel, JTextArea.class).getText());
            panel.setStatus("Saved at noon");
            assertTrue(find(panel, JTextArea.class).getText().startsWith("Last sync: "));
        });
    }

    private <T extends Component> T find(Container parent, Class<T> type)
    {
        for (Component child : parent.getComponents())
        {
            if (type.isInstance(child))
                return type.cast(child);
            if (child instanceof Container)
            {
                T found = find((Container)child, type);
                if (found != null)
                    return found;
            }
        }
        return null;
    }
}
