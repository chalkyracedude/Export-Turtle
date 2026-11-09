package com.exportturtle.selfie;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.exportturtle.ui.ExportTheme;
import java.awt.Component;
import java.awt.Container;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Standalone Swing controls with a mocked client; never sends game input. */
public class SelfieControlsTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void occupiedSlotsKeepThemeAndRequireInlineConfirmation() throws Exception
    {
        AtomicReference<SelfiePanel> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            ClientThread thread = mock(ClientThread.class);
            doAnswer(call -> {
                ((Runnable)call.getArgument(0)).run();
                return null;
            })
                .when(thread)
                .invoke(any(Runnable.class));
            SelfiePanel panel = new SelfiePanel(PortraitPerformanceTest.client(), thread,
                Filepath.Unchecked.getRooted(temporary.getRoot().toPath()));
            reference.set(panel);
            assertNull(button(panel, "Take selfie"));
            button(panel, "Refresh").doClick(0);
        });
        SwingUtilities.invokeAndWait(() -> {
            SelfiePanel panel = reference.get();
            try
            {
                JComboBox<String> slots = combo(panel);
                @SuppressWarnings("unchecked")
                Map<String, String> details =
                    (Map<String, String>)slots.getClientProperty("exportturtle.savedPoses");
                details.put("Pose 1", "Saved Nov 2, 2026, 9:15 AM");
                ExportTheme.apply(panel);
                JLabel rendered = (JLabel)slots.getRenderer().getListCellRendererComponent(
                    new JList<String>(), "Pose 1", 0, false, false);
                assertTrue(rendered.getText().endsWith("•"));
                assertTrue(rendered.getToolTipText().contains("9:15 AM"));
                slots.setSelectedItem("Pose 1");
                button(panel, "Store pose").doClick(0);
                assertNotNull(button(panel, "Replace 1?"));
                assertTrue(button(panel, "Cancel").isVisible());
                button(panel, "Cancel").doClick(0);
                assertNotNull(button(panel, "Store pose"));
                assertFalse(button(panel, "Cancel").isVisible());
                button(panel, "Store pose").doClick(0);
                slots.setSelectedItem("Pose 2");
                assertNotNull(button(panel, "Store pose"));
                assertFalse(button(panel, "Cancel").isVisible());
            }
            finally
            {
                panel.close();
            }
        });
    }

    private JButton button(Container parent, String text)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JButton && text.equals(((JButton)child).getText()))
                return (JButton)child;
            if (child instanceof Container)
            {
                JButton found = button((Container)child, text);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") private JComboBox<String> combo(Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JComboBox)
                return (JComboBox<String>)child;
            if (child instanceof Container)
            {
                JComboBox<String> found = combo((Container)child);
                if (found != null)
                    return found;
            }
        }
        return null;
    }
}
