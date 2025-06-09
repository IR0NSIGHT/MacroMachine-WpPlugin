package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.DistanceToLayerEdgeGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ILayerGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.pepsoft.worldpainter.Dimension;

import java.util.Collection;
import java.util.function.Predicate;

public class PointApplicator {
    private final Collection<MappingAction> actions;
    private final Dimension dimension;

    public PointApplicator(Collection<MappingAction> actions, Dimension dimension) {
        this.actions = actions;
        this.dimension = dimension;
    }

    public void apply(int x, int y) {
        ActionFilterIO.instance.setSelected(true); //by default, each block is selected.

        for (MappingAction action : actions)
            action.applyToPoint(dimension, x, y);
    }

    public ExecutionStatistic toStatistic() {
        ExecutionStatistic stat = new ExecutionStatistic();
        stat.actions = this.actions;
        return stat;
    }

    public TileFilter earlyAbortFilter() {
        TileFilter earlyAbortFilter = new TileFilter();

        // special case: get-distance-to-edge, then intermediate values, then write to map
        boolean isDistanceOperation = actions.stream().anyMatch(a -> a.input instanceof  DistanceToLayerEdgeGetter);
        boolean canFilterByDistance =  actions.stream()
                .allMatch(a -> (a.input instanceof DistanceToLayerEdgeGetter && a.map(0) == 0) || a.input.isVirtual());
        if (isDistanceOperation && canFilterByDistance) {
            String[] layerIds = actions.stream()
                    .filter(f -> f.input instanceof DistanceToLayerEdgeGetter)
                    .map(MappingAction::getInput)
                    .map(in -> ((DistanceToLayerEdgeGetter) in).getLayerId())
                    .toArray(String[]::new);
            if (layerIds.length != 0)
                return earlyAbortFilter.withLayer(TileFilter.FilterType.ONLY_ON, layerIds);
        }

        // special case: step filters for layers being present. layer=0 -> output=0
        // similar to ONLY ON LAYER PINES as tool-filter
        Predicate<MappingAction> absentLayerIsFilteredOut = a -> a.input instanceof ILayerGetter && a.map(0) == 0;
        boolean filterByLayer =
                actions.stream().allMatch(a -> absentLayerIsFilteredOut.test(a) || a.input.isVirtual()) &&
        actions.stream().anyMatch(absentLayerIsFilteredOut);
        if (filterByLayer) {
            String[] layerIds = actions.stream()
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

        return earlyAbortFilter;
    }
}
