package org.ironsight.wpplugin.macromachine;

import static org.ironsight.wpplugin.macromachine.threeDRendering.TestData.createDimension;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.ironsight.wpplugin.macromachine.REST.MacroApplication;
import org.ironsight.wpplugin.macromachine.operations.*;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.Dimension;

public class WebUIServer
{
    private final MacroApplicator applicator;
    private final MacroContainer macros;
    private final MappingActionContainer actions;
    private HttpServer server;
    private final InputOutputProvider ioProvider;

    public WebUIServer(MacroApplicator applicator, MappingActionContainer actions, MacroContainer macros,
            InputOutputProvider ioProvider) {
        this.actions = actions;
        this.applicator = applicator;
        this.macros = macros;
        this.ioProvider = ioProvider;
    }

    public static void main(String[] args) throws IOException {
        var macros = new MacroContainer("./src/main/resources/DefaultMacros.json");
        var actions = new MappingActionContainer("./src/main/resources/DefaultActions.json");
        MacroContainer.SetInstance(macros);
        MappingActionContainer.SetInstance(actions);

        MacroContainer.getInstance().readFromFile();
        MappingActionContainer.getInstance().readFromFile();

        Dimension dimension = createDimension(new Rectangle(0, 0, 256, 256), 256);
        var applicator = new MacroConcurrentApplicator(macros, actions, () -> dimension,
                (uuid) -> System.out.println("macro finished running: " + uuid));

        macros.queryAll().stream().map(Macro::getUid).forEach(applicator::queueMacro);

        applicator.start();

        new WebUIServer(applicator, MappingActionContainer.getInstance(), MacroContainer.getInstance(),
                InputOutputProvider.INSTANCE).start();
    }

    public void start() throws IOException {
        URI uri = URI.create("http://127.0.0.1:8080/");

        server = GrizzlyHttpServerFactory.createHttpServer(uri,
                new MacroApplication(applicator, actions, macros, ioProvider));

        server.getServerConfiguration()
                .addHttpHandler(new CLStaticHttpHandler(getClass().getClassLoader(), "/static/"), "/");

        System.out.println("REST server started:");
        System.out.println("http://localhost:8080/");
    }

    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
    }
}
