package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.DistanceToLayerEdgeGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ShadowMapIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.pepsoft.worldpainter.Dimension;

import java.util.Collections;

public class PointApplicator {
    private final MappingAction action;
    private final Dimension dimension;

    public void prepareRightBeforeRun(Dimension dimension, int[] tileX, int[] tileY) {
        if (action.getInput() instanceof ShadowMapIO)
            ((ShadowMapIO) action.getInput()).calculateShadowMap(dimension, new TerrainHeightIO(-5000,5000), tileX, tileY);
    }

    public void releaseAfterRun() {
        if (action.getInput() instanceof ShadowMapIO)
            ((ShadowMapIO) action.getInput()).releaseShadowMap();
    }

    public PointApplicator(MappingAction actions, Dimension dimension) {
        this.action = actions;
        this.dimension = dimension;
    }

    /**
     * does this applicator take the action filter and change blocks from BLOCK to PASS?
     *
     * @return
     */
    public boolean doesIncreaseActionFilter() {
        return action.output.getProviderType().equals(ProviderType.INTERMEDIATE_SELECTION) &&
                (action.actionType.equals(ActionType.SET) ||
                        action.actionType.equals(ActionType.INCREMENT) ||
                        action.actionType.equals(ActionType.MULTIPLY) ||
                        action.actionType.equals(ActionType.AT_LEAST));
    }

    public void apply(int x, int y) {
        action.applyToPoint(dimension, x, y);
    }

    public ExecutionStatistic toStatistic() {
        ExecutionStatistic stat = new ExecutionStatistic();
        stat.actions = Collections.singleton(this.action);
        return stat;
    }

    public TileFilter earlyAbortFilter() {
        TileFilter earlyAbortFilter = new TileFilter();

        return earlyAbortFilter; //FIXME i think the logic to be able to use early abort filters is flawed, possibly
        // the whole concept doesnt work? disabled for now

        /*
        // special case: get-distance-to-edge, then intermediate values, then write to map
        boolean isDistanceOperation = action.stream().anyMatch(a -> a.input instanceof DistanceToLayerEdgeGetter);
        boolean canFilterByDistance = action.stream()
                .allMatch(a -> (a.input instanceof DistanceToLayerEdgeGetter && a.map(0) == 0) || a.input.isVirtual());
        if (isDistanceOperation && canFilterByDistance) {
            String[] layerIds = action.stream()
                    .filter(f -> f.input instanceof DistanceToLayerEdgeGetter)
                    .map(MappingAction::getInput)
                    .map(in -> ((DistanceToLayerEdgeGetter) in).getLayerId())
                    .toArray(String[]::new);
            if (layerIds.length != 0)
                return earlyAbortFilter.withLayer(TileFilter.FilterType.ONLY_ON, layerIds);
        }


        // special case: step filters for layers being present. layer=0 -> output=0
        // similar to ONLY ON LAYER PINES as tool-filter
        Predicate<MappingAction> absentLayerIsFilteredOut =
                a -> a.input instanceof ILayerGetter && a.output instanceof  && a.map(0) == 0;
        boolean filterByLayer =
                action.stream().allMatch(a -> absentLayerIsFilteredOut.test(a) || a.input.isVirtual()) &&
                        action.stream().anyMatch(absentLayerIsFilteredOut);
        if (filterByLayer) {
            String[] layerIds = action.stream()
                    .filter(f -> f.input instanceof ILayerGetter)
                    .map(MappingAction::getInput)
                    .map(in -> ((ILayerGetter) in).getLayerId())
                    .toArray(String[]::new);
            return earlyAbortFilter.withLayer(TileFilter.FilterType.ONLY_ON, layerIds);
        }

        //FIXME use other filter types also:
        // by height
        // by terrain
        // by selection

        return earlyAbortFilter;*/
    }
}
