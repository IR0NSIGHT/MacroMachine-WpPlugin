package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.*;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;

import java.awt.*;
import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;

class LayerMappingTest {

    @Test
    void serialize() {
        try {
            // Create an instance of the object
            LayerMapping originalObject = new LayerMapping(new HeightProvider(),
                    new NibbleLayerSetter(Annotations.INSTANCE),
                    new MappingPoint[]{new MappingPoint(7, 12)},
                    ActionType.DIVIDE,
                    "hello",
                    "world with a space",
                    UUID.randomUUID());

            // Serialize the object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(originalObject);
            oos.close();

            // Deserialize the object
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            LayerMapping deserializedObject = (LayerMapping) ois.readObject();
            assertEquals(originalObject, deserializedObject);

        } catch (IOException | ClassNotFoundException e) {
            fail("Exception occurred during serialization/deserialization: " + e.getMessage());
        }
    }

    @Test
    void serializeAll() {
        for (ProviderType type : ProviderType.values()) {
            IMappingValue provider = ProviderType.fromTypeDefault(type);
            LayerMapping original = null;
            if (provider instanceof IPositionValueGetter) {
                original = new LayerMapping((IPositionValueGetter) provider,
                        new TestInputOutput(),
                        new MappingPoint[]{new MappingPoint(provider.getMinValue(), 5),
                                new MappingPoint(provider.getMaxValue(), -3)},
                        ActionType.SET,
                        "test",
                        "test description",
                        UUID.randomUUID());
            } else if (provider instanceof IPositionValueSetter) {
                original = new LayerMapping(new TestInputOutput(),
                        (IPositionValueSetter) provider,
                        new MappingPoint[]{new MappingPoint(provider.getMinValue(), 5),
                                new MappingPoint(provider.getMaxValue(), -3)},
                        ActionType.SET,
                        "test",
                        "test description",
                        UUID.randomUUID());
            } else {
                fail();
            }
            assertNotNull(original);
            ActionJsonWrapper wrapper = new ActionJsonWrapper(original);
            LayerMapping restored = LayerMapping.fromJsonWrapper(wrapper);
            assertEquals(original, restored);
        }
    }

    @Test
    void discreteMap() {
        LayerMapping action = new LayerMapping(
                new HeightProvider(),
                IntermediateSelectionIO.instance,
                new MappingPoint[]{ new MappingPoint(100,0),
                new MappingPoint(200, 1),
                new MappingPoint(250,0)},
                ActionType.SET,
                "",
                "",
                UUID.randomUUID()
        );
        for (int input = action.input.getMinValue(); input <= action.input.getMaxValue(); input++) {
            int output = action.map(input);
            if (input <= 100)
                assertEquals(0, output,""+input);
            else if (input <= 200)
                assertEquals(1, output, ""+input);
            else
                assertEquals(0, output, ""+input);
        }
    }

    @Test
    void map() {

        {   // SHORT RANGE MAPPING
            LayerMapping linear = new LayerMapping(new TestInputOutput(),
                    new TestInputOutput(),
                    new MappingPoint[]{new MappingPoint(1, 100),
                            new MappingPoint(6, 100 + 500),
                            new MappingPoint(11, 1100)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            assertEquals(100, linear.map(1));
            assertEquals(600, linear.map(6));
            assertEquals(1100, linear.map(11));

            assertEquals(100 + 500, linear.map(6));
            assertEquals(100 + 300, linear.map(4));
        }


        {   // LINEAR WITH 3 POINTS
            LayerMapping linear = new LayerMapping(new TestInputOutput(),
                    new TestInputOutput(),
                    new MappingPoint[]{new MappingPoint(10, 100),
                            new MappingPoint(50 + 10, 100 + 500),
                            new MappingPoint(110, 1100)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            assertEquals(100, linear.map(10));
            assertEquals(600, linear.map(60));
            assertEquals(1100, linear.map(110));

            assertEquals(100 + 500, linear.map(50 + 10));
            assertEquals(100 + 300, linear.map(30 + 10));
        }

        {   // STATIC ONE POINT
            LayerMapping mapper = new LayerMapping(new TestInputOutput(),
                    new TestInputOutput(),
                    new MappingPoint[]{new MappingPoint(57, 89)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            for (int i = new TestInputOutput().getMinValue(); i < new TestInputOutput().getMaxValue(); i++) {
                assertEquals(89, mapper.map(i));
            }
        }

        {   // 2 POINT LINEAR AT FIRST THAN PLATEAU
            LayerMapping mapper = new LayerMapping(new HeightProvider(),
                    new NibbleLayerSetter(Annotations.INSTANCE),
                    new MappingPoint[]{new MappingPoint(50, 0), new MappingPoint(150, 10),},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            for (int i = 0; i < 50; i++) { //plateau before first point
                assertEquals(0, mapper.map(i), i + "->" + mapper.map(i));
            }
            for (int i = 50; i < 150; i++) { //linear between points
                assertEquals(Math.round(((float) i - 50) / 10), mapper.map(i), i + "->" + mapper.map(i));
            }
            for (int i = 150; i < 300; i++) { //plateau after second point
                assertEquals(10, mapper.map(i));
            }
        }
    }

    @Test
    void applyToPoint() {   //an most simple test to check it can run without crashing
        IntermediateSelectionIO.instance.setSelected(true);
        {   //one point
            Dimension dim = TestData.createDimension(new Rectangle(-2 * TILE_SIZE,
                    -2 * TILE_SIZE,
                    3 * TILE_SIZE,
                    3 * TILE_SIZE), 0);
            LayerMapping mapper = new LayerMapping(new TestInputOutput(),
                    new AnnotationSetter(),
                    new MappingPoint[]{new MappingPoint(57, 3)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());
            mapper.input.prepareForDimension(dim);
            mapper.output.prepareForDimension(dim);
            mapper.applyToPoint(dim, 0, 0);
            assertEquals(3, dim.getLayerValueAt(Annotations.INSTANCE, 0, 0));
        }
        {   // no points
            Dimension dim = TestData.createDimension(new Rectangle(-2 * TILE_SIZE,
                    -2 * TILE_SIZE,
                    3 * TILE_SIZE,
                    3 * TILE_SIZE), 0);
            LayerMapping mapper = new LayerMapping(new TestInputOutput(),
                    new AnnotationSetter(),
                    new MappingPoint[]{},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            mapper.applyToPoint(dim, 0, 0);
            assertEquals(0, dim.getLayerValueAt(Annotations.INSTANCE, 0, 0));
        }
    }
}