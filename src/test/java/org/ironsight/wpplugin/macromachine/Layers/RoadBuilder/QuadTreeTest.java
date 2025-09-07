package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuadTreeTest {

    @Test
    void insert() {
        Random random = new Random(12345);
        var root = new QuadTree(-1000,-1000,1000,1000);
        List<Point2i> points =
                IntStream.range(0, 40000)
                        .mapToObj(i -> new Point2i(random.nextInt(-100, 100), random.nextInt(-100, 100)))
                        .toList();

        points.forEach(root::insert);

        Consumer<Point2i> testTreeQueryCorrect = query -> {
            var treeClosest = root.getClosest(query);
            AtomicReference<Float> bestDistance = new AtomicReference<>(Float.MAX_VALUE);
            AtomicReference<Point2i> bruteForceClosest = new AtomicReference<>(null);
            points.forEach(p -> {
                float distance = p.distanceSquared(query);
                if (distance < bestDistance.get()) {
                    bestDistance.set(distance);
                    bruteForceClosest.set(p);
                }
            });
            //there is no guarantee that the points are equal, because the order of testing decides which one is picked between equally distant points.
            assertEquals(query.distanceSquared(bruteForceClosest.get()), query.distanceSquared(treeClosest), "mismatch for query " + query);
        };


        var point1 = new Point2i(5,-50);
        testTreeQueryCorrect.accept(point1);


        IntStream.range(0, 2090)
                .mapToObj(i -> new Point2i(random.nextInt(-100, 100), random.nextInt(-100, 100)))
                .forEach(testTreeQueryCorrect);


    }

    @Test
    void getClosest() {
    }
}