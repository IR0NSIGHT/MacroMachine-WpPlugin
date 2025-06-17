package org.ironsight.wpplugin.macromachine.Gui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class IconManager {
    enum Icon {
        MACRO("/icons/macro.png"),
        ACTION("/icons/action.png"),
        INPUT("/icons/input.png"),
        OUTPUT("/icons/output.png"),
        PARAM("/icons/param.png"),
        INVALID("/icons/invalid.png");
        private Icon(String resourcePath) {
            this.resourcePath = resourcePath;
        }
        ImageIcon icon;
        String resourcePath;
    }

    public static ImageIcon getIcon(Icon icon) {
        if (icon.icon != null) {
            return icon.icon;
        }
        //load
        URL iconUrl = IconManager.class.getResource(icon.resourcePath);

        if (iconUrl != null) {

            ImageIcon originalIcon = new ImageIcon(iconUrl);
            Image scaledImage = originalIcon.getImage().getScaledInstance(20,20, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            return scaledIcon;
        } else {
            System.err.println("Icon file not found!");
            throw new RuntimeException("icon file not found " + icon + " at url " + iconUrl);
        }
    }
}
