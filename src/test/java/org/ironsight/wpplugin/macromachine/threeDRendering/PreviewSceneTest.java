package org.ironsight.wpplugin.macromachine.threeDRendering;

import static org.junit.jupiter.api.Assertions.*;

import javax.vecmath.Point3i;
import org.junit.jupiter.api.Test;
import org.pepsoft.minecraft.Material;

class PreviewSceneTest
{
    @Test
    void welcomeHasOneGrassBlockWithAFlowerDirectlyAboveIt() {
        var object = PreviewScene.welcomeObject();
        assertEquals(new Point3i(1, 1, 2), object.getDimensions());
        assertEquals(Material.GRASS_BLOCK, object.getMaterial(0, 0, 0));
        assertEquals(Material.get("minecraft:dandelion"), object.getMaterial(0, 0, 1));
        assertTrue(object.getMask(0, 0, 0));
        assertTrue(object.getMask(0, 0, 1));
    }
}
