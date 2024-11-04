package org.demo.wpplugin;

import java.util.ArrayList;
import java.util.List;

import static org.demo.wpplugin.pathing.PointUtils.getPositionalDistance;

public interface Subdivide {
    static ArrayList<float[]> subdivide(float[] xsPos, float[] ysPos, int startHandleIdx, int subdivisionAmount,
                                        Subdivide action) {
        List<Float> xs = new ArrayList<>();
        xs.add(xsPos[startHandleIdx]);
        xs.add(xsPos[startHandleIdx + 1]);

        List<Float> ys = new ArrayList<>();
        ys.add(ysPos[startHandleIdx]);
        ys.add(ysPos[startHandleIdx + 1]);

        float minDist = subdivisionAmount;
        for (int s = 0; s < 7; s++) {        //add middle points for all neighbouring values
            List<Float> newXs = new ArrayList<>(xs.size() * 2);
            List<Float> newYs = new ArrayList<>(xs.size() * 2);
            newXs.addAll(xs);
            newYs.addAll(ys);
            int targetIdx = 0;
            boolean changed = false;
            for (int i = 0; i < xs.size() - 1; i++) {
                float dist = getPositionalDistance(
                        new float[]{xs.get(i), ys.get(i)},
                        new float[]{xs.get(i + 1), ys.get(i + 1)}, 2);
                if (dist < minDist)
                    continue;
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


        float[] outXs = ArrayUtility.toFloatArray(xs);
        float[] outYs = ArrayUtility.toFloatArray(ys);
        ArrayList<float[]> outFlats = new ArrayList<>();
        outFlats.add(outXs);
        outFlats.add(outYs);
        return outFlats;
    }

    float[] subdividePoints(float x1, float x2, float y1, float y2);

}
