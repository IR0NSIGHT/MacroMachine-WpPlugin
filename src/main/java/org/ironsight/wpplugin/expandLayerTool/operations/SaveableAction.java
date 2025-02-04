package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IDisplayUnit;

import java.io.Serializable;
import java.util.UUID;

public interface SaveableAction extends Serializable, IDisplayUnit {
    UUID getUid();
}
