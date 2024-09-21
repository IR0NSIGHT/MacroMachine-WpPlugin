package org.demo.wpplugin;

import org.demo.wpplugin.platforms.exporters.DemoPlatformExporter;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.GameType;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Platform.Capability;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.AbstractWorldExporter;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.platforms.AbstractPlatformProvider;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.plugins.CustomObjectProvider;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

/**
 * A plugin class for providing (a) new {@link Platform}/map format(s). This is class is separate from
 * {@link DemoWPPlugin} because otherwise {@link PlatformProvider#getKeys()} and {@link CustomObjectProvider#getKeys()}
 * would clash. This is a flaw in the plugin architecture design.
 *
 * <p>This simple example extends {@link AbstractPlatformProvider}, which is appropriate for formats which bear no
 * resemblance to a Minecraft-like block or voxel based format.
 *
 * <p>For Minecraft-like block or voxel-based games, additionally implementing {@link BlockBasedPlatformProvider} might
 * be appropriate. It means the format should be based on (or be able to emulate) Minecraft-like 16x16 chunks, and you
 * would need to provide implementations of {@link Chunk}, {@link ChunkStore} and {@link PostProcessor} to do the actual
 * reading and writing of chunks. That would allow the exporter to be an {@link AbstractWorldExporter} which does a lot
 * of the heavy lifting of turning WorldPainter's data into blocks for you.
 */
@SuppressWarnings("unused") // Instantiated by WorldPainter
public class DemoWPPlatformProvider extends AbstractPlatformProvider {
    public DemoWPPlatformProvider() {
        super(Version.VERSION, PLATFORMS, "Demo WP Platforms");
    }

    @Override
    public WorldExporter getExporter(World2 world, WorldExportSettings exportSettings) {
        ensurePlatformSupported(world.getPlatform());
        return new DemoPlatformExporter(world, exportSettings);
    }

    @Override
    public File getDefaultExportDir(Platform platform) {
        ensurePlatformSupported(platform);
        // Change this to return the default directory to which maps should be exported for this map format
        return DesktopUtils.getDocumentsFolder();
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument") // May be expanded
    private static final Platform DEMO_PLATFORM = new Platform(
            "org.demo.wpplugin.DemoPlatform.v1",  // The globally unique, stable identifier for this platform/map format/file type
            "Demo Platform",                      // A short name. This is displayed to the user and may change
            128, 256, 512,                        // The minimum, default and maximum maxHeights of the format. These may be the same if the format only supports one maxHeight. Note that maxHeight is _exclusive_; it is one more than the highest block may be placed
                                                  // This version of the constructor results in a platform with a fixed minHeight (inclusive) of zero. Take a look at the other constructors to provide a configurable minHeight
            Integer.MIN_VALUE, Integer.MAX_VALUE, // The minimum and maximum supported X (east-west) coordinates of the format
            Integer.MIN_VALUE, Integer.MAX_VALUE, // The minimum and maximum supported Z (north-south) coordinates of the format
            asList(GameType.SURVIVAL),            // The supported game types. Add any additional supported game types here
            asList(Generator.DEFAULT),            // The supported generator types. Add any additional supported generator types here
            asList(DIM_NORMAL),                   // The supported dimensions, using the Constants.DIM_* constants. Add Nether and/or End here if supported
            EnumSet.of(Capability.BLOCK_BASED),   // The set of supported capabilities. Take a look at all the capabilities in the Capability enum and add all that apply. It is important not to miss any, as the capabilities determine what functionalities WorldPainter will offer to the user for this format
            new Object[0]);                       // Optional extra private attributes

    private static final Set<Platform> PLATFORMS = singleton(DEMO_PLATFORM);
}