package org.demo.wpplugin;

public class HalfWaySubdivider implements Subdivide {
    public float subdividePoints(float x1, float x2) {
        return (x1 + x2) / 2f;
    }
}

