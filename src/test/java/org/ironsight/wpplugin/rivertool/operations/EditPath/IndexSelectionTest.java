package org.ironsight.wpplugin.rivertool.operations.EditPath;

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
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9}, sel.getSelectedIdcs(false));
    }

    @Test
    void invertHandleSelection() {
        IndexSelection sel = new IndexSelection(10);
        sel.setHandleSelection(5, true);
        assertTrue(sel.isHandleSelected(5, false));
        sel.invertHandleSelection(5);
        assertFalse(sel.isHandleSelected(5, false));
    }

    @Test
    void translateToNew() {
        IndexSelection selection = new IndexSelection(10);
        selection.setCursorHandleIdx(5);
        selection.selectAllBetweenCursorAnd(8);
        assertEquals(5, selection.getCursorHandleIdx());
        assertArrayEquals(new int[]{5, 6, 7, 8}, selection.getSelectedIdcs(false));

        {        //remove index 7
            int[] mapping = new int[]{0, 1, 2, 3, 4, 5, 6, 8, 9};
            // means: mapping[new8] -> old8, 8 -> 9
            IndexSelection translated = IndexSelection.translateToNew(selection, mapping);
            assertArrayEquals(new int[]{5, 6, 7}, translated.getSelectedIdcs(false), "previously selected was 5,6,7,8" +
                    ". 7 is deleted, so new idcs 5,6,7 remain (old 8 is new 7)");
            assertEquals(5,translated.getCursorHandleIdx(),"cursor was at 5, still should be at 5 because 0..6  are untouched");
        }

        {        //add after index 6 (new 7 doesnt exist in old)
            int[] mapping = new int[]{0, 1, 2, 3, 4, 5, 6, -1, 7, 8, 9};
            // means: mapping[new8] -> old8, 8 -> 9
            IndexSelection translated = IndexSelection.translateToNew(selection, mapping);
            assertArrayEquals(new int[]{5, 6, 8, 9}, translated.getSelectedIdcs(false), "");
            assertEquals(5,translated.getCursorHandleIdx(),"cursor was at 5, still should be at 5 because 0..6  are untouched");
        }
    }
}