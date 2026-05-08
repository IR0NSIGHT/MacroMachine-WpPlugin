package org.ironsight.wpplugin.macromachine.REST.DTOs;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class InputOutputDTOTest
{

    @Test
    public void fromOutputSetter() {
        var setter = new AnnotationSetter();
        var dto = InputOutputDTO.fromOutputSetter(new AnnotationSetter());
        var expected = new InputOutputDTO(setter.getName(), setter.getDescription(), setter.getMinValue(),
                setter.getMaxValue(), IPositionValueSetter.getIgnoreValue(setter),
                Arrays.stream(setter.getAllOutputValues()).mapToObj(setter::valueToString).toArray(String[]::new),
                setter.isDiscrete(), setter.getProviderType());
        assertEquals(expected, dto);
    }

    @Test
    public void fromInputSetter() {
        var setter = new AnnotationSetter();
        var dto = InputOutputDTO.fromInputGetter(new AnnotationSetter());
        var expected = new InputOutputDTO(setter.getName(), setter.getDescription(), setter.getMinValue(),
                setter.getMaxValue(), IPositionValueSetter.getIgnoreValue(setter),
                Arrays.stream(setter.getAllInputValues()).mapToObj(setter::valueToString).toArray(String[]::new),
                setter.isDiscrete(), setter.getProviderType());
        assertEquals(expected, dto);
    }

    @Test
    public void fromDTO() {
        var setter = new AnnotationSetter();
        var dto = InputOutputDTO.fromInputGetter(new AnnotationSetter());
        var construced = dto.toGetter();

        assertEquals(setter, construced);

    }
}
