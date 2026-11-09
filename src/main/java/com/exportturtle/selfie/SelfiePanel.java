package com.exportturtle.selfie;

import com.exportturtle.ui.ExportTheme;
import com.exportturtle.ui.SidebarLayout;
import com.exportturtle.ui.SidebarLayout.StatusTone;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;

/** Sidebar-only interaction. Rendering and PNG writes never run on the client thread. */
@Slf4j public final class SelfiePanel extends JPanel implements AutoCloseable
{
    private final Client client;
    private final ClientThread clientThread;
    private final Filepath directory;
    private com.google.gson.Gson poseGson;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "exportturtle-portrait");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<RenderRequest> pending = new AtomicReference<>();
    private final AtomicBoolean rendering = new AtomicBoolean();
    private final AtomicLong revision = new AtomicLong();
    private final JTextArea status = SidebarLayout.status("Waiting...");
    private final JButton storePoseButton = new JButton("Store pose");
    private final JButton cancelOverwrite = new JButton("Cancel");
    private final Map<String, String> poseSavedAt = new java.util.HashMap<>();
    private String overwriteSlot;
    private boolean savingPose;
    private boolean loadingPoses;
    private boolean saveError;
    private final java.util.ArrayDeque<PortraitScene> animationFrames = new java.util.ArrayDeque<>();
    private final JCheckBox animate = new JCheckBox("Live");
    private final AtomicBoolean capturePending = new AtomicBoolean();
    private final Timer animationTimer = new Timer(75, event -> captureAnimation());
    private final JCheckBox halfRotation = new JCheckBox("180\u00b0");
    private final JCheckBox rotation = new JCheckBox("360\u00b0");
    private final Map<String, PortraitScene> poses = new java.util.HashMap<>();
    private final javax.swing.JComboBox<String> pose =
        new javax.swing.JComboBox<>(new String[] {"Select pose", "Pose 1", "Pose 2", "Pose 3"});
    private final Map<String, java.util.List<PortraitScene>> poseAnimations = new java.util.HashMap<>();
    private final Map<String, boolean[]> poseOptions = new java.util.HashMap<>();
    private java.util.List<PortraitScene> activePose = java.util.Collections.emptyList();
    private int poseFrame;
    private int captureAnimationId = Integer.MIN_VALUE;
    private int readinessTenths = -1;
    private final Timer posePlayback = new Timer(75, event -> playPose());
    private final Preview preview = new Preview();
    private volatile boolean closed;
    private PortraitScene scene;
    private final Map<String, double[]> poseCameras = new java.util.HashMap<>();
    private double yaw = Math.PI, pitch = .12, zoom = 1;
    private boolean interacting;
    private long displayedRevision;
    private final Timer settle = new Timer(140, event -> {
        interacting = false;
        requestRender();
    });
    private double rotationOffset;
    private final Timer rotationTimer = new Timer(75, event -> rotatePreview());

    public SelfiePanel(Client client, ClientThread clientThread, Filepath directory)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.directory = directory;
        settle.setRepeats(false);
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder());
        preview.setPreferredSize(new Dimension(200, 180));
        preview.setMinimumSize(new Dimension(80, 180));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        preview.setToolTipText("Drag to rotate. Scroll to zoom.");
        SidebarLayout.addRow(this, preview, 0);
        JPanel controls = new JPanel(new java.awt.BorderLayout(4, 0));
        controls.setOpaque(false);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        refresh.setMargin(new java.awt.Insets(4, 2, 4, 2));
        controls.add(refresh, java.awt.BorderLayout.CENTER);
        cancelOverwrite.setVisible(false);
        cancelOverwrite.addActionListener(event -> {
            cancelOverwrite();
            setStatus("Overwrite cancelled.");
        });
        controls.add(cancelOverwrite, java.awt.BorderLayout.EAST);
        animate.setOpaque(false);
        animate.setToolTipText("Follow your character and pet's current in-game pose and movement.");
        animate.addActionListener(event -> {
            if (!activePose.isEmpty())
            {
                if (animate.isSelected())
                    posePlayback.start();
                else
                    posePlayback.stop();
            }
            else if (animate.isSelected())
            {
                animationFrames.clear();
                captureAnimationId = Integer.MIN_VALUE;
                readinessTenths = -1;
                updateReadiness();
                animationTimer.start();
            }
            else
                animationTimer.stop();
        });
        JPanel animationControls = new JPanel(new GridLayout(1, 3, 4, 0));
        animationControls.setOpaque(false);
        rotation.setOpaque(false);
        rotation.setToolTipText("Rotate the preview and saved selfie through 360 degrees.");
        rotation.addItemListener(event -> {
            if (rotation.isSelected())
                halfRotation.setSelected(false);
            rotationOffset = 0;
            updateRotationTimer();
            if (scene != null)
                requestRender();
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0)
                updateRotationTimer();
        });
        animationControls.add(animate);
        animationControls.add(rotation);
        halfRotation.setOpaque(false);
        halfRotation.setToolTipText("Sweep the preview and saved selfie back and forth through 180 degrees.");
        halfRotation.addItemListener(event -> {
            if (halfRotation.isSelected())
                rotation.setSelected(false);
            rotationOffset = 0;
            updateRotationTimer();
            if (scene != null)
                requestRender();
        });
        animationControls.add(halfRotation);
        for (JCheckBox control : new JCheckBox[] {animate, rotation, halfRotation})
        {
            control.setFont(control.getFont().deriveFont(13f));
            control.setMargin(new java.awt.Insets(0, 0, 0, 0));
        }
        SidebarLayout.addRow(this, animationControls, 2);
        JPanel poseControls = new JPanel(new GridLayout(1, 2, 4, 0));
        poseControls.setOpaque(false);
        JButton storePose = storePoseButton;
        storePose.setMargin(new java.awt.Insets(4, 2, 4, 2));
        storePose.setToolTipText(
            "Store a movement loop when Live animation is on, or a still pose when off. Saved locally.");
        storePose.addActionListener(event -> storePose());
        pose.putClientProperty("exportturtle.savedPoses", poseSavedAt);
        ExportTheme.apply(pose);
        pose.addActionListener(event -> {
            cancelOverwrite();
            pose.setToolTipText(poseSavedAt.getOrDefault((String)pose.getSelectedItem(), "Empty slot"));
            String slot = (String)pose.getSelectedItem();
            if ("Select pose".equals(slot))
                return;
            if (scene == null)
            {
                setStatus("Refresh to load this character's saved poses.");
                return;
            }
            PortraitScene selected = poses.get(slot);
            if (selected == null)
            {
                setStatus(slot + " is empty. Use Store pose to fill it.");
                return;
            }
            if (scene == null || !selected.getAccountName().equals(scene.getAccountName()))
                return;
            animationTimer.stop();
            animate.setSelected(false);
            animationFrames.clear();
            scene = selected;
            activatePose((String)pose.getSelectedItem());
            setStatus("Pose loaded.");
            requestRender();
        });
        poseControls.add(pose);
        poseControls.add(storePose);
        SidebarLayout.addRow(this, poseControls, 2);

        SidebarLayout.addRow(this, controls, 6);
        SidebarLayout.addRow(this, status, 4);
        MouseAdapter mouse = new MouseAdapter() {
            private int lastX, lastY;

            @Override public void mousePressed(MouseEvent event)
            {
                lastX = event.getX();
                lastY = event.getY();
            }

            @Override public void mouseDragged(MouseEvent event)
            {
                interacting = true;
                yaw += (event.getX() - lastX) * .02;
                pitch = Math.max(-.6, Math.min(.6, pitch + (event.getY() - lastY) * .01));
                lastX = event.getX();
                lastY = event.getY();
                requestRender();
            }

            @Override public void mouseReleased(MouseEvent event)
            {
                interacting = false;
                requestRender();
            }

            @Override public void mouseWheelMoved(MouseWheelEvent event)
            {
                interacting = true;
                settle.restart();
                zoom = Math.max(.5, Math.min(1.6, zoom - event.getPreciseWheelRotation() * .08));
                requestRender();
            }
        };
        preview.addMouseListener(mouse);
        preview.addMouseMotionListener(mouse);
        preview.addMouseWheelListener(mouse);
    }

    public void enablePoseStorage(com.google.gson.Gson gson)
    {
        poseGson = gson;
    }

    private void persistPose(String slot, java.util.List<PortraitScene> frames)
    {
        boolean live = animate.isSelected(), half = halfRotation.isSelected(),
                rotate = rotation.isSelected() || half;
        double savedYaw = yaw, savedPitch = pitch, savedZoom = zoom;
        if (poseGson == null)
        {
            setStatus("Pose storage unavailable.", StatusTone.ERROR);
            return;
        }
        savingPose = true;
        storePoseButton.setEnabled(false);
        setStatus("Saving " + slot.toLowerCase(java.util.Locale.ROOT) + "...");
        clientThread.invoke(() -> {
            if (closed || client.getLocalPlayer() == null ||
                !frames.get(0).getAccountName().equals(client.getLocalPlayer().getName()))
            {
                SwingUtilities.invokeLater(() -> finishPoseSave("Log in and store again.", StatusTone.ERROR));
                return;
            }
            PortraitScene head = captureHead();
            worker.execute(() -> {
                try
                {
                    PosePortraits.write(
                        directory, slot, frames, live, rotate, savedYaw, savedPitch, savedZoom, true, half);
                    if (head != null)
                    {
                        java.awt.image.BufferedImage image = PortraitRenderer.renderHeadshot(head);
                        PortraitStore.write(
                            directory, head.getAccountName(), image, ".pose" + slot.charAt(5) + ".head.png");
                        PortraitStore.write(directory, head.getAccountName(), image, ".head.png");
                    }
                    else
                    {
                        directory
                            .joinSegment(
                                com.exportturtle.snapshot.SnapshotStore.fileName(frames.get(0).getAccountName())
                                    .replace(".json", ".pose" + slot.charAt(5) + ".head.png"))
                            .deleteIfExists();
                    }
                    PoseStore.write(directory.join("poses"), slot, frames, poseGson, live, rotate, savedYaw,
                        savedPitch, savedZoom, half);
                    String savedAt =
                        ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a"));
                    SwingUtilities.invokeLater(() -> {
                        if (closed || scene == null ||
                            !scene.getAccountName().equals(frames.get(0).getAccountName()))
                            return;
                        poses.put(slot, frames.get(0));
                        if (live)
                            poseAnimations.put(slot, frames);
                        else
                            poseAnimations.remove(slot);
                        poseOptions.put(slot, new boolean[] {live, rotate && !half, half});
                        poseCameras.put(slot, new double[] {savedYaw, savedPitch, savedZoom});
                        poseSavedAt.put(slot, "Saved " + savedAt);
                        pose.setToolTipText(poseSavedAt.get((String)pose.getSelectedItem()));
                        pose.repaint();
                        finishPoseSave(head == null
                                           ? slot + " saved; head capture failed. Refresh and store again."
                                           : slot + " saved.",
                            head == null ? StatusTone.ERROR : StatusTone.SUCCESS);
                    });
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not save pose", error);
                    SwingUtilities.invokeLater(
                        () -> finishPoseSave("Could not save pose. Try again.", StatusTone.ERROR));
                }
            });
        });
    }

    private void loadPoses(String account)
    {
        if (poseGson == null)
            return;
        loadingPoses = true;
        storePoseButton.setEnabled(false);
        worker.execute(() -> {
            for (int slot = 1; slot <= 3; slot++)
            {
                String name = "Pose " + slot;
                try
                {
                    PoseStore.SavedPose stored =
                        PoseStore.readPose(directory.join("poses"), account, name, poseGson);
                    java.util.List<PortraitScene> frames = stored.frames;
                    if (!frames.isEmpty())
                    {
                        String filename = com.exportturtle.snapshot.SnapshotStore.fileName(account).replace(
                            ".json", ".pose" + slot + ".png");
                        if (!directory.joinSegment(filename).exists())
                            PosePortraits.write(directory, name, frames, stored.live, stored.rotation,
                                stored.yaw, stored.pitch, stored.zoom, false, stored.half);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (closed || scene == null || !scene.getAccountName().equals(account) ||
                            frames.isEmpty() || poses.containsKey(name))
                            return;
                        poseOptions.put(
                            name, new boolean[] {stored.live, stored.rotation && !stored.half, stored.half});
                        poseCameras.put(name, new double[] {stored.yaw, stored.pitch, stored.zoom});
                        poses.put(name, frames.get(0));
                        String stamp = "Saved locally";
                        if (stored.savedAt != null)
                        {
                            try
                            {
                                stamp = "Saved " +
                                        java.time.Instant.parse(stored.savedAt)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a"));
                            }
                            catch (java.time.DateTimeException invalid)
                            {
                                log.debug("Invalid pose timestamp", invalid);
                            }
                        }
                        poseSavedAt.put(name, stamp);
                        pose.setToolTipText(
                            poseSavedAt.getOrDefault((String)pose.getSelectedItem(), "Empty slot"));
                        pose.repaint();
                        if (frames.size() > 1)
                            poseAnimations.put(name, frames);
                    });
                }
                catch (IOException | RuntimeException error)
                {
                    log.debug("Could not load pose", error);
                    SwingUtilities.invokeLater(() -> {
                        if (closed || scene == null || !scene.getAccountName().equals(account))
                            return;
                        poseSavedAt.put(name, "Saved pose could not be read");
                        pose.repaint();
                        setStatus("Could not load " + name.toLowerCase(java.util.Locale.ROOT) + ".",
                            StatusTone.ERROR);
                    });
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (closed || scene == null || !scene.getAccountName().equals(account))
                    return;
                loadingPoses = false;
                storePoseButton.setEnabled(!savingPose);
            });
        });
    }

    private java.util.function.BooleanSupplier exportAllowed = () -> true;
    public void setExportAllowed(java.util.function.BooleanSupplier allowed)
    {
        exportAllowed = allowed;
    }

    private void storePose()
    {
        if (!exportAllowed.getAsBoolean())
        {
            setStatus("Portraits disabled in settings.");
            return;
        }
        if (savingPose || loadingPoses)
            return;
        saveError = false;
        String slot = (String)pose.getSelectedItem();
        if ("Select pose".equals(slot))
        {
            setStatus("Choose a pose slot before storing.");
            return;
        }
        if (scene == null)
        {
            setStatus("Refresh to load your character.");
            return;
        }
        if (poseSavedAt.containsKey(slot) && !slot.equals(overwriteSlot))
        {
            overwriteSlot = slot;
            storePoseButton.setText("Replace " + slot.charAt(5) + "?");
            cancelOverwrite.setVisible(true);
            setStatus("Replace " + slot.toLowerCase(java.util.Locale.ROOT) + "?");
            revalidate();
            return;
        }
        cancelOverwrite();
        if (animate.isSelected())
        {
            if (activePose.isEmpty() && animationFrames.size() < 72)
            {
                setStatus("Let Live animation run for 6 seconds before storing.");
                return;
            }
            java.util.List<PortraitScene> clip =
                java.util.List.copyOf(activePose.isEmpty() ? animationFrames : activePose);
            persistPose(slot, clip);
            animationTimer.stop();
            animationFrames.clear();
            activePose = clip;
            scene = clip.get(0);
            poseFrame = 0;
            posePlayback.start();
            requestRender();
            return;
        }
        clientThread.invoke(() -> {
            try
            {
                PortraitScene captured = PortraitScene.capture(client);
                SwingUtilities.invokeLater(() -> {
                    if (closed)
                        return;
                    persistPose(slot, java.util.Collections.singletonList(captured));
                    posePlayback.stop();
                    activePose = java.util.Collections.emptyList();
                    animationTimer.stop();
                    animate.setSelected(false);
                    animationFrames.clear();
                    scene = captured;
                    requestRender();
                });
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> {
                    if (!closed)
                        setStatus("Log in and try storing the pose again.");
                });
            }
        });
    }

    private void activatePose(String slot)
    {
        posePlayback.stop();
        rotationOffset = 0;
        boolean[] options = poseOptions.getOrDefault(slot, new boolean[] {false, false});
        animate.setSelected(options[0]);
        rotation.setSelected(options[1]);
        halfRotation.setSelected(options.length > 2 && options[2]);
        double[] camera = poseCameras.getOrDefault(slot, new double[] {Math.PI, .12, 1});
        yaw = camera[0];
        pitch = camera[1];
        zoom = camera[2];
        updateRotationTimer();
        activePose = poseAnimations.getOrDefault(slot, java.util.Collections.emptyList());
        poseFrame = 0;
        if (!activePose.isEmpty())
        {
            scene = activePose.get(0);
            if (animate.isSelected())
                posePlayback.start();
        }
    }

    private void updateRotationTimer()
    {
        if (!closed && scene != null && isShowing() && (rotation.isSelected() || halfRotation.isSelected()))
            rotationTimer.start();
        else
            rotationTimer.stop();
    }

    private void rotatePreview()
    {
        if (closed || scene == null || !isShowing() || !(rotation.isSelected() || halfRotation.isSelected()))
        {
            rotationTimer.stop();
            return;
        }
        if (interacting)
            return;
        rotationOffset =
            (rotationOffset + Math.PI * 2 / (halfRotation.isSelected() ? 144 : 72)) % (Math.PI * 2);
        requestRender();
    }

    private void playPose()
    {
        if (closed || activePose.isEmpty() || !isShowing())
            return;
        scene = activePose.get(poseFrame);
        poseFrame = (poseFrame + 1) % activePose.size();
        requestRender();
    }

    private void captureAnimation()
    {
        if (!exportAllowed.getAsBoolean())
        {
            setStatus("Portraits disabled in settings.");
            return;
        }
        if (closed || scene == null || !isShowing() || !capturePending.compareAndSet(false, true))
            return;
        clientThread.invoke(() -> {
            try
            {
                PortraitScene captured = PortraitScene.capture(client);
                int currentAnimation = client.getLocalPlayer().getAnimation();
                int identity = currentAnimation == -1 ? -2 - client.getLocalPlayer().getPoseAnimation()
                                                      : currentAnimation;
                SwingUtilities.invokeLater(() -> {
                    if (!closed && animate.isSelected() && scene != null &&
                        scene.getAccountName().equals(captured.getAccountName()))
                    {
                        if (identity != captureAnimationId)
                        {
                            captureAnimationId = identity;
                            animationFrames.clear();
                            readinessTenths = -1;
                        }
                        scene = captured.withStableCamera(scene);
                        animationFrames.addLast(scene);
                        if (animationFrames.size() > 72)
                            animationFrames.removeFirst();
                        updateReadiness();
                        requestRender();
                    }
                    capturePending.set(false);
                });
            }
            catch (RuntimeException exception)
            {
                capturePending.set(false);
                log.debug("Animation model unavailable", exception);
            }
        });
    }

    private void updateReadiness()
    {
        int remaining = (int)Math.ceil(Math.max(0, 72 - animationFrames.size()) * .75);
        if (remaining == readinessTenths)
            return;
        readinessTenths = remaining;
        if (remaining == 0)
            setStatus("Ready to store.", StatusTone.SUCCESS);
        else
            setStatus(String.format(java.util.Locale.ROOT, "Recording: %.1fs until ready", remaining / 10.0));
    }

    private PortraitScene captureHead()
    {
        try
        {
            return ChatHeadCapture.capture(client);
        }
        catch (RuntimeException error)
        {
            log.debug("Dedicated chat-head capture unavailable", error);
            return null;
        }
    }

    private void refresh()
    {
        if (!exportAllowed.getAsBoolean())
        {
            setStatus("Portraits disabled in settings.");
            return;
        }
        if (savingPose || loadingPoses)
            return;
        saveError = false;
        cancelOverwrite();
        long captureRevision = revision.incrementAndGet();
        clientThread.invoke(() -> {
            if (closed)
                return;
            try
            {
                PortraitScene captured = PortraitScene.capture(client);
                SwingUtilities.invokeLater(() -> {
                    if (closed || captureRevision != revision.get())
                        return;
                    animationFrames.clear();
                    posePlayback.stop();
                    activePose = java.util.Collections.emptyList();
                    scene = captured;
                    captureAnimationId = Integer.MIN_VALUE;
                    readinessTenths = -1;
                    if (animate.isSelected())
                    {
                        updateReadiness();
                        animationTimer.start();
                    }
                    loadPoses(captured.getAccountName());
                    yaw = Math.PI;
                    pitch = .12;
                    zoom = 1;
                    rotationOffset = 0;
                    updateRotationTimer();
                    if (!animate.isSelected())
                        setStatus("Ready to store.");
                    requestRender();
                });
            }
            catch (IllegalStateException exception)
            {
                SwingUtilities.invokeLater(() -> {
                    if (!closed)
                        setStatus(exception.getMessage());
                });
            }
            catch (RuntimeException exception)
            {
                log.debug("Could not copy portrait models", exception);
                SwingUtilities.invokeLater(() -> {
                    if (!closed)
                        setStatus("Model not ready. Try refreshing again.");
                });
            }
        });
    }

    private void requestRender()
    {
        if (scene == null || closed)
            return;
        pending.set(new RenderRequest(scene,
            yaw + (halfRotation.isSelected() ? Math.sin(rotationOffset) * Math.PI / 2 : rotationOffset),
            pitch, zoom, revision.incrementAndGet(), interacting));
        if (!rendering.compareAndSet(false, true))
            return;
        try
        {
            worker.execute(this::renderPending);
        }
        catch (RejectedExecutionException exception)
        {
            rendering.set(false);
        }
    }

    private void renderPending()
    {
        try
        {
            RenderRequest request;
            while (!closed && (request = pending.getAndSet(null)) != null)
            {
                RenderRequest rendered = request;
                BufferedImage image = request.interactive
                                          ? PortraitRenderer.renderPreview(
                                                request.scene, request.yaw, request.pitch, request.zoom)
                                          : PortraitRenderer.render(request.scene, request.yaw, request.pitch,
                                                request.zoom, 420, 520);
                SwingUtilities.invokeLater(() -> {
                    if (!closed && rendered.scene == scene && rendered.revision > displayedRevision)
                    {
                        displayedRevision = rendered.revision;
                        preview.image = image;
                        preview.repaint();
                    }
                });
            }
        }
        catch (RuntimeException exception)
        {
            log.debug("Could not render portrait", exception);
        }
        finally
        {
            rendering.set(false);
            SwingUtilities.invokeLater(() -> {
                if (!closed && pending.get() != null)
                    requestRender();
            });
        }
    }

    public void clear()
    {
        rotationTimer.stop();
        rotationOffset = 0;
        animationTimer.stop();
        animationFrames.clear();
        poses.clear();
        poseSavedAt.clear();
        pose.repaint();
        savingPose = false;
        loadingPoses = false;
        saveError = false;
        storePoseButton.setEnabled(true);
        cancelOverwrite();
        poseAnimations.clear();
        poseOptions.clear();
        poseCameras.clear();
        posePlayback.stop();
        activePose = java.util.Collections.emptyList();
        animate.setSelected(false);
        scene = null;
        settle.stop();
        interacting = false;
        pending.set(null);
        revision.incrementAndGet();
        preview.image = null;
        preview.repaint();
        if (!closed)
            setStatus("Waiting...");
    }

    private void cancelOverwrite()
    {
        overwriteSlot = null;
        storePoseButton.setText("Store pose");
        cancelOverwrite.setVisible(false);
        revalidate();
    }

    private void finishPoseSave(String message, StatusTone tone)
    {
        if (closed)
            return;
        savingPose = false;
        storePoseButton.setEnabled(true);
        setStatus(message, tone);
    }

    private void setStatus(String message)
    {
        setStatus(message, StatusTone.WAITING);
    }

    private void setStatus(String message, StatusTone tone)
    {
        if (saveError && tone != StatusTone.ERROR)
            return;
        if (savingPose && !message.startsWith("Saving "))
            return;
        if (tone == StatusTone.ERROR)
            saveError = true;
        SidebarLayout.updateStatus(status, message, tone);
    }

    @Override public void close()
    {
        closed = true;
        pending.set(null);
        revision.incrementAndGet();
        worker.shutdownNow();
        SwingUtilities.invokeLater(this::clear);
    }

    private static final class Preview extends JPanel
    {
        private BufferedImage image;

        Preview()
        {
            setOpaque(true);
            setBackground(ExportTheme.PANEL);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            // Keep the actual capture aspect visible inside the wider sidebar card.
            int frameHeight = Math.max(1, getHeight() - 12);
            int frameWidth =
                Math.min(Math.max(1, getWidth() - 12), (int)Math.round(frameHeight * 240.0 / 296));
            frameHeight = Math.min(frameHeight, (int)Math.round(frameWidth * 296.0 / 240));
            int frameX = (getWidth() - frameWidth) / 2;
            int frameY = (getHeight() - frameHeight) / 2;
            graphics.setColor(ExportTheme.INPUT);
            graphics.fillRect(frameX, frameY, frameWidth, frameHeight);
            if (image != null)
            {
                double ratio =
                    Math.min((double)frameWidth / image.getWidth(), (double)frameHeight / image.getHeight());
                int width = (int)Math.round(image.getWidth() * ratio);
                int height = (int)Math.round(image.getHeight() * ratio);
                graphics.drawImage(
                    image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
            }
            if (image == null)
            {
                graphics.setFont(getFont().deriveFont(14f));
                graphics.setColor(ExportTheme.GOLD);
                java.awt.FontMetrics metrics = graphics.getFontMetrics();
                String[] lines = {"Log in, then", "click Refresh"};
                for (int i = 0; i < lines.length; i++)
                    graphics.drawString(lines[i], (getWidth() - metrics.stringWidth(lines[i])) / 2,
                        getHeight() / 2 + (i * 18));
            }
            graphics.setColor(ExportTheme.PREVIEW_BORDER);
            graphics.drawRect(frameX, frameY, frameWidth - 1, frameHeight - 1);
        }
    }

    private static final class RenderRequest
    {
        final PortraitScene scene;
        final double yaw, pitch, zoom;
        final long revision;
        final boolean interactive;

        RenderRequest(
            PortraitScene scene, double yaw, double pitch, double zoom, long revision, boolean interactive)
        {
            this.scene = scene;
            this.yaw = yaw;
            this.pitch = pitch;
            this.zoom = zoom;
            this.revision = revision;
            this.interactive = interactive;
        }
    }
}
