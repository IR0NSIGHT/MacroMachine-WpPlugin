package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PointUtils {

    public static Rectangle pointExtent(Rectangle extent) {
        Rectangle rect = new Rectangle(extent);
        rect.setBounds(extent.x*128,extent.y*128,extent.width*128,extent.height*128);
        return rect;
    }

    // Method to add two points
    public static Point add(Point p1, Point p2) {
        return new Point(p1.x + p2.x, p1.y + p2.y);
    }

    // Method to subtract two points
    public static Point subtract(Point p1, Point p2) {
        return new Point(p1.x - p2.x, p1.y - p2.y);
    }

    // Method to multiply a point by a factor
    public static Point multiply(Point p, float factor) {
        return new Point(Math.round(p.x * factor), Math.round(p.y * factor));
    }

    // Method to divide a point by a factor
    public static Point divide(Point p, float factor) {
        if (factor == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed.");
        }
        return new Point(Math.round(p.x / factor), Math.round(p.y / factor));
    }

    // Method to normalize a point
    public static Point normalize(Point p) {
        float magnitude = (float)Math.sqrt(p.x * p.x + p.y * p.y);
        if (magnitude == 0) {
            throw new IllegalArgumentException("Cannot normalize a point at the origin (0,0).");
        }
        return divide(p, magnitude);
    }

    public static double calculatePathLength(Point[] path) {
        if (path == null || path.length < 2) {
            throw new IllegalArgumentException("Path must contain at least two points.");
        }
        double totalLength = 0.0;
        for (int i = 1; i < path.length; i++) {
            totalLength += path[i - 1].distance(path[i]);
        }
        return totalLength;
    }

    public static ArrayList<AxisAlignedBoundingBox2d> toBoundingBoxes(ArrayList<Point> curve, int boxSizeFactor,
                                                                      double radius) {
        ArrayList<AxisAlignedBoundingBox2d> bbxs = new ArrayList<>(curve.size() / boxSizeFactor + 1);
        for (int i = 0; i < curve.size(); i += boxSizeFactor) {
            int boxId = i / boxSizeFactor;
            List<Point> subcurve = curve.subList(i, Math.min(i + boxSizeFactor, curve.size()));
            AxisAlignedBoundingBox2d box = new AxisAlignedBoundingBox2d(subcurve, boxId).expand(radius);
            bbxs.add(box);
            assert box.id == boxId;
        }
        return bbxs;
    }

    public static void drawCircle(Point center, float radius, Dimension dimension, Layer layer) {
        int radiusI = Math.round(radius);
        for (int x = -radiusI; x <= radiusI; x++) {
            for (int y = -radiusI; y <= radiusI; y++) {
                Point p = new Point(center.x + x, center.y + y);
                if (center.distance(p) <= radius && center.distance(p) >= radiusI - 1) {
                    dimension.setLayerValueAt(layer, p.x, p.y, 15);
                }
            }
        }
    }

    /**
     * draws an X on the map in given color and size
     *
     * @param p
     * @param layer
     * @param color
     * @param size, 0 size = single dot on map
     */
    public static void markPoint(Point p, Layer layer, int color, int size, Dimension dim) {
        for (int i = -size; i <= size; i++) {
            dim.setLayerValueAt(layer, p.x + i, p.y - i, color);
            dim.setLayerValueAt(layer, p.x + i, p.y + i, color);
        }
    }

    public static void markLine(Point p0, Point p1, Layer layer, int color, Dimension dim) {
        double length = p0.distance(p1);
        for (double i = 0; i <= length; i++) {
            double factor = i / length;
            Point inter = new Point((int) (p0.x * factor + p1.x * (1 - factor)),
                    (int) (p0.y * factor + p1.y * (1 - factor)));
            dim.setLayerValueAt(layer, inter.x, inter.y, color);
        }
    }

    static float getPositionalLength(float[] pointA, int positionDigits) {
        //euclidian distance of B and C positions
        float dist = 0;
        for (int i = 0; i < positionDigits; i++) {
            float distI = pointA[i];
            dist += distI * distI;
        }
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    public static float getPositionalDistance(float[] pointA, float[] pointB, int positionDigits) {
        //euclidian distance of B and C positions
        float dist = 0;
        for (int i = 0; i < positionDigits; i++) {
            float distI = pointA[i] - pointB[i];
            dist += distI * distI;
        }
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    public static boolean arePositionalsEqual(float[] pointA, float[] pointB, int positionDigits) {
        return point2dFromN_Vector(pointA).equals(point2dFromN_Vector(pointB));
    }

    public static Point point2dFromN_Vector(float[] nVector) {
        if (nVector == null)
            return null;
        return new Point(Math.round(nVector[0]), Math.round(nVector[1]));
    }

    public static ArrayList<Point> point2DfromNVectorArr(ArrayList<float[]> points ) {
        ArrayList<Point> out = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            out.add(point2dFromN_Vector(points.get(i)));
        }
        return out;
    }

    public static float[] setPosition2D(float[] point, float[] position) {
        float[] out = point.clone();
        for (int i = 0; i < RiverHandleInformation.PositionSize.SIZE_2_D.value; i++) {
            out[i] = position[i];
        }
        return out;
    }
}
