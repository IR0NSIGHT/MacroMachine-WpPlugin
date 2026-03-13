package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;

import java.awt.*;

@Deprecated // does not work anymore with iterating the map once per action
public class IntermediateValueIO extends AnnotationSetter
{

    @Override
    public String getName() {
        return "Intermediate Value";
    }

    @Override
    public String getDescription() {
        return "obsolete and discontinued.";
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.INTERMEDIATE;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new IntermediateValueIO();
    }
}
