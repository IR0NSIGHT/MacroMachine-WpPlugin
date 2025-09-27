package org.ironsight.wpplugin.macromachine.Gui;

import java.util.prefs.Preferences;

public class Settings {
    private static final Preferences prefs =
            Preferences.userNodeForPackage(Settings.class);

    public static boolean getBool(String setting) {
        return prefs.getBoolean(setting, false); // default false
    }

    public static void setBool(String setting, boolean value) {
        prefs.putBoolean(setting, value);
    }
}