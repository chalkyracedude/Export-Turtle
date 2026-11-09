package com.exportturtle.selfie;

import com.google.gson.Gson;
import com.exportturtle.snapshot.SnapshotStore;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.runelite.client.util.Filepath;

/** One atomic, compressed pose file per character and slot. */
final class PoseStore
{
    private static final int MAX_BYTES = 128 * 1024 * 1024;

    private static String filename(String account, String slot)
    {
        if (!slot.matches("Pose [123]"))
            throw new IllegalArgumentException("Unknown pose slot");
        return SnapshotStore.fileName(account).replace(".json", "-" + slot.charAt(5) + ".json.gz");
    }

    static void write(Filepath directory, String slot, List<PortraitScene> frames, Gson gson, boolean live,
        boolean rotation) throws IOException
    {
        write(directory, slot, frames, gson, live, rotation, Math.PI, .12, 1);
    }

    static void write(Filepath directory, String slot, List<PortraitScene> frames, Gson gson, boolean live,
        boolean rotation, double yaw, double pitch, double zoom) throws IOException
    {
        write(directory, slot, frames, gson, live, rotation, yaw, pitch, zoom, false);
    }

    static void write(Filepath directory, String slot, List<PortraitScene> frames, Gson gson, boolean live,
        boolean rotation, double yaw, double pitch, double zoom, boolean half) throws IOException
    {
        if (frames.size() != 1 && frames.size() != 72)
            throw new IllegalArgumentException("Invalid pose length");
        Data data = new Data();
        data.savedAt = java.time.Instant.now().toString();
        data.half = half;
        data.live = live;
        data.rotation = rotation;
        data.yaw = yaw;
        data.pitch = pitch;
        data.zoom = zoom;
        data.frames = frames.toArray(new PortraitScene[0]);
        IdentityHashMap<BufferedImage, Integer> images = new IdentityHashMap<>();
        for (PortraitScene scene : frames)
        {
            for (PortraitScene.Mesh mesh : scene.meshes)
            {
                int[] binding = new int[mesh.a.length];
                Arrays.fill(binding, -1);
                if (mesh.textures != null)
                    for (int face = 0; face < binding.length; face++)
                    {
                        BufferedImage image = mesh.textures[face];
                        if (image == null)
                            continue;
                        Integer index = images.get(image);
                        if (index == null)
                        {
                            index = data.pixels.size();
                            images.put(image, index);
                            data.pixels.add(image.getRGB(
                                0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
                        }
                        binding[face] = index;
                    }
                data.bindings.add(binding);
            }
        }
        directory.createDirectories();
        Filepath target = directory.joinSegment(filename(frames.get(0).getAccountName(), slot));
        Filepath temporary = directory.createTempFile("pose-", ".tmp");
        try
        {
            try (Writer writer = new OutputStreamWriter(
                     new GZIPOutputStream(temporary.openOutputStream()), StandardCharsets.UTF_8))
            {
                gson.toJson(data, writer);
            }
            try
            {
                temporary.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException error)
            {
                temporary.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            temporary.deleteIfExists();
        }
    }

    static SavedPose readPose(Filepath directory, String account, String slot, Gson gson) throws IOException
    {
        Data data;
        try (InputStream input =
                 new GZIPInputStream(directory.joinSegment(filename(account, slot)).openInputStream()))
        {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES)
                throw new IOException("Pose file exceeds size limit");
            data = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), Data.class);
        }
        catch (NoSuchFileException error)
        {
            return new SavedPose(Collections.emptyList(), false, false);
        }
        if (data == null || data.frames == null || (data.frames.length != 1 && data.frames.length != 72))
            throw new IOException("Invalid pose file");
        List<BufferedImage> images = new ArrayList<>();
        for (int[] pixels : data.pixels)
        {
            int size = (int)Math.sqrt(pixels.length);
            if (size < 1 || size > 256 || size * size != pixels.length)
                throw new IOException("Invalid pose texture");
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            int[] rgba = pixels.clone();
            for (int pixel = 0; pixel < rgba.length; pixel++)
                rgba[pixel] = (rgba[pixel] & 0xffffff) == 0 ? 0 : rgba[pixel] | 0xff000000;
            image.setRGB(0, 0, size, size, rgba, 0, size);
            images.add(image);
        }
        int binding = 0;
        String expected = SnapshotStore.fileName(account);
        for (PortraitScene scene : data.frames)
        {
            if (!expected.equals(SnapshotStore.fileName(scene.getAccountName())))
                throw new IOException("Pose account mismatch");
            for (PortraitScene.Mesh mesh : scene.meshes)
            {
                int[] map = data.bindings.get(binding++);
                if (map.length != mesh.a.length)
                    throw new IOException("Invalid pose material map");
                mesh.textures = new BufferedImage[map.length];
                for (int face = 0; face < map.length; face++)
                    if (map[face] >= 0)
                        mesh.textures[face] = images.get(map[face]);
            }
        }
        SavedPose pose =
            new SavedPose(Arrays.asList(data.frames), data.live == null ? data.frames.length > 1 : data.live,
                Boolean.TRUE.equals(data.rotation), finiteOr(data.yaw, Math.PI), finiteOr(data.pitch, .12),
                Math.max(.5, Math.min(1.6, finiteOr(data.zoom, 1))), data.savedAt);
        pose.half = Boolean.TRUE.equals(data.half);
        return pose;
    }

    private static double finiteOr(Double value, double fallback)
    {
        return value != null && Double.isFinite(value) ? value : fallback;
    }

    static final class SavedPose
    {
        final List<PortraitScene> frames;
        final boolean live, rotation;
        boolean half;
        final String savedAt;
        final double yaw, pitch, zoom;
        SavedPose(List<PortraitScene> frames, boolean live, boolean rotation)
        {
            this(frames, live, rotation, Math.PI, .12, 1);
        }
        SavedPose(
            List<PortraitScene> frames, boolean live, boolean rotation, double yaw, double pitch, double zoom)
        {
            this(frames, live, rotation, yaw, pitch, zoom, null);
        }
        SavedPose(List<PortraitScene> frames, boolean live, boolean rotation, double yaw, double pitch,
            double zoom, String savedAt)
        {
            this.savedAt = savedAt;
            this.frames = frames;
            this.live = live;
            this.rotation = rotation;
            this.yaw = yaw;
            this.pitch = pitch;
            this.zoom = zoom;
        }
    }

    private static final class Data
    {
        String savedAt;
        Boolean live, rotation, half;
        Double yaw, pitch, zoom;
        PortraitScene[] frames;
        List<int[]> pixels = new ArrayList<>();
        List<int[]> bindings = new ArrayList<>();
    }
}
