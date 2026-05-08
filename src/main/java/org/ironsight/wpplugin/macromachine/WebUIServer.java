package org.ironsight.wpplugin.macromachine;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.ironsight.wpplugin.macromachine.REST.MacroApplication;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class WebUIServer
{
    private HttpServer server;

    public static void main(String[] args) throws IOException {
        MacroContainer.SetInstance(new MacroContainer("./src/main/resources/DefaultMacros.json"));
        MappingActionContainer.SetInstance(new MappingActionContainer("./src/main/resources/DefaultActions.json"));

        MacroContainer.getInstance().readFromFile();
        MappingActionContainer.getInstance().readFromFile();

        new WebUIServer().start();
    }

    public void start() throws IOException {
        URI uri = URI.create("http://0.0.0.0:8080/");

        server = GrizzlyHttpServerFactory.createHttpServer(uri, new MacroApplication());

        System.out.println("REST server started:");
        System.out.println("http://localhost:8080/");
    }

    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
    }
}
