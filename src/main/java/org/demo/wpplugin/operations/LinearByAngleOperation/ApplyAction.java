package org.demo.wpplugin.operations.LinearByAngleOperation;

import java.awt.*;

public interface ApplyAction {
    /**
     *
     * @param strength 0 to 1: 0 = none, 1 = 100%
     */
    public void applyWithStrength(float strength, Point p) ;

}
