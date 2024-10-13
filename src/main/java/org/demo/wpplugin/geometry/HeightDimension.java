package org.demo.wpplugin.geometry;

public interface HeightDimension {
    static HeightDimension getDummyDimension62() {
        return new HeightDimension() {

            @Override
            public float getHeight(int x, int y) {
                return 62;
            }

            @Override
            public void setHeight(int x, int y, float z) {

            }
        };
    }

    float getHeight(int x, int y);

    void setHeight(int x, int y, float z);
}

