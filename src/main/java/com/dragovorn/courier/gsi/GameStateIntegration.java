package com.dragovorn.courier.gsi;

import com.dragovorn.courier.Courier;
import com.dragovorn.courier.Version;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class GameStateIntegration implements HttpHandler {

    private HttpServer server;

    public GameStateIntegration(int port) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0); // Make our http server
            this.server.createContext("/", this);
            this.server.setExecutor(null);
            this.server.start();

            Courier.getInstance().getLogger().info("Initiated GSI http server!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.server.stop(0);
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

            JsonObject object = new Gson().fromJson(qry, JsonObject.class);

            DiscordRichPresence presence = new DiscordRichPresence();
            presence.details = "Courier v" + Version.getVersion();

            if (object.has("map")) {
                String game = object.get("map").getAsJsonObject().get("customgamename").getAsString();
                String hero = object.get("hero").getAsJsonObject().get("name").getAsString();

                switch (object.get("map").getAsJsonObject().get("customgamename").getAsString()) {
                    case "hero_demo":
                        game = "Demo Hero";
                        break;
                }

                switch (object.get("hero").getAsJsonObject().get("name").getAsString()) {
                    case "npc_dota_hero_meepo":
                        hero = "Meepo";
                        break;
                }

                Courier.getInstance().getLogger().info(game + ": " + hero + " (Level: " + object.get("hero").getAsJsonObject().get("level").getAsInt() + ")");
                presence.startTimestamp = System.currentTimeMillis() / 1000L - object.get("map").getAsJsonObject().get("clock_time").getAsInt();
                presence.state = game + ": " + hero + " (Level: " + object.get("hero").getAsJsonObject().get("level").getAsInt() + ")";
            } else {
                Courier.getInstance().getLogger().info("Sending new menu presence...");
                presence.state = "Main Menu";
            }

            DiscordRPC.DiscordUpdatePresence(presence);
        }

        exchange.close();
    }
}