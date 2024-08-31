package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.BoundingBox;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class PathGeometryHelper {
    private final Path path;
    private final Collection<BoundingBox> boundingBoxes;

    public PathGeometryHelper(Path path) {
        this.path = path;
        boundingBoxes = new ArrayList<>(Math.max(0, path.amountHandles() - 3));
        for (int i = 0; i < path.amountHandles() - 3; i++) {
            BoundingBox box = CubicBezierSpline.boundingBoxCurveSegment(path.handleByIndex(i),
                    path.handleByIndex(i + 1), path.handleByIndex(i + 2), path.handleByIndex(i + 3));
            boundingBoxes.add(box);
        }
    }

    public static void main(String[] args) {
        Path p = new Path(Arrays.asList(new Point(0, 0), new Point(-2, 5), new Point(4, 4), new Point(5, 5)));
        PathGeometryHelper o = new PathGeometryHelper(p);
    }

}
