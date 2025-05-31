package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.themes.SimpleTheme;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.Terrain.GRASS;
import static org.pepsoft.worldpainter.Terrain.STONE;

public class TestDimension {
    static class DimensionParams {
        public DimensionParams() {};

        public DimensionParams(Rectangle mapArea, int minHeight, int maxHeight, int terrainHeight, int seed,
                               int waterLevel,
                               Platform platform, Terrain surface) {
            this.mapArea = mapArea;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.terrainHeight = terrainHeight;
            this.seed = seed;
            this.waterLevel = waterLevel;
            this.platform = platform;
            this.surface = surface;
        }

        Rectangle mapArea = new Rectangle(500,500);
        int minHeight = -256;
        int maxHeight = 512;
        int terrainHeight = 70;
        int seed = 123456789;
        int waterLevel = 62;
        Platform platform = DefaultPlugin.JAVA_ANVIL_1_19;
        Terrain surface = GRASS;
    }

    public static Dimension createDimension(DimensionParams params) {

        final TileFactory tileFactory = createTileFactory(params.terrainHeight, params.minHeight, params.maxHeight,
                params.waterLevel,
                params.seed, params.surface);

        World2 world = new World2(params.platform, params.minHeight, params.maxHeight);
        final Dimension dimension = new Dimension(world, "Surface", params.seed, tileFactory, NORMAL_DETAIL);
        {
            final int tileX1 = params.mapArea.x / TILE_SIZE,
                    tileX2 = (params.mapArea.x + params.mapArea.width - 1) / TILE_SIZE,
                    tileY1 = params.mapArea.y / TILE_SIZE,
                    tileY2 = (params.mapArea.y + params.mapArea.height - 1) / TILE_SIZE;
            for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                    dimension.addTile(tileFactory.createTile(tileX, tileY));
                }
            }
        }
        return dimension;
    }
    private static TileFactory createTileFactory(int terrainHeight, int minHeight, int maxHeight, int waterLevel,
                                                int seed, Terrain terrain) {
        SimpleTheme theme = SimpleTheme.createSingleTerrain(terrain, minHeight, maxHeight, waterLevel);
        return new HeightMapTileFactory(seed, new ConstantHeightMap(terrainHeight), minHeight, maxHeight, false,
                theme);
    }

    /**
     *  assert the dimension is created with the values the user has defined, such as terrainheight, waterlevel etc
     */
    @Test
    void testDimensionHasParamValues() {

        DimensionParams params = new DimensionParams(new Rectangle(50,70), -200,300,70, 3456789, 60,
                DefaultPlugin.JAVA_ANVIL_1_19, STONE);
        assertEquals(0, params.mapArea.x);
        assertEquals(0, params.mapArea.y);
        assertEquals(50, params.mapArea.width);
        assertEquals(70, params.mapArea.height);

        Dimension dim = createDimension(params);
        for (int x = params.mapArea.x; x < params.mapArea.x + params.mapArea.width; x++) {
            for (int y = params.mapArea.y; y < params.mapArea.y + params.mapArea.height; y++) {
                assertEquals(params.terrainHeight, dim.getHeightAt(x,y));
                assertEquals(params.waterLevel, dim.getWaterLevelAt(x,y));
                assertEquals(params.surface, dim.getTerrainAt(x,y));
            }
        }
    }

    /**
     *  assert the dimension is created with the values the user has defined, such as terrainheight, waterlevel etc
     */
    @Test
    void testDimensionCanBeModified() {

        DimensionParams params = new DimensionParams(new Rectangle(50,70), -200,300,70, 3456789, 60,
                DefaultPlugin.JAVA_ANVIL_1_19, STONE);
        assertEquals(0, params.mapArea.x);
        assertEquals(0, params.mapArea.y);
        assertEquals(50, params.mapArea.width);
        assertEquals(70, params.mapArea.height);

        Dimension dim = createDimension(params);

        dim.setTerrainAt(12,13,Terrain.LAVA);
        assertEquals(Terrain.LAVA, dim.getTerrainAt(12,13));

        dim.setHeightAt(14,15,124);
        assertEquals(124, dim.getHeightAt(14,15));

        dim.setWaterLevelAt(17,18,-3);
        assertEquals(-3, dim.getWaterLevelAt(17,18));
    }
}
