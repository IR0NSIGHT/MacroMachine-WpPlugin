package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import java.util.Collection;

public interface IMappingValueProvider {
    Collection<IMappingValue> getItems();

    void subscribeToUpdates(Runnable r);
    boolean existsItem(Object item);
}
