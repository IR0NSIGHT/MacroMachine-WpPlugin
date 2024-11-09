package org.ironsight.wpplugin.rivertool;

import java.util.ArrayList;
import java.util.List;

public interface Subdivide {
    static ArrayList<float[]> subdivide(float[] xsPos, float[] ysPos, int startHandleIdx, int subdivisionAmount,
                                        Subdivide action) {
        List<Float> xs = new ArrayList<>();
        xs.add(xsPos[startHandleIdx]);
        xs.add(xsPos[startHandleIdx + 1]);

        List<Float> ys = new ArrayList<>();
        ys.add(ysPos[startHandleIdx]);
        ys.add(ysPos[startHandleIdx + 1]);

        for (int s = 0; s < subdivisionAmount; s++) {        //add middle points for all neighbouring values
            List<Float> newXs = new ArrayList<>(xs.size() * 2);
            List<Float> newYs = new ArrayList<>(xs.size() * 2);
            newXs.addAll(xs);
            newYs.addAll(ys);
            int targetIdx = 0;
            boolean changed = false;
            for (int i = 0; i < xs.size() - 1; i++) {
                changed = true;
                float[] middleX = action.subdividePoints(xs.get(i), xs.get(i + 1), ys.get(i), ys.get(i + 1));

                //add into new list
                targetIdx++;
                newXs.add(targetIdx, middleX[0]);
                newYs.add(targetIdx, middleX[1]);
                targetIdx++;
            }
            xs = newXs;
            ys = newYs;
            if (!changed)
                break;
        }

        // add old positions from zero to start
        for (int i = 0; i < startHandleIdx; i++) {
            xs.add(i,xsPos[i]);
            ys.add(i,ysPos[i]);
        }

        //add old positions from end to end-of-array
        for (int i = startHandleIdx + 2; i < xsPos.length; i++) {
            xs.add(xsPos[i]);
            ys.add(ysPos[i]);
        }

        float[] outXs = ArrayUtility.toFloatArray(xs);
        float[] outYs = ArrayUtility.toFloatArray(ys);
        ArrayList<float[]> outFlats = new ArrayList<>();
        outFlats.add(outXs);
        outFlats.add(outYs);
        return outFlats;
    }

    float[] subdividePoints(float x1, float x2, float y1, float y2);

}
