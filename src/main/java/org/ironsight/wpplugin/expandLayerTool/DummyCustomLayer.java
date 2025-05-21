package org.ironsight.wpplugin.expandLayerTool;

import org.pepsoft.worldpainter.layers.CustomLayer;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class DummyCustomLayer extends CustomLayer {
    public DummyCustomLayer() {
        super("MacroHook", "Dont touch this layer, its a workaround for autosaving", DataSize.BIT, 0, null);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
