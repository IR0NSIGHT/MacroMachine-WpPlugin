package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.junit.jupiter.api.Test;

import javax.swing.event.TableModelEvent;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MappingActionValueTableModelTest {

    @Test
    void rebuildDataWithAction() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        assertNull(model.getAction());
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withNewPoints(new MappingPoint[]{new MappingPoint(1, 2),
                        new MappingPoint(5, 6),
                        new MappingPoint(9, -1)})
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter());
        model.rebuildDataWithAction(action);
        assertEquals(action, model.getAction());

    }

    @Test
    void getAction() {
    }

    @Test
    void setOnlyControlPointMode() {
    }

    @Test
    void getRowCount() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withNewPoints(new MappingPoint[]{new MappingPoint(1, 2),
                        new MappingPoint(5, 6),
                        new MappingPoint(9, -1)})
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter());
        model.rebuildDataWithAction(action);
        model.setOnlyControlPointMode(false);

        assertEquals(IMappingValue.range(action.input), model.getRowCount());

        model.setOnlyControlPointMode(true);
        assertEquals(action.getMappingPoints().length, model.getRowCount());
    }

    @Test
    void getColumnCount() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        assertEquals(2, model.getRowCount());
    }

    @Test
    void getColumnName() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withNewPoints(new MappingPoint[]{new MappingPoint(1, 2),
                        new MappingPoint(5, 6),
                        new MappingPoint(9, -1)})
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter());
        model.rebuildDataWithAction(action);
        model.setOnlyControlPointMode(false);

        assertEquals(action.getInput().getName(), model.getColumnName(0));
        assertEquals(action.getOutput().getName(), model.getColumnName(1));

    }

    @Test
    void getColumnClass() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        assertEquals(MappingPointValue.class, model.getColumnClass(0));
        assertEquals(MappingPointValue.class, model.getColumnClass(1));
    }

    @Test
    void isCellEditable() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter());

        model.rebuildDataWithAction(action.withNewPoints(new MappingPoint[]{new MappingPoint(17, 18)}));
        model.setOnlyControlPointMode(false);

        assertEquals(IMappingValue.range(action.input), model.getRowCount());
        for (int row = 0; row < model.getRowCount(); row++) {
            if (row == 17) {
                assertTrue(model.isCellEditable(row, 0));
                assertTrue(model.isCellEditable(row, 1));
            } else {
                assertFalse(model.isCellEditable(row, 0));
                assertFalse(model.isCellEditable(row, 1));
            }
        }
        model.setOnlyControlPointMode(true);
        for (int row = 0; row < model.getRowCount(); row++) {
            assertTrue(model.isCellEditable(row, 0));
            assertTrue(model.isCellEditable(row, 1));
        }
    }

    @Test
    void getValueAt() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter())
                .withNewPoints(new MappingPoint[]{new MappingPoint(17, 18)});

        model.rebuildDataWithAction(action);
        model.setOnlyControlPointMode(false);
        for (int row = 0; row < model.getRowCount(); row++) {
            assertEquals(row, ((MappingPointValue) model.getValueAt(row, 0)).numericValue);
            assertEquals(action.map(row), ((MappingPointValue) model.getValueAt(row, 1)).numericValue);
        }

        model.setOnlyControlPointMode(true);
        int rowIndex = 0;
        for (MappingPoint mp : action.getMappingPoints()) {
            assertEquals(mp.input, ((MappingPointValue) model.getValueAt(rowIndex, 0)).numericValue);
            assertEquals(mp.output, ((MappingPointValue) model.getValueAt(rowIndex, 1)).numericValue);
            rowIndex++;
        }
    }

    @Test
    void setValueAt() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter())
                .withNewPoints(new MappingPoint[]{new MappingPoint(17, 18)});
        final MappingAction[] updatedAction = new MappingAction[1];
        final int[] updateCalls = {0};
        final int[] headerChangedCalls = {0};

        model.addTableModelListener(e -> {
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                headerChangedCalls[0]++;
            }
            if (e.getType() == TableModelEvent.UPDATE) {
                updatedAction[0] = model.getAction();
                updateCalls[0]++;
            }


        });
        assertEquals(0,headerChangedCalls[0]);
        assertEquals(0,updateCalls[0]);

        model.rebuildDataWithAction(action);
        assertEquals(1,headerChangedCalls[0], "model must notify listener that header changed.");
        assertEquals(1,updateCalls[0]);

        model.setOnlyControlPointMode(true);

        //edit mapping point in input column
        model.setValueAt(new MappingPointValue(3,action.getInput()),0,0);
        assertEquals(2,updateCalls[0]);
        assertEquals(action.withNewPoints(new MappingPoint[]{new MappingPoint(3,18)}),model.getAction());
        //set same value again
        model.setValueAt(new MappingPointValue(3,action.getInput()),0,0);
        assertEquals(2,updateCalls[0], "no update should occur when same value is set again");
        assertEquals(action.withNewPoints(new MappingPoint[]{new MappingPoint(3,18)}),model.getAction());

        //edit mapping point in output column
        model.setValueAt(new MappingPointValue(9,action.getOutput()),0,1);
        assertEquals(3,updateCalls[0]);
        assertEquals(action.withNewPoints(new MappingPoint[]{new MappingPoint(3,9)}),model.getAction());
    }

    @Test
    void addTableModelListener() {
    }

    @Test
    void removeTableModelListener() {
    }
}