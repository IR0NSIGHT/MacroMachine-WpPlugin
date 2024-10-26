package org.demo.wpplugin.platforms.exporters;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.AbstractWorldExporter;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * The class that is responsible for actually taking the WorldPainter {@link World2 world} and exporting it to disk in
 * the format this exporter supports.
 *
 * <p>This simple example implements {@link WorldExporter} directly, which is appropriate for formats which bear no
 * resemblance to a Minecraft-like block or voxel based format. This allows complete freedom in translating
 * WorldPainter's terrain and water levels, terrain types and painted layers to an arbitrary map format.
 *
 * <p>For Minecraft-like block or voxel-based games, subclassing {@link AbstractWorldExporter} might be more
 * appropriate, which does a lot of the heavy lifting of translating the WorldPainter data into blocks. For that to work
 * the platform provider should be a {@link BlockBasedPlatformProvider}.
 */
public class DemoPlatformExporter implements WorldExporter {
    public DemoPlatformExporter(World2 world, WorldExportSettings exportSettings) {
        this.world = world;
    }

    @Override
    public World2 getWorld() {
        return world;
    }

    @Override
    public File selectBackupDir(File mapDir) throws IOException {
        // This is an example implementation that creates a backups directory as a sibling to the maps directory, but
        // you are free to provide your own implementation
        File mapsDir = mapDir.getParentFile();
        File parentDir = mapsDir.getParentFile();
        File backupsDir = new File(parentDir, "backups");
        if ((! backupsDir.isDirectory()) &&  (! backupsDir.mkdirs())) {
            throw new IOException("Could not create " + backupsDir);
        }
        return new File(backupsDir, mapDir.getName() + "." + DATE_FORMAT.format(new Date()));
    }

    @Override
    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // 1. Backup the existing map, if any, to backupDir
        // 2. Export the world to the provided baseDir using the specified name. Use the exportSettings to determine exactly what to export
        // 3. At your option, report progress to the progressReceiver. NOTE that it may be null!
        // 4. Return statistics per dimension, keyed by the corresponding Constants.DIM_* constant
        return emptyMap();
    }

    private final World2 world;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
}