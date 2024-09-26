package org.demo.wpplugin.operations.ApplyPath;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.RIVER_RADIUS;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.WATER_Z;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.getValue;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;
import static org.demo.wpplugin.pathing.PointUtils.getPositionalDistance;

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

        ArrayList<float[]> curve = p.continousCurve(false);
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
        for (float[] curveP : curve) {
            Point point = getPoint2D(curveP);
            startX = Math.min(startX, point.x);
            startY = Math.min(startY, point.y);
            endX = Math.max(endX, point.x);
            endY = Math.max(endY, point.y);
            maxRadius = Math.max(maxRadius, getValue(curveP, RIVER_RADIUS));
        }
        startX -= maxRadius;
        startY -= maxRadius;
        endX += maxRadius;
        endY += maxRadius;


        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                //every point (simmulates kernel)
                float[] self = new float[]{x, y};
                float[] closestOne = null;
                float closestDist = Float.MAX_VALUE;
                int closestI = Integer.MAX_VALUE;

                //test every curve point if this position is within the curve points radius
                for (int i = 0; i < curve.size(); i++) {
                    float[] curveP = curve.get(i);

                    float d = getPositionalDistance(self, curveP, 2);
                    if (d < getValue(curveP, RIVER_RADIUS) && d < closestDist) {
                        //i am a river
                        closestDist = d;
                        closestOne = curveP;
                        closestI = i;
                    }
                }
                if (closestOne != null) {
                    float waterZ = getValue(closestOne, WATER_Z);
                    assert waterZ >= 0;
                    dim.setHeight(x, y, waterZ);
                }
            }
        }

        for (float[] a : curve) {
            float waterZ = getValue(a, WATER_Z);
            assert waterZ >= 1 && waterZ <= 255;
            Point point = getPoint2D(a);
            dim.setHeight(point.x, point.y, waterZ);
        }
        for (float[] handle : p) {
            Point point = getPoint2D(handle);
            dim.setHeight(point.x, point.y, 255);
        }


     //   toImage(dim, 100, 100);

    //    toImage(curve1D(curve, WATER_Z), curve.size(), 255);
    }
}