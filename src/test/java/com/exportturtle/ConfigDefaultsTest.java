package com.exportturtle;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConfigDefaultsTest
{
    @Test public void featuresAreEnabledAndHistoryIsKeptByDefault()
    {
        ExportTurtleConfig config = new ExportTurtleConfig() {};
        assertFalse(config.turtleCursor());
        assertTrue(config.localSnapshots());
        assertTrue(config.itemExports());
        assertTrue(config.collectionExports());
        assertTrue(config.eventHistory());
        assertTrue(config.portraitExports());
        assertTrue(config.clanExports());
        assertTrue(config.diagnostics());
        assertEquals(30, config.syncSeconds());
        assertEquals(0, config.eventRetentionDays());
    }
}
