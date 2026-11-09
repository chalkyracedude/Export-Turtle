package com.exportturtle;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.exportturtle.selfie.SelfiePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SidebarLayoutTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void keepsControlsInsideNarrowSidebarAtTwoFontSizes() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            for (int fontSize : new int[] {12, 16})
            {
                Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
                Object oldLabel = UIManager.get("Label.font");
                Object oldButton = UIManager.get("Button.font");
                Object oldCheckbox = UIManager.get("CheckBox.font");
                UIManager.put("Label.font", font);
                UIManager.put("Button.font", font);
                UIManager.put("CheckBox.font", font);
                SelfiePanel selfie = new SelfiePanel(mock(Client.class), mock(ClientThread.class),
                    Filepath.Unchecked.getRooted(temporary.getRoot().toPath()));
                try
                {
                    ExportPanel panel = new ExportPanel(() -> {}, selfie, () -> {}, file -> {});
                    panel.setSize(225, 650);
                    layout(panel);
                    checkBounds(panel);
                    JPanel body = (JPanel)panel.getComponent(0);
                    JPanel cursorRow = (JPanel)body.getComponent(2);
                    assertEquals("Soup Cursor", ((javax.swing.JCheckBox)cursorRow.getComponent(0)).getText());
                    assertNotNull(((javax.swing.JLabel)cursorRow.getComponent(1)).getIcon());
                    for (Component child : body.getComponents())
                    {
                        assertEquals("Every top-level control shares the same left edge", 0, child.getX());
                    }
                    assertTrue("Sidebar height at font " + fontSize + ": " + body.getHeight(),
                        body.getHeight() < 800);
                }
                finally
                {
                    selfie.close();
                    UIManager.put("Label.font", oldLabel);
                    UIManager.put("Button.font", oldButton);
                    UIManager.put("CheckBox.font", oldCheckbox);
                }
            }
        });
    }

    private void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents())
        {
            if (child instanceof Container)
                layout((Container)child);
        }
    }

    private void checkBounds(Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (!child.isVisible())
                continue;
            assertTrue(child.getX() >= 0);
            assertTrue(child.getX() + child.getWidth() <= parent.getWidth());
            if (child instanceof AbstractButton)
            {
                AbstractButton button = (AbstractButton)child;
                int required = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
                required += button.getInsets().left + button.getInsets().right;
                if (button instanceof javax.swing.JCheckBox)
                    required += 24;
                assertTrue("Unclipped button: " + button.getText(), button.getWidth() >= required);
            }
            if (child instanceof Container)
                checkBounds((Container)child);
        }
    }
}
