package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;

public class PointInterpreter {
    public enum PointType {
        POSITION_2D(2),
        RIVER_2D(2 + RiverHandleInformation.RiverInformation.values().length),
        ROAD_2D(2);

        public final int size;

        PointType(int size) {
            this.size = size;
        }
    }


}
