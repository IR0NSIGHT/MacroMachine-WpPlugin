package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import java.util.Objects;

public class Point2i {
    public float x, y;

    public Point2i(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float distanceSquared(Point2i other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point2i point2i)
            return point2i.x == x && point2i.y == y;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("%.2f,%.2f", x, y);
    }
}
