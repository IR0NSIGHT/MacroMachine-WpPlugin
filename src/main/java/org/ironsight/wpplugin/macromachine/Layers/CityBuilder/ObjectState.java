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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ObjectState state)) {
            return false;
        }
        return mirrored == state.mirrored && objectIndex == state.objectIndex && xPos == state.xPos
                && yPos == state.yPos && rotation == state.rotation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rotation, mirrored, objectIndex, xPos, yPos);
    }
}
