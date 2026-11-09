package com.exportturtle.selfie;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Orthographic mesh renderer with captured textures and per-pixel depth. */
public final class PortraitRenderer
{
    private PortraitRenderer()
    {
    }

    public static BufferedImage render(
        PortraitScene scene, double yaw, double pitch, double zoom, int width, int height)
    {
        return render(scene, yaw, pitch, zoom, width, height, true, scene.cameraReference != null, false);
    }

    public static BufferedImage renderPreview(PortraitScene scene, double yaw, double pitch, double zoom)
    {
        return render(scene, yaw, pitch, zoom, 210, 260, false, scene.cameraReference != null, false);
    }

    public static BufferedImage renderHeadshot(PortraitScene scene)
    {
        if (scene.dedicatedHead)
            return render(scene, Math.PI + .18, 0, 1, 240, 240, true, false, false);
        // Legacy pose support; new captures use dedicated head assets.
        BufferedImage image = render(scene.headOnly(), Math.PI + .18, 0, 1, 240, 240, true, false, true);
        removeDetachedFragments(image);
        return image;
    }

    static void removeDetachedFragments(BufferedImage image)
    {
        int width = image.getWidth(), height = image.getHeight();
        int[] labels = new int[width * height], queue = new int[labels.length];
        int next = 0, largest = 0, largestSize = 0;
        for (int start = 0; start < labels.length; start++)
        {
            if (labels[start] != 0 || (image.getRGB(start % width, start / width) >>> 24) == 0)
                continue;
            int label = ++next, read = 0, write = 1;
            queue[0] = start;
            labels[start] = label;
            while (read < write)
            {
                int current = queue[read++], x = current % width, y = current / width;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                    {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height)
                            continue;
                        int index = ny * width + nx;
                        if (labels[index] == 0 && (image.getRGB(nx, ny) >>> 24) != 0)
                        {
                            labels[index] = label;
                            queue[write++] = index;
                        }
                    }
            }
            if (write > largestSize)
            {
                largestSize = write;
                largest = label;
            }
        }
        for (int i = 0; i < labels.length; i++)
            if (labels[i] != largest)
                image.setRGB(i % width, i / width, 0);
    }

    public static BufferedImage renderAnimation(
        List<PortraitScene> frames, double yaw, double pitch, double zoom, boolean rotate)
    {
        return renderAnimation(frames, yaw, pitch, zoom, rotate, false);
    }

    public static BufferedImage renderAnimation(
        List<PortraitScene> frames, double yaw, double pitch, double zoom, boolean rotate, boolean half)
    {
        if (frames.size() != 72)
            throw new IllegalArgumentException("Expected 72 animation frames");
        BufferedImage atlas = new BufferedImage(2880, 1776, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        try
        {
            for (int frame = 0; frame < 72; frame++)
            {
                if (Thread.currentThread().isInterrupted())
                    throw new java.util.concurrent.CancellationException();
                BufferedImage image =
                    render(frames.get(frame), yaw + (rotate ? rotationAngle(frame, half) : 0), pitch, zoom,
                        240, 296, true, rotate, false);
                graphics.drawImage(image, frame % 12 * 240, frame / 12 * 296, null);
            }
        }
        finally
        {
            graphics.dispose();
        }
        return atlas;
    }

    static double rotationAngle(int frame, boolean half)
    {
        return half ? Math.sin(frame * Math.PI / 36) * Math.PI / 2 : frame * Math.PI / 36;
    }

    public static BufferedImage renderRotation(PortraitScene scene, double yaw, double pitch, double zoom)
    {
        return renderRotation(scene, yaw, pitch, zoom, false);
    }

    public static BufferedImage renderRotation(
        PortraitScene scene, double yaw, double pitch, double zoom, boolean half)
    {
        BufferedImage atlas = new BufferedImage(2880, 1776, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        try
        {
            for (int frame = 0; frame < 72; frame++)
            {
                if (Thread.currentThread().isInterrupted())
                    throw new java.util.concurrent.CancellationException();
                BufferedImage angle =
                    render(scene, yaw + rotationAngle(frame, half), pitch, zoom, 240, 296, true, true, false);
                graphics.drawImage(angle, frame % 12 * 240, frame / 12 * 296, null);
            }
        }
        finally
        {
            graphics.dispose();
        }
        return atlas;
    }

    private static BufferedImage render(PortraitScene scene, double yaw, double pitch, double zoom, int width,
        int height, boolean antialias, boolean turntable, boolean headshot)
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
            List<Face> faces = new ArrayList<>();
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            double sinYaw = Math.sin(yaw), cosYaw = Math.cos(yaw);
            double sinPitch = Math.sin(pitch), cosPitch = Math.cos(pitch);
            int modelIndex = 0;
            for (PortraitScene.Mesh mesh : scene.meshes)
            {
                int model = modelIndex++;
                double[] x = new double[mesh.x.length], y = new double[x.length], z = new double[x.length];
                for (int i = 0; i < x.length; i++)
                {
                    double horizontal = mesh.x[i] + mesh.offset;
                    double depth = mesh.z[i];
                    x[i] = horizontal * cosYaw + depth * sinYaw;
                    double rotatedZ = depth * cosYaw - horizontal * sinYaw;
                    y[i] = mesh.y[i] * cosPitch - rotatedZ * sinPitch;
                    z[i] = mesh.y[i] * sinPitch + rotatedZ * cosPitch;
                    minX = Math.min(minX, x[i]);
                    maxX = Math.max(maxX, x[i]);
                    minY = Math.min(minY, y[i]);
                    maxY = Math.max(maxY, y[i]);
                }
                for (int i = 0; i < mesh.a.length; i++)
                {
                    if (mesh.alpha[i] == 0)
                        continue;
                    int a = mesh.a[i], b = mesh.b[i], c = mesh.c[i];
                    Face face = new Face(new double[] {x[a], x[b], x[c]}, new double[] {y[a], y[b], y[c]},
                        (z[a] + z[b] + z[c]) / 3, mesh.colors[i], mesh.alpha[i]);
                    face.model = model;
                    face.priority = mesh.renderPriorities == null ? 0 : mesh.renderPriorities[i] & 0xff;
                    face.z = new double[] {z[a], z[b], z[c]};
                    face.bias = mesh.depthBias == null ? 0 : (mesh.depthBias[i] & 0xff) / 128.0;
                    if (mesh.textures != null && mesh.textures[i] != null)
                    {
                        face.texture = mesh.textures[i];
                        int ta = mesh.textureA[i], tb = mesh.textureB[i], tc = mesh.textureC[i];
                        double[] coordinates = textureCoordinates(mesh, new int[] {a, b, c}, ta, tb, tc);
                        if (coordinates == null)
                            face.texture = null;
                        else
                        {
                            double size = face.texture.getWidth();
                            face.tx = new double[] {
                                coordinates[0] * size, coordinates[2] * size, coordinates[4] * size};
                            face.ty = new double[] {
                                coordinates[1] * size, coordinates[3] * size, coordinates[5] * size};
                        }
                    }
                    faces.add(face);
                }
            }
            PortraitScene framing = scene.cameraReference == null ? scene : scene.cameraReference;
            minX = minY = Double.POSITIVE_INFINITY;
            maxX = maxY = Double.NEGATIVE_INFINITY;
            for (PortraitScene.Mesh mesh : framing.meshes)
            {
                for (int i = 0; i < mesh.x.length; i++)
                {
                    double horizontal = mesh.x[i] + mesh.offset;
                    double x = horizontal * cosYaw + mesh.z[i] * sinYaw;
                    double y = mesh.y[i] * cosPitch - (mesh.z[i] * cosYaw - horizontal * sinYaw) * sinPitch;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
            double scale =
                Math.min((width - 32) / Math.max(1, maxX - minX), (height - 40) / Math.max(1, maxY - minY)) *
                zoom;
            double centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2;
            if (turntable)
            {
                // Fixed camera bounds prevent the character from growing and shrinking during a turn.
                double left = Double.POSITIVE_INFINITY, right = Double.NEGATIVE_INFINITY;
                double near = Double.POSITIVE_INFINITY, far = Double.NEGATIVE_INFINITY;
                double top = Double.POSITIVE_INFINITY, bottom = Double.NEGATIVE_INFINITY;
                for (PortraitScene.Mesh mesh : framing.meshes)
                {
                    for (int i = 0; i < mesh.x.length; i++)
                    {
                        left = Math.min(left, mesh.x[i] + mesh.offset);
                        right = Math.max(right, mesh.x[i] + mesh.offset);
                        near = Math.min(near, mesh.z[i]);
                        far = Math.max(far, mesh.z[i]);
                        top = Math.min(top, mesh.y[i]);
                        bottom = Math.max(bottom, mesh.y[i]);
                    }
                }
                double worldX = (left + right) / 2, worldZ = (near + far) / 2;
                double radius = Math.hypot(right - left, far - near) / 2;
                scale = Math.min((width - 32) / Math.max(1, 2 * radius),
                            (height - 40) / Math.max(1, (bottom - top) * Math.abs(cosPitch) +
                                                            2 * radius * Math.abs(sinPitch))) *
                        zoom;
                centerX = worldX * cosYaw + worldZ * sinYaw;
                centerY = (top + bottom) / 2 * cosPitch - (worldZ * cosYaw - worldX * sinYaw) * sinPitch;
            }
            if (headshot)
            {
                // Human avatar coordinates: frame the face and headwear, above the shoulders.
                // Fixed framing also keeps tall weapons from shrinking the face.
                centerX = 0;
                centerY = -185;
                scale = Math.min(width, height) / 105.0;
            }
            faces.sort(Comparator.comparingDouble(face -> face.depth));
            double[] depth = new double[width * height];
            java.util.Arrays.fill(depth, Double.NEGATIVE_INFINITY);
            int[] owners = new int[width * height], priorities = new int[width * height];
            java.util.Arrays.fill(owners, -1);
            for (Face face : faces)
                rasterize(image, depth, owners, priorities, face, scale, centerX, centerY);
        }
        finally
        {
            graphics.dispose();
        }
        return image;
    }

    private static void rasterize(BufferedImage image, double[] depth, int[] owners, int[] priorities,
        Face face, double scale, double centerX, double centerY)
    {
        int width = image.getWidth(), height = image.getHeight();
        double[] x = new double[3], y = new double[3];
        for (int i = 0; i < 3; i++)
        {
            x[i] = width / 2.0 + (face.x[i] - centerX) * scale;
            y[i] = height / 2.0 + (face.y[i] - centerY) * scale;
        }
        double area = (x[1] - x[0]) * (y[2] - y[0]) - (y[1] - y[0]) * (x[2] - x[0]);
        if (area <= 1e-8)
            return;
        int left = Math.max(0, (int)Math.floor(Math.min(x[0], Math.min(x[1], x[2]))));
        int right = Math.min(width - 1, (int)Math.ceil(Math.max(x[0], Math.max(x[1], x[2]))));
        int top = Math.max(0, (int)Math.floor(Math.min(y[0], Math.min(y[1], y[2]))));
        int bottom = Math.min(height - 1, (int)Math.ceil(Math.max(y[0], Math.max(y[1], y[2]))));
        for (int py = top; py <= bottom; py++)
            for (int px = left; px <= right; px++)
            {
                double sx = px + .5, sy = py + .5;
                double a = ((x[1] - sx) * (y[2] - sy) - (y[1] - sy) * (x[2] - sx)) / area;
                double b = ((x[2] - sx) * (y[0] - sy) - (y[2] - sy) * (x[0] - sx)) / area;
                double c = 1 - a - b;
                if (a < -1e-8 || b < -1e-8 || c < -1e-8)
                    continue;
                int index = py * width + px;
                double z = a * face.z[0] + b * face.z[1] + c * face.z[2] - face.bias;
                boolean sameModel = owners[index] == face.model;
                // Regular equipment layers take priority within one avatar; keep other
                // actors and the special distance-sorted groups on geometric depth.
                boolean layered = sameModel && face.priority < 10 && priorities[index] < 10;
                if (layered && face.priority < priorities[index])
                    continue;
                if ((!layered || face.priority == priorities[index]) && z < depth[index] - 1e-6)
                    continue;
                int color = face.color, alpha = face.alpha;
                if (face.texture != null)
                {
                    int u = Math.floorMod((int)Math.floor(a * face.tx[0] + b * face.tx[1] + c * face.tx[2]),
                        face.texture.getWidth());
                    int v = Math.floorMod((int)Math.floor(a * face.ty[0] + b * face.ty[1] + c * face.ty[2]),
                        face.texture.getHeight());
                    int texel = face.texture.getRGB(u, v);
                    alpha = alpha * (texel >>> 24) / 255;
                    color = texel & 0xffffff;
                }
                if (alpha == 0)
                    continue;
                if (alpha == 255)
                {
                    image.setRGB(px, py, 0xff000000 | color);
                    depth[index] = z;
                    owners[index] = face.model;
                    priorities[index] = face.priority;
                }
                else
                {
                    int previous = image.getRGB(px, py), oldAlpha = previous >>> 24;
                    int outAlpha = alpha + oldAlpha * (255 - alpha) / 255;
                    int blended = outAlpha << 24;
                    for (int shift : new int[] {16, 8, 0})
                    {
                        int channel = (((color >> shift) & 255) * alpha +
                                          ((previous >> shift) & 255) * oldAlpha * (255 - alpha) / 255) /
                                      outAlpha;
                        blended |= channel << shift;
                    }
                    image.setRGB(px, py, blended);
                }
            }
    }

    static double[] textureCoordinates(PortraitScene.Mesh mesh, int[] face, int a, int b, int c)
    {
        double ux = mesh.x[b] - mesh.x[a], uy = mesh.y[b] - mesh.y[a], uz = mesh.z[b] - mesh.z[a];
        double vx = mesh.x[c] - mesh.x[a], vy = mesh.y[c] - mesh.y[a], vz = mesh.z[c] - mesh.z[a];
        double uu = ux * ux + uy * uy + uz * uz;
        double vv = vx * vx + vy * vy + vz * vz;
        double uv = ux * vx + uy * vy + uz * vz;
        double determinant = uu * vv - uv * uv;
        if (Math.abs(determinant) < 1e-8)
            return null;
        double[] coordinates = new double[6];
        for (int i = 0; i < 3; i++)
        {
            int vertex = face[i];
            double dx = mesh.x[vertex] - mesh.x[a], dy = mesh.y[vertex] - mesh.y[a],
                   dz = mesh.z[vertex] - mesh.z[a];
            double du = dx * ux + dy * uy + dz * uz;
            double dv = dx * vx + dy * vy + dz * vz;
            coordinates[i * 2] = (du * vv - dv * uv) / determinant;
            coordinates[i * 2 + 1] = (dv * uu - du * uv) / determinant;
        }
        return coordinates;
    }

    static int hslToRgb(int packed)
    {
        double hue = ((packed >> 10) & 63) / 64.0 + 1.0 / 128;
        double saturation = ((packed >> 7) & 7) / 8.0 + 1.0 / 16;
        double lightness = (packed & 127) / 128.0;
        double chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
        double segment = hue * 6;
        double x = chroma * (1 - Math.abs(segment % 2 - 1));
        double red = 0, green = 0, blue = 0;
        if (segment < 1)
        {
            red = chroma;
            green = x;
        }
        else if (segment < 2)
        {
            red = x;
            green = chroma;
        }
        else if (segment < 3)
        {
            green = chroma;
            blue = x;
        }
        else if (segment < 4)
        {
            green = x;
            blue = chroma;
        }
        else if (segment < 5)
        {
            red = x;
            blue = chroma;
        }
        else
        {
            red = chroma;
            blue = x;
        }
        double m = lightness - chroma / 2;
        return new Color((float)(red + m), (float)(green + m), (float)(blue + m)).getRGB() & 0xffffff;
    }

    private static final class Face
    {
        final double[] x, y;
        final double depth;
        final int color, alpha;
        BufferedImage texture;
        double[] tx, ty, z;
        double bias;
        int model, priority;

        Face(double[] x, double[] y, double depth, int color, int alpha)
        {
            this.x = x;
            this.y = y;
            this.depth = depth;
            this.color = color;
            this.alpha = alpha;
        }
    }
}
