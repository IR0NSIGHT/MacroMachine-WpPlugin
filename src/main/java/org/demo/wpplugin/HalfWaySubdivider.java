package org.demo.wpplugin;

import javax.vecmath.Vector2f;
import java.util.Random;

import static org.demo.wpplugin.operations.ApplyPath.ApplyRiverOperation.angleOf;

public class HalfWaySubdivider implements Subdivide {
    private final float xRange;
    private final float yRange;
    private final boolean relative;
    private final Random r;
    public HalfWaySubdivider(float xRange, float yRange, boolean relative) {
        this.xRange = xRange;
        this.yRange = yRange;
        this.relative = relative;
        r = new Random(42069);
    }

    @Override
    public float[] subdividePoints(float x1, float x2, float y1, float y2) {
        float halfWayX = (x1 + x2) / 2f;
        float halfWayY = (y1+y2) /2f;
        float dist = (float)Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
        float randomX = r.nextFloat() * (r.nextBoolean() ? 1 : -1);
        assert randomX > -1 && randomX < 1;
        double tangentAngle = angleOf(Math.round(x2-x1), Math.round(y2-y1));
        dist *= randomX;
        int x = (int) Math.round(dist * xRange * Math.cos(tangentAngle+Math.PI/2));
        int y = (int) Math.round(dist * xRange * Math.sin(tangentAngle+Math.PI/2));

        return new float[]{ halfWayX + x, halfWayY + y};
    }
}

