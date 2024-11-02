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
}
