package org.ironsight.wpplugin.macromachine.REST.DTOs;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.NibbleLayerSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.layers.PineForest;

class InputOutputDTOTest
{

    @Test
    public void fromOutputSetter() {
        var setter = new AnnotationSetter();
        var dto = InputOutputDTO.fromOutputSetter(new AnnotationSetter());
        int[] values = Arrays.stream(setter.getAllOutputValues()).filter(i -> !setter.isIgnoreValue(i)).toArray();
        var expected = new InputOutputDTO(setter.getName(), setter.getDescription(), setter.getMinValue(),
                setter.getMaxValue(), IPositionValueSetter.getIgnoreValue(setter),
                Arrays.stream(values).mapToObj(setter::valueToString).toArray(String[]::new),
                Arrays.stream(values).map(setter::getColorForValue).toArray(),
                Arrays.stream(values).mapToObj(setter::getIconForValue).toArray(String[]::new),
                setter.getIconName(), setter.isDiscrete(),
                setter.getProviderType(), Arrays.asList(setter.getSaveData()));
        assertEquals(expected, dto);
    }

    @Test
    public void fromInputSetter() {
        var setter = new AnnotationSetter();
        var dto = InputOutputDTO.fromInputGetter(new AnnotationSetter());
        int[] values = setter.getAllInputValues();
        var expected = new InputOutputDTO(setter.getName(), setter.getDescription(), setter.getMinValue(),
                setter.getMaxValue(), IPositionValueSetter.getIgnoreValue(setter),
                Arrays.stream(values).mapToObj(setter::valueToString).toArray(String[]::new),
                Arrays.stream(values).map(setter::getColorForValue).toArray(),
                Arrays.stream(values).mapToObj(setter::getIconForValue).toArray(String[]::new),
                setter.getIconName(), setter.isDiscrete(),
                setter.getProviderType(), Arrays.asList(setter.getSaveData()));
        assertEquals(expected, dto);
    }

    @Test
    void debugSerialization() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        var io = new TerrainHeightIO(-5, 17);

        InputOutputDTO dto = InputOutputDTO.fromInputGetter(io);

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        var reconstructedIo = dto.toGetter();
        assertEquals(io, reconstructedIo);
    }

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldSerializeAndDeserializeInputOutputDTO() throws Exception {

        InputOutputDTO original = InputOutputDTO.fromInputGetter(new TerrainHeightIO(-64, 319));

        // serialize
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(original);

        // deserialize
        InputOutputDTO restored = mapper.readValue(json, InputOutputDTO.class);

        // object equality
        assertEquals(original, restored);

        // explicit verification of ioParameters
        assertNotNull(restored.getIoParameters());

        assertEquals(original.getIoParameters(), restored.getIoParameters());

        assertArrayEquals(original.getIoParameters().toArray(), restored.getIoParameters().toArray());
    }

    @Test
    public void fromDTO() {
        var setter = new AnnotationSetter();
        var dto = InputOutputDTO.fromInputGetter(new AnnotationSetter());
        var construced = dto.toGetter();

        assertEquals(setter, construced);
    }

    @Test
    public void fromDTOwithParams() {
        var io = new NibbleLayerSetter(PineForest.INSTANCE, false);
        var dto = InputOutputDTO.fromInputGetter(io);
        var constructed = dto.toGetter();

        assertEquals(io, constructed);
    }
}
