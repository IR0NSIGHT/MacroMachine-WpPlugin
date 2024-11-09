package org.demo.wpplugin.operations.EditPath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexSelectionTest {
    @Test
    void selectAllBetweenCursorAnd() {
        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(3);
            sel.selectAllBetweenCursorAnd(6);
            int[] selected = sel.getSelectedIdcs(false);
            assertArrayEquals(new int[]{3, 4, 5, 6}, selected);
        }

        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(6);
            sel.selectAllBetweenCursorAnd(3);
            int[] selected = sel.getSelectedIdcs(false);
            assertArrayEquals(new int[]{3, 4, 5, 6}, selected);
        }

        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(-6);
            sel.selectAllBetweenCursorAnd(3);
            int[] selected = sel.getSelectedIdcs(false);
            assertArrayEquals(new int[]{0, 1, 2, 3}, selected);
        }

        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(3);
            sel.selectAllBetweenCursorAnd(13);
            int[] selected = sel.getSelectedIdcs(false);
            assertArrayEquals(new int[]{3, 4, 5, 6, 7, 8, 9}, selected);
        }
    }

    @Test
    void getSetCursorHandleIdx() {
        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(5);
            assertEquals(5, sel.getCursorHandleIdx());
        }
        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(-5);
            assertEquals(0, sel.getCursorHandleIdx());
        }
        {
            IndexSelection sel = new IndexSelection(10);
            sel.setCursorHandleIdx(51);
            assertEquals(9, sel.getCursorHandleIdx());
        }
    }


    @Test
    void deselectAllSelectAll() {
        IndexSelection sel = new IndexSelection(10);
        sel.selectAll();
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, sel.getSelectedIdcs(false));
        sel.deselectAll();
        assertArrayEquals(new int[]{}, sel.getSelectedIdcs(false));

    }

    @Test
    void invertSelection() {
        IndexSelection sel = new IndexSelection(10);
        sel.setHandleSelection(5, true);
        assertTrue(sel.isHandleSelected(5, false));
        sel.invertSelection();
        assertArrayEquals(new int[]{0,1,2,3,4,6,7,8,9},sel.getSelectedIdcs(false));
    }

    @Test
    void invertHandleSelection() {
        IndexSelection sel = new IndexSelection(10);
        sel.setHandleSelection(5, true);
        assertTrue(sel.isHandleSelected(5, false));
        sel.invertHandleSelection(5);
        assertFalse(sel.isHandleSelected(5, false));
    }
}