package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class ApplyAction {

    public static ExecutionStatistic applyToDimensionWithFilter(Dimension dim, TileFilter filter,
                                                                MappingAction action,
                                                                ApplyActionCallback callback) {
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

        if (action.getInput() instanceof ILimitedMapOperation)
            ((ILimitedMapOperation) action.getInput()).prepareRightBeforeRun(dim, tileX, tileY);
        if (action.getOutput() instanceof ILimitedMapOperation)
            ((ILimitedMapOperation) action.getOutput()).prepareRightBeforeRun(dim, tileX, tileY);

        callback.setProgressOfAction(0);
        for (int i = 0; i < tileX.length; i++) {
            Tile tile = dim.getTile(tileX[i], tileY[i]);

            TileFilter.passType pass = filter.testTile(tile);
            statistic.touchedTiles++;
            if (callback.isActionAbort())
                break;
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
                if (callback.isActionAbort())
                    break;
            }
            //this pass is done, for this tile calculate new minMax
            ActionFilterIO.instance.getTileContainer().calculateMinMax(tile.getX() * TILE_SIZE,
                    tile.getY() * TILE_SIZE);
            tilesVisitedCount++;
            callback.setProgressOfAction(Math.round(100f * tilesVisitedCount / (totalTiles)));
            callback.afterEachTile(tile.getX(),tile.getY());
        }
        callback.setProgressOfAction(100);

        if (action.getInput() instanceof ILimitedMapOperation)
            ((ILimitedMapOperation) action.getInput()).releaseRightAfterRun();
        if (action.getOutput() instanceof ILimitedMapOperation)
            ((ILimitedMapOperation) action.getOutput()).releaseRightAfterRun();

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
                                                                    ApplyActionCallback ui) {
        ArrayList<ExecutionStatistic> statistics = new ArrayList<>(actions.size());
        ui.setAllActionsBeforeRun(actions);
        for (MappingAction action : actions) {
            if (!dim.isEventsInhibited()) {
                dim.setEventsInhibited(true);
            }
            ui.beforeEachAction(action, dim);
            if (ui.isActionAbort())
                break;
            TileFilter earlyAbortFilter = new TileFilter(action);
            ExecutionStatistic statistic = null;
            try {
                statistic = applyToDimensionWithFilter(dim,earlyAbortFilter,action,ui);
            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                GlobalActionPanel.ErrorPopUpString(sw.toString());
                break;
            } finally {
                statistics.add(statistic);
                ui.afterEachAction(statistic);
                if (ui.isUpdateMapAfterEachAction() && dim.isEventsInhibited()) {
                    dim.setEventsInhibited(false);
                }
            }
        }
        ui.afterEverything();

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
