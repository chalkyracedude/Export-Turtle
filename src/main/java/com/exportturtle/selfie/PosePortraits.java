package com.exportturtle.selfie;

import java.io.IOException;
import java.util.List;
import net.runelite.client.util.Filepath;

/** Render saved slots and optionally update the latest portrait files. */
final class PosePortraits
{
    static void write(Filepath directory, String slot, List<PortraitScene> frames, boolean live,
        boolean rotate) throws IOException
    {
        write(directory, slot, frames, live, rotate, Math.PI, .12, 1);
    }

    static void write(Filepath directory, String slot, List<PortraitScene> frames, boolean live,
        boolean rotate, double yaw, double pitch, double zoom) throws IOException
    {
        write(directory, slot, frames, live, rotate, yaw, pitch, zoom, false);
    }

    static void write(Filepath directory, String slot, List<PortraitScene> frames, boolean live,
        boolean rotate, double yaw, double pitch, double zoom, boolean latest) throws IOException
    {
        write(directory, slot, frames, live, rotate, yaw, pitch, zoom, latest, false);
    }

    static void write(Filepath directory, String slot, List<PortraitScene> frames, boolean live,
        boolean rotate, double yaw, double pitch, double zoom, boolean latest, boolean half)
        throws IOException
    {
        if (!slot.matches("Pose [123]") || frames.isEmpty())
            throw new IllegalArgumentException("Invalid pose slot");
        PortraitScene scene = frames.get(0);
        String suffix = ".pose" + slot.charAt(5);
        java.awt.image.BufferedImage still = PortraitRenderer.render(scene, yaw, pitch, zoom, 600, 740);
        PortraitStore.write(directory, scene.getAccountName(), still, suffix + ".png");
        if (latest)
            PortraitStore.write(directory, scene.getAccountName(), still);
        if (rotate || live && frames.size() == 72)
        {
            java.awt.image.BufferedImage atlas =
                live && frames.size() == 72
                    ? PortraitRenderer.renderAnimation(frames, yaw, pitch, zoom, rotate, half)
                    : PortraitRenderer.renderRotation(scene, yaw, pitch, zoom, half);
            PortraitStore.write(directory, scene.getAccountName(), atlas, suffix + ".360.png");
            if (latest)
                PortraitStore.write(directory, scene.getAccountName(), atlas, ".360.png");
        }
        else
        {
            if (latest)
                PortraitStore.removeRotation(directory, scene.getAccountName());
            directory
                .joinSegment(com.exportturtle.snapshot.SnapshotStore.fileName(scene.getAccountName())
                        .replace(".json", suffix + ".360.png"))
                .deleteIfExists();
        }
    }
}
