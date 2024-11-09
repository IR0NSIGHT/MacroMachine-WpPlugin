package org.ironsight.wpplugin.rivertool.operations.LinearByAngleOperation;

import java.awt.*;

public interface ApplyAction {
    /**
     *
     * @param strength 0 to 1: 0 = none, 1 = 100%
     */
    void applyWithStrength(float strength, Point p) ;

}
