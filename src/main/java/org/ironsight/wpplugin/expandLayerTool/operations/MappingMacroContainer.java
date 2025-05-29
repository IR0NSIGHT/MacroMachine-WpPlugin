package org.ironsight.wpplugin.expandLayerTool.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pepsoft.worldpainter.Configuration;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer.isDebugMode;

public class MappingMacroContainer extends AbstractOperationContainer<MappingMacro> {
    private final static MappingMacroContainer instance = new MappingMacroContainer();

    public MappingMacroContainer() {
        super(MappingMacro.class, getActionsFilePath(), "/DefaultMacros.json");
    }

    public static MappingMacroContainer getInstance() {
        return instance;
    }

    private static String getActionsFilePath() {
        String currentWorkingDir = System.getProperty("user.dir");
        if (isDebugMode()) return currentWorkingDir+"/src/main/resources/DefaultMacros.json";
        else return new File(Configuration.getConfigDir(), "plugins").getPath() + "/macros.json";
    }

    @Override
    protected MappingMacro getNewAction() {
        return new MappingMacro("New Mapping Macro",
                "this macro is a collection of Mappings, each applied in order " + "to" + " the map to achieve " +
                        "complex, reusable, one-click operations.",
                new UUID[0],
                getUUID());
    }

    @Override
    protected void fromSaveObject(String jsonString) throws JsonProcessingException {
        assert jsonString != null;
        ObjectMapper objectMapper = new ObjectMapper();
        MappingMacro[] obj = objectMapper.readValue(jsonString, MappingMacro[].class);
        for (MappingMacro instance : obj) {
            this.putMapping(instance);
        }
    }

    @Override
    public void updateMapping(MappingMacro macro, Consumer<String> onError) {
        boolean loop = macro.hasLoop(new HashSet<>());
        if (loop) {
            onError.accept("Macro has an infinite loop, caused by a nested macro. Can not save.");
            return;
        }
        super.updateMapping(macro, onError);
    }

    @Override
    protected <T extends Serializable> T toSaveObject() {
        return (T) queryAll().toArray(new MappingMacro[0]);
    }
}
