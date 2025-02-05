package org.ironsight.wpplugin.expandLayerTool.operations;

import java.util.UUID;

public class MappingMacroContainer extends AbstractOperationContainer<MappingMacro> {
    private final static MappingMacroContainer instance = new MappingMacroContainer();

    public MappingMacroContainer() {
        super(MappingMacro.class, "/home/klipper/Documents/worldpainter/macros.txt");
    }

    public static MappingMacroContainer getInstance() {
        return instance;
    }

    @Override
    protected MappingMacro getNewAction() {
        return new MappingMacro("New Mapping Macro",
                "this macro is a collection of Mappings, each applied in order to" +
                        " the map to achieve complex, reusable, one-click operations.",
                new UUID[0],
                getUUID());
    }
}
