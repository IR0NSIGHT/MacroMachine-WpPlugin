package org.ironsight.wpplugin.rivertool;

import java.util.ArrayList;

public interface Subdivide {
    static ArrayList<float[]> subdivide(ArrayList<float[]> handles,
                                        int subdivisionAmount,
                                        Subdivide action) {
        if (subdivisionAmount == 0)
            return handles;

        ArrayList<float[]> outHandles = new ArrayList<>(handles.size() << subdivisionAmount);
        int i = 0;
        for (; i < handles.size() - 1; i++) {
            //add into new list
            outHandles.add(handles.get(i));
            float[] middleX = action.subdividePoints(handles.get(i), handles.get(i + 1));
            outHandles.add(middleX);
        }
        outHandles.add(handles.get(handles.size() - 1));
        return subdivide(outHandles, subdivisionAmount - 1, action);
    }


    float[] subdividePoints(float[] a, float[] b);

}
