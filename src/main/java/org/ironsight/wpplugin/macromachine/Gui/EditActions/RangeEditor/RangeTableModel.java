package org.ironsight.wpplugin.macromachine.Gui.EditActions.RangeEditor;

import org.ironsight.wpplugin.macromachine.Gui.EditActions.MappingPointValue;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.*;

import static org.ironsight.wpplugin.macromachine.operations.MappingAction.calculateRanges;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter.isLegalInput;
import static org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter.isLegalOutput;

public class RangeTableModel extends DefaultTableModel
{
    private final ArrayList<ValueRange> ranges = new ArrayList<>();

    private MappingAction action;
    private HashSet<Integer> illegalStartRows = new HashSet<>();
    private HashSet<Integer> illegalEndRows = new HashSet<>();

    @Override
    public int getRowCount() {
        return ranges == null ? 0 : ranges.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    public boolean isCellIllegalValue(int row, int column) {
        if (column == 0)
            return illegalStartRows.contains(row);
        else if (column == 1)
            return illegalEndRows.contains(row);
        return false;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> "start";
            case 1 -> "end";
            case 2 -> "output";
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return MappingPointValue.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return switch (columnIndex) {
            case 0 -> ranges.get(rowIndex).start;
            case 1 -> ranges.get(rowIndex).end;
            case 2 -> ranges.get(rowIndex).value;
            default -> null;
        };
    }

    public void deleteRows(int[] rows) {
        if (rows.length == 0)
            return;
        for (int i = rows.length - 1; i >= 0; i--) {
            ranges.remove(rows[i]);
        }
        fireTableDataChanged();
        SwingUtilities.invokeLater(this::updateValidationData);
    }

    public void addRows(int[] rows) {
        if (rows.length == 0) {
            rows = new int[]{0};
        }
        for (int i = rows.length - 1; i >= 0; i--) {
            ranges.add(rows[i],
                    new ValueRange(new MappingPointValue(action.getInput().getMinValue(), action.getInput()),
                            new MappingPointValue(action.getInput().getMaxValue(), action.getInput()),
                            new MappingPointValue(action.getOutput().getMinValue(), action.getOutput())));
        }
        fireTableDataChanged();
        SwingUtilities.invokeLater(this::updateValidationData);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue instanceof MappingPointValue newValue && rowIndex >= 0 && rowIndex < ranges.size()) {
            var oldRange = ranges.get(rowIndex);
            var newRange = switch (columnIndex) {
                case 0 -> new ValueRange(newValue, oldRange.end, oldRange.value);
                case 1 -> new ValueRange(oldRange.start, newValue, oldRange.value);
                case 2 -> new ValueRange(oldRange.start, oldRange.end, newValue);
                default -> null;
            };
            ranges.set(rowIndex, newRange);
            fireTableCellUpdated(rowIndex, columnIndex);

            SwingUtilities.invokeLater(this::updateValidationData);

        }
    }

    private ValueRange fromValues(double start, double end, int value) {
        return new ValueRange(new MappingPointValue((int) Math.round(start), action.getInput()),
                new MappingPointValue((int) Math.round(end), action.getInput()),
                new MappingPointValue(value, action.getOutput()));
    }

    public void setAction(MappingAction action) {
        this.action = action.deepCopy();

        // construct ranges array with mappingpoint values
        var newRanges = calculateRanges(action).stream()
                .map(startEnd -> fromValues(startEnd.x, startEnd.y, action.map((int) Math.round(startEnd.y))))
                .sorted(Comparator.comparing(ValueRange::start))
                .toList();

        this.ranges.clear();
        this.ranges.addAll(newRanges);
        // fire model events to notify about changed layout
        fireTableDataChanged();
    }

    /**
     * illegal if: start < start of previous range
     *
     * @param rows
     * @return
     */
    private List<Integer> getRowsWithIncorrectStarts(List<ValueRange> rows) {
        if (rows.size() < 2)
            return List.of();
        ArrayList<Integer> incorrectRows = new ArrayList<>(rows.size());
        List<ValueRange> sortedRows = rows.stream().sorted(Comparator.comparing(r -> r.start().numericValue)).toList();
        for (int row = 1; row < sortedRows.size(); row++) {
            var thisRange = sortedRows.get(row);
            var previousRange = sortedRows.get(row - 1);
            if (thisRange.start.numericValue <= previousRange.end.numericValue)
                incorrectRows.add(row);
        }

        return incorrectRows;
    }

    private List<Integer> getRowsWithIncorrectEnd(List<ValueRange> rows, IPositionValueGetter getter) {
        ArrayList<Integer> incorrectRows = new ArrayList<>(rows.size());

        // test if a range starts where another already is
        boolean[] values = new boolean[getter.getAllInputValues().length];
        for (int row = 0; row < rows.size(); row++) {
            var r = rows.get(row);
            int idxStart = r.start().numericValue - getter.getMinValue();
            int idxEnd = r.end().numericValue - getter.getMinValue();
            if (idxStart > idxEnd) {
                incorrectRows.add(row);
            }

            if (values[idxEnd]) {// already set
                incorrectRows.add(row);
            }

            // mark this range in the boolean set, so others know this interval is not
            // available.
            for (int i = idxStart; i <= idxEnd; i++) {
                values[i] = true;
            }
        }

        return incorrectRows;
    }

