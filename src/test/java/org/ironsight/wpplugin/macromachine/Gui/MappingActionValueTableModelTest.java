package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.AnnotationSetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IMappingValue;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.TerrainHeightIO;
import org.junit.jupiter.api.Test;

import javax.swing.event.TableModelEvent;

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
        model.rebuildModelFromAction(action);
        assertEquals(action, model.constructMapping());

        for (MappingPoint mp: action.getMappingPoints()) {
            int row = mp.input;
            assertTrue(model.isMappingPoint(row));
            assertTrue(model.isCellEditable(row,0));
            assertTrue(model.isCellEditable(row,1));

        }
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
        model.rebuildModelFromAction(action);

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
        model.rebuildModelFromAction(action);

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

        model.rebuildModelFromAction(action.withNewPoints(new MappingPoint[]{new MappingPoint(17, 18)}));

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

        model.rebuildModelFromAction(action);
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

        model.rebuildModelFromAction(action);
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
    void setValuesAt() {
        MappingPoint point1 = new MappingPoint(17, 4);
        MappingPoint point2 = new MappingPoint(21, 5);
        MappingPoint point3 = new MappingPoint(33, 6);

        MappingAction action = MappingAction.getNewEmptyAction()
                .withName("test")
                .withDescription("test")
                .withInput(new TerrainHeightIO(0, 255))
                .withOutput(new AnnotationSetter())
                .withNewPoints(new MappingPoint[]{point1, point2, point3});
        final MappingAction[] updatedAction = new MappingAction[1];
        final int[] updateCalls = {0};
        final int[] headerChangedCalls = {0};

        MappingActionValueTableModel model = new MappingActionValueTableModel();
        model.rebuildModelFromAction(action);

        model.addTableModelListener(e -> {
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                headerChangedCalls[0]++;
            }
            if (e.getType() == TableModelEvent.UPDATE) {
                updatedAction[0] = model.constructMapping();
                updateCalls[0]++;
            }
        });

        // point1
        assertEquals(new MappingPointValue(point1.input,action.getInput()), model.getValueAt(point1.input,0));
        assertEquals(new MappingPointValue(point1.output,action.getOutput()), model.getValueAt(point1.input,1));

        // point2
        assertEquals(new MappingPointValue(point2.input,action.getInput()), model.getValueAt(point2.input,0));
        assertEquals(new MappingPointValue(point2.output,action.getOutput()), model.getValueAt(point2.input,1));

        // point3
        assertEquals(new MappingPointValue(point3.input,action.getInput()), model.getValueAt(point3.input,0));
        assertEquals(new MappingPointValue(point3.output,action.getOutput()), model.getValueAt(point3.input,1));

    // single point change: modify output of point1

        model.setValuesAt(new MappingPointValue(point1.output + 5,action.getInput()), new int[]{point1.input},1);
        assertEquals(new MappingPointValue(point1.input,action.getInput()), model.getValueAt(point1.input,0));
        assertEquals(new MappingPointValue(point1.output + 5,action.getOutput()), model.getValueAt(point1.input,1));
        point1 = new MappingPoint(point1.input, point1.output + 5);

    // attempt change interpolated point: (impossible bc can only edit control points)
        int row = 4; // interpolated row
        model.setValuesAt(new MappingPointValue(137,action.getInput()), new int[]{row}
                ,1);
        // value didnt change, input was ignored. point1 as closest controlpoint defines output value
        assertEquals(new MappingPointValue(row,action.getInput()), model.getValueAt(row,0));
        assertEquals(new MappingPointValue(point1.output,action.getOutput()), model.getValueAt(row,1));

    // change many control points at once, output modified
        model.setValuesAt(new MappingPointValue(point1.output + 5,action.getInput()), new int[]{point1.input,
                point2.input, point3.input},1);
        assertEquals(new MappingPointValue(point1.input,action.getInput()), model.getValueAt(point1.input,0));
        assertEquals(new MappingPointValue(point1.output + 5,action.getOutput()), model.getValueAt(point1.input,1));
        //test point 2
        assertEquals(new MappingPointValue(point2.input,action.getInput()), model.getValueAt(point2.input,0));
        assertEquals(new MappingPointValue(point1.output + 5,action.getOutput()), model.getValueAt(point2.input,1));
        // test point 3
        assertEquals(new MappingPointValue(point3.input,action.getInput()), model.getValueAt(point3.input,0));
        assertEquals(new MappingPointValue(point1.output + 5,action.getOutput()), model.getValueAt(point3.input,1));

        assertTrue(model.isMappingPoint(point1.input));
        assertTrue(model.isMappingPoint(point2.input));
        assertTrue(model.isMappingPoint(point3.input));

        // change all entries at once, control points and interpolated.
        model.rebuildModelFromAction(action);
        assert action.input.getMinValue() == 0 : "prerequisite";
        int[] allIndices = new int[action.input.getMaxValue()];
        for (int i = 0; i < allIndices.length; i++)
            allIndices[i] = i;

        model.setValuesAt(new MappingPointValue(point1.output, action.getOutput()), allIndices,1);

        // point1
        assertEquals(new MappingPointValue(point1.input,action.getInput()), model.getValueAt(point1.input,0));
        assertEquals(new MappingPointValue(point1.output,action.getOutput()), model.getValueAt(point1.input,1));

        // point2
        assertEquals(new MappingPointValue(point2.input,action.getInput()), model.getValueAt(point2.input,0));
        assertEquals(new MappingPointValue(point1.output,action.getOutput()), model.getValueAt(point2.input,1));

        // point3
        assertEquals(new MappingPointValue(point3.input,action.getInput()), model.getValueAt(point3.input,0));
        assertEquals(new MappingPointValue(point1.output,action.getOutput()), model.getValueAt(point3.input,1));

        assertTrue(model.isMappingPoint(point1.input));
        assertTrue(model.isMappingPoint(point2.input));
        assertTrue(model.isMappingPoint(point3.input));

        for (int rowIdx = 0; rowIdx < allIndices.length; rowIdx++) {
            if (rowIdx == point1.input || rowIdx == point2.input || rowIdx == point3.input)
                continue;
            assertFalse(model.isMappingPoint(rowIdx));
        }
    }

    @Test
    void addTableModelListener() {
    }

    @Test
    void removeTableModelListener() {
    }
}