package org.demo.wpplugin.Gui;

import java.io.*;
import java.util.ArrayList;

public class DefaultHeightMap {
    public static void saveFloatArrayToFile(float[][] array, String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (float[] row : array) {
                for (int j = 0; j < row.length; j++) {
                    writer.write(Float.toString(row[j]));
                    if (j < row.length - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine(); // Move to the next line for the next row
            }
        }
    }

    public static float[][] loadFloatArrayFromFile(String fileName)  {
        ArrayList<float[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                float[] row = new float[values.length];
                for (int i = 0; i < values.length; i++) {
                    row[i] = Float.parseFloat(values[i]);
                }
                rows.add(row);
            }
        } catch (IOException ex) {
            return new float[][]{
                    {1,2,3},
                    {4,5,6},
                    {20,20,24}
            };
        }

        // Convert ArrayList to float[][]
        float[][] array = new float[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            array[i] = rows.get(i);
        }
        return array;
    }
}
