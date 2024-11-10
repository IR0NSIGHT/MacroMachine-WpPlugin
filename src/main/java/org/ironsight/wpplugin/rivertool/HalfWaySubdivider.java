package org.ironsight.wpplugin.rivertool;

import java.util.Arrays;
import java.util.Random;

import static org.ironsight.wpplugin.rivertool.operations.ApplyPath.ApplyRiverOperation.angleOf;
import static org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation.INHERIT_VALUE;

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
    public float[] subdividePoints(float[] a, float[] b) {
        float[] out = new float[a.length];
        Arrays.fill(out, INHERIT_VALUE);
        float x1, x2, y1, y2;
        x1 = a[0];
        y1 = a[1];
        x2 = b[0];
        y2 = b[1];
        float dist = (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        float randomX = r.nextFloat() * (r.nextBoolean() ? 1 : -1);
        assert randomX > -1 && randomX < 1;
        double tangentAngle = angleOf(Math.round(x2 - x1), Math.round(y2 - y1));
        dist *= randomX * xRange;
        //a point 90° to a-b line and with distance = dist
        int x = (int) Math.round(dist * Math.cos(tangentAngle + Math.PI / 2));
        int y = (int) Math.round(dist * Math.sin(tangentAngle + Math.PI / 2));
        out[0] = (x1 + x2) / 2 + x;
        out[1] = (y1 + y2) / 2 + y;
        return out;
    }
}

