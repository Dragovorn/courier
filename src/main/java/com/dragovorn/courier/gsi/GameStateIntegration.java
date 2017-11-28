package com.dragovorn.courier.gsi;

import com.dragovorn.courier.Courier;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class GameStateIntegration implements HttpHandler {

    private HttpServer server;

    private long previous;

    public GameStateIntegration(int port) {
        this.previous = 0;

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

//        Courier.getInstance().getLogger().info("Received request | " + exchange.getRequestMethod());

        if (exchange.getRequestMethod().equalsIgnoreCase("post")) { // Make sure we are getting post requests
            if (!(System.currentTimeMillis() >= this.previous + 15000)) {
                return;
            }

            this.previous = System.currentTimeMillis();


//            discordRichPresence.setDetails("Java | Discord RPC API");
//            discordRichPresence.setState("Developing");
//            discordRichPresence.setStartTimestamp(new Date().getTime());
//            discordRichPresence.setEndTimestamp(end);
//            discordRichPresence.setLargeImageKey("icon-large");
//            discordRichPresence.setSmallImageKey("icon-small");
//            discordRichPresence.setPartyId("ALONE");
//            discordRichPresence.setPartySize(1);
//            discordRichPresence.setPartyMax(2);
//            discordRichPresence.setMatchSecret("hello");
//            discordRichPresence.setJoinSecret("join");
//            discordRichPresence.setSpectateSecret("look");
//            discordRichPresence.setInstance(false);


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

//            Courier.getInstance().getLogger().info(qry);

            JsonObject object = new Gson().fromJson(qry, JsonObject.class);

//            DiscordRichPresence presence = new DiscordRichPresence();
//            presence.setDetails("Courier v" + Version.getVersion());
//            presence.setInstance(false);

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

//                presence.setState(game + ": " + hero + " (Level: " + object.get("hero").getAsJsonObject().get("level").getAsInt() + ")");
                Courier.getInstance().getLogger().info(game + ": " + hero + " (Level: " + object.get("hero").getAsJsonObject().get("level").getAsInt() + ")");
//                presence.setStartTimestamp(this.start.getTime());
//                presence.setEndTimestamp(new Date().getTime());
            } else {
                Courier.getInstance().getLogger().info("Sending new menu presence...");
//                presence.setState("Main Menu");
            }

//            if (object.has("map")) {
//                presence.setStartTimestamp(new Date().getTime());
//                presence.setEndTimestamp(end);
//                presence.setLargeImageKey("icon-large");
//                presence.setSmallImageKey("icon-small");
//                presence.setPartyId("ALONE");
//                presence.setPartySize(0);
//                presence.setPartyMax(0);
//                presence.setMatchSecret("hello");
//                presence.setJoinSecret("join");
//                presence.setSpectateSecret("look");

//            Courier.getInstance().getRpc().runCallbacks();
//            Courier.getInstance().getRpc().updatePresence(presence);
//            Courier.getInstance().getRpc().runCallbacks();
//            }

        }

        exchange.close();
    }
}