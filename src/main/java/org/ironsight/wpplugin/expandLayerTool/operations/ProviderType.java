package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.*;

public enum ProviderType {
    HEIGHT, SLOPE, ANNOTATION, BINARY_LAYER, BINARY_SPRAYPAINT, BLOCK_DIRECTION, INTERMEDIATE, NIBBLE_LAYER,
    SELECTION, STONE_PALETTE, TERRAIN, TEST, VANILLA_BIOME, WATER_DEPTH, WATER_HEIGHT;

    public static IMappingValue fromType(Object[] data, ProviderType type) {
        switch (type) {
            case TEST:
                return new TestInputOutput().instantiateFrom(data);
            case SLOPE:
                return new SlopeProvider().instantiateFrom(data);
            case HEIGHT:
                return new HeightProvider().instantiateFrom(data);
            case TERRAIN:
                return new TerrainProvider().instantiateFrom(data);
            case WATER_DEPTH:
                return new WaterDepthProvider().instantiateFrom(data);
            case INTERMEDIATE:
                return new IntermediateValueIO().instantiateFrom(data);
            case STONE_PALETTE:
                return new StonePaletteApplicator().instantiateFrom(data);
            case VANILLA_BIOME:
                return new VanillaBiomeProvider().instantiateFrom(data);
            case BLOCK_DIRECTION:
                return new BlockFacingDirectionIO().instantiateFrom(data);
            case SELECTION:
                return new SelectionIO().instantiateFrom(data);
            case ANNOTATION:
                return new AnnotationSetter().instantiateFrom(data);
            case WATER_HEIGHT:
                return new WaterHeightAbsoluteIO().instantiateFrom(data);

            case BINARY_SPRAYPAINT:
                return new BitLayerBinarySpraypaintApplicator().instantiateFrom(data);
            case BINARY_LAYER:
                return new BinaryLayerIO(null).instantiateFrom(data);
            case NIBBLE_LAYER:
                return new NibbleLayerSetter().instantiateFrom(data);

            default:
                throw new IllegalArgumentException("not implemented: can not instantiate providers that need extra " + "information");
        }
    }
}
