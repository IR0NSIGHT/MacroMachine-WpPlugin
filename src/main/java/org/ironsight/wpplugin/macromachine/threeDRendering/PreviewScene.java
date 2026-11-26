package org.ironsight.wpplugin.macromachine.threeDRendering;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;

final class PreviewScene
{
    static WPObject welcomeObject() {
        return new GenericObject("3D Preview", 1, 1, 2,
                new Material[]{Material.GRASS_BLOCK, Material.get("minecraft:dandelion")});
    }
}
