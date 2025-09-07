package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.painting.Paint;

import javax.vecmath.Point2f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static java.lang.Math.*;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE_BITS;

public class RoadToolBackend {
    /**
     * generates path between positions, excludes startPos
     *
     * @param startPos
     * @param endPos
     */
    static ArrayList<Point4f> plotPathBetween(Point4f startPos, Point4f endPos) {
        ArrayList<Point4f> path = new ArrayList<>();
        float steps = max(abs(endPos.x - startPos.x), abs(endPos.y - startPos.y));
        path.ensureCapacity((int) ceil(steps));

        for (int i = 1; i < steps; i++) {
            float t = i / steps;
            Point4f interpolated = interpolate(endPos, startPos, t);
            path.add(interpolated);
        }
        path.add(endPos);
        return path;
    }

    static void forcePathToMinPos(ArrayList<Point4f> path, Function<Point4f, Float> getHeightAt) {
        for (Point4f point : path) {
            point.z = Math.min(point.z, getHeightAt.apply(point));
        }
    }

    static FloatTile cloneHeightMapData(Tile tile) {
        var floatTile = new FloatTile(0);
        for (int xx = 0; xx < TILE_SIZE; xx++)
            for (int yy = 0; yy < TILE_SIZE; yy++) {
                floatTile.setValueAt(xx, yy, tile.getHeight(xx, yy));
            }
        return floatTile;
    }

    static void writeHeightMapDataToTile(FloatTile in, Tile out) {
        for (int xx = 0; xx < TILE_SIZE; xx++)
            for (int yy = 0; yy < TILE_SIZE; yy++) {
                out.setHeight(xx, yy, in.getValueAt(xx, yy));
            }
    }

    static void writePaintDataToDimension(FloatTile in, Dimension out, Paint paint) {
        if (in == null || out == null || paint == null)
            return;
        int tileX = in.tilePosX << TILE_SIZE_BITS;
        int tileY = in.tilePosY << TILE_SIZE_BITS;
        for (int xx = 0; xx < TILE_SIZE; xx++)
            for (int yy = 0; yy < TILE_SIZE; yy++) {
                if (in.getValueAt(xx, yy) == 1)
                    paint.applyPixel(out, xx + tileX, yy + tileY);
            }
    }

    static void forcePathOnlyDownhill(ArrayList<Point4f> path) {
        float previousZ = Float.MAX_VALUE;
        for (Point4f point : path) {
            point.z = Math.min(point.z, previousZ);
            previousZ = point.z;
        }
    }

    static void forcePathToHeight(ArrayList<Point4f> path, float height) {
        for (Point4f point : path) {
            point.z = height;
        }
    }

    static Set<Point3i> collectTilesAroundPath(Collection<Point4f> path, float radiusMultiplier) {
        HashSet<Point3i> tilePositions = new HashSet<>(path.size() / TILE_SIZE);
        for (Point4f pathPoint : path) {
            int tileRadius = (int) Math.ceil((pathPoint.w * radiusMultiplier) / TILE_SIZE);
            int x = Math.round(pathPoint.x) >> TILE_SIZE_BITS;
            int y = Math.round(pathPoint.y) >> TILE_SIZE_BITS;
            for (int xx = -tileRadius; xx <= tileRadius; xx++)
                for (int yy = -tileRadius; yy <= tileRadius; yy++) {
                    int tilex = x + xx;
                    int tiley = y + yy;
                    Point3i tilePos = new Point3i(tilex, tiley, 0);
                    tilePositions.add(tilePos);
                }
        }
        return tilePositions;
    }

    private static Point4f interpolate(Point4f a, Point4f b, float t) {
        Point4f out = new Point4f(b);
        out.interpolate(a, t);
        return out;
    }

    protected static double xyDistSq(Point4f a, Point4f b) {
        float dX = a.x - b.x, dy = a.y - b.y;
        double dist = dX * dX + dy * dy;
        return dist;
    }

