package org.demo.wpplugin;

import java.util.ArrayList;
import java.util.List;

public interface Subdivide {
    static float[] subdivide(float[] xsPos, int startHandleIdx, int subdivisionAmount, Subdivide action) {
        List<Float> xs = new ArrayList<>();
        xs.add(xsPos[startHandleIdx]);
        xs.add(xsPos[startHandleIdx + 1]);

        for (int s = 0; s < subdivisionAmount; s++) {        //add middle points for all neighbouring values
            List<Float> newXs = new ArrayList<>(xs.size() * 2);
            newXs.addAll(xs);
            int targetIdx = 0;
            for (int i = 0; i < xs.size() - 1; i++) {
                float xStart = xs.get(i);
                float xEnd = xs.get(i + 1);
                float middleX = action.subdividePoints(xStart, xEnd);

                //add into new list
                targetIdx++;
                newXs.add(targetIdx, middleX);
                targetIdx++;
            }
            xs = newXs;
        }

        float[] out = ArrayUtility.toFloatArray(xs);
        return out;
    }

    float subdividePoints(float x1, float x2);

}
