package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ObjectStateTest
{
    @Test
    void equalStatesCompareAllFields() {
        ObjectState state = new ObjectState(CityLayer.Direction.SOUTH, true, 3, -12, 19);

        assertEquals(state, new ObjectState(CityLayer.Direction.SOUTH, true, 3, -12, 19));
        assertEquals(state.hashCode(), new ObjectState(CityLayer.Direction.SOUTH, true, 3, -12, 19).hashCode());
        assertNotEquals(state, new ObjectState(CityLayer.Direction.NORTH, true, 3, -12, 19));
        assertNotEquals(state, new ObjectState(CityLayer.Direction.SOUTH, false, 3, -12, 19));
        assertNotEquals(state, new ObjectState(CityLayer.Direction.SOUTH, true, 4, -12, 19));
        assertNotEquals(state, new ObjectState(CityLayer.Direction.SOUTH, true, 3, -11, 19));
        assertNotEquals(state, new ObjectState(CityLayer.Direction.SOUTH, true, 3, -12, 20));
    }
}
