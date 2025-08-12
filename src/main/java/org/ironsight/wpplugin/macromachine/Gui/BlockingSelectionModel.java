package org.ironsight.wpplugin.macromachine.Gui;

import javax.swing.*;

class BlockingSelectionModel extends DefaultListSelectionModel {
    private boolean selectionBlocked = false;
    private JTable table;
    private int lastSelectedModelRow = -1;

    public void setSelectionBlocked(boolean blocked) {
        this.selectionBlocked = blocked;
    }

    public void setTable(JTable table) {
        this.table = table;
    }

    public int[] getSelectedModelRows() {
        int[] rows = new int[table.getSelectedRows().length];
        int i = 0;
        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            rows[i++] = modelRow;
        }
        return rows;
    }

    public int getLastSelectedModelRow() {
        return lastSelectedModelRow;
    }

    public void setSelectionModelRow(int row) {
        int viewRow = table.convertRowIndexToView(row);
        if (viewRow == -1) {
            return;
        }
        setSelectionInterval(viewRow, viewRow);
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
        if (!selectionBlocked) {
            super.setSelectionInterval(index0, index1);
            lastSelectedModelRow = table.convertRowIndexToModel(index1);
        }
    }

    @Override
    public void addSelectionInterval(int index0, int index1) {
        if (!selectionBlocked) {
            super.addSelectionInterval(index0, index1);
        }
    }

    public void addSelectionRows(int[] rows) {
        selectionBlocked = true;
        for (int row : rows) {
            addSelectionInterval(row,row);
        }
        selectionBlocked = false;
    }
}
