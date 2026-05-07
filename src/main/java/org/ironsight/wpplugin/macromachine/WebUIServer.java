package org.ironsight.wpplugin.macromachine;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.ironsight.wpplugin.macromachine.REST.MacroApplication;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebUIServer
{
    private HttpServer server;

    public static void main(String[] args) throws IOException {
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
