package org.demo.wpplugin.geometry;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class Smoother {
    final HashMap<Point, Float> xSmoothedPoints;
    final int radius;
    final HeightDimension dimension;
    final Collection<Point> points;

    public Smoother(Collection<Point> points, int radius, HeightDimension dimension) {
        this.radius = radius;
        this.xSmoothedPoints = new HashMap<>(points.size());
        this.dimension = dimension;
        this.points = points;
    }

    public void smoothAverage() {
        float[] kernel = new float[2 * radius + 1];
        float sum = kernel.length;
        Arrays.fill(kernel, 1);

        smoothPoints(kernel, sum);
    }

    public void smoothPoints(float[] kernel, float kernelSum) {
        //smooth in x dir, store locally to not influence other points being calculated
        for (Point curvePoint : points) {
            float sum = 0;
            for (int x = 0; x < kernel.length; x++) {
                float factor = kernel[x];
                int xPos = curvePoint.x + x - radius;
                int yPos = curvePoint.y;
                if (curvePoint.equals(new Point(5,-10))) {
                    System.out.println("smooth x: ("+xPos + "," + yPos+ "), z="+dimension.getHeight(xPos, yPos)+ " factor="+factor);
                }
                sum += dimension.getHeight(xPos, yPos) * factor;
            }
            sum /= kernelSum;
            if (curvePoint.equals(new Point(5,-10))) {
                System.out.println("final z: "+sum);
            }
            xSmoothedPoints.put(curvePoint, sum);
        }

        //write back x smoothed points to dimension
        for (Point point : points) {
            dimension.setHeight(point.x, point.y, xSmoothedPoints.get(point));
        }
        xSmoothedPoints.clear();

        //smooth in x dir
        for (Point curvePoint : points) {
            float sum = 0;
            for (int y = 0; y < kernel.length; y++) {
                float factor = kernel[y];
                if (curvePoint.equals(new Point(5,-10))) {
                    System.out.println("smooth y: ("+curvePoint.x + "," + (curvePoint.y + y - radius)+ "), z="+dimension.getHeight(curvePoint.x, curvePoint.y + y - radius)+ " factor="+factor);
                }
                sum += dimension.getHeight(curvePoint.x, curvePoint.y + y - radius) * factor;
            }
            sum /= kernelSum;
            if (curvePoint.equals(new Point(5,-10))) {
                System.out.println("final z: "+sum);
            }
            xSmoothedPoints.put(curvePoint, sum);
        }
        //write back x smoothed points to dimension
        for (Point point : points) {
            dimension.setHeight(point.x, point.y, xSmoothedPoints.get(point));
        }
    }
}
