package com.exportturtle;

import com.exportturtle.snapshot.SnapshotStore;
import net.runelite.client.util.Filepath;

/** Fixed export targets; never accepts arbitrary paths from the sidebar. */
enum ExportFile
{
    PROGRESS("Progress", "snapshots", ".json"),
    ITEMS("Items and vault", "items", ".json"),
    COLLECTION("Collection log", "collections", ".json"),
    EVENTS("Recent events", "events", ".json"),
    DIAGNOSTICS("Diagnostics", "diagnostics", ".json"),
    PORTRAIT("Portrait", "selfies", ".png"),
    HEADSHOT("Headshot", "selfies", ".head.png"),
    ANIMATION("Animation atlas", "selfies", ".360.png"),
    POSE_ONE("Pose 1", "selfies", ".pose1.png"),
    POSE_TWO("Pose 2", "selfies", ".pose2.png"),
    POSE_THREE("Pose 3", "selfies", ".pose3.png");

    private final String label, folder, suffix;

    ExportFile(String label, String folder, String suffix)
    {
        this.label = label;
        this.folder = folder;
        this.suffix = suffix;
    }

    Filepath forAccount(Filepath root, String account)
    {
        if (account == null || account.trim().isEmpty())
            throw new IllegalArgumentException("No active account");
        return root.join(folder).joinSegment(SnapshotStore.fileName(account).replace(".json", suffix));
    }

    @Override public String toString()
    {
        return label;
    }
}
