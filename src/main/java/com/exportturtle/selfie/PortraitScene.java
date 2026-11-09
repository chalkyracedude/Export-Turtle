package com.exportturtle.selfie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.TextureProvider;

/** Immutable geometry copied on the client thread; never retains live actors or models. */
public final class PortraitScene
{
    final List<Mesh> meshes;
    boolean dedicatedHead;
    private final String accountName;
    private final String petName;
    final PortraitScene cameraReference;

    private PortraitScene(String accountName, String petName, List<Mesh> meshes)
    {
        this(accountName, petName, meshes, null);
    }

    private PortraitScene(
        String accountName, String petName, List<Mesh> meshes, PortraitScene cameraReference)
    {
        this.cameraReference = cameraReference;
        this.accountName = accountName;
        this.petName = petName;
        this.meshes = Collections.unmodifiableList(new ArrayList<>(meshes));
    }

    static PortraitScene chatHead(String name, Model model, TextureProvider textures)
    {
        PortraitScene result =
            new PortraitScene(name, null, Collections.singletonList(Mesh.copy(model, textures, 0)));
        result.dedicatedHead = true;
        return result;
    }

    public static PortraitScene capture(Client client)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            throw new IllegalStateException("Log in before refreshing your portrait.");
        }
        String name = client.getLocalPlayer().getName();
        Model player = client.getLocalPlayer().getModel();
        if (name == null || player == null)
        {
            throw new IllegalStateException("Your character model is not ready yet.");
        }
        List<Mesh> meshes = new ArrayList<>();
        Mesh avatar = Mesh.copy(player, client.getTextureProvider(), 0);
        meshes.add(avatar);
        NPC follower = client.getFollower();
        String petName = null;
        if (follower != null)
        {
            Model pet = follower.getModel();
            if (pet == null)
            {
                throw new IllegalStateException("Your pet model is not ready. Try again shortly.");
            }
            Mesh petMesh = Mesh.copy(pet, client.getTextureProvider(), 0).beside(avatar);
            meshes.add(petMesh);
            petName = follower.getName();
        }
        return new PortraitScene(name, petName, meshes);
    }

    public PortraitScene withStableCamera(PortraitScene previous)
    {
        PortraitScene reference = previous.cameraReference == null ? previous : previous.cameraReference;
        List<Mesh> stable = new ArrayList<>(meshes);
        if (stable.size() > 1 && reference.meshes.size() > 1)
        {
            Mesh pet = stable.get(1);
            stable.set(1, pet.atOffset(reference.meshes.get(1).offset));
        }
        return new PortraitScene(accountName, petName, stable, reference);
    }

    public PortraitScene headOnly()
    {
        Mesh source = meshes.get(0);
        List<Integer> faces = new ArrayList<>();
        for (int face = 0; face < source.a.length; face++)
        {
            int a = source.a[face], b = source.b[face], c = source.c[face];
            if (headVertex(source, a) && headVertex(source, b) && headVertex(source, c))
                faces.add(face);
        }
        int count = faces.size();
        float[] x = new float[count * 3], y = new float[count * 3], z = new float[count * 3];
        int[] a = new int[count], b = new int[count], c = new int[count];
        int[] colors = new int[count], alpha = new int[count];
        for (int i = 0; i < count; i++)
        {
            int face = faces.get(i);
            int[] vertices = {source.a[face], source.b[face], source.c[face]};
            for (int corner = 0; corner < 3; corner++)
            {
                int target = i * 3 + corner, vertex = vertices[corner];
                x[target] = source.x[vertex];
                y[target] = source.y[vertex];
                z[target] = source.z[vertex];
            }
            a[i] = i * 3;
            b[i] = i * 3 + 1;
            c[i] = i * 3 + 2;
            colors[i] = source.colors[face];
            alpha[i] = source.alpha[face];
        }
        return new PortraitScene(
            accountName, null, Collections.singletonList(new Mesh(x, y, z, a, b, c, colors, alpha, 0)));
    }

    private static boolean headVertex(Mesh mesh, int vertex)
    {
        // Human-avatar head volume; excludes shoulders and separately held equipment.
        return mesh.y[vertex] <= -155 && Math.abs(mesh.x[vertex]) <= 35 && Math.abs(mesh.z[vertex]) <= 40;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public String getPetName()
    {
        return petName;
    }

    static final class Mesh
    {
        final float[] x, y, z;
        final int[] a, b, c, colors, alpha;
        final float offset;
        byte[] depthBias;
        byte[] renderPriorities;
        transient java.awt.image.BufferedImage[] textures;
        int[] textureA, textureB, textureC;

        Mesh(float[] x, float[] y, float[] z, int[] a, int[] b, int[] c, int[] colors, int[] alpha,
            float offset)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.a = a;
            this.b = b;
            this.c = c;
            this.colors = colors;
            this.alpha = alpha;
            this.offset = offset;
        }

        static Mesh copy(Model model, TextureProvider textures, float offset)
        {
            int vertices = model.getVerticesCount();
            int faces = model.getFaceCount();
            if (vertices < 1 || vertices > 50000 || faces < 1 || faces > 100000)
            {
                throw new IllegalStateException("This model is not available for a portrait.");
            }
            int[] colors = new int[faces];
            int[] alpha = new int[faces];
            int[] lit = model.getFaceColors1();
            int[] shaded = model.getFaceColors3();
            short[] faceTextures = model.getFaceTextures();
            byte[] transparencies = model.getFaceTransparencies();
            for (int i = 0; i < faces; i++)
            {
                int color = lit[i];
                if (faceTextures != null && faceTextures[i] != -1 && textures != null)
                {
                    color = textures.getDefaultColor(faceTextures[i] & 0xffff);
                }
                colors[i] = PortraitRenderer.hslToRgb(color);
                alpha[i] = shaded[i] == -2          ? 0
                           : transparencies == null ? 255
                                                    : 255 - (transparencies[i] & 0xff);
            }
            Mesh result = new Mesh(Arrays.copyOf(model.getVerticesX(), vertices),
                Arrays.copyOf(model.getVerticesY(), vertices), Arrays.copyOf(model.getVerticesZ(), vertices),
                Arrays.copyOf(model.getFaceIndices1(), faces), Arrays.copyOf(model.getFaceIndices2(), faces),
                Arrays.copyOf(model.getFaceIndices3(), faces), colors, alpha, offset);
            byte[] priorities = model.getFaceRenderPriorities();
            result.renderPriorities = priorities == null ? null : Arrays.copyOf(priorities, faces);
            byte[] bias = model.getFaceBias();
            result.depthBias = bias == null ? null : Arrays.copyOf(bias, faces);
            result.textures = new java.awt.image.BufferedImage[faces];
            result.textureA = result.a.clone();
            result.textureB = result.b.clone();
            result.textureC = result.c.clone();
            Map<Integer, java.awt.image.BufferedImage> images = new java.util.HashMap<>();
            byte[] mapping = model.getTextureFaces();
            for (int face = 0; face < faces; face++)
            {
                if (faceTextures == null || faceTextures[face] == -1 || textures == null)
                    continue;
                int id = faceTextures[face] & 0xffff;
                java.awt.image.BufferedImage image = images.get(id);
                if (image == null)
                {
                    int[] pixels = textures.load(id);
                    if (pixels == null)
                        continue;
                    int side = (int)Math.sqrt(pixels.length);
                    if (side < 1 || side * side != pixels.length)
                        continue;
                    image = new java.awt.image.BufferedImage(
                        side, side, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    int[] rgba = pixels.clone();
                    for (int pixel = 0; pixel < rgba.length; pixel++)
                        rgba[pixel] = (rgba[pixel] & 0xffffff) == 0 ? 0 : rgba[pixel] | 0xff000000;
                    image.setRGB(0, 0, side, side, rgba, 0, side);
                    images.put(id, image);
                }
                result.textures[face] = image;
                if (mapping != null && mapping[face] != -1 && model.getTexIndices1() != null)
                {
                    int index = mapping[face] & 0xff;
                    result.textureA[face] = model.getTexIndices1()[index];
                    result.textureB[face] = model.getTexIndices2()[index];
                    result.textureC[face] = model.getTexIndices3()[index];
                }
            }
            return result;
        }

        Mesh beside(Mesh avatar)
        {
            float avatarRight = Float.NEGATIVE_INFINITY;
            float petLeft = Float.POSITIVE_INFINITY;
            for (float value : avatar.x)
                avatarRight = Math.max(avatarRight, value);
            for (float value : x)
                petLeft = Math.min(petLeft, value);
            return atOffset(avatarRight - petLeft + 24);
        }

        Mesh atOffset(float value)
        {
            Mesh result = new Mesh(x, y, z, a, b, c, colors, alpha, value);
            result.depthBias = depthBias;
            result.renderPriorities = renderPriorities;
            result.textures = textures;
            result.textureA = textureA;
            result.textureB = textureB;
            result.textureC = textureC;
            return result;
        }
    }
}
