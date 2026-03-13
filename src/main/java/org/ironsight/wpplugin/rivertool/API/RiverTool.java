package org.ironsight.wpplugin.rivertool.API;

public final class RiverTool
{
    private RiverTool() {
    } // prevent instantiation

    public static RiverToolAPI create() {
        return new RiverToolAPIImplementation();
    }
}
