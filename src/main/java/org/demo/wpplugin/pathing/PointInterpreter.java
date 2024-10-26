package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation;

public class PointInterpreter {
    public enum PointType {
        POSITION_2D(new RiverInformation[0], 2), RIVER_2D(RiverInformation.values(), 2), ROAD_2D(new RiverInformation[0], 2);

        public final int size;
        public final int posSize;
        public final RiverInformation[] information;

        PointType(RiverInformation[] information, int posSize) {
            this.information = information;
            this.size = posSize + information.length;
            this.posSize = posSize;
        }
    }


}
