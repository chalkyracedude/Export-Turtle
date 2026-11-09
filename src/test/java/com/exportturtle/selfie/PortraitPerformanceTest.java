package com.exportturtle.selfie;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Player;
import org.junit.Test;

public class PortraitPerformanceTest
{
    static Client client()
    {
        int faces = 6000;
        float[] x = new float[faces * 3], y = x.clone(), z = x.clone();
        int[] a = new int[faces], b = a.clone(), c = a.clone(), colors = a.clone();
        for (int i = 0; i < faces; i++)
        {
            int v = i * 3;
            float angle = (float)(i * 2.39996);
            x[v] = (float)Math.cos(angle) * 45;
            z[v] = (float)Math.sin(angle) * 45;
            y[v] = -(i % 200);
            x[v + 1] = x[v] + 8;
            y[v + 1] = y[v];
            z[v + 1] = z[v];
            x[v + 2] = x[v];
            y[v + 2] = y[v] - 8;
            z[v + 2] = z[v] + 5;
            a[i] = v;
            b[i] = v + 1;
            c[i] = v + 2;
            colors[i] = 12345 + i;
        }
        Model model = mock(Model.class);
        when(model.getVerticesCount()).thenReturn(x.length);
        when(model.getFaceCount()).thenReturn(faces);
        when(model.getVerticesX()).thenReturn(x);
        when(model.getVerticesY()).thenReturn(y);
        when(model.getVerticesZ()).thenReturn(z);
        when(model.getFaceIndices1()).thenReturn(a);
        when(model.getFaceIndices2()).thenReturn(b);
        when(model.getFaceIndices3()).thenReturn(c);
        when(model.getFaceColors1()).thenReturn(colors);
        when(model.getFaceColors3()).thenReturn(colors);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Test Player");
        when(player.getModel()).thenReturn(model);
        Client client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        return client;
    }

    @Test public void measuresRotationFrames()
    {
        PortraitScene scene = PortraitScene.capture(client());
        for (int i = 0; i < 3; i++)
            PortraitRenderer.render(scene, i * .1, .12, 1, 420, 520);
        long start = System.nanoTime();
        for (int i = 0; i < 10; i++)
        {
            assertEquals(420, PortraitRenderer.render(scene, i * .1, .12, 1, 420, 520).getWidth());
        }
        System.out.printf(
            "Portrait full-quality benchmark: %.1f ms/frame%n", (System.nanoTime() - start) / 10000000.0);
        for (int i = 0; i < 3; i++)
            PortraitRenderer.renderPreview(scene, i * .1, .12, 1);
        start = System.nanoTime();
        for (int i = 0; i < 10; i++)
        {
            assertEquals(210, PortraitRenderer.renderPreview(scene, i * .1, .12, 1).getWidth());
        }
        System.out.printf(
            "Portrait drag benchmark: %.1f ms/frame%n", (System.nanoTime() - start) / 10000000.0);
    }
}
