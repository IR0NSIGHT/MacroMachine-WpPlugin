package org.ironsight.wpplugin.macromachine;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebUIServer {
    private static final int PORT = 8080;
    private HttpServer server;

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        Path webuiPath = Paths.get("target/webui-dist");

        // Serve static files
        server.createContext("/", new StaticFileHandler(webuiPath));

        // Dummy REST endpoints
        server.createContext("/macros", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "[{\"id\":1,\"name\":\"Sample Macro\",\"description\":\"A dummy macro\"}]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.createContext("/actions", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "[{\"id\":1,\"name\":\"Sample Action\",\"type\":\"apply\"}]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
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