package org.ironsight.wpplugin.rivertool.Gui;

import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.operations.ContinuousCurve;
import org.ironsight.wpplugin.rivertool.pathing.Path;
import org.ironsight.wpplugin.rivertool.pathing.PointInterpreter;

import javax.swing.*;
import java.util.Random;

import static org.ironsight.wpplugin.rivertool.pathing.Path.newFilledPath;

class PathHistogramTest {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Path Histogram App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);


        Path p = newFilledPath(504, PointInterpreter.PointType.RIVER_2D);
        HeightDimension dim = HeightDimension.getEmptyMutableDimension();

        ContinuousCurve curve = ContinuousCurve.fromPath(p, dim);
        Random random = new Random(320);
        float height = 62;
        for (int i = 0; i < curve.curveLength(); i++) {
            dim.setHeight(curve.getPosX(i), curve.getPosY(i), height);
            height += random.nextFloat() * 1.5f * (random.nextBoolean() ? 1 : -1);
        }
        PathHistogram pathHistogram = new PathHistogram(p, 120, dim);
        frame.add(pathHistogram);

        frame.setVisible(true);
    }
}