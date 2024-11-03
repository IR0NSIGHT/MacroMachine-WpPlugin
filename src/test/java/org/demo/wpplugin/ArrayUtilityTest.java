package org.demo.wpplugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.INHERIT_VALUE;
import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilityTest {

    @Test
    void toIntArray() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(-100);
        list.add(100);
        int[] arr = ArrayUtility.toIntArray(list);
        assertArrayEquals(new int[]{1,-100, 100}, arr);

    }

    @Test
    void linearSearch() {
        {
            float[] arr = new float[]{1100, 5, 7, 1, 7, 1, INHERIT_VALUE, 7, 4, 2, 3, 7, 8, INHERIT_VALUE, 64, 787,
                    -12, 5};
            boolean found = ArrayUtility.linearSearch(arr, INHERIT_VALUE);
            assertTrue(found);
        }

        {
            float[] arr = new float[]{INHERIT_VALUE};
            boolean found = ArrayUtility.linearSearch(arr, INHERIT_VALUE);
            assertTrue(found);
        }

        {
            float[] arr = new float[]{INHERIT_VALUE, -10, INHERIT_VALUE};
            boolean found = ArrayUtility.linearSearch(arr, INHERIT_VALUE);
            assertTrue(found);
        }

        {
            float[] arr = new float[]{ -10, INHERIT_VALUE-1};
            boolean found = ArrayUtility.linearSearch(arr, INHERIT_VALUE);
            assertFalse(found);
        }

        {
            float[] arr = new float[]{ -10};
            boolean found = ArrayUtility.linearSearch(arr, INHERIT_VALUE);
            assertFalse(found);
        }


    }

    @Test
    void sumIndices() {
        float[] values = new float[]{5,10,0,10,-5};
        boolean[] toRemove = new boolean[]{false,true,false,true,true};

        float[] summed = ArrayUtility.sumIndices(values,toRemove);
        assertEquals(2, summed.length,"we kept 2 values and summed up 3");
        assertArrayEquals(new float[]{5+10,0+10f-5},summed);
    }

    @Test
    void removePositions() {
    }

    @Test
    void testRemovePositions() {
    }

    @Test
    void transposeHandles() {
    }

    @Test
    void flattenNestedList() {
    }
}