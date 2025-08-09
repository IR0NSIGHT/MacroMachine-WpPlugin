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
        assertNull(model.constructMapping());
        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withNewPoints(new MappingPoint[]{new MappingPoint(1, 2),
                        new MappingPoint(5, 6),
                        new MappingPoint(9, -1)})
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter());
        model.rebuildDataWithAction(action);
        assertEquals(action, model.constructMapping());

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

        assertEquals(IMappingValue.range(action.input), model.getRowCount());
    }

    @Test
    void getColumnCount() {
        MappingActionValueTableModel model = new MappingActionValueTableModel();
        assertEquals(2, model.getColumnCount());
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
        for (int row = 0; row < model.getRowCount(); row++) {
            assertEquals(row, ((MappingPointValue) model.getValueAt(row, 0)).numericValue);
            assertEquals(action.map(row), ((MappingPointValue) model.getValueAt(row, 1)).numericValue);
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
                updatedAction[0] = model.constructMapping();
                updateCalls[0]++;
            }


        });
        assertEquals(0,headerChangedCalls[0]);
        assertEquals(0,updateCalls[0]);

        model.rebuildDataWithAction(action);
        assertEquals(1,headerChangedCalls[0], "model must notify listener that header changed.");
        assertEquals(2,updateCalls[0],"data has changed");
    //FIXME test if INSERT and DELETE calls are also correctly fired

        assertEquals(256, model.getRowCount());
        assertEquals(new MappingPointValue(17,action.getInput()), model.getValueAt(17,0),"mapping point at input=17");


        //change mapping point, edit input
        model.setValueAt(new MappingPointValue(3,action.getInput()),17,0);
        assertEquals(3,updateCalls[0],"another update was called");
        assertEquals(new MappingPointValue(3,action.getInput()), model.getValueAt(3,0),"value was inserted but it " +
                "lives at a different index now");
        assertEquals(action.withNewPoints(new MappingPoint[]{new MappingPoint(3,18)}),model.constructMapping(),"the model " +
                        "constructed the correct action with the new point.");
        assertTrue(model.isCellEditable(3,0),"its a mapping point -> editable");
        assertTrue(model.isCellEditable(3,1),"its a mapping point -> editable");

        //set same value again
        model.setValueAt(new MappingPointValue(3,action.getInput()),0,0);
        assertEquals(3,updateCalls[0], "no update should occur when same value is set again");
        assertEquals(action.withNewPoints(new MappingPoint[]{new MappingPoint(3,18)}),model.constructMapping());

        //edit mapping point in output column from (3,18) to (3,9)
        assertTrue(model.isCellEditable(3,1));
        model.setValueAt(new MappingPointValue(9,action.getOutput()),3,1);
        assertEquals(new MappingPointValue(9,action.getOutput()), model.getValueAt(3,1),"value was inserted at index");
        assertEquals(4,updateCalls[0]," one more update");
        assertEquals(action.withNewPoints(new MappingPoint[]{new MappingPoint(3,9)}),model.constructMapping());
    }

    @Test
    void addTableModelListener() {
    }

    @Test
    void removeTableModelListener() {
    }
}