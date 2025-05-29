package org.ironsight.wpplugin.expandLayerTool.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.expandLayerTool.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.*;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LayerMappingContainer extends AbstractOperationContainer<LayerMapping> {
    public static LayerMappingContainer INSTANCE = new LayerMappingContainer(getActionsFilePath());

    public LayerMappingContainer(String filePath) {
        super(LayerMapping.class, filePath == null ? getActionsFilePath() : filePath, "/DefaultActions.json");

        // Register a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook executed. System.exit() was called.");
            this.writeToFile();
        }));
    }

    private static String getActionsFilePath() {
        String currentWorkingDir = System.getProperty("user.dir");
        if (isDebugMode()) return currentWorkingDir+"/src/main/resources/DefaultActions.json";
        else return new File(Configuration.getConfigDir(), "plugins").getPath() + "/mappings.json";
    }

    public static boolean isDebugMode() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMXBean.getInputArguments();

        for (String arg : arguments) {
            if (arg.contains("jdwp") || arg.contains("-Xdebug")) {
                return true;
            }
        }
        return false;
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
        container.updateMapping(m, f -> {
        });

        m = container.addMapping();
        m = new LayerMapping(new HeightProvider(),
                new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE),
                new MappingPoint[]{new MappingPoint(150, 0), new MappingPoint(230, 100)},
                ActionType.AT_LEAST,
                "frosted " + "peaks",
                "gradually add snow the higher a mountain goes",
                m.getUid());
        container.updateMapping(m, f -> {
        });
        m = container.addMapping();
        m = new LayerMapping(new SlopeProvider(),
                new NibbleLayerSetter(PineForest.INSTANCE),
                new MappingPoint[]{new MappingPoint(0, 15), new MappingPoint(70, 15), new MappingPoint(80, 0)},
                ActionType.LIMIT_TO,
                "no steep pines",
                "limit pines from growing on vertical cliffs",
                m.getUid());
        container.updateMapping(m, f -> {
        });
        m = container.addMapping();
        m = new LayerMapping(new AnnotationSetter(),
                new TestInputOutput(),
                new MappingPoint[0],
                ActionType.SET,
                "colors",
                "",
                m.getUid());
        container.updateMapping(m, f -> {
        });
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

    @Override
    public void writeToFile() {
        super.writeToFile();
        /* // FIXME DISABLED UNTIL LAYER SAVING/LOADING IS FIGURED OUT
        //FIXME also save layers that are used as input?
        Predicate<LayerMapping> usesLayer = l -> l.output instanceof ILayerGetter;
        //collect all layers used in all actions
        HashSet<String> usedLayers = queryAll().stream()
                .filter(usesLayer)
                .map(l -> (ILayerGetter) l.output)
                .map(ILayerGetter::getLayerId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        String layerFolder = "/home/klipper/Documents/worldpainter/layers/";
        ArrayList<IOException> errors = new ArrayList<IOException>();
        LayerObjectContainer.getInstance().writeToFolder(layerFolder, errors::add, usedLayers.toArray(new String[0]));
        if (!errors.isEmpty())
            GlobalActionPanel.logMessage(errors.stream().map(Throwable::getMessage).collect(Collectors.joining("\n")));

         */
    }


}

