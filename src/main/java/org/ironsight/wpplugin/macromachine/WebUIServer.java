package org.ironsight.wpplugin.macromachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.ironsight.wpplugin.macromachine.REST.MMActionBuilder;
import org.ironsight.wpplugin.macromachine.REST.MacroBuilder;
import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.ironsight.wpplugin.macromachine.REST.IOMapper.toInputOutputJson;

public class WebUIServer {
    private static final int PORT = 8080;
    private HttpServer server;
    private static final ObjectMapper mapper = new ObjectMapper();
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        Path webuiPath = Paths.get("target/webui-dist");

        // Serve static files
        server.createContext("/", new StaticFileHandler(webuiPath));

        server.createContext("/macroList", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {

                try {
                    final var macros = MacroContainer.getInstance().queryAll();

                    if (macros.isEmpty()) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }

                    // ✅ ONLY UUIDs
                    List<String> responseList = macros.stream()
                            .map(macro -> macro.getUid().toString())
                            .toList();

                    String response = mapper.writeValueAsString(responseList);
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }

            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.createContext("/macro", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {

                try {
                    final var macros = MacroContainer.getInstance().queryAll();

                    if (macros.isEmpty()) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }

                    // ✅ read UUID query param
                    var query = exchange.getRequestURI().getQuery();
                    String uuidParam = null;

                    if (query != null && query.contains("uuid=")) {
                        uuidParam = query.split("uuid=")[1].split("&")[0];
                    }

                    // ❌ missing uuid
                    if (uuidParam == null) {
                        String response = """
                        {
                          "error": "MISSING_UUID",
                          "message": "Query parameter 'uuid' is required"
                        }
                        """;

                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(400, bytes.length);

                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                        return;
                    }

                    UUID macroID;

                    try {
                        macroID = UUID.fromString(uuidParam);
                    } catch (IllegalArgumentException ex) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }

                    // ✅ fetch directly from container (same as action style)
                    var macro = MacroContainer.getInstance().queryById(macroID);

                    if (macro == null) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }

                    String response = MacroBuilder.buildMacroJson(macro);
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }

            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.createContext("/action", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {

                final var actions = MappingActionContainer.getInstance().queryAll();

                if (actions.isEmpty()) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                // ✅ NEW: read UUID query param
                var query = exchange.getRequestURI().getQuery();
                String uuidParam = null;
                if (query != null && query.contains("uuid=")) {
                    uuidParam = query.split("uuid=")[1].split("&")[0];
                }

                if (uuidParam == null) {
                    String response = """
                            {
                              "error": "MISSING_UUID",
                              "message": "Query parameter 'uuid' is required"
                            }
                            """;

                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(400, bytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    return;
                }

                UUID actionID = null;
                try {
                    actionID = UUID.fromString(uuidParam);
                } catch (IllegalArgumentException ex) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                if (!MappingActionContainer.getInstance().queryContains(actionID)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                var action = MappingActionContainer.getInstance().queryById(actionID);

                try {
                    String response = MMActionBuilder.buildMMActionJson(
                            action.getName(),
                            action.getDescription(),
                            action.getUid().toString(),
                            action.getActionType().displayName,
                            toInputOutputJson(action.getInput(), true),
                            toInputOutputJson(action.getOutput(), false),
                            Arrays.stream(action.getInput().getAllInputValues()).boxed().toList(),
                            Arrays.stream(action.getInput().getAllInputValues()).map(action::map).boxed().toList()
                    );

                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                    System.out.println("sending action GET query: \n" + response);

                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }

            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("WebUI Server started on port " + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("WebUI Server stopped");
        }
    }

    static class StaticFileHandler implements HttpHandler {
        private final Path rootPath;

        public StaticFileHandler(Path rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }
            Path filePath = rootPath.resolve(requestPath.substring(1));
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                String contentType = getContentType(filePath);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(filePath);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }

        private String getContentType(Path path) {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(".html")) return "text/html";
            if (fileName.endsWith(".css")) return "text/css";
            if (fileName.endsWith(".js")) return "application/javascript";
            return "application/octet-stream";
        }
    }
}
