package org.demo.wpplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArrayUtility {

    public static boolean isValidArray(float[] arr) {
        for (float element : arr) {
            if (Float.isNaN(element) || Float.isInfinite(element)) {
                return false;
            }
        }
        return true;
    }


    /**
     * will turn a n x m list into an m x n list
     * deep-clones inputs
     *
     * @param input matrix N x M
     * @return matrix M x N
     */
    public static ArrayList<float[]> transposeMatrix(ArrayList<float[]> input) {
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
     *
     * @param original
     * @param toBeRemoved
     * @return
     * @ensures sum of input = sum of output
     * @ensures length output = amount FALSE in toBeRemoved
     */
    public static float[] sumIndices(float[] original, boolean[] toBeRemoved) {
        // Step 1: Count how many elements will remain
        int count = 0;
        for (boolean remove : toBeRemoved) {
            if (!remove) {
                count++;
            }
        }
        count = Math.max(1, count);
        // Step 2: Create a new array with the correct size
        float[] result = new float[count];

        // Step 3: Copy elements that are not marked for removal
        int resultI = 0;
        float acc = 0;
        Map<Integer, Float> summedSegment = new HashMap<>();
        for (int i = 0; i < original.length; i++) {
            if (!toBeRemoved[i]) {
                summedSegment.put(resultI - 1, summedSegment.getOrDefault(resultI - 1, 0f) + acc);
                System.out.println("add acc to result i " + (resultI - 1));

                acc = 0;
                acc += original[i];
                System.out.println("acc + idx " + i);
                resultI++;

                if (i == original.length - 1) {
                    summedSegment.put(resultI - 1, summedSegment.getOrDefault(resultI - 1, 0f) + acc);
                    System.out.println("final add acc to result i " + (resultI - 1));
                }
            } else if (i == original.length - 1) {
                //i is the last index
                acc += original[i];
                System.out.println("acc + idx " + i);

                summedSegment.put(resultI - 1, summedSegment.getOrDefault(resultI - 1, 0f) + acc);
                System.out.println("final add acc to result i " + (resultI - 1));

            } else {
                acc += original[i];
                System.out.println("acc + idx " + i);
            }
        }
        for (Map.Entry<Integer, Float> entry : summedSegment.entrySet()) {
            if (entry.getKey() < 0) {
                result[0] += entry.getValue();
            } else if (entry.getKey() >= result.length) {
                result[result.length - 1] += entry.getValue();
            } else
                result[entry.getKey()] += entry.getValue();
        }
        assert sumArray(result) == sumArray(original) :
                "the sum must be the same for both arrays:\n" + Arrays.toString(result) + "\n" + Arrays.toString(original);
        return result;
    }

    public static float sumArray(float[] arr) {
        float sum = 0;
        for (float f : arr)
            sum += f;
        return sum;
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
