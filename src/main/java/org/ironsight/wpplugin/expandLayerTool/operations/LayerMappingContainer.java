package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.Gui.MappingEditorPanel;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.*;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import javax.swing.*;

public class LayerMappingContainer extends AbstractOperationContainer<LayerMapping> {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer();

    public LayerMappingContainer() {
        super(LayerMapping.class, "/home/klipper/Documents/worldpainter/mappings.txt");
    }

    public static void addDefaultMappings(LayerMappingContainer container) {
        LayerMapping m = container.addMapping();
        m = new LayerMapping(new SlopeProvider(),
                new StonePaletteApplicator(),
                new MappingPoint[]{new MappingPoint(30, 3),
                        new MappingPoint(50, 8),
                        new MappingPoint(70, 5),
                        new MappingPoint(80, 9)},
                ActionType.SET,
                "paint mountainsides",
                "apply stone and rocks " + "based" + " on slope to make mountain sides colorful and interesting",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new HeightProvider(),
                new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE),
                new MappingPoint[]{new MappingPoint(150, 0), new MappingPoint(230, 100)},
                ActionType.MAX,
                "frosted " + "peaks",
                "gradually add snow the higher a mountain goes",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new SlopeProvider(),
                new NibbleLayerSetter(PineForest.INSTANCE),
                new MappingPoint[]{new MappingPoint(0, 15), new MappingPoint(70, 15), new MappingPoint(80, 0)},
                ActionType.MIN,
                "no steep pines",
                "limit pines from growing on vertical cliffs",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new AnnotationSetter(),
                new TestInputOutput(),
                new MappingPoint[0],
                ActionType.SET,
                "colors",
                "",
                m.getUid());
        container.updateMapping(m);
    }

    @Override
    public void readFromFile() {
        super.readFromFile();
        for (LayerMapping m : queryAll()) { //force through constructor to enforce assertions
            LayerMapping ignored = m.withName(m.getName());
        }
    }

    @Override
    protected LayerMapping getNewAction() {
        return new LayerMapping(new HeightProvider(),
                new AnnotationSetter(),
                new MappingPoint[0],
                ActionType.SET,
                "Height-to-colors",
                "paint annotations based on terrain height",
                getUUID());

    }
}

