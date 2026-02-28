package services;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LocalRedirectServer {

    private HttpServer server;
    private String authorizationCode;
    private boolean codeReceived = false;
    private int port = 8889; // Start from 8889 instead of 8888
    private int maxAttempts = 10;

    public void startServer() throws IOException {
        boolean started = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                int currentPort = port + attempt;
                server = HttpServer.create(new InetSocketAddress(currentPort), 0);
                server.createContext("/", new RedirectHandler());
                server.setExecutor(Executors.newSingleThreadExecutor());
                server.start();
                this.port = currentPort;
                System.out.println("✅ Local redirect server started on http://localhost:" + currentPort);
                started = true;
                break;

            } catch (BindException e) {
                System.out.println("⚠️ Port " + (port + attempt) + " is busy, trying next...");
            }
        }

        if (!started) {
            throw new IOException("Could not find an available port after " + maxAttempts + " attempts");
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 Local redirect server stopped");
        }
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public boolean isCodeReceived() {
        return codeReceived;
    }

    public int getPort() {
        return port;
    }

    private class RedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String response;
            int statusCode;

            if (query != null && query.contains("code=")) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    if (pair.startsWith("code=")) {
                        authorizationCode = pair.substring(5);
                        codeReceived = true;
                        break;
                    }
                }

                response = "<html>" +
                        "<head><title>Authentication Successful</title></head>" +
                        "<body style='font-family: Arial; text-align: center; padding: 50px;'>" +
                        "<h1 style='color: #2e7d32;'>✅ Authentication Successful!</h1>" +
                        "<p>You have successfully signed in with Google.</p>" +
                        "<p>You can close this window now.</p>" +
                        "<script>window.close();</script>" +
                        "</body></html>";
                statusCode = 200;
            } else {
                response = "<html>" +
                        "<head><title>Error</title></head>" +
                        "<body style='font-family: Arial; text-align: center; padding: 50px;'>" +
                        "<h1 style='color: #c62828;'>❌ Authentication Failed</h1>" +
                        "<p>No authorization code received.</p>" +
                        "</body></html>";
                statusCode = 400;
                codeReceived = true;
            }

            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}