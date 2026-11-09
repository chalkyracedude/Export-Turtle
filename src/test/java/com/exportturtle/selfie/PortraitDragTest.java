package com.exportturtle.selfie;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Exercises a standalone Swing panel, never a running RuneLite game client. */
public class PortraitDragTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void displaysFramesDuringContinuousDragging() throws Exception
    {
        CompletableFuture<Integer> completed = new CompletableFuture<>();
        SwingUtilities.invokeAndWait(() -> {
            ClientThread thread = mock(ClientThread.class);
            doAnswer(invocation -> {
                ((Runnable)invocation.getArgument(0)).run();
                return null;
            })
                .when(thread)
                .invoke(any(Runnable.class));
            SelfiePanel panel = new SelfiePanel(PortraitPerformanceTest.client(), thread,
                Filepath.Unchecked.getRooted(temporary.getRoot().toPath()));
            panel.setSize(210, 340);
            layout(panel);
            JPanel preview = (JPanel)panel.getComponent(0);
            findRefresh(panel).doClick(0);
            Set<Integer> frames = new HashSet<>();
            int[] ticks = {0};
            Timer timer = new Timer(10, null);
            timer.addActionListener(event -> {
                try
                {
                    preview.dispatchEvent(new MouseEvent(preview, MouseEvent.MOUSE_DRAGGED,
                        System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, ++ticks[0] * 3, 100, 0,
                        false, MouseEvent.NOBUTTON));
                    BufferedImage image = new BufferedImage(210, 248, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    preview.paint(graphics);
                    graphics.dispose();
                    frames.add(Arrays.hashCode(image.getRGB(0, 0, 210, 248, null, 0, 210)));
                    if (ticks[0] == 60)
                    {
                        timer.stop();
                        panel.close();
                        completed.complete(frames.size());
                    }
                }
                catch (Throwable failure)
                {
                    timer.stop();
                    panel.close();
                    completed.completeExceptionally(failure);
                }
            });
            timer.start();
        });
        assertTrue("Frames must update before dragging stops", completed.get(10, TimeUnit.SECONDS) >= 5);
    }

    private static void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents())
        {
            if (child instanceof Container)
                layout((Container)child);
        }
    }

    private static JButton findRefresh(Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JButton && "Refresh".equals(((JButton)child).getText()))
            {
                return (JButton)child;
            }
            if (child instanceof Container)
            {
                JButton found = findRefresh((Container)child);
                if (found != null)
                    return found;
            }
        }
        return null;
    }
}
