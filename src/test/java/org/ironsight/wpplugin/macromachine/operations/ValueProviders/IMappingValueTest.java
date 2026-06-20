package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.junit.jupiter.api.Test;

class IMappingValueTest
{

    @Test
    void testEquals() {
        for (ProviderType type : ProviderType.values()) {
            IMappingValue value1 = ProviderType.fromTypeDefault(type);
            IMappingValue value2 = ProviderType.fromTypeDefault(type);
            assertEquals(value1, value2, "two providers have to be equal by value-based-equality.");
        }
    }
}
