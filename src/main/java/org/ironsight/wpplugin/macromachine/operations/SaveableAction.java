package org.ironsight.wpplugin.macromachine.operations;

import java.io.Serializable;
import java.util.UUID;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;

public interface SaveableAction extends Serializable, IDisplayUnit
{
    UUID getUid();

    boolean isActive();

    void setActive(boolean active);
}
