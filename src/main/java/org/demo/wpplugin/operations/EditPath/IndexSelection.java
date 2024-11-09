package org.demo.wpplugin.operations.EditPath;

public class IndexSelection {
    private final boolean[] selectedIdcs;

    private int cursorHandleIdx;
    public IndexSelection(boolean[] selectedIdcs, int amountHandles, int cursorHandleIdx) {
        this.selectedIdcs = selectedIdcs;
        this.amountHandles = amountHandles;
        this.cursorHandleIdx = cursorHandleIdx;
    }

    public IndexSelection(int amountHandles) {
        this.selectedIdcs = new boolean[amountHandles];
        this.amountHandles = amountHandles;
        this.cursorHandleIdx = 0;
    }

    public void selectAllBetweenCursorAnd(int end) {
        int endIdxSafe = Math.max(0, Math.min(selectedIdcs.length-1,end));
        for (int i = Math.min(getCursorHandleIdx(), endIdxSafe); i <= Math.max(getCursorHandleIdx(), endIdxSafe); i++) {
            setHandleSelection(i, true);
        }
    }


    private final int amountHandles;

    public int getCursorHandleIdx() {
        return cursorHandleIdx;
    }

    /**
     * will purify input to be in range of selection array zero to length-1
     * @param cursorHandleIdx
     */
    public void setCursorHandleIdx(int cursorHandleIdx) {
        this.cursorHandleIdx = Math.min(selectedIdcs.length-1,Math.max(0, cursorHandleIdx));
    }

    public int[] getSelectedIdcs(boolean andCursor) {
        int count = 0;
        for (int i = 0; i < amountHandles; i++) {
            if (isHandleSelected(i, andCursor))
                count++;
        }
        int[] idcs = new int[count];
        int sel = 0;
        for (int i = 0; i < amountHandles; i++) {
            if (isHandleSelected(i,andCursor))
                idcs[sel++] = i;
        }
        return idcs;
    }

    public void deselectAll() {
        for (int i = 0; i < amountHandles; i++) {
            setHandleSelection(i,false);
        }
    }

    public void selectAll() {
        for (int i = 0; i < amountHandles; i++) {
            setHandleSelection(i,true);
        }
    }

    public void invertSelection() {
        for (int i = 0; i < amountHandles; i++) {
            setHandleSelection(i, !isHandleSelected(i,false));
        }
    }

    public void invertHandleSelection(int handle) {
        selectedIdcs[handle] = !isHandleSelected(handle, false);
    }

    public void setHandleSelection(int handle, boolean state) {
        selectedIdcs[handle] = state;
    }

    public boolean isHandleSelected(int idx, boolean orCursor) {
        if (idx < 0 || idx >= amountHandles)
            throw new ArrayIndexOutOfBoundsException("thats not a valid idx:" + idx);
        return (orCursor && cursorHandleIdx == idx) || selectedIdcs[idx];
    }
}
