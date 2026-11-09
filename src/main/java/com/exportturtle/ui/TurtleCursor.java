package com.exportturtle.ui;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.input.MouseManager;
import net.runelite.client.util.ImageUtil;

/** Cosmetic cursor animation only; mouse events always continue unchanged. */
public final class TurtleCursor implements AutoCloseable
{
    private static final int[] SEQUENCE = {1, 2, 2, 1, 0};
    private final Component game;
    private final MouseManager mouseManager;
    private final Map<Component, Cursor> originals = new IdentityHashMap<>();
    private final Timer timer = new Timer(70, event -> advance());
    private final java.awt.event.MouseAdapter sidebarMouse = new java.awt.event.MouseAdapter() {
        @Override public void mousePressed(MouseEvent event) { animate(); }
    };
    private final net.runelite.client.input.MouseAdapter gameMouse =
        new net.runelite.client.input.MouseAdapter() {
            @Override public MouseEvent mousePressed(MouseEvent event)
            {
                SwingUtilities.invokeLater(TurtleCursor.this::animate);
                return event;
            }
        };
    private final ContainerAdapter children = new ContainerAdapter() {
        @Override public void componentAdded(ContainerEvent event)
        {
            if (enabled)
                attach(event.getChild());
        }
        @Override public void componentRemoved(ContainerEvent event) { detach(event.getChild()); }
    };
    private Cursor[] frames;
    private Cursor previous;
    private Cursor applied;
    private boolean enabled;
    private boolean closed;
    private int step;

    public TurtleCursor(Component game, MouseManager mouseManager)
    {
        this(game, mouseManager, null);
    }

    TurtleCursor(Component game, MouseManager mouseManager, Cursor[] frames)
    {
        this.game = game;
        this.mouseManager = mouseManager;
        this.frames = frames;
    }

    /** Called on Swing's event thread, including plugin shutdown. */
    public void setEnabled(boolean value, Component sidebar)
    {
        if (closed || value == enabled)
            return;
        if (!value)
        {
            disable();
            return;
        }
        if (frames == null)
        {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension size = toolkit.getBestCursorSize(48, 48);
            if (size.width == 0 || size.height == 0)
                throw new IllegalStateException("Custom cursors are unavailable on this system.");
            BufferedImage source = ImageUtil.loadImageResource(TurtleCursor.class, "soup.png");
            frames = new Cursor[3];
            for (int i = 0; i < frames.length; i++)
                frames[i] = toolkit.createCustomCursor(render(source, size.width, size.height, i),
                    new Point(size.width / 5, size.height / 7), "Soup " + i);
        }
        previous = game.isCursorSet() ? game.getCursor() : null;
        enabled = true;
        attach(sidebar);
        mouseManager.registerMouseListener(gameMouse);
        apply(0);
    }

    private void attach(Component component)
    {
        if (originals.containsKey(component))
            return;
        originals.put(component, component.isCursorSet() ? component.getCursor() : null);
        component.addMouseListener(sidebarMouse);
        component.setCursor(frames[0]);
        if (component instanceof Container)
        {
            Container container = (Container)component;
            container.addContainerListener(children);
            for (Component child : container.getComponents())
                attach(child);
        }
    }

    private void detach(Component component)
    {
        if (!originals.containsKey(component))
            return;
        Cursor original = originals.remove(component);
        component.removeMouseListener(sidebarMouse);
        if (ours(component.getCursor()))
            component.setCursor(original);
        if (component instanceof Container)
        {
            Container container = (Container)component;
            container.removeContainerListener(children);
            for (Component child : container.getComponents())
                detach(child);
        }
    }

    private boolean ours(Cursor cursor)
    {
        if (frames != null)
            for (Cursor frame : frames)
                if (cursor == frame)
                    return true;
        return false;
    }

    private void animate()
    {
        if (!enabled || closed)
            return;
        step = 0;
        apply(SEQUENCE[step++]);
        timer.restart();
    }

