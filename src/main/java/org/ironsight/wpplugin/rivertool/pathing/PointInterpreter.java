package org.ironsight.wpplugin.rivertool.pathing;

import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;

public class PointInterpreter {
    public enum PointType {
        POSITION_2D(new RiverHandleInformation.RiverInformation[0], 2), RIVER_2D(RiverHandleInformation.RiverInformation.values(), 2), ROAD_2D(new RiverHandleInformation.RiverInformation[0], 2);

        public final int size;
        public final int posSize;
        public final RiverHandleInformation.RiverInformation[] information;

        PointType(RiverHandleInformation.RiverInformation[] information, int posSize) {
            this.information = information;
            this.size = posSize + information.length;
            this.posSize = posSize;
        }
    }


}
