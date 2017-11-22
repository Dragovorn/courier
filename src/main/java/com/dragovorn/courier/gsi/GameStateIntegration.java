package com.dragovorn.courier.gsi;

import com.dragovorn.courier.Courier;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class GameStateIntegration implements HttpHandler {

    public GameStateIntegration(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0); // Make our http server
            server.createContext("/", this);
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, 0);

        if (exchange.getRequestMethod().equalsIgnoreCase("post")) { // Make sure we are getting post requests
            String encoding = "ISO-8859-1";
            String qry;
            InputStream in = exchange.getRequestBody();
            try { // Decode post data
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte buf[] = new byte[4096];
                for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                    out.write(buf, 0, n);
                }
                qry = new String(out.toByteArray(), encoding);
            } finally {
                in.close();
            }

            Courier.getInstance().getLogger().info(qry);
        }
    }
}