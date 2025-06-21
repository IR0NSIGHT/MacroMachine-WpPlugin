package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import java.util.*;
import java.util.function.Consumer;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class ApplyAction {

    public static ExecutionStatistic applyToDimensionWithFilter(Dimension dim, TileFilter filter,
                                                                PointApplicator applyToPoint,
                                                                Consumer<Float> setProgress) {
        filter.setDimension(dim);
        Iterator<? extends Tile> t = dim.getTiles().iterator();
        ExecutionStatistic statistic = applyToPoint.toStatistic();
        long startTime = System.currentTimeMillis();
        int totalTiles = dim.getTiles().size();
        int tilesVisitedCount = 0;

        applyToPoint.prepareRightBeforeRun(dim);
        while (t.hasNext()) {
            Tile tile = t.next();
            boolean actionFilterWouldSkipTile = ActionFilterIO.instance.getTileContainer().getTileAt(tile.getX() * TILE_SIZE,
                    tile.getY() * TILE_SIZE).getMax() == ActionFilterIO.BLOCK_VALUE;
            if (actionFilterWouldSkipTile && !applyToPoint.doesIncreaseActionFilter()) {
                continue;
            }
            TileFilter.passType pass = filter.testTile(tile);
            if (pass == TileFilter.passType.NO_BLOCKS) continue;

            statistic.touchedTiles++;
            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    final int x = xInTile + (tile.getX() << TILE_SIZE_BITS);
                    final int y = yInTile + (tile.getY() << TILE_SIZE_BITS);
                    if (pass == TileFilter.passType.ALL_BLOCKS || filter.pass(x, y)) {
                        applyToPoint.apply(x, y);
                        statistic.touchedBlocks++;
                    }
                }
            }
            //this pass is done, for this tile calculate new minMax
            ActionFilterIO.instance.getTileContainer().calculateMinMax(tile.getX() * TILE_SIZE,
                    tile.getY() * TILE_SIZE);
            tilesVisitedCount++;
            setProgress.accept(100f * tilesVisitedCount / (totalTiles));
        }
        applyToPoint.releaseAfterRun();
        statistic.durationMillis = System.currentTimeMillis() - startTime;
        return statistic;
    }

    public static ArrayList<ExecutionStatistic> applyExecutionSteps(Dimension dim,
                                                                    List<MappingAction> actions,
                                                                    Consumer<Progess> setProgress) {
        ArrayList<ExecutionStatistic> statistics = new ArrayList<>(actions.size());
        int i = 0;
        for (MappingAction a : actions) {
            PointApplicator stepApplicator = new PointApplicator(a, dim);
            TileFilter earlyAbortFilter = stepApplicator.earlyAbortFilter();
            final int stepIndex = i;
            statistics.add(applyToDimensionWithFilter(dim, earlyAbortFilter, stepApplicator,
                    percent -> {
                        setProgress.accept(new Progess(stepIndex, actions.size(), percent));
                    }));
            i++;
        }

        return statistics;
    }

    public static class Progess {
        public final int step;
        public final int totalSteps;
        public final float progressInStep; //0..1
        public Progess(int step, int totalSteps, float progressInStep) {
            this.step = step;
            this.totalSteps = totalSteps;
            this.progressInStep = progressInStep;
        }
    }

}
