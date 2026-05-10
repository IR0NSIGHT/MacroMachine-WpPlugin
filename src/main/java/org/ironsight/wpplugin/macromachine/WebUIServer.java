package org.ironsight.wpplugin.macromachine;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.ironsight.wpplugin.macromachine.REST.MacroApplication;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import static org.ironsight.wpplugin.macromachine.threeDRendering.TestData.createDimension;

public class WebUIServer {
    private final MacroApplicator applicator;
    private final MacroContainer macros;
    private final MappingActionContainer actions;
    private HttpServer server;

    public WebUIServer(MacroApplicator applicator, MappingActionContainer actions, MacroContainer macros) {
        this.actions = actions;
        this.applicator = applicator;
        this.macros = macros;
    }

    public static void main(String[] args) throws IOException {
        var macros = new MacroContainer("./src/main/resources/DefaultMacros.json");
        var actions = new MappingActionContainer("./src/main/resources/DefaultActions.json");
        MacroContainer.SetInstance(macros);
        MappingActionContainer.SetInstance(actions);

        MacroContainer.getInstance().readFromFile();
        MappingActionContainer.getInstance().readFromFile();

        Dimension dimension = createDimension(new Rectangle(0,0,256,256),256);
        var applicator = new MacroConcurrentApplicator(macros, actions, ()->dimension);

        applicator.queueMacro(macros.queryAll().get(0).getUid());
        applicator.queueMacro(macros.queryAll().get(0).getUid());
        applicator.queueMacro(macros.queryAll().get(0).getUid());
        applicator.queueMacro(macros.queryAll().get(0).getUid());
        applicator.queueMacro(macros.queryAll().get(0).getUid());

        applicator.start();

        new WebUIServer(applicator, MappingActionContainer.getInstance(), MacroContainer.getInstance()).start();

        System.out.println("");


    }

    public void start() throws IOException {
        URI uri = URI.create("http://127.0.0.1:8080/");

        server = GrizzlyHttpServerFactory.createHttpServer(uri, new MacroApplication(applicator, actions, macros));

        System.out.println("REST server started:");
        System.out.println("http://localhost:8080/");
    }

    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
    }
}
