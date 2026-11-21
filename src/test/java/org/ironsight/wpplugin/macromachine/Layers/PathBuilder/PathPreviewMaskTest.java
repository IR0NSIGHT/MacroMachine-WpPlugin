package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

import java.util.BitSet;
import java.util.List;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import org.junit.jupiter.api.Test;

class PathPreviewMaskTest
{
    @Test
    void matchesLegacyPaintAcrossPathsAndProfiles() {
        var nodes = List.of(new Point4f(-8, -2, 70, 0), new Point4f(4, 9, 80, 14), new Point4f(125, 15, 60, 2),
                new Point4f(130, -3, 75, 20));
        var profiles = List.of(new CrossSectionShape("triangle", "") {
        }, new CrossSectionShape("sqrt", "") {
            @Override
            public float getStrengthAt(float t) {
                return (float) Math.sqrt(t);
            }
        }, new CrossSectionShape("sin", "") {
            @Override
            public float getStrengthAt(float t) {
                return (float) (Math.sin((t - .5f) * Math.PI) / 2 + .5);
            }
        }, new CrossSectionShape("square", "") {
            @Override
            public float getStrengthAt(float t) {
                return t * t;
            }
        }, new CrossSectionShape("cubic", "") {
            @Override
            public float getStrengthAt(float t) {
                return t * t * t;
            }
        }, new CrossSectionShape("log", "") {
            final float log2 = (float) Math.log(2);
            @Override
            public float getStrengthAt(float t) {
                return (float) (Math.log(t + 1) / log2);
            }
        });
        var tiles = List.of(new PathPreviewMask.TilePosition(-1, -1), new PathPreviewMask.TilePosition(0, 0),
                new PathPreviewMask.TilePosition(1, 0));
        for (int count = 0; count <= nodes.size(); count++)
            for (float strength : new float[]{0, 1})
                for (float multiplier : new float[]{1, 2, 6})
                    for (var profile : profiles) {
                        var path = PathToolBackend.getPathFromNodes(nodes.subList(0, count), strength).path;
                        PathToolBackend.forceRadiusAtLeast(path, .5f);
                        compare(path, tiles, multiplier, profile);
                    }
        compare(List.of(new Point4f(0, 0, 1, 1), new Point4f(0, 0, 2, 30), new Point4f(8, 0, 3, 1),
                new Point4f(12, 0, 4, 25)), tiles, 6, profiles.getFirst());
    }

    void compare(List<Point4f> path, List<PathPreviewMask.TilePosition> tiles, float multiplier,
            CrossSectionShape profile) {
        var snapshot = PathPreviewMask.snapshot(path);
        var masks = PathPreviewMask.calculate(snapshot, tiles, multiplier, profile);
        for (var position : tiles) {
            int x = position.x() * TILE_SIZE, y = position.y() * TILE_SIZE;
            var subPath = PathToolBackend.getSubPathFor(new Point2i(x, y), new Point2i(x + TILE_SIZE, y + TILE_SIZE),
                    path, multiplier);
            var paint = new FloatTile(0);
            PathToolBackend.applyToTile(new FloatTile(0), paint, new FloatTile(0),
                    new Point3i(position.x(), position.y(), 0), profile, subPath, multiplier, false);
            var expected = new BitSet();
            for (int xx = 0; xx < TILE_SIZE; xx++)
                for (int yy = 0; yy < TILE_SIZE; yy++)
                    if (paint.getValueAt(xx, yy) >= 1)
                        expected.set(yy * TILE_SIZE + xx);
            assertEquals(expected, masks.getOrDefault(position, new BitSet()),
                    "tile " + position + " multiplier " + multiplier + " profile " + profile);
        }
    }

    @Test
    void snapshotDoesNotAliasMutablePoints() {
        var point = new Point4f(1, 2, 3, 4);
        var snapshot = PathPreviewMask.snapshot(List.of(point));
        point.set(9, 9, 9, 9);
        assertEquals(List.of(new PathPreviewMask.Sample(1, 2, 4)), snapshot);
    }
}
