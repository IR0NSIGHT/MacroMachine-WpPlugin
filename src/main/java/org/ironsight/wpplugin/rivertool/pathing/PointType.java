package org.ironsight.wpplugin.rivertool.pathing;

import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;

    public class PointType {
        public static final PointType POSITION_2D = new PointType(new RiverHandleInformation.RiverInformation[0], 2);
        public static final PointType RIVER_2D = new PointType(RiverHandleInformation.RiverInformation.RIVER_INFORMATIONS, 2);

        public final int size;
        public final int posSize;
        public int handleStrengthIndex = -1;
        public final RiverHandleInformation.RiverInformation[] information;

        public PointType(RiverHandleInformation.RiverInformation[] information, int posSize) {
            this.information = information;
            this.size = posSize + information.length;
            this.posSize = posSize;
        }
    }