    private void advance()
    {
        if (!enabled || step == SEQUENCE.length)
        {
            timer.stop();
            return;
        }
        apply(SEQUENCE[step++]);
        if (step == SEQUENCE.length)
            timer.stop();
    }

    private void apply(int frame)
    {
        // Do not overwrite a cursor another plugin selected after ours.
        if (applied == null || game.getCursor() == applied)
        {
            game.setCursor(frames[frame]);
            applied = frames[frame];
        }
        for (Component component : originals.keySet())
            if (ours(component.getCursor()))
                component.setCursor(frames[frame]);
    }

    private void disable()
    {
        enabled = false;
        timer.stop();
        mouseManager.unregisterMouseListener(gameMouse);
        if (game.getCursor() == applied)
            game.setCursor(previous);
        for (Component component : originals.keySet().toArray(new Component[0]))
            detach(component);
        previous = null;
        applied = null;
    }

    @Override public void close()
    {
        if (enabled)
            disable();
        closed = true;
    }

    /** Keep the shell/head fixed and rotate only the two front flippers. */
    static BufferedImage render(BufferedImage source, int width, int height, int frame)
    {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = Math.min(width / 265.0, height / 235.0);
            graphics.translate(width / 2.0, height / 2.0);
            graphics.scale(scale, scale);
            graphics.translate(-231 / 2.0, -197 / 2.0);
            Polygon left = new Polygon(new int[] {16, 20, 30, 51, 61, 67, 45, 27},
                new int[] {158, 124, 101, 88, 107, 126, 145, 155}, 8);
            Polygon right = new Polygon(new int[] {125, 143, 172, 194, 177, 153, 139},
                new int[] {43, 33, 30, 33, 46, 54, 62}, 7);
            Area body = new Area(new Rectangle2D.Double(0, 0, 231, 197));
            body.subtract(new Area(left));
            body.subtract(new Area(right));
            double angle = frame == 0 ? 0 : frame == 1 ? .26 : .52;
            drawPart(graphics, source, left, -angle, 56, 106);
            drawPart(graphics, source, right, angle, 137, 48);
            Graphics2D fixed = (Graphics2D)graphics.create();
            fixed.clip(body);
            fixed.drawImage(source, 0, 0, 231, 197, null);
            fixed.dispose();
        }
        finally { graphics.dispose(); }
        strengthenColours(result);
        addOutline(result);
        return result;
    }

    /** Share the actual idle frame with the sidebar preview. */
    public static BufferedImage preview()
    {
        return render(ImageUtil.loadImageResource(TurtleCursor.class, "soup.png"), 32, 32, 0);
    }

    private static void strengthenColours(BufferedImage image)
    {
        float[] hsb = new float[3];
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
            {
                int pixel = image.getRGB(x, y), alpha = pixel >>> 24;
                if (alpha == 0)
                    continue;
                Color.RGBtoHSB((pixel >>> 16) & 255, (pixel >>> 8) & 255, pixel & 255, hsb);
                hsb[1] = Math.min(1f, hsb[1] * 1.8f);
                hsb[2] = Math.max(0f, Math.min(1f, (hsb[2] - .5f) * 1.25f + .44f));
                // Solid interior pixels avoid a translucent, washed-out native cursor.
                if (alpha >= 128)
                    alpha = 255;
                image.setRGB(x, y, (alpha << 24) | (Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff));
            }
    }

    /** Separate the silhouette from scenery. */
    private static void addOutline(BufferedImage image)
    {
        int width = image.getWidth(), height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
            {
                int pixel = pixels[y * width + x], alpha = pixel >>> 24;
                if (alpha == 0)
                {
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++)
                        {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                                (pixels[ny * width + nx] >>> 24) >= 128)
                                image.setRGB(x, y, 0xd7202818);
                        }
                }
            }
    }

    private static void drawPart(Graphics2D graphics, BufferedImage source, Polygon shape,
        double angle, int x, int y)
    {
        Graphics2D part = (Graphics2D)graphics.create();
        part.rotate(angle, x, y);
        part.clip(shape);
        part.drawImage(source, 0, 0, 231, 197, null);
        part.dispose();
    }
}
