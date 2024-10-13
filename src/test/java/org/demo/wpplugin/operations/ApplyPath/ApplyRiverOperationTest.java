package org.demo.wpplugin.operations.ApplyPath;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;

import static org.demo.wpplugin.geometry.HeightDimension.getDummyDimension62;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.RIVER_RADIUS;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.WATER_Z;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;

class ApplyRiverOperationTest {

    public static float maxZ = 0f;


    @Test
    void applyRiverPath() {
        Path p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.RIVER_2D);
        p = p.addPoint(RiverHandleInformation.riverInformation(10, 10, 3, 6, 7, 30, 255));
        p = p.addPoint(RiverHandleInformation.riverInformation(11, 10));

        p = p.addPoint(RiverHandleInformation.riverInformation(20, 30));
        p = p.addPoint(RiverHandleInformation.riverInformation(50, 30));

        p = p.addPoint(RiverHandleInformation.riverInformation(50, 70));
        p = p.addPoint(RiverHandleInformation.riverInformation(50, 71, 10, 6, 7, 8, 128));

        ContinuousCurve curve = ContinuousCurve.fromPath(p, getDummyDimension62());
    /*
        for (float[] a : curve) {
            assertEquals(5, getValue(a, RIVER_RADIUS), 0.01f);
            assertEquals(6, getValue(a, RIVER_DEPTH), 0.01f);
            assertEquals(7, getValue(a, BEACH_RADIUS), 0.01f);
            assertEquals(30, getValue(a, TRANSITION_RADIUS), 0.01f);
        }
    */

        HeightDimension dim = new HeightDimension() {
            final HashMap<Point, Float> heightMap = new HashMap<>();

            @Override
            public float getHeight(int x, int y) {
                return heightMap.getOrDefault(new Point(x, y), 0f);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                maxZ = Math.max(z, maxZ);
                heightMap.put(new Point(x, y), z);
            }
        };

        //find bounding box of river
        int startX = 0, startY = 0, endX = 0, endY = 0;
        float maxRadius = 0;
        for (int i = 0; i < curve.curveLength(); i++) {
            Point point = curve.getPos(i);
            startX = Math.min(startX, point.x);
            startY = Math.min(startY, point.y);
            endX = Math.max(endX, point.x);
            endY = Math.max(endY, point.y);
            maxRadius = Math.max(maxRadius, curve.getInfo(RIVER_RADIUS, i));
        }
        startX -= maxRadius;
        startY -= maxRadius;
        endX += maxRadius;
        endY += maxRadius;


        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                //every point (simmulates kernel)
                Point self = new Point(x, y);
                int closestOne = -1;
                float closestDist = Float.MAX_VALUE;

                //test every curve point if this position is within the curve points radius
                for (int i = 0; i < curve.curveLength(); i++) {
                    float d = (float) self.distance(curve.getPos(i));
                    if (d < curve.getInfo(RIVER_RADIUS, i) && d < closestDist) {
                        //i am a river
                        closestDist = d;
                        closestOne = i;
                    }
                }
                if (closestOne != -1) {
                    float waterZ = curve.getInfo(WATER_Z, closestOne);
                    assert waterZ >= 0;
                    dim.setHeight(x, y, waterZ);
                }
            }
        }

        for (int i = 0; i < curve.curveLength(); i++) {
            float waterZ = curve.getInfo(WATER_Z, i);
            assert waterZ >= 1 && waterZ <= 255;
            Point point = curve.getPos(i);
            dim.setHeight(point.x, point.y, waterZ);
        }
        for (float[] handle : p) {
            Point point = getPoint2D(handle);
            dim.setHeight(point.x, point.y, 255);
        }
    }
}