package org.ironsight.wpplugin.macromachine;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for making the Maven project version number available to code.
 */
public class Version {
    public static final String VERSION;

    static {
        Properties versionProps = new Properties();
        try {
            versionProps.load(Version.class.getResourceAsStream("/org.ironsight.wpplugin.macromachine.properties"));
            VERSION = versionProps.getProperty("org.ironsight.wpplugin.macromachine.version");
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading version number from classpath", e);
        }
    }

    private Version() {
        // Prevent instantiation
    }
}
