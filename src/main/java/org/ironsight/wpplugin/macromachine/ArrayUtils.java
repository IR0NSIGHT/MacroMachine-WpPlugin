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

    public static float[] append(float[] a, float[] b) {
        if (a == null) return (b == null) ? new float[0] : b.clone();
        if (b == null) return a.clone();

        float[] result = new float[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static float[] reverse(float[] a) {
        if (a == null) return null;

        float[] result = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[a.length - 1 - i];
        }
        return result;
    }

    /**
     *
     * @param center
     * @param right
     * @return
     */
    public static float[] padLeftRight(float[] center, float[] right) {
        float[] out = append(reverse(right),append(center,right));
        return out;
    }

}
