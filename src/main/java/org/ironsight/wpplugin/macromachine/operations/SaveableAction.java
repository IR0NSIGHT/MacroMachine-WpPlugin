package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

import java.io.Serializable;
import java.util.UUID;

public interface SaveableAction extends Serializable, IDisplayUnit {
    UUID getUid();
}
