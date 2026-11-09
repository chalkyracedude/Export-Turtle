package com.exportturtle.ui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.Test;

public class TurtleCursorTest
{
    @Test public void headKeepsReferenceOrientationWithStrongerColour()
    {
        BufferedImage source = new BufferedImage(231, 197, BufferedImage.TYPE_INT_ARGB);
        var graphics = source.createGraphics();
        graphics.setColor(new java.awt.Color(160, 100, 100));
        graphics.fillRect(40, 16, 26, 26);
        graphics.dispose();
        BufferedImage frame = TurtleCursor.render(source, 64, 64, 0);
        int samples = 0, totalX = 0, totalY = 0;
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++)
            {
                int pixel = frame.getRGB(x, y);
                int red = (pixel >>> 16) & 255, green = (pixel >>> 8) & 255;
                if ((pixel >>> 24) == 255 && red > green)
                {
                    assertTrue("Reference hue has stronger saturation", red - green > 60);
                    assertTrue("Highlights are darker rather than whitewashed", red < 160);
                    samples++;
                    totalX += x;
                    totalY += y;
                }
            }
        assertTrue(samples > 0);
        assertTrue("Head points upper left like the supplied reference", totalX / samples < 32 && totalY / samples < 32);
        assertEquals(0, frame.getRGB(0, 0));
    }

    @Test public void sidebarPreviewIsCompact()
    {
        assertEquals(32, TurtleCursor.preview().getWidth());
        assertEquals(32, TurtleCursor.preview().getHeight());
    }

    @Test public void restoresCursorsAndMouseListenersWithoutConsumingClicks() throws Exception
    {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            javax.swing.JPanel game = new javax.swing.JPanel();
            javax.swing.JPanel sidebar = new javax.swing.JPanel();
            javax.swing.JButton button = new javax.swing.JButton("Test");
            sidebar.add(button);
            java.awt.Cursor original = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR);
            button.setCursor(original);
            int listeners = button.getMouseListeners().length;
            var mouse = mock(net.runelite.client.input.MouseManager.class);
            java.awt.Cursor[] frames = {
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR),
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR),
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
            };
            TurtleCursor cursor = new TurtleCursor(game, mouse, frames);
            cursor.setEnabled(true, sidebar);
            assertSame(frames[0], game.getCursor());
            assertSame(frames[0], button.getCursor());
            var registered = org.mockito.ArgumentCaptor.forClass(net.runelite.client.input.MouseListener.class);
            verify(mouse).registerMouseListener(registered.capture());
            var event = new java.awt.event.MouseEvent(game, java.awt.event.MouseEvent.MOUSE_PRESSED,
                0, 0, 1, 1, 1, false);
            assertSame(event, registered.getValue().mousePressed(event));
            assertFalse(event.isConsumed());
            javax.swing.JLabel added = new javax.swing.JLabel("Dynamic child");
            sidebar.add(added);
            assertSame(frames[0], added.getCursor());
            cursor.close();
            assertFalse(game.isCursorSet());
            assertFalse(sidebar.isCursorSet());
            assertFalse(added.isCursorSet());
            assertSame(original, button.getCursor());
            assertEquals(listeners, button.getMouseListeners().length);
            verify(mouse).unregisterMouseListener(registered.getValue());
        });
    }

    @Test public void framesKeepTransparencyAndAnimateOnlyFlippers() throws Exception
    {
        BufferedImage source;
        try (var input = TurtleCursor.class.getResourceAsStream("soup.png"))
        {
            assertNotNull(input);
            source = ImageIO.read(input);
        }
        BufferedImage idle = TurtleCursor.render(source, 64, 64, 0);
        BufferedImage pressed = TurtleCursor.render(source, 64, 64, 2);
        int changed = 0, visible = 0;
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++)
            {
                if (idle.getRGB(x, y) != pressed.getRGB(x, y))
                    changed++;
                if ((idle.getRGB(x, y) >>> 24) != 0)
                    visible++;
            }
        assertTrue("Flippers move", changed > 10);
        assertTrue("Transparent padding", visible < 64 * 64 / 2);
        for (int y = 26; y < 38; y++)
            for (int x = 26; x < 38; x++)
                assertEquals("Shell stays fixed", idle.getRGB(x, y), pressed.getRGB(x, y));
        assertEquals(0, idle.getRGB(0, 0));
        assertEquals(0, pressed.getRGB(0, 0));
    }
}
