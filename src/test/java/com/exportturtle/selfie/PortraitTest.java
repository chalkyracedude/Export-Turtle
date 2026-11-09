package com.exportturtle.selfie;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PortraitTest
{
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Model model()
    {
        Model model = mock(Model.class);
        when(model.getVerticesCount()).thenReturn(3);
        when(model.getFaceCount()).thenReturn(1);
        when(model.getVerticesX()).thenReturn(new float[] {-30, 30, 0, 9999});
        when(model.getVerticesY()).thenReturn(new float[] {0, 0, -100, 9999});
        when(model.getVerticesZ()).thenReturn(new float[] {0, 0, 10, 9999});
        when(model.getFaceIndices1()).thenReturn(new int[] {0});
        when(model.getFaceIndices2()).thenReturn(new int[] {1});
        when(model.getFaceIndices3()).thenReturn(new int[] {2});
        when(model.getFaceColors1()).thenReturn(new int[] {12345});
        when(model.getFaceColors3()).thenReturn(new int[] {12345});
        return model;
    }

    private Client client(Model model)
    {
        Client client = mock(Client.class);
        Player player = mock(Player.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test Player");
        when(player.getModel()).thenReturn(model);
        return client;
    }

    @Test public void copiesOnlyActiveGeometryAndIncludesFollowingPet()
    {
        Model model = model();
        Client client = client(model);
        NPC pet = mock(NPC.class);
        when(client.getFollower()).thenReturn(pet);
        Model petModel = model();
        when(pet.getModel()).thenReturn(petModel);
        when(pet.getName()).thenReturn("Pet rock");
        PortraitScene scene = PortraitScene.capture(client);
        assertEquals(2, scene.meshes.size());
        assertEquals("Pet rock", scene.getPetName());
        assertEquals(3, scene.meshes.get(0).x.length);
        model.getVerticesX()[0] = 999;
        assertEquals(-30, scene.meshes.get(0).x[0], 0);
        assertTrue(scene.meshes.get(1).offset > 0);
        PortraitScene moved = PortraitScene.capture(client).withStableCamera(scene);
        assertSame(scene, moved.cameraReference);
        assertEquals(scene.meshes.get(1).offset, moved.meshes.get(1).offset, 0);
        PortraitScene next = PortraitScene.capture(client).withStableCamera(moved);
        assertSame(scene, next.cameraReference);
    }

    @Test public void refusesLoggedOutAndUnloadedPets()
    {
        Client client = client(model());
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        assertThrows(IllegalStateException.class, () -> PortraitScene.capture(client));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getFollower()).thenReturn(mock(NPC.class));
        assertThrows(IllegalStateException.class, () -> PortraitScene.capture(client));
    }

    @Test public void dedicatedChatHeadLoadsOnlyHeadAssets()
    {
        Client client = mock(Client.class);
        Player player = mock(Player.class);
        net.runelite.api.PlayerComposition appearance = mock(net.runelite.api.PlayerComposition.class);
        net.runelite.api.IndexDataBase config = mock(net.runelite.api.IndexDataBase.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test Player");
        when(player.getPlayerComposition()).thenReturn(appearance);
        when(appearance.getTransformedNpcId()).thenReturn(-1);
        when(appearance.getEquipmentId(any())).thenReturn(-1);
        when(appearance.getKitId(any())).thenReturn(-1);
        when(appearance.getEquipmentId(net.runelite.api.kit.KitType.HEAD)).thenReturn(900);
        when(client.getIndexConfig()).thenReturn(config);
        when(config.loadData(10, 900)).thenReturn(new byte[] {90, 0, 42, 0});
        net.runelite.api.ModelData data = mock(net.runelite.api.ModelData.class);
        when(client.loadModelData(42)).thenReturn(data);
        when(data.shallowCopy()).thenReturn(data);
        when(data.cloneColors()).thenReturn(data);
        when(data.cloneTextures()).thenReturn(data);
        when(client.mergeModels(any(net.runelite.api.ModelData[].class))).thenReturn(data);
        Model headModel = model();
        when(data.light()).thenReturn(headModel);
        PortraitScene head = ChatHeadCapture.capture(client);
        assertTrue(head.dedicatedHead);
        assertEquals(1, head.meshes.size());
        assertNull(head.getPetName());
        verify(player, never()).getModel();
        verify(client).loadModelData(42);
        verify(data, never()).cloneTextures();
        verify(data, never()).cloneColors();
    }

    @Test public void chatHeadDefinitionsSelectGenderAndSkipBodyModels()
    {
        byte[] item = {23, 0, 12, 0, 90, 0, 42, 91, 0, 43, 92, 0, 44, 93, 0, 45, 0};
        assertEquals(java.util.Arrays.asList(42, 44), ChatHeadDefinition.item(item, false).models);
        assertEquals(java.util.Arrays.asList(43, 45), ChatHeadDefinition.item(item, true).models);
        assertEquals(java.util.Collections.singletonList(42),
            ChatHeadDefinition.kit(new byte[] {2, 1, 0, 12, 60, 0, 42, 0}).models);
        assertThrows(
            IllegalArgumentException.class, () -> ChatHeadDefinition.item(new byte[] {(byte)250, 0}, false));
    }

    @Test public void headshotRemovesDetachedWeaponFragments()
    {
        BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        for (int y = 5; y < 20; y++)
            for (int x = 5; x < 20; x++)
                image.setRGB(x, y, 0xff998877);
        image.setRGB(25, 1, 0xff112233);
        image.setRGB(26, 2, 0xff112233);
        PortraitRenderer.removeDetachedFragments(image);
        assertEquals(0xff998877, image.getRGB(10, 10));
        assertEquals(0, image.getRGB(25, 1));
        assertEquals(0, image.getRGB(26, 2));
    }

    @Test public void headshotFramesTheFaceWithoutTorsoOrPet()
    {
        Model avatar = model();
        when(avatar.getVerticesCount()).thenReturn(6);
        when(avatar.getFaceCount()).thenReturn(2);
        when(avatar.getVerticesX()).thenReturn(new float[] {-20, 20, 0, -30, 30, 0});
        when(avatar.getVerticesY()).thenReturn(new float[] {-200, -200, -160, -120, -120, -60});
        when(avatar.getVerticesZ()).thenReturn(new float[] {0, 0, 0, 0, 0, 0});
        when(avatar.getFaceIndices1()).thenReturn(new int[] {0, 3});
        when(avatar.getFaceIndices2()).thenReturn(new int[] {2, 5});
        when(avatar.getFaceIndices3()).thenReturn(new int[] {1, 4});
        when(avatar.getFaceColors1()).thenReturn(new int[] {12345, 23456});
        when(avatar.getFaceColors3()).thenReturn(new int[] {12345, 23456});
        BufferedImage image = PortraitRenderer.renderHeadshot(PortraitScene.capture(client(avatar)));
        assertEquals(240, image.getWidth());
        assertEquals(240, image.getHeight());
        assertEquals(PortraitRenderer.hslToRgb(12345), image.getRGB(120, 120) & 0xffffff);
        assertEquals(0, image.getRGB(120, 239) >>> 24);
    }

    @Test public void storedPosesReloadAndOverwriteWithoutAccumulatingFiles() throws Exception
    {
        PortraitScene scene = PortraitScene.capture(client(model()));
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        com.google.gson.Gson gson = new com.google.gson.Gson();
        PoseStore.write(directory, "Pose 1", java.util.Collections.singletonList(scene), gson, false, false);
        assertEquals(1, PoseStore.readPose(directory, "test_player", "Pose 1", gson).frames.size());
        PoseStore.write(directory, "Pose 1", java.util.Collections.nCopies(72, scene), gson, true, false);
        java.util.List<PortraitScene> loaded =
            PoseStore.readPose(directory, "Test Player", "Pose 1", gson).frames;
        assertEquals(72, loaded.size());
        PoseStore.write(directory, "Pose 1", loaded, gson, true, true, 1.7, .3, 1.25);
        PoseStore.SavedPose settings = PoseStore.readPose(directory, "Test Player", "Pose 1", gson);
        assertNotNull(settings.savedAt);
        java.time.Instant.parse(settings.savedAt);
        assertTrue(settings.live);
        assertTrue(settings.rotation);
        assertEquals(1.7, settings.yaw, .00001);
        assertEquals(.3, settings.pitch, .00001);
        assertEquals(1.25, settings.zoom, .00001);
        PoseStore.write(directory, "Pose 1", loaded, gson, false, false);
        settings = PoseStore.readPose(directory, "Test Player", "Pose 1", gson);
        assertFalse(settings.live);
        assertFalse(settings.rotation);
        assertEquals(Math.PI, settings.yaw, .00001);
        assertEquals(.12, settings.pitch, .00001);
        assertEquals(1, settings.zoom, .00001);
        assertEquals(3, loaded.get(0).meshes.get(0).x.length);
        assertTrue(PoseStore.readPose(directory, "Other", "Pose 1", gson).frames.isEmpty());
        try (var files = directory.walk(1))
        {
            assertEquals(1, files.filter(file -> file.getFileName().endsWith(".json.gz")).count());
        }
    }

    @Test public void equipmentPriorityKeepsCapeAboveOverlappingArmor()
    {
        Model avatar = model();
        when(avatar.getVerticesCount()).thenReturn(6);
        when(avatar.getFaceCount()).thenReturn(2);
        when(avatar.getVerticesX()).thenReturn(new float[] {-30, 30, 0, -30, 30, 0});
        when(avatar.getVerticesY()).thenReturn(new float[] {0, 0, -100, 0, 0, -100});
        when(avatar.getVerticesZ()).thenReturn(new float[] {0, 0, 0, -5, -5, -5});
        when(avatar.getFaceIndices1()).thenReturn(new int[] {0, 3});
        when(avatar.getFaceIndices2()).thenReturn(new int[] {1, 4});
        when(avatar.getFaceIndices3()).thenReturn(new int[] {2, 5});
        when(avatar.getFaceColors1()).thenReturn(new int[] {12345, 23456});
        when(avatar.getFaceColors3()).thenReturn(new int[] {12345, 23456});
        when(avatar.getFaceRenderPriorities()).thenReturn(new byte[] {5, 1});
        BufferedImage image =
            PortraitRenderer.render(PortraitScene.capture(client(avatar)), Math.PI, 0, 1, 300, 370);
        assertEquals(PortraitRenderer.hslToRgb(12345), image.getRGB(150, 250) & 0xffffff);
    }

    @Test public void honorsGameDepthBiasForOverlappingEquipment()
    {
        Model avatar = model();
        when(avatar.getVerticesCount()).thenReturn(6);
        when(avatar.getFaceCount()).thenReturn(2);
        when(avatar.getVerticesX()).thenReturn(new float[] {-30, 30, 0, -30, 30, 0});
        when(avatar.getVerticesY()).thenReturn(new float[] {0, 0, -100, 0, 0, -100});
        when(avatar.getVerticesZ()).thenReturn(new float[] {0, 0, 0, -.01f, -.01f, -.01f});
        when(avatar.getFaceIndices1()).thenReturn(new int[] {0, 3});
        when(avatar.getFaceIndices2()).thenReturn(new int[] {1, 4});
        when(avatar.getFaceIndices3()).thenReturn(new int[] {2, 5});
        when(avatar.getFaceColors1()).thenReturn(new int[] {12345, 23456});
        when(avatar.getFaceColors3()).thenReturn(new int[] {12345, 23456});
        byte[] bias = new byte[] {0, 8};
        when(avatar.getFaceBias()).thenReturn(bias);
        PortraitScene copied = PortraitScene.capture(client(avatar));
        bias[1] = 0;
        BufferedImage image = PortraitRenderer.render(copied, Math.PI, 0, 1, 300, 370);
        assertEquals(PortraitRenderer.hslToRgb(12345), image.getRGB(150, 250) & 0xffffff);
    }

    @Test public void intersectingFacesResolveDepthPerPixel()
    {
        Model avatar = model();
        when(avatar.getVerticesCount()).thenReturn(6);
        when(avatar.getFaceCount()).thenReturn(2);
        when(avatar.getVerticesX()).thenReturn(new float[] {-30, 30, 0, -30, 30, 0});
        when(avatar.getVerticesY()).thenReturn(new float[] {0, 0, -100, 0, 0, -100});
        when(avatar.getVerticesZ()).thenReturn(new float[] {-20, 20, 0, -10, 10, 0});
        when(avatar.getFaceIndices1()).thenReturn(new int[] {0, 3});
        when(avatar.getFaceIndices2()).thenReturn(new int[] {1, 4});
        when(avatar.getFaceIndices3()).thenReturn(new int[] {2, 5});
        when(avatar.getFaceColors1()).thenReturn(new int[] {12345, 23456});
        when(avatar.getFaceColors3()).thenReturn(new int[] {12345, 23456});
        BufferedImage image =
            PortraitRenderer.render(PortraitScene.capture(client(avatar)), Math.PI, 0, 1, 300, 370);
        assertEquals(PortraitRenderer.hslToRgb(23456), image.getRGB(100, 250) & 0xffffff);
        assertEquals(PortraitRenderer.hslToRgb(12345), image.getRGB(200, 250) & 0xffffff);
    }

    @Test public void rearFacingCapeLiningCannotCoverTheOuterSurface()
    {
        Model avatar = model();
        when(avatar.getFaceCount()).thenReturn(2);
        when(avatar.getFaceIndices1()).thenReturn(new int[] {0, 0});
        when(avatar.getFaceIndices2()).thenReturn(new int[] {1, 2});
        when(avatar.getFaceIndices3()).thenReturn(new int[] {2, 1});
        when(avatar.getFaceColors1()).thenReturn(new int[] {12345, 0});
        when(avatar.getFaceColors3()).thenReturn(new int[] {12345, 0});
        BufferedImage image =
            PortraitRenderer.render(PortraitScene.capture(client(avatar)), Math.PI, .12, 1, 300, 370);
        assertEquals(PortraitRenderer.hslToRgb(12345), image.getRGB(150, 150) & 0xffffff);
    }

    @Test public void textureCoordinatesRemainValidForAnEdgeOnMappingPlane()
    {
        PortraitScene.Mesh mesh =
            new PortraitScene.Mesh(new float[] {0, 0, 0}, new float[] {0, 0, 100}, new float[] {0, 100, 0},
                new int[] {0}, new int[] {1}, new int[] {2}, new int[] {12345}, new int[] {255}, 0);
        assertArrayEquals(new double[] {0, 0, 1, 0, 0, 1},
            PortraitRenderer.textureCoordinates(mesh, new int[] {0, 1, 2}, 0, 1, 2), 1e-8);
    }

    @Test public void copiesTexturePixelsAndRendersTheirPattern() throws Exception
    {
        Model avatar = model();
        when(avatar.getFaceTextures()).thenReturn(new short[] {0});
        net.runelite.api.TextureProvider textures = mock(net.runelite.api.TextureProvider.class);
        int[] pixels = new int[4096];
        java.util.Arrays.fill(pixels, 0xff0000);
        when(textures.load(0)).thenReturn(pixels);
        Client client = client(avatar);
        when(client.getTextureProvider()).thenReturn(textures);
        PortraitScene scene = PortraitScene.capture(client);
        java.util.Arrays.fill(pixels, 0x0000ff);
        BufferedImage image = PortraitRenderer.render(scene, Math.PI, .12, 1, 300, 370);
        assertEquals("Texture pixels must be copied before leaving the client thread", 0xff0000,
            image.getRGB(150, 150) & 0xffffff);
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        com.google.gson.Gson gson = new com.google.gson.Gson();
        PoseStore.write(directory, "Pose 2", java.util.Collections.singletonList(scene), gson, false, false);
        PortraitScene loaded = PoseStore.readPose(directory, "Test Player", "Pose 2", gson).frames.get(0);
        BufferedImage restored = PortraitRenderer.render(loaded, Math.PI, .12, 1, 300, 370);
        assertEquals(0xff0000, restored.getRGB(150, 150) & 0xffffff);
        java.util.Arrays.fill(pixels, 0);
        PortraitScene transparent = PortraitScene.capture(client);
        BufferedImage cutout = PortraitRenderer.render(transparent, Math.PI, .12, 1, 300, 370);
        assertEquals("Transparent effect texels must not become solid black triangles", 0,
            cutout.getRGB(150, 150) >>> 24);
        PoseStore.write(
            directory, "Pose 2", java.util.Collections.singletonList(transparent), gson, false, false);
        PortraitScene transparentReload =
            PoseStore.readPose(directory, "Test Player", "Pose 2", gson).frames.get(0);
        cutout = PortraitRenderer.render(transparentReload, Math.PI, .12, 1, 300, 370);
        assertEquals(0, cutout.getRGB(150, 150) >>> 24);
    }

    @Test public void exportsChangingGeometryWithStableFraming()
    {
        Model avatar = model();
        Client client = client(avatar);
        PortraitScene first = PortraitScene.capture(client);
        when(avatar.getVerticesY()).thenReturn(new float[] {-30, -30, -130});
        PortraitScene moved = PortraitScene.capture(client).withStableCamera(first);
        java.util.List<PortraitScene> frames = new java.util.ArrayList<>();
        for (int i = 0; i < 72; i++)
            frames.add(i < 36 ? first : moved);
        BufferedImage atlas = PortraitRenderer.renderAnimation(frames, Math.PI, .12, 1, false);
        boolean changed = false;
        for (int y = 0; y < 296 && !changed; y++)
            for (int x = 0; x < 240; x++)
                if (atlas.getRGB(x, y) != atlas.getRGB(x, y + 888))
                {
                    changed = true;
                    break;
                }
        assertTrue("Export must preserve movement, not repeat one frozen pose", changed);
    }

    @Test public void rotationUsesTransparentSeventyTwoFrameAtlas()
    {
        PortraitScene scene = PortraitScene.capture(client(model()));
        BufferedImage atlas = PortraitRenderer.renderRotation(scene, Math.PI, .12, 1);
        assertEquals(2880, atlas.getWidth());
        assertEquals(1776, atlas.getHeight());
        for (int row = 0; row < 6; row++)
            for (int column = 0; column < 12; column++)
                assertEquals(0, atlas.getRGB(column * 240, row * 296) >>> 24);
    }

    @Test public void rendersGeometryAndReplacesOnePngPerCharacter() throws Exception
    {
        PortraitScene scene = PortraitScene.capture(client(model()));
        BufferedImage image = PortraitRenderer.render(scene, Math.PI, .12, 1, 300, 370);
        assertEquals(300, image.getWidth());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertEquals(PortraitRenderer.hslToRgb(12345), image.getRGB(150, 150) & 0xffffff);
        Filepath directory = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
        PortraitStore.write(directory, "Test Player", image);
        PortraitStore.write(directory, "test_player", image);
        PortraitStore.write(directory, "Test Player", image, ".360.png");
        PortraitStore.removeRotation(directory, "test_player");
        try (var files = directory.walk(1))
        {
            assertEquals(1, files.filter(file -> file.getFileName().endsWith(".png")).count());
        }
        try (var files = directory.walk(1))
        {
            assertFalse(files.anyMatch(file -> file.getFileName().endsWith(".tmp")));
        }
        try (var files = directory.walk(1))
        {
            Filepath png = files.filter(file -> file.getFileName().endsWith(".png")).findFirst().get();
            try (InputStream input = png.openInputStream())
            {
                assertEquals(370, ImageIO.read(input).getHeight());
            }
        }
    }
}
