package org.ironsight.wpplugin.macromachine.operations;

import java.util.Collection;
import java.util.stream.Collectors;

public class ExecutionStatistic {
    long durationMillis;
    long touchedTiles;
    long touchedBlocks;
    Collection<MappingAction> actions;

    @Override
    public String toString() {
        return "Execution Statistic for: [\n" +
                actions.stream().map(f -> "\t"+f.getName()).collect(Collectors.joining(",\n")) +
                "\n]\nduration: " +
                durationMillis + "ms\n" + "visited tiles:" + touchedTiles + "\nvisitied blocks:" + touchedBlocks + "\n";
    }
}
