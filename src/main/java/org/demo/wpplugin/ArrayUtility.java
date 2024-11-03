package org.demo.wpplugin;

import java.util.ArrayList;

public class ArrayUtility {
    /**
     * will turn a n x m list into an m x n list
     * deep-clones inputs
     *
     * @param input matrix N x M
     * @return matrix M x N
     */
    public static ArrayList<float[]> transposeHandles(ArrayList<float[]> input) {
        if (input.size() == 0) {
            return new ArrayList<>();
        }
        int nLength = input.get(0).length;
        int mLenght = input.size();
        ArrayList<float[]> output = new ArrayList<>(nLength);

        for (int n = 0; n < nLength; n++) {
            float[] nThList = new float[mLenght];
            output.add(nThList);
            for (int m = 0; m < mLenght; m++) {
                float handle = input.get(m)[n];
                nThList[m] = handle;
            }
        }

        return output;
    }

    public static int[] toIntArray(ArrayList<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static float[] flattenNestedList(ArrayList<float[]> nested) {
        int totalLength = 0;
        for (float[] segment : nested) {
            totalLength += segment.length;
        }
        float[] out = new float[totalLength];
        int it = 0;
        for (float[] segment : nested) {
            for (int j = 0; j < segment.length; j++) {
                out[it++] = segment[j];
            }
        }
        return out;
    }

    public static boolean linearSearch(float[] arr, float val) {
        for (float element : arr) {
            if (element == val) {
                return true;
            }
        }
        return false;
    }

    /**
     * will take original array + marker array and combine marked indices with the previous ones.
     * all marked positions will be removed
     * unmarked positions gain the sum of marked values following it
     * 1,2,3,4,5 with 1 and 5 unmarked => {1+2+3+4,5
     * }
     * @param original
     * @param toBeRemoved
     * @return
     */
    public static float[] sumIndices(float[] original, boolean[] toBeRemoved) {
        // Step 1: Count how many elements will remain
        int count = 0;
        for (boolean remove : toBeRemoved) {
            if (!remove) {
                count++;
            }
        }

        // Step 2: Create a new array with the correct size
        float[] result = new float[count];
        int index = 0;

        // Step 3: Copy elements that are not marked for removal
        for (int i = 0; i < original.length; i++) {
            result[index] += original[i];
            if (i+1 < original.length && !toBeRemoved[i+1]) {
                index++;
            }
        }

        return result;
    }

    public static int[] removePositions(int[] original, boolean[] toBeRemoved) {
        // Step 1: Count how many elements will remain
        int count = 0;
        for (boolean remove : toBeRemoved) {
            if (!remove) {
                count++;
            }
        }

        // Step 2: Create a new array with the correct size
        int[] result = new int[count];
        int index = 0;

        // Step 3: Copy elements that are not marked for removal
        for (int i = 0; i < original.length; i++) {
            if (i < toBeRemoved.length && !toBeRemoved[i]) {
                result[index++] = original[i];
            }
        }

        return result;
    }

    public static float[] removePositions(float[] original, boolean[] toBeRemoved) {
        // Step 1: Count how many elements will remain
        int count = 0;
        for (boolean remove : toBeRemoved) {
            if (!remove) {
                count++;
            }
        }

        // Step 2: Create a new array with the correct size
        float[] result = new float[count];
        int index = 0;

        // Step 3: Copy elements that are not marked for removal
        for (int i = 0; i < original.length; i++) {
            if (i < toBeRemoved.length && !toBeRemoved[i]) {
                result[index++] = original[i];
            }
        }

        return result;
    }
}
