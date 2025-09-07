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

    @Test
    void append() {
        float[] a = {1.0f, 2.0f};
        float[] b = {3.0f, 4.0f};
        float[] expected = {1.0f, 2.0f, 3.0f, 4.0f};
        assertArrayEquals(expected, ArrayUtils.append(a, b));

        // Test appending empty array
        assertArrayEquals(a, ArrayUtils.append(a, new float[0]));
        assertArrayEquals(b, ArrayUtils.append(new float[0], b));

        // Test null handling
        assertArrayEquals(b, ArrayUtils.append(null, b));
        assertArrayEquals(a, ArrayUtils.append(a, null));
        assertArrayEquals(new float[0], ArrayUtils.append(null, null));
    }

    @Test
    void reverse() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] expected = {3.0f, 2.0f, 1.0f};
        assertArrayEquals(expected, ArrayUtils.reverse(a));

        // Test single element
        float[] single = {42.0f};
        assertArrayEquals(single, ArrayUtils.reverse(single));

        // Test empty array
        assertArrayEquals(new float[0], ArrayUtils.reverse(new float[0]));

        // Test null
        assertNull(ArrayUtils.reverse(null));
    }

    @Test
    void padLeftRight() {
        float[] center = {10,11,12,13};
        float[] right = {6,7,8};
        float[] expected = {8,7,6,10,11,12,13,6,7,8};
        float[] res = ArrayUtils.padLeftRight(center,right);
        assertArrayEquals(expected,res);
    }
}
