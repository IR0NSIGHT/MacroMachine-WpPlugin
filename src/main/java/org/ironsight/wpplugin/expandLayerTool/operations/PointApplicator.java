package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.IntermediateSelectionIO;
import org.pepsoft.worldpainter.Dimension;

import java.util.Collection;

public class PointApplicator {
    private final Collection<LayerMapping> actions;
    private final Dimension dimension;

    public PointApplicator(Collection<LayerMapping> actions, Dimension dimension) {
        this.actions = actions;
        this.dimension = dimension;
    }

    public void apply(int x, int y) {
        IntermediateSelectionIO.instance.setSelected(true); //by default, each block is selected.

        for (LayerMapping action : actions)
            action.applyToPoint(dimension, x, y);
    }

    public ExecutionStatistic toStatistic() {
        ExecutionStatistic stat = new ExecutionStatistic();
        stat.actions = this.actions;
        return stat;
    }
}
