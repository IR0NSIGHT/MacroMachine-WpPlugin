package org.ironsight.wpplugin.macromachine.Gui;

import javax.swing.*;

class BlockingSelectionModel extends DefaultListSelectionModel {
    private boolean selectionBlocked = false;

    public void setSelectionBlocked(boolean blocked) {
        System.out.println("SELECTION IS " + (blocked ? "BLOCKED" : "FREE"));
        this.selectionBlocked = blocked;
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
        if (!selectionBlocked) {
            super.setSelectionInterval(index0, index1);
        }
    }

    @Override
    public void addSelectionInterval(int index0, int index1) {
        if (!selectionBlocked) {
            super.addSelectionInterval(index0, index1);
        }
    }
}
