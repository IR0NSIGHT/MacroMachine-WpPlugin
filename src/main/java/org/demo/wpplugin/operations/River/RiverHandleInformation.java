package org.demo.wpplugin.operations.River;

public class RiverHandleInformation {
    public static float getValue(float[] point, RiverInformation information) {
        return point[PositionSize.SIZE_2_D.value + information.idx];
    }

    public static float[] setValue(float[] point, RiverInformation information, float value) {
        float[] out = point.clone();
        out[PositionSize.SIZE_2_D.value +information.idx] = value;
        return out;
    }

    public static float[] riverInformation(int x, int y, float riverRadius, float riverDepth, int beachRadius,
                                           int transitionRadius) {
        return new float[]{x, y, riverRadius, riverDepth, beachRadius, transitionRadius};
    }

    public static float[] riverInformation(int x, int y) {
        return new float[]{x, y, 10, 3, 5, 25};
    }

    public enum RiverInformation {
        RIVER_RADIUS(0),
        RIVER_DEPTH(1),
        BEACH_RADIUS(2),
        TRANSITION_RADIUS(3);
        public final int idx;

        RiverInformation(int idx) {
            this.idx = idx;
        }
    }

    public enum PositionSize {
        SIZE_1_D(1),
        SIZE_2_D(2),
        SIZE_3_D(3);
        public final int value;

        PositionSize(int idx) {
            this.value = idx;
        }
    }
}
