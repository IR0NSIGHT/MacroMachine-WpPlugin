package org.ironsight.wpplugin.rivertool.API;

import org.ironsight.wpplugin.rivertool.pathing.PointType;

import java.util.ArrayList;
import java.util.Collection;

public interface RiverToolAPI {

    /**
     * take a bunch of handles and interpolate all positions between them so they build a connected path using bezier curves takes x and y for bezier, rest is interpolated linear
     *
     * @param handles
     * @return
     */
    ArrayList<PositionInformation> handlesToConnectedBezierPath(Collection<PositionInformation> handles, PointType handleDataDescriptor);

    class PositionInformation {
        public final float[] data;

        public PositionInformation(float[] data) {
            this.data = data.clone();
        }
    }

}

