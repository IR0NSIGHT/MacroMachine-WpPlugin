package org.ironsight.wpplugin.expandLayerTool.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.*;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import java.io.File;
import java.io.Serializable;

public class LayerMappingContainer extends AbstractOperationContainer<LayerMapping> {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer();

    public LayerMappingContainer() {
        super(LayerMapping.class, new File(Configuration.getConfigDir(), "plugins").getPath() + "/mappings.json");
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
                ActionType.AT_LEAST,
                "frosted " + "peaks",
                "gradually add snow the higher a mountain goes",
                m.getUid());
        container.updateMapping(m);

        m = container.addMapping();
        m = new LayerMapping(new SlopeProvider(),
                new NibbleLayerSetter(PineForest.INSTANCE),
                new MappingPoint[]{new MappingPoint(0, 15), new MappingPoint(70, 15), new MappingPoint(80, 0)},
                ActionType.LIMIT_TO,
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
    protected LayerMapping getNewAction() {
        return new LayerMapping(new HeightProvider(),
                new AnnotationSetter(),
                new MappingPoint[0],
                ActionType.SET,
                "New Action " + Math.round(Math.random() * 1000),
                "description of the action",
                getUUID());

    }

    @Override
    public void readFromFile() {
        super.readFromFile();
        for (LayerMapping m : queryAll()) { //force through constructor to enforce assertions
            LayerMapping ignored = m.withName(m.getName());
        }
    }

    @Override
    protected void fromSaveObject(String jsonString) throws JsonProcessingException {
        assert jsonString != null;
        ObjectMapper objectMapper = new ObjectMapper();
        ActionJsonWrapper[] obj = objectMapper.readValue(jsonString, ActionJsonWrapper[].class);
       // System.out.println("READ JSON STRING: \n" + jsonString);
        for (ActionJsonWrapper wrapper : obj) {
            LayerMapping m = LayerMapping.fromJsonWrapper(wrapper);
            this.putMapping(m);
       //     System.out.println(m);
        }
    }

    @Override
    protected <T extends Serializable> T toSaveObject() {
        ActionJsonWrapper[] saveObject = new ActionJsonWrapper[this.queryAll().size()];
        int i = 0;
        for (LayerMapping m : queryAll()) {
            saveObject[i++] = new ActionJsonWrapper(m);
        }
        return (T) saveObject;
    }
}

