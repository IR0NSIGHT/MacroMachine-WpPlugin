package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;

import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO.BLOCK_VALUE;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.IGNORE_VALUE;

public class FilterUtils {
    /**
     *
     * @param filter
     * @param start
     * @param end
     * @param insideRangePass true=values between start and end will pass the filter, false: values outside of [start,end] will pass
     * @return
     */
    public static MappingAction asRangeFilter(MappingAction filter, int start, int end, boolean insideRangePass) {
        var outsideValue = insideRangePass ? BLOCK_VALUE : IGNORE_VALUE;
        var insideValue = insideRangePass ? IGNORE_VALUE : BLOCK_VALUE;
        return filter.withNewPoints(new MappingPoint[]{
                new MappingPoint(start, insideValue),
                new MappingPoint(end, insideValue),
                // put the caluclated ones at the end, bc they might get filtered out
                new MappingPoint(start-1, outsideValue),
                new MappingPoint(end+1, outsideValue)
        });
    }
}
