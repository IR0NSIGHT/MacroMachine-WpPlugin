package org.ironsight.wpplugin.macromachine;

public class ArrayUtils {
    public static int findMin(int[] array) {
        if (array.length == 0)
            return Integer.MAX_VALUE;
        int min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static int findMax(int[] array) {
        if (array.length == 0)
            return Integer.MIN_VALUE;
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }
}
