package org.ironsight.wpplugin.macromachine.Gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class WebUIViewPanel extends JPanel {
    private JFXPanel jfxPanel;
    private WebView webView;

    public WebUIViewPanel() {
        setLayout(new BorderLayout());
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            webView = new WebView();
            webView.getEngine().load("http://localhost:8080");
            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });
    }
}