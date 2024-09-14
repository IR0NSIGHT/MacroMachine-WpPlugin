package org.demo.wpplugin.pathing;

public class PointInterpreter {
    public enum PointType {
        POSITION_2D(2),
        RIVER_2D(2+4),
        ROAD_2D(2);

        public final int size;
        PointType(int size) {
            this.size = size;
        }
    }


}
