package com.dragovorn.courier.gsi;

import com.dragovorn.courier.Courier;
import com.dragovorn.util.lang.Locale;
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

    private Locale locale;

    public GameStateIntegration(int port) {
        this.locale = new Locale("/lang/en-US.lang", "en-US");

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

            if (object.has("map")) {
                String unlocalized_hero = object.get("hero").getAsJsonObject().get("name").getAsString();

                String paused = "";

                if (object.get("map").getAsJsonObject().get("paused").getAsBoolean()) {
                    paused = " (PAUSED)";
                } else {
                    presence.startTimestamp = System.currentTimeMillis() / 1000L - object.get("map").getAsJsonObject().get("clock_time").getAsInt();
                }

                presence.details = (object.get("hero").getAsJsonObject().get("alive").getAsBoolean() ? "ALIVE" : "DEAD") + paused;
                presence.state = "Level " + object.get("hero").getAsJsonObject().get("level").getAsInt() + " " + this.locale.translate(unlocalized_hero);
                presence.largeImageKey = unlocalized_hero;
                presence.largeImageText =  (object.get("hero").getAsJsonObject().get("buyback_cooldown").getAsInt() == 0 ? "BUYBACK OFF CD (" + object.get("hero").getAsJsonObject().get("buyback_cost").getAsInt() + " G)" : "BUYBACK ON CD (" + object.get("hero").getAsJsonObject().get("buyback_cooldown").getAsInt() + " S)");
                presence.smallImageKey = "unranked";
                presence.smallImageText = "Comming Soon...";
            } else {
                presence.details = "Main Menu";
                presence.largeImageKey = "main_menu";
                presence.smallImageKey = "unranked";
                presence.smallImageText = "Comming Soon...";
            }

            DiscordRPC.DiscordUpdatePresence(presence);
        }

        exchange.close();
    }
}