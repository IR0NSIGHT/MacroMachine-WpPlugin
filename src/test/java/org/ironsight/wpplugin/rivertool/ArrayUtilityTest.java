package org.ironsight.wpplugin.rivertool;

import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilityTest {

    @Test
    void toIntArray() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(-100);
        list.add(100);
        int[] arr = ArrayUtility.toIntArray(list);
        assertArrayEquals(new int[]{1, -100, 100}, arr);

    }

    @Test
    void linearSearch() {
        {
            float[] arr = new float[]{1100, 5, 7, 1, 7, 1, RiverHandleInformation.INHERIT_VALUE, 7, 4, 2, 3, 7, 8, RiverHandleInformation.INHERIT_VALUE, 64, 787,
                    -12, 5};
            boolean found = ArrayUtility.linearSearch(arr, RiverHandleInformation.INHERIT_VALUE);
            assertTrue(found);
        }

        {
            float[] arr = new float[]{RiverHandleInformation.INHERIT_VALUE};
            boolean found = ArrayUtility.linearSearch(arr, RiverHandleInformation.INHERIT_VALUE);
            assertTrue(found);
        }

        {
            float[] arr = new float[]{RiverHandleInformation.INHERIT_VALUE, -10, RiverHandleInformation.INHERIT_VALUE};
            boolean found = ArrayUtility.linearSearch(arr, RiverHandleInformation.INHERIT_VALUE);
            assertTrue(found);
        }

        {
            float[] arr = new float[]{-10, RiverHandleInformation.INHERIT_VALUE - 1};
            boolean found = ArrayUtility.linearSearch(arr, RiverHandleInformation.INHERIT_VALUE);
            assertFalse(found);
        }

        {
            float[] arr = new float[]{-10};
            boolean found = ArrayUtility.linearSearch(arr, RiverHandleInformation.INHERIT_VALUE);
            assertFalse(found);
        }


    }

    @Test
    void sumIndices() {
        {   //trivial 1
            float[] values = new float[]{5};
            boolean[] toRemove = new boolean[]{false};

            float[] summed = ArrayUtility.sumIndices(values, toRemove);
            assertArrayEquals(new float[]{5 }, summed);
        }
        {   //trivial 2
            float[] values = new float[]{5, 10};
            boolean[] toRemove = new boolean[]{false, false};

            float[] summed = ArrayUtility.sumIndices(values, toRemove);
            assertArrayEquals(new float[]{5, 10 }, summed);
        }


        {   //normal
            float[] values = new float[]{5, 10, 0, 10, -5};
            boolean[] toRemove = new boolean[]{false, true, false, true, true};

            float[] summed = ArrayUtility.sumIndices(values, toRemove);
            assertEquals(2, summed.length, "we kept 2 values and summed up 3");
            assertArrayEquals(new float[]{5 + 10, 0 + 10f - 5}, summed);
        }
        {   //only keep last
            float[] values = new float[]{5, 10, 0, 10, -5};
            boolean[] toRemove = new boolean[]{true, true, true, true, false};

            float[] summed = ArrayUtility.sumIndices(values, toRemove);
            assertEquals(1, summed.length, "we kept 2 values and summed up 3");
            assertArrayEquals(new float[]{5 + 10 + 0 + 10f - 5}, summed);
        }
        {   //dont keep any
            float[] values = new float[]{5, 10, 0, 10, -5};
            boolean[] toRemove = new boolean[]{true, true, true, true, true};

            float[] summed = ArrayUtility.sumIndices(values, toRemove);
            assertEquals(1, summed.length, "we kept 2 values and summed up 3");
            assertArrayEquals(new float[]{5 + 10 + 0 + 10f - 5}, summed);
        }
        {   //only keep first
            float[] values = new float[]{5, 10, 0, 10, -5};
            boolean[] toRemove = new boolean[]{false, true, true, true, true};

            float[] summed = ArrayUtility.sumIndices(values, toRemove);
            assertEquals(1, summed.length, "we kept 2 values and summed up 3");
            assertArrayEquals(new float[]{5 + 10 + 0 + 10f - 5}, summed);
        }
    }

    @Test
    void removePositionsFloat() {
        float[] values = new float[]{5, 10, 0, 10, -5};
        boolean[] toRemove = new boolean[]{false, true, false, true, true};

        float[] filtered = ArrayUtility.removePositions(values, toRemove);
        assertEquals(2, filtered.length, "we kept 2 values and summed up 3");
        assertArrayEquals(new float[]{5, 0}, filtered);
    }

    @Test
    void removePositionsInt() {
        int[] values = new int[]{5, 10, 0, 10, -5};
        boolean[] toRemove = new boolean[]{false, true, false, true, true};

        int[] filtered = ArrayUtility.removePositions(values, toRemove);
        assertEquals(2, filtered.length, "we kept 2 values and summed up 3");
        assertArrayEquals(new int[]{5, 0}, filtered);
    }

    @Test
    void transposeHandles() {
        {        //5x1
            ArrayList<float[]> matrix = new ArrayList<>();
            matrix.add(new float[]{5, 10, 0, 10, -5});
            matrix = ArrayUtility.transposeMatrix(matrix);

            assertEquals(5, matrix.size());
            assertArrayEquals(new float[]{5}, matrix.get(0));
            assertArrayEquals(new float[]{10}, matrix.get(1));
            assertArrayEquals(new float[]{0}, matrix.get(2));
            assertArrayEquals(new float[]{10}, matrix.get(3));
            assertArrayEquals(new float[]{-5}, matrix.get(4));
        }

        {        //5x2
            ArrayList<float[]> matrix = new ArrayList<>();
            matrix.add(new float[]{5, 10, 0, 10, -5});
            matrix.add(new float[]{5, 4, 3, 2, 1});
            matrix = ArrayUtility.transposeMatrix(matrix);

            assertEquals(5, matrix.size());
            assertArrayEquals(new float[]{5, 5}, matrix.get(0));
            assertArrayEquals(new float[]{10, 4}, matrix.get(1));
            assertArrayEquals(new float[]{0, 3}, matrix.get(2));
            assertArrayEquals(new float[]{10, 2}, matrix.get(3));
            assertArrayEquals(new float[]{-5, 1}, matrix.get(4));
        }

        {        //0x2
            ArrayList<float[]> matrix = new ArrayList<>();
            matrix.add(new float[]{});
            matrix.add(new float[]{});
            matrix = ArrayUtility.transposeMatrix(matrix);

            assertEquals(0, matrix.size());
        }

        {        //0x0
            ArrayList<float[]> matrix = new ArrayList<>();
            matrix = ArrayUtility.transposeMatrix(matrix);
            assertEquals(0, matrix.size());
        }

        { // M^T^T = M
            ArrayList<float[]> matrix = new ArrayList<>();
            matrix.add(new float[]{5, 10, 0, 10, -5});
            matrix.add(new float[]{5, 4, 3, 2, 1});
            ArrayList<float[]> matrixTT = ArrayUtility.transposeMatrix(ArrayUtility.transposeMatrix(matrix));
            for (int i = 0; i < matrix.size(); i++) {
                assertArrayEquals(matrixTT.get(i), matrix.get(i));
            }
        }
    }

    @Test
    void flattenNestedList() {
        {
            ArrayList<float[]> matrix = new ArrayList<>();
            matrix.add(new float[]{5, 10, 0, 10, -5});
            matrix.add(new float[]{5, 4, 3, 2, 1});
            float[] flat = ArrayUtility.flattenNestedList(matrix);
            assertArrayEquals(new float[]{5, 10, 0, 10, -5, 5, 4, 3, 2, 1}, flat);
        }

        {
            ArrayList<float[]> nest = new ArrayList<>(5);
            nest.add(new float[]{1, 2, 3, 4});
            nest.add(new float[]{5, 6, 7, 8, 9, 10});
            nest.add(new float[]{});
            nest.add(new float[]{11});

            float[] flat = ArrayUtility.flattenNestedList(nest);
            assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, flat);
        }
    }
}