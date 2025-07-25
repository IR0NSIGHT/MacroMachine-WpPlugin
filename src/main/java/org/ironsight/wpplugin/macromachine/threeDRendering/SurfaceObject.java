package org.ironsight.wpplugin.macromachine.threeDRendering;

import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurfaceObject implements WPObject {
    private String name = "Surface";
    @Override
    public String getName() {
        return name;
    }

    private float[][] height = new float[][]{
            new float[]{62,63,64,65,66},
            new float[]{62,63,64,65,66},
            new float[]{62,63,64,65,66},
            new float[]{62,63,64,65,66},
    };
    private float[][] waterheight =  new float[][]{
            new float[]{64,64,64,64,64},
            new float[]{64,64,64,64,64},
            new float[]{64,64,64,64,64},
            new float[]{64,64,64,64,64},
    };
    Material[][] terrain = new Material[][]{
            new Material[]{Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK},
            new Material[]{Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK},
            new Material[]{Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK},
            new Material[]{Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK,Material.GRASS_BLOCK},
    };

    public void setTerrainData(float[][] height, Material[][] terrain, float[][] waterheight) {
        min = 10000;
        max = 0;
        this.height = height;
        this.terrain = terrain;
        this.waterheight = waterheight;
        for (float[] row: height)
            for (float p : row) {
                max = Math.max(max,p);
                min = Math.min(min,p);
            }
        for (float[] row: waterheight)
            for (float p : row) {
                max = Math.max(max,p);
            }
    }

    private float min = 0, max = 0;

    @Override
    public void setName(String s) {
        name = s;
    }


    @Override
    public Point3i getDimensions() {
        try {
            return new Point3i(terrain.length, terrain[0].length, (int) Math.ceil(max-min));
        } catch (Exception ex) {
            return new Point3i(0,0,0);
        }
    }

    @Override
    public Point3i getOffset() {
        return new Point3i(-getDimensions().x/2,-getDimensions().y/2,-getDimensions().z/2);
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        float terrainH = height[x][y];
        float waterHeigt = waterheight[x][y];
        if (terrainH < (z+min) && (z+min) <= waterHeigt)
            return Material.WATER;
        return terrain[x][y];
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return height[x][y] >= (z+min) || waterheight[x][y] >= (z+min);
    }

    @Override
    public List<Entity> getEntities() {
        return Collections.emptyList();
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return Collections.emptyList();
    }

    @Override
    public void prepareForExport(Dimension dimension) {

    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return new HashMap<>();
    }

    @Override
    public void setAttributes(Map<String, Serializable> map) {

    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> attributeKey, T t) {

    }

    @Override
    public WPObject clone() {
        return new SurfaceObject();
    }
}
