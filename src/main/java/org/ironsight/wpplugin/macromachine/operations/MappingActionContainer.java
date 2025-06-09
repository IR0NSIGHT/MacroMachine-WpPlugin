package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ironsight.wpplugin.macromachine.MacroMachinePlugin;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.PineForest;

import java.io.File;
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * this class stores MappingActions by UUID
 * it can read and write to file
 * its the central authority on which actions exist. if its not in the container, its considered non-existent.
 */
public class MappingActionContainer extends AbstractOperationContainer<MappingAction> {
    private static MappingActionContainer INSTANCE;

    public MappingActionContainer(String filePath) {
        super(MappingAction.class, filePath == null ? getActionsFilePath() : filePath, "/DefaultActions.json");
        MacroMachinePlugin.error("INSTANTIATE NEW MAPPING CONTAINER:" + this);
    }

    public static MappingActionContainer getInstance() {
        return INSTANCE;
    }

    public static void SetInstance(MappingActionContainer container) {
        assert INSTANCE == null;
        INSTANCE = container;
    }

    private static String getActionsFilePath() {
        String currentWorkingDir = System.getProperty("user.dir");
        if (isDebugMode()) return currentWorkingDir + "/mappings.json";
        else return new File(Configuration.getConfigDir(), "plugins").getPath() + "/mappings.json";
    }

    public static boolean isDebugMode() {
        return false;/*
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMXBean.getInputArguments();

        for (String arg : arguments) {
            if (arg.contains("jdwp") || arg.contains("-Xdebug")) {
                return true;
            }
        }
        return false;
        */
    }

    public static void addDefaultMappings(MappingActionContainer container) {
        MappingAction m = container.addMapping();
        m = new MappingAction(new SlopeProvider(),
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
        m = new MappingAction(new TerrainHeightIO(),
                new BitLayerBinarySpraypaintApplicator(Frost.INSTANCE),
                new MappingPoint[]{new MappingPoint(150, 0), new MappingPoint(230, 100)},
                ActionType.AT_LEAST,
                "frosted " + "peaks",
                "gradually add snow the higher a mountain goes",
                m.getUid());
        container.updateMapping(m, f -> {
        });
        m = container.addMapping();
        m = new MappingAction(new SlopeProvider(),
                new NibbleLayerSetter(PineForest.INSTANCE),
                new MappingPoint[]{new MappingPoint(0, 15), new MappingPoint(70, 15), new MappingPoint(80, 0)},
                ActionType.LIMIT_TO,
                "no steep pines",
                "limit pines from growing on vertical cliffs",
                m.getUid());
        container.updateMapping(m, f -> {
        });
        m = container.addMapping();
        m = new MappingAction(new AnnotationSetter(),
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
    protected MappingAction getNewAction() {
        return new MappingAction(new TerrainHeightIO(),
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
        for (MappingAction m : queryAll()) { //force through constructor to enforce assertions
            MappingAction ignored = m.withName(m.getName());
        }
        assert !queryAll().isEmpty() : "not supposed to happen";
    }

    @Override
    protected void fromSaveObject(String jsonString) throws JsonProcessingException {
        assert jsonString != null;
        ObjectMapper objectMapper = new ObjectMapper();
        ActionJsonWrapper[] obj = objectMapper.readValue(jsonString, ActionJsonWrapper[].class);
        // System.out.println("READ JSON STRING: \n" + jsonString);
        for (ActionJsonWrapper wrapper : obj) {
            MappingAction m = MappingAction.fromJsonWrapper(wrapper);
            this.putMapping(m);
            //     System.out.println(m);
        }
    }

    @Override
    protected <T extends Serializable> T toSaveObject() {
        ActionJsonWrapper[] saveObject = new ActionJsonWrapper[this.queryAll().size()];
        int i = 0;
        for (MappingAction m : queryAll()) {
            saveObject[i++] = new ActionJsonWrapper(m);
        }
        return (T) saveObject;
    }

    @Override
    public void updateMapping(MappingAction mapping, Consumer<String> onError) {
        super.updateMapping(mapping, onError);
        System.out.println("updated mapping in container with input:" + mapping.getInput());
    }

    @Override
    public void writeToFile() {
        MacroMachinePlugin.error("Action Container write to file" + this);
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

