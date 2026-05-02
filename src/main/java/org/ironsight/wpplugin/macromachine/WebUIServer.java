package org.ironsight.wpplugin.macromachine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.ironsight.wpplugin.macromachine.REST.MMActionBuilder;
import org.ironsight.wpplugin.macromachine.REST.MacroBuilder;
import org.ironsight.wpplugin.macromachine.operations.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WebUIServer {
    private static final int PORT = 8080;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    private void handleMacroList(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

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

    }

    private void handleMacro(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

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

    }

    private MappingAction getActionFromQuery(HttpExchange exchange) throws IOException {
        final var actions = MappingActionContainer.getInstance().queryAll();

        if (actions.isEmpty()) {
            exchange.sendResponseHeaders(204, -1);
            return null;
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
            return null;
        }

        UUID actionID = null;
        try {
            actionID = UUID.fromString(uuidParam);
        } catch (IllegalArgumentException ex) {
            exchange.sendResponseHeaders(404, -1);
            return null;
        }
        if (!MappingActionContainer.getInstance().queryContains(actionID)) {
            exchange.sendResponseHeaders(404, -1);
            return null;
        }
        var action = MappingActionContainer.getInstance().queryById(actionID);
        return action;
    }

    private MappingPoint[] getRequestedPoints(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isEmpty()) {
            return null; // or throw if required
        }

        String pointsParam = null;

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && "points".equals(keyValue[0])) {
                pointsParam = keyValue[1];
                break;
            }
        }

        if (pointsParam == null) {
            return null; // or throw if required
        }

        try {
            // Decode URL-encoded JSON
            String decodedJson = URLDecoder.decode(pointsParam, StandardCharsets.UTF_8);

            // Convert JSON -> MappingPoint[]
            return objectMapper.readValue(decodedJson, MappingPoint[].class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse 'points' query parameter", e);
        }
    }

    private void handleAction(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("GET".equals(exchange.getRequestMethod())) {
            var action = getActionFromQuery(exchange);
            if (action == null)
                return; // already handled internally
            var requestedPoints = getRequestedPoints(exchange);
            if (requestedPoints != null)
                action = action.withNewPoints(requestedPoints);

            try {
                String response = MMActionBuilder.buildMMActionJson(action);

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                System.out.println("sending action GET query: \n" + response);

            } catch (JsonProcessingException e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }

        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        Path webuiPath = Paths.get("target/webui-dist");

        // Serve static files
        server.createContext("/", new StaticFileHandler(webuiPath));

        server.createContext("/macroList", this::handleMacroList);

        server.createContext("/macro", this::handleMacro);

        server.createContext("/action", this::handleAction);

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
