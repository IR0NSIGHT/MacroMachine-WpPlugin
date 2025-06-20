package org.ironsight.wpplugin.macromachine.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pepsoft.worldpainter.Configuration;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;

import static org.ironsight.wpplugin.macromachine.operations.MappingActionContainer.isDebugMode;

public class MacroContainer extends AbstractOperationContainer<Macro> {
    private static MacroContainer instance;

    public MacroContainer(String filePath) {
        super(Macro.class, filePath == null ? getActionsFilePath() : filePath, "/DefaultMacros.json");
    }

    public static void SetInstance(MacroContainer container) {
        assert instance == null;
        instance = container;
    }

    public static MacroContainer getInstance() {
        assert instance != null : "we have to set a global isntance first";
        return instance;
    }

    public static String getActionsFilePath() {
        return new File(Configuration.getConfigDir(), "plugins").getPath() + "/macroMachine";
    }

    @Override
    protected Macro getNewAction() {
        return getNewAction(getUUID());
    }

    @Override
    protected Macro getNewAction(UUID uuid) {
        return new Macro("New Mapping Macro",
                "this macro is a collection of Mappings, each applied in order " + "to" + " the map to achieve " +
                        "complex, reusable, one-click operations.",
                new UUID[0],
                uuid, new boolean[0]);
    }

    @Override
    protected void fromSaveObject(String jsonString) throws JsonProcessingException {
        assert jsonString != null;
        ObjectMapper objectMapper = new ObjectMapper();
        Macro[] obj = objectMapper.readValue(jsonString, Macro[].class);
        for (Macro instance : obj) {
            this.putMapping(instance);
        }
    }

    @Override
    public void updateMapping(Macro macro, Consumer<String> onError) {
        boolean loop = macro.hasLoop(new HashSet<>(), this);
        if (loop) {
            onError.accept("Macro has an infinite loop, caused by a nested macro. Can not save.");
            return;
        }
        super.updateMapping(macro, onError);
    }

    @Override
    protected <T extends Serializable> T toSaveObject() {
        return (T) queryAll().toArray(new Macro[0]);
    }
}
