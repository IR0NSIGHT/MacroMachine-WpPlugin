package org.ironsight.wpplugin.macromachine.operations;

public class ExecutionStatistic {
    public ExecutionStatistic(MappingAction action) {
        this.action = action;
    }
    public long durationMillis;
    public long touchedTiles;
    public long touchedBlocks;
    public MappingAction action;

    @Override
    public String toString() {
        return String.format("%s took %.1fs, visited %d tiles, visited %d blocks", action.getName(),
                durationMillis/1000f,touchedTiles,touchedBlocks);
    }
}
