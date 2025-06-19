package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.FileIO.ActionJsonWrapper;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.*;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;

import javax.vecmath.Point2d;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;

class LayerMappingTest {

    @Test
    void serialize() {
        try {
            // Create an instance of the object
            MappingAction originalObject = new MappingAction(new TerrainHeightIO(-64,319),
                    new NibbleLayerSetter(Annotations.INSTANCE, false),
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
            MappingAction deserializedObject = (MappingAction) ois.readObject();
            assertEquals(originalObject, deserializedObject);

        } catch (IOException | ClassNotFoundException e) {
            fail("Exception occurred during serialization/deserialization: " + e.getMessage());
        }
    }

    @Test
    void serializeAll() {
        for (ProviderType type : ProviderType.values()) {
            IMappingValue provider = ProviderType.fromTypeDefault(type);
            MappingAction original = null;
            if (provider instanceof IPositionValueGetter) {
                original = new MappingAction((IPositionValueGetter) provider,
                        new TestInputOutput(),
                        new MappingPoint[]{new MappingPoint(provider.getMinValue(), 5),
                                new MappingPoint(provider.getMaxValue(), -3)},
                        ActionType.SET,
                        "test",
                        "test description",
                        UUID.randomUUID());
            } else if (provider instanceof IPositionValueSetter) {
                original = new MappingAction(new TestInputOutput(),
                        (IPositionValueSetter) provider,
                        new MappingPoint[]{new MappingPoint(provider.getMinValue(), 5),
                                new MappingPoint(provider.getMaxValue(), 1)},
                        ActionType.SET,
                        "test",
                        "test description",
                        UUID.randomUUID());
            } else {
                fail();
            }
            assertNotNull(original);
            ActionJsonWrapper wrapper = new ActionJsonWrapper(original);
            MappingAction restored = MappingAction.fromJsonWrapper(wrapper);
            assertEquals(original, restored, "wrapper changed " + original);
        }
    }

    @Test
    void discreteMap() {
        MappingAction action = new MappingAction(new TerrainHeightIO(-64,319),
                ActionFilterIO.instance,
                new MappingPoint[]{new MappingPoint(100, 0), new MappingPoint(200, 1), new MappingPoint(250, 0)},
                ActionType.SET,
                "",
                "",
                UUID.randomUUID());
        for (int input = action.input.getMinValue(); input <= action.input.getMaxValue(); input++) {
            int output = action.map(input);
            if (input <= 100) assertEquals(0, output, "" + input);
            else if (input <= 200) assertEquals(1, output, "" + input);
            else assertEquals(0, output, "" + input);
        }
    }

    @Test
    void map() {

        {   // SHORT RANGE MAPPING
            MappingAction linear = new MappingAction(new TestInputOutput(),
                    new TestInputOutput(),
                    new MappingPoint[]{new MappingPoint(1, 100),
                            new MappingPoint(6, 100 + 500),
                            new MappingPoint(11, 1000)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            assertEquals(100, linear.map(1));
            assertEquals(600, linear.map(6));
            assertEquals(1000, linear.map(11));

            assertEquals(100 + 500, linear.map(6));
            assertEquals(100 + 300, linear.map(4));
        }


        {   // LINEAR WITH 3 POINTS
            MappingAction linear = new MappingAction(new TestInputOutput(),
                    new TestInputOutput(),
                    new MappingPoint[]{new MappingPoint(10, 100),
                            new MappingPoint(50 + 10, 100 + 500),
                            new MappingPoint(110, 1000)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());

            assertEquals(100, linear.map(10));
            assertEquals(600, linear.map(60));
            assertEquals(1000, linear.map(110));

            assertEquals(100 + 500, linear.map(50 + 10));
            assertEquals(100 + 300, linear.map(30 + 10));
        }

        {   // STATIC ONE POINT
            MappingAction mapper = new MappingAction(new TestInputOutput(),
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
            MappingAction mapper = new MappingAction(new TerrainHeightIO(-64,319),
                    new NibbleLayerSetter(Annotations.INSTANCE, false),
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
        {   //one point
            Dimension dim = TestData.createDimension(new Rectangle(-2 * TILE_SIZE,
                    -2 * TILE_SIZE,
                    3 * TILE_SIZE,
                    3 * TILE_SIZE), 0);
            MappingAction mapper = new MappingAction(new TestInputOutput(),
                    new AnnotationSetter(),
                    new MappingPoint[]{new MappingPoint(57, 3)},
                    ActionType.SET,
                    "",
                    "",
                    UUID.randomUUID());
            mapper.input.prepareForDimension(dim);
            mapper.output.prepareForDimension(dim);
            ActionFilterIO.instance.prepareForDimension(dim);

            mapper.applyToPoint(dim, 0, 0);
            assertEquals(3, dim.getLayerValueAt(Annotations.INSTANCE, 0, 0));
        }
        {   // no points
            Dimension dim = TestData.createDimension(new Rectangle(-2 * TILE_SIZE,
                    -2 * TILE_SIZE,
                    3 * TILE_SIZE,
                    3 * TILE_SIZE), 0);
            MappingAction mapper = new MappingAction(new TestInputOutput(),
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

    @Test
    void calculateRanges() {
        {
            MappingAction mapper = new MappingAction(new TestInputOutput(),   //-5 .. 1000
                    new AnnotationSetter(), new MappingPoint[]{}, ActionType.SET, "", "", UUID.randomUUID());
            List<Point2d> ranges = MappingAction.calculateRanges(mapper);
            assertEquals(1, ranges.size());
            assertEquals(new Point2d(-5, 1000), ranges.get(0));
        }

        {
            MappingAction mapper = new MappingAction(new TestInputOutput(),   //-5 .. 1000
                    new AnnotationSetter(), new MappingPoint[]{
                            new MappingPoint(10,3), new MappingPoint(100,7)
            }, ActionType.SET, "", "", UUID.randomUUID());
            List<Point2d> ranges = MappingAction.calculateRanges(mapper);
            assertEquals(2, ranges.size());
            assertEquals(new Point2d(-5, 10), ranges.get(0));
            assertEquals(new Point2d(11, 1000), ranges.get(1));
            int total =0;
            for (Point2d range : ranges) {
                int outValue = mapper.map((int)range.x);
                for (int i = (int)range.x; i <= range.y; i++) {
                    assertEquals(mapper.map(i), outValue, "range did not contain only one output value" + range + " " +
                            "i="+i);
                    total++;
                }
            }

            assertEquals(mapper.input.getMaxValue()-mapper.input.getMinValue() +1, total,"ranges did not cover all " +
                    "input" +
                    " values");
        }
    }
}