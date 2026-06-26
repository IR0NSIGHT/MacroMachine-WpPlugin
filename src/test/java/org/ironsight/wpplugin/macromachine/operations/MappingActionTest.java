package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.FilterUtils;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.NibbleLayerSetter;
import org.junit.jupiter.api.Test;
import org.pepsoft.worldpainter.layers.DeciduousForest;

import java.util.Arrays;
import java.util.UUID;

import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO.BLOCK_VALUE;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.IGNORE_VALUE;
import static org.junit.jupiter.api.Assertions.*;

class FilterUtilsTest
{
    @Test
    void asOutsideRangeFilter() {
        MappingAction filter = new MappingAction(new NibbleLayerSetter(DeciduousForest.INSTANCE, false),
                new ActionFilterIO(), new MappingPoint[]{new MappingPoint(5, IGNORE_VALUE)}, ActionType.SET,
                "my test range filter", "descr", UUID.randomUUID());
        {
            var outputs = Arrays.stream(filter.getInput().getAllInputValues()).map(filter::map).toArray();
            assertTrue(Arrays.stream(outputs).allMatch(o -> o == IGNORE_VALUE));
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 4, 9, false);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            // array shows which index of input will pass (true) or be filtered out (false)
            assertArrayEquals(new Boolean[]{true, true, true, true, false, false, false, false, false, false, true,
                    true, true, true, true, true}, passingOutputs, "outside range filter failed");
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 0, 9, false);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            assertArrayEquals(new Boolean[]{false, false, false, false, false, false, false, false, false, false, true,
                    true, true, true, true, true}, passingOutputs, "outside range filter failed");
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 9, 15, false);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            assertArrayEquals(new Boolean[]{true, true, true, true, true, true, true, true, true, false, false, false,
                    false, false, false, false}, passingOutputs, "outside range filter failed");
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 0, 15, false);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            assertArrayEquals(new Boolean[]{false, false, false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false}, passingOutputs, "outside range filter failed");
        }
    }
    @Test
    void asInsideRangeFilter() {
        MappingAction filter = new MappingAction(new NibbleLayerSetter(DeciduousForest.INSTANCE, false),
                new ActionFilterIO(), new MappingPoint[]{new MappingPoint(5, IGNORE_VALUE)}, ActionType.SET,
                "my test range filter", "descr", UUID.randomUUID());
        {
            var outputs = Arrays.stream(filter.getInput().getAllInputValues()).map(filter::map).toArray();
            assertTrue(Arrays.stream(outputs).allMatch(o -> o == IGNORE_VALUE));
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 4, 9, true);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            // array shows which index of input will pass (true) or be filtered out (false)
            assertArrayEquals(new Boolean[]{false, false, false, false, true, true, true, true, true, true, false,
                    false, false, false, false, false}, passingOutputs, "inside range filter failed");
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 0, 9, true);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            assertArrayEquals(new Boolean[]{true, true, true, true, true, true, true, true, true, true, false, false,
                    false, false, false, false}, passingOutputs, "inside range filter failed");
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 9, 15, true);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            assertArrayEquals(new Boolean[]{false, false, false, false, false, false, false, false, false, true, true,
                    true, true, true, true, true}, passingOutputs, "inside range filter failed");
        }
        {
            var rangeFilter = FilterUtils.asRangeFilter(filter, 0, 15, true);
            var passingOutputs = Arrays.stream(rangeFilter.getInput().getAllInputValues())
                    .map(rangeFilter::map)
                    .mapToObj(o -> o != BLOCK_VALUE)
                    .toArray();
            assertArrayEquals(new Boolean[]{true, true, true, true, true, true, true, true, true, true, true, true,
                    true, true, true, true}, passingOutputs, "inside range filter failed");
        }
    }
}