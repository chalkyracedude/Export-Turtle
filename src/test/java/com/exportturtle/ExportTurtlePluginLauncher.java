package com.exportturtle;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExportTurtlePluginLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ExportTurtlePlugin.class);
        RuneLite.main(args);
    }
}