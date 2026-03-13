package org.ironsight.wpplugin.rivertool.API;

import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.operations.ContinuousCurve;
import org.ironsight.wpplugin.rivertool.pathing.Path;
import org.ironsight.wpplugin.rivertool.pathing.PointType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

final class RiverToolAPIImplementation implements RiverToolAPI
{

    /**
     * take a bunch of handles and interpolate all positions between them so they
     * build a connected path using bezier curves. takes x and y for bezier, rest is
     * interpolated linear
     *
     * @param handles
     * @return
     */
    @Override
    public ArrayList<RiverToolAPI.PositionInformation> handlesToConnectedBezierPath(
            Collection<RiverToolAPI.PositionInformation> handles, PointType handleDataDescriptor) {
        List<float[]> handleData = new ArrayList<>(handles.size());
        for (var point : handles) {
            handleData.add(point.data);
        }
        var path = new Path(handleData, handleDataDescriptor);
        var connectedCurve = ContinuousCurve.fromPath(path, HeightDimension.getImmutableDimension62());

        return connectedCurve.getCurveAsPositions()
                .stream()
                .map(PositionInformation::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
