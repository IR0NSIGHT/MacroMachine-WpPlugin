package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.util.Objects;

public class ObjectState
{
    final CityLayer.Direction rotation;
    final boolean mirrored;
    final int objectIndex;
    final int xPos;
    final int yPos;
    public ObjectState(CityLayer.Direction rotation, boolean mirrored, int objectIndex, int xPos, int yPos) {
        this.rotation = rotation;
        this.mirrored = mirrored;
        this.objectIndex = objectIndex;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    @Override
    public String toString() {
        return "ObjectState{" + "rotation=" + rotation + ", mirrored=" + mirrored + ", objectIndex=" + objectIndex
                + ", xPos=" + xPos + ", yPos=" + yPos + '}';
    }
}
