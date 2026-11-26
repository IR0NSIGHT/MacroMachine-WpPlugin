package org.ironsight.wpplugin.macromachine.threeDRendering;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.ironsight.cubearray.render.CubeSetup;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;

public class RenderFixtures
{
    public static CubeSetup setup(String name) {
        BufferedImage atlas = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        atlas.setRGB(0, 0, 0xFFFFFFFF);
        Vector4f[] uv = new Vector4f[6];
        Arrays.setAll(uv, index -> new Vector4f(0, 0, 1, 1));
        return new CubeSetup(new Vector3f[]{new Vector3f()}, new int[]{0}, new Vector3f[]{new Vector3f(1)},
                new Vector3f[]{new Vector3f(1)}, new Vector3f[]{new Vector3f()}, new Vector3f[]{new Vector3f()}, uv,
                atlas, new Vector3f(-1), new Vector3f(1), name, false);
    }

    static WPObject object(String name) {
        return new GenericObject(name, 1, 1, 1, new Material[]{Material.STONE});
    }
}