    protected static Point2i[] getMinMaxPos(ArrayList<Point4f> path) {
        Point2f start = new Point2f(Float.MAX_VALUE, Float.MAX_VALUE);
        Point2f end = new Point2f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        for (var p : path) {
            start.x = Math.min(start.x, p.x);
            start.y = Math.min(start.y, p.y);
            end.x = Math.max(end.x, p.x);
            end.y = Math.max(end.y, p.y);
        }

        return new Point2i[]{new Point2i(start.x, start.y)
                , new Point2i(end.x, end.y)
        };
    }

    /**
     * collect all points whos radius will have an impact on the area defiend by start and end
     *
     * @param start
     * @param end
     * @return
     */
    protected static ArrayList<Point4f> getSubPathFor(Point2i start, Point2i end, List<Point4f> path, float radiusMultiplier) {
        ArrayList<Point4f> subPath = new ArrayList<>();
        for (var pathPoint : path) {
            int radius = (int) Math.ceil(pathPoint.w * radiusMultiplier);
            if (contains(pathPoint, new Point2i(start.x - radius, start.y - radius), new Point2i(end.x + radius, end.y + radius)))
                subPath.add(pathPoint);
        }
        return subPath;
    }

    private static boolean contains(Point4f query, Point2i start, Point2i end) {
        return query.x <= end.x && query.y <= end.y && query.x >= start.x && query.y >= start.y;
    }

    protected static FloatTile applyToTile(FloatTile heightInputTile, FloatTile paintOutput, Point3i tilePos, CrossSectionShape filterCrossSection, ArrayList<Point4f> subPath,
                                           float transitionMultiplier) {
        if (heightInputTile == null)
            return null;
        if (subPath.isEmpty())
            return null;

        var ext = RoadToolBackend.getMinMaxPos(subPath);
        QuadTree tree = new QuadTree(ext[0].x, ext[0].y, ext[1].x, ext[1].y);
        for (var p : subPath)
            tree.insert(new Point2i(p.x, p.y));

        HashMap<Point2i, Point4f> posToData = new HashMap<>();
        subPath.forEach(pathPoint -> {
            var pos = new Point2i(pathPoint.x, pathPoint.y);
            posToData.put(pos, pathPoint);
        });

        FloatTile outputTile = new FloatTile(0);
        outputTile.tilePosX = tilePos.x;
        outputTile.tilePosY = tilePos.y;
        for (int xx = 0; xx < TILE_SIZE; xx++)
            for (int yy = 0; yy < TILE_SIZE; yy++) {
                // find closest point
                var query = new Point2i((tilePos.x << TILE_SIZE_BITS) + xx, (tilePos.y << TILE_SIZE_BITS) + yy);
                var closestPos = tree.getClosest(query);
                var closest = posToData.get(closestPos);
                var distance = closest == null ? Float.MAX_VALUE : Math.sqrt(xyDistSq(closest, new Point4f(query.x, query.y, 0, 0)));

                // calculate height based on closest point and filter
                float originalHeight = heightInputTile.getValueAt(xx, yy);
                float outHeight;
                float filterStrength;
                if (closest == null || distance > closest.w * transitionMultiplier) {
                    outHeight = originalHeight;
                    filterStrength = 0;
                } else if (distance <= closest.w) {
                    outHeight = closest.z;
                    filterStrength = 1;
                } else {
                    //transition with terrain
                    filterStrength = filterStrengthFor((float) distance, closest.w, transitionMultiplier, filterCrossSection);
                    outHeight = filterStrength * closest.z + (1 - filterStrength) * originalHeight;
                }
                paintOutput.setValueAt(xx, yy, filterStrength);
                outputTile.setValueAt(xx, yy, outHeight);
            }
        return outputTile;
    }

    protected static float filterStrengthFor(float distance, float maxDistance, float transitionMultiplier, CrossSectionShape filterCrossSection) {
        if (distance < maxDistance)
            return 1;
        float baseMaxDist = maxDistance * transitionMultiplier - maxDistance;
        float thisDist = distance - maxDistance;
        float distanceT = 1 - Math.min(1, Math.max(0, (float) (thisDist / baseMaxDist)));
        assert distanceT >= 0 && distanceT <= 1;
        float filterStrength = filterCrossSection.getStrengthAt(distanceT);
        return filterStrength;
    }
}
