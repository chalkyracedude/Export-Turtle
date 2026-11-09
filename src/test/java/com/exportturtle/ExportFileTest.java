package com.exportturtle;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.util.HashSet;
import net.runelite.client.util.Filepath;
import org.junit.Test;

public class ExportFileTest
{
    @Test public void exportTargetsStayInsideTheFolderAndSeparateAccounts()
    {
        Filepath root = Filepath.Unchecked.getRooted(Path.of(System.getProperty("java.io.tmpdir"), "export-target-test"));
        var paths = new HashSet<String>();
        for (ExportFile file : ExportFile.values())
        {
            Filepath first = file.forAccount(root, "../First Player");
            assertTrue(first.startsWith(root));
            assertTrue(paths.add(first.toString()));
            assertNotEquals(first, file.forAccount(root, "Second Player"));
            assertThrows(IllegalArgumentException.class, () -> file.forAccount(root, ""));
        }
    }
}
