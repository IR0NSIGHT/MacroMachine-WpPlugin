package org.ironsight.wpplugin.macromachine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilsTest {

    // ---- findMin tests ----

    @Test
    void findMin_emptyArray_returnsMinusOne() {
        int[] arr = {};
        int res = ArrayUtils.findMin(arr);
        assertEquals(Integer.MAX_VALUE, res, "Empty array should return int-max");
    }

    @Test
    void findMin_singleElement_returnsThatElement() {
        int[] arr = {42};
        int res = ArrayUtils.findMin(arr);
        assertEquals(42, res, "Single-element array should return that element");
    }

    @Test
    void findMin_multipleElements_returnsSmallest() {
        int[] arr = {5, 3, 8, 2, 9};
        int res = ArrayUtils.findMin(arr);
        assertEquals(2, res, "Should return the smallest element");
    }

    // ---- findMax tests ----

    @Test
    void findMax_emptyArray_returnsMinusOne() {
        int[] arr = {};
        int res = ArrayUtils.findMax(arr);
        assertEquals(Integer.MIN_VALUE, res, "Empty array should return min value");
    }

    @Test
    void findMax_singleElement_returnsThatElement() {
        int[] arr = {42};
        int res = ArrayUtils.findMax(arr);
        assertEquals(42, res, "Single-element array should return that element");
    }

    @Test
    void findMax_multipleElements_returnsLargest() {
        int[] arr = {5, 3, 8, 2, 9};
        int res = ArrayUtils.findMax(arr);
        assertEquals(9, res, "Should return the largest element");
    }
}
