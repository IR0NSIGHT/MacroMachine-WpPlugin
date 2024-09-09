package org.demo.wpplugin.pathing;

public interface Interpolatable {
    public Interpolatable interpolateWith(float t, Interpolatable other);
}
