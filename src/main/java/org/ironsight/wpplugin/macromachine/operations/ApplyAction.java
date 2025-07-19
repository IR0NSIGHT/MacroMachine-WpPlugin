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
                                                                MappingAction action,
                                                                Consumer<Float> setProgress) {
        filter.setDimension(dim);
        Iterator<? extends Tile> t = dim.getTiles().iterator();
        ExecutionStatistic statistic = new ExecutionStatistic(action);
        long startTime = System.currentTimeMillis();
        int totalTiles = dim.getTiles().size();
        int tilesVisitedCount = 0;


        // find area of operations: all chunks that will be visited:
        int[] tileX = new int[dim.getTiles().size()], tileY = new int[dim.getTiles().size()];
        int tileArrIdx = 0;

        while (t.hasNext()) {
            Tile tile = t.next();
            if (ActionFilterIO.instance.skipTile(tile.getX(), tile.getY()) &&
                    !doesIncreaseActionFilter(action)) {
                continue;
            }
            TileFilter.passType pass = filter.testTile(tile);
            if (pass == TileFilter.passType.NO_BLOCKS)
                continue;
            tileX[tileArrIdx] = tile.getX();
            tileY[tileArrIdx] = tile.getY();
            tileArrIdx++;
        }
        tileX = Arrays.copyOf(tileX, tileArrIdx);
        tileY = Arrays.copyOf(tileY, tileArrIdx);

        if (tileX.length == 0 || tileY.length == 0)
            return statistic;

        if (action instanceof ILimitedMapOperation)
            ((ILimitedMapOperation) action).prepareRightBeforeRun(dim, tileX, tileY);

        for (int i = 0; i < tileX.length; i++) {
            Tile tile = dim.getTile(tileX[i], tileY[i]);

            TileFilter.passType pass = filter.testTile(tile);
            statistic.touchedTiles++;

            //special case for actionfilter
            if (action.getOutput() instanceof ActionFilterIO) {
                boolean skipTile = ActionFilterIO.instance.testTileEarlyAbort(tile, action);
                if (skipTile)
                    continue;
            }

            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                    final int x = xInTile + (tile.getX() << TILE_SIZE_BITS);
                    final int y = yInTile + (tile.getY() << TILE_SIZE_BITS);
                    if (pass == TileFilter.passType.ALL_BLOCKS || filter.pass(x, y)) {
                        action.applyToPoint(dim, x, y);
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
        if (action instanceof ILimitedMapOperation)
            ((ILimitedMapOperation) action).releaseRightAfterRun();
        statistic.durationMillis = System.currentTimeMillis() - startTime;
        return statistic;
    }

    /**
     * does this action take the actionFilter and change blocks from BLOCK to PASS?
     *
     * @return
     */
    public static boolean doesIncreaseActionFilter(MappingAction action) {
        return action.output.getProviderType().equals(ProviderType.INTERMEDIATE_SELECTION) &&
                (action.actionType.equals(ActionType.SET) ||
                        action.actionType.equals(ActionType.INCREMENT) ||
                        action.actionType.equals(ActionType.MULTIPLY) ||
                        action.actionType.equals(ActionType.AT_LEAST));
    }

    public static ArrayList<ExecutionStatistic> applyExecutionSteps(Dimension dim,
                                                                    List<MappingAction> actions,
                                                                    Consumer<Progess> setProgress) {
        ArrayList<ExecutionStatistic> statistics = new ArrayList<>(actions.size());
        int i = 0;
        for (MappingAction a : actions) {
            TileFilter earlyAbortFilter = new TileFilter();
            final int stepIndex = i;
            statistics.add(applyToDimensionWithFilter(dim, earlyAbortFilter, a,
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
