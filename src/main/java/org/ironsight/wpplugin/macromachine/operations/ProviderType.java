package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.MacroSelectionLayer;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

public enum ProviderType {
    HEIGHT,
    SLOPE,
    ANNOTATION,
    BINARY_LAYER,
    BINARY_SPRAYPAINT,
    BLOCK_DIRECTION,
    INTERMEDIATE,
    //action filter
    INTERMEDIATE_SELECTION,
    NIBBLE_LAYER,
    SELECTION,
    STONE_PALETTE,
    TERRAIN,
    TEST,
    VANILLA_BIOME,
    WATER_DEPTH,
    WATER_HEIGHT,
    ALWAYS,
    DISTANCE_TO_EDGE,
    PERLIN_NOISE,
    SHADOW,
    VORONOI_NOISE
    ;

    public static IMappingValue fromType(Object[] data, ProviderType type) {
        IMappingValue newV = fromTypeDefault(type).instantiateFrom(data);
        assert newV.getProviderType() == type;
        return newV;
    }

    public static IMappingValue fromTypeDefault(ProviderType type) {
        switch (type) {
            case TEST:
                return new TestInputOutput();
            case SLOPE:
                return new SlopeProvider();
            case HEIGHT:
                return new TerrainHeightIO(-64,319);
            case TERRAIN:
                return new TerrainProvider();
            case WATER_DEPTH:
                return new WaterDepthProvider();
            case INTERMEDIATE:
                return new IntermediateValueIO(0,100,"");
            case STONE_PALETTE:
                return new StonePaletteApplicator();
            case VANILLA_BIOME:
                return new VanillaBiomeProvider();
            case BLOCK_DIRECTION:
                return new BlockFacingDirectionIO();
            case SELECTION:
                return new SelectionIO();
            case ANNOTATION:
                return new AnnotationSetter();
            case WATER_HEIGHT:
                return new WaterHeightAbsoluteIO(-64,319);
            case BINARY_SPRAYPAINT:
                return new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE, false);
            case BINARY_LAYER:
                return new BinaryLayerIO(Frost.INSTANCE,false);
            case NIBBLE_LAYER:
                return new NibbleLayerSetter(PineForest.INSTANCE, false);
            case INTERMEDIATE_SELECTION:
                return ActionFilterIO.instance;
            case ALWAYS:
                return AlwaysIO.instance;
            case DISTANCE_TO_EDGE:
                return new DistanceToLayerEdgeGetter(MacroSelectionLayer.INSTANCE,100);
            case PERLIN_NOISE:
                return new PerlinNoiseIO(1,1, 42069,5);
            case SHADOW:
                return new ShadowMapIO();
            case VORONOI_NOISE:
                return new VoronoiIO(0,100,987654321,5,100);
            default:
                throw new IllegalArgumentException(
                        "not implemented: can not instantiate providers that need extra " + "information");

        }
    }
}
