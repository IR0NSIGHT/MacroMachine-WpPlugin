package org.demo.wpplugin;

import java.awt.*;

public class PointUtils {

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
}
