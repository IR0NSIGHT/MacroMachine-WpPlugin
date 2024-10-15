package org.demo.wpplugin.geometry;

import org.demo.wpplugin.kernel.KernelConvolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KernelConvolutionTest {

    @Test
    void calculateGradient() {
        float[] points = new float[] {
                0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30
        };
        float[] gradients = KernelConvolution.calculateGradient(points);
        assertEquals(points.length, gradients.length);
        for (int i = 0; i < gradients.length; i++) {
            assertEquals(1, gradients[i],0.01f);
        }

        //doesnt crash on empty list
        points = new float[0];
        gradients = KernelConvolution.calculateGradient(points);
        assertEquals(points.length, gradients.length);


        //doesnt crash on to short list
        points = new float[] {
                0
        };
        gradients = KernelConvolution.calculateGradient(points);
        assertEquals(points.length, gradients.length);
        for (Float f : gradients) {
            assertEquals(0, f,0.01f);
        }

        //doesnt crash on to short list
        points = new float[] {
                0,1
        };
        gradients = KernelConvolution.calculateGradient(points);
        assertEquals(points.length, gradients.length);
        for (Float f : gradients) {
            assertEquals(0, f,0.01f);
        }

        //doesnt crash on single gradient list
        points = new float[] {
                -23,-22,-21,-20,-19 //default kernel is 5 wide total
        };
        gradients = KernelConvolution.calculateGradient(points);
        assertEquals(points.length, gradients.length);
        for (Float f : gradients) {
            assertEquals(1, f,0.01f);
        }
     }
}