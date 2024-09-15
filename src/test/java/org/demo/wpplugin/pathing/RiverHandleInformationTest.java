package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.PaintDimension;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.RIVER_RADIUS;
import static org.demo.wpplugin.pathing.PointInterpreter.PointType.RIVER_2D;
import static org.demo.wpplugin.pathing.PointUtils.setPosition2D;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RiverHandleInformationTest {
    @Test
    public void drawRiverPath() {
        ArrayList<float[]> handles = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            float[] handle = new float[RIVER_2D.size];
            setPosition2D(handle,new float[]{i, 2*i});
            handle = setValue(handle, RIVER_RADIUS, INHERIT_VALUE);
            handles.add(handle);
        }
        Path p = new Path(handles, RIVER_2D);
        for (float[] handle : handles) {
            assertEquals(INHERIT_VALUE, getValue(handle,RIVER_RADIUS));
        }

        PaintDimension dim = new PaintDimension() {
            @Override
            public int getValue(int x, int y) {
                return 0;
            }

            @Override
            public void setValue(int x, int y, int v) {

            }
        };
        Path clone = p.clone();
        DrawRiverPath(p, dim);
        for (float[] handle : handles) {
            assertEquals(INHERIT_VALUE, getValue(handle,RIVER_RADIUS), "final values were changed");
        }
        assertEquals(clone, p, "path was mutated by drawing it");
    }
}