package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

public class ObjectState {
    final CityLayer.Direction rotation;
    final boolean mirrored;
    final int objectIndex;


    public ObjectState(CityLayer.Direction rotation, boolean mirrored, int objectIndex) {
        this.rotation = rotation;
        this.mirrored = mirrored;
        this.objectIndex = objectIndex;
    }
}