    private ArrayList<ValueRange> validateRanges(List<ValueRange> ranges, IPositionValueGetter getter) {
        // Step 1 : sort by start value
        ArrayList<ValueRange> newRanges = new ArrayList<>(
                ranges.stream().sorted(Comparator.comparing(r -> r.start.numericValue)).toList());

        // Step 1.5 iterate ranges, ensure start <= end
        for (var range : newRanges) {
            if (range.start().numericValue > range.end().numericValue) {
                range.end().numericValue = range.start().numericValue;
            }
        }

        // Step 2 : ensure no range starts where another already is, push up overlaps
        boolean[] values = new boolean[getter.getAllInputValues().length];
        for (ValueRange r : newRanges) {
            int idxStart = r.start().numericValue - getter.getMinValue();
            int idx = idxStart;
            // mutate the ranges start, so it walks to the right until it finds a free spot
            for (; idx < values.length; idx++) {
                if (!values[idx]) // not yet set, stop
                    break;
                r.start().numericValue++; // shift 1 to the right, try again next round
            }
            // end of mutation

            // mark this range in the boolean set, so others know this interval is not
            // available.
            int idxEnd = r.end().numericValue - getter.getMinValue();
            for (int i = idx; i <= idxEnd; i++) {
                values[i] = true;
            }
        }

        // iterate ranges, ensure start <= end
        for (var range : newRanges) {
            if (range.start().numericValue > range.end().numericValue) {
                range.end().numericValue = range.start().numericValue;
            }
        }

        newRanges = new ArrayList<>(newRanges.stream()
                .filter(range -> isLegalInput((IPositionValueGetter) range.start().mappingValue,
                        range.start().numericValue))
                .filter(range -> isLegalInput((IPositionValueGetter) range.end.mappingValue, range.end.numericValue))
                .filter(range -> isLegalOutput((IPositionValueSetter) range.value.mappingValue,
                        range.value.numericValue))
                .filter(range -> !action.getOutput().isIgnoreValue(range.value.numericValue))
                .toList());

        return newRanges;
    }

    private void updateValidationData() {
        HashSet<Integer> newIllegalEndRows = new HashSet<>(getRowsWithIncorrectEnd(ranges, action.getInput()));
        HashSet<Integer> newIllegalStartRows = new HashSet<>(getRowsWithIncorrectStarts(ranges));

        if (newIllegalEndRows.equals(illegalEndRows) && newIllegalStartRows.equals(illegalStartRows))
            return;
        illegalStartRows = newIllegalStartRows;
        illegalEndRows = newIllegalEndRows;
        fireTableDataChanged();
    }

    /**
     * validate data and overwrite content of table model with validated data
     */
    public void enforceDataValidation() {
        var newRanges = validateRanges(ranges, action.getInput());
        ranges.clear();
        ranges.addAll(newRanges);
        fireTableDataChanged();
    }

    public MappingAction constructAction() {
        var validatedRanges = validateRanges(ranges, action.getInput());
        var missingIntervals = calculatedMissingIntervals(action.getInput().getMinValue(),
                action.getInput().getMaxValue(), validatedRanges, Integer.MAX_VALUE, action.getInput(),
                action.getOutput());
        validatedRanges.addAll(missingIntervals);
        validatedRanges.sort(Comparator.comparing(range -> range.start().numericValue));

        var mappingPoints = validatedRanges.stream()
                .map(range -> List.of(new MappingPoint(range.start().numericValue, range.value().numericValue),
                        new MappingPoint(range.end().numericValue, range.value().numericValue)))
                .flatMap(Collection::stream)
                .toArray(MappingPoint[]::new);
        var newAction = action.withNewPoints(mappingPoints);
        return newAction;
    }

    private List<ValueRange> calculatedMissingIntervals(int min, int max, List<ValueRange> existingIntervals,
            int defaultValue, IPositionValueGetter getter, IPositionValueSetter setter) {

        List<ValueRange> result = new ArrayList<>();

        // sort intervals by start
        List<ValueRange> sorted = existingIntervals.stream()
                .sorted(Comparator.comparingInt(v -> v.start().numericValue))
                .toList();

        int current = min;

        for (ValueRange vr : sorted) {

            int start = vr.start().numericValue;
            int end = vr.end().numericValue;

            if (start > current) {
                result.add(new ValueRange(new MappingPointValue(current, getter),
                        new MappingPointValue(start - 1, getter), new MappingPointValue(defaultValue, setter)));
            }

            current = Math.max(current, end + 1);

            if (current > max) {
                break;
            }
        }

        if (current <= max) {
            result.add(new ValueRange(new MappingPointValue(current, getter), new MappingPointValue(max, getter),
                    new MappingPointValue(defaultValue, setter)));
        }

        return result;
    }

    private record ValueRange(MappingPointValue start, MappingPointValue end, MappingPointValue value) {
    }

}
