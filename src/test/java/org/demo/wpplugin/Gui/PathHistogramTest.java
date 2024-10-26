package org.demo.wpplugin.Gui;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PointInterpreter;

import javax.swing.*;
import java.util.Random;

import static org.demo.wpplugin.pathing.Path.newFilledPath;

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