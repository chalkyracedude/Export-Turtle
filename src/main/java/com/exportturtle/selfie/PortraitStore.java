package com.exportturtle.selfie;

import com.exportturtle.snapshot.SnapshotStore;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import net.runelite.client.util.Filepath;

/** Replaces one portrait per character after a complete PNG has been written. */
public final class PortraitStore
{
    private PortraitStore()
    {
    }

    public static void removeRotation(Filepath directory, String accountName) throws IOException
    {
        directory.joinSegment(SnapshotStore.fileName(accountName).replace(".json", ".360.png"))
            .deleteIfExists();
    }

    public static void write(Filepath directory, String accountName, BufferedImage image) throws IOException
    {
        write(directory, accountName, image, ".png");
    }

    public static void write(Filepath directory, String accountName, BufferedImage image, String suffix)
        throws IOException
    {
        directory.createDirectories();
        String name = SnapshotStore.fileName(accountName).replace(".json", suffix);
        Filepath target = directory.joinSegment(name);
        Filepath temporary = directory.createTempFile("selfie-", ".tmp");
        try
        {
            try (OutputStream output = temporary.openOutputStream())
            {
                if (!ImageIO.write(image, "png", output))
                    throw new IOException("PNG encoder unavailable");
            }
            try
            {
                temporary.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                temporary.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            temporary.deleteIfExists();
        }
    }
}
