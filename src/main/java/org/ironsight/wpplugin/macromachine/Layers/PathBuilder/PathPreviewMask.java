package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point4f;

/**
 * Pure preview calculation; coordinates and masks are owned by the
 * snapshot/result.
 */
final class PathPreviewMask
{
    record Sample(float x, float y, float radius) {
    }

    record TilePosition(int x, int y) {
    }

    static List<Sample> snapshot(List<Point4f> path) {
        return path.stream().map(p -> new Sample(p.x, p.y, p.w)).toList();
    }

    static Map<TilePosition, BitSet> calculate(List<Sample> samples, List<TilePosition> tiles, float multiplier,
            CrossSectionShape profile) {
        var path = new ArrayList<Point4f>(samples.size());
        for (var sample : samples)
            path.add(new Point4f(sample.x(), sample.y(), 0, sample.radius()));
        var result = new HashMap<TilePosition, BitSet>();
        for (var tile : tiles) {
            int x = tile.x() << TILE_SIZE_BITS, y = tile.y() << TILE_SIZE_BITS;
            var subPath = PathToolBackend.getSubPathFor(new Point2i(x, y), new Point2i(x + TILE_SIZE, y + TILE_SIZE),
                    path, multiplier);
            if (subPath.isEmpty())
                continue;
            var ext = PathToolBackend.getMinMaxPos(subPath);
            var tree = new QuadTree(ext[0].x, ext[0].y, ext[1].x, ext[1].y);
            var data = new HashMap<Point2i, Point4f>();
            float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
            for (var point : subPath) {
                var pos = new Point2i(point.x, point.y);
                tree.insert(pos);
                data.put(pos, point);
                float radius = point.w * multiplier;
                minX = Math.min(minX, point.x - radius);
                minY = Math.min(minY, point.y - radius);
                maxX = Math.max(maxX, point.x + radius);
                maxY = Math.max(maxY, point.y + radius);
            }
            int left = Math.max(0, (int) Math.floor(minX - x));
            int top = Math.max(0, (int) Math.floor(minY - y));
            int right = Math.min(TILE_SIZE - 1, (int) Math.ceil(maxX - x));
            int bottom = Math.min(TILE_SIZE - 1, (int) Math.ceil(maxY - y));
            var bits = new BitSet(TILE_SIZE * TILE_SIZE);
            for (int xx = left; xx <= right; xx++)
                for (int yy = top; yy <= bottom; yy++) {
                    var query = new Point2i(x + xx, y + yy);
                    var closest = data.get(tree.getClosest(query));
                    if (closest == null)
                        continue;
                    float dx = closest.x - query.x, dy = closest.y - query.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= closest.w * multiplier && (distance <= closest.w || PathToolBackend
                            .filterStrengthFor((float) distance, closest.w, multiplier, profile) >= 1))
                        bits.set(yy * TILE_SIZE + xx);
                }
            if (!bits.isEmpty())
                result.put(tile, bits);
        }
        return result;
    }
}
