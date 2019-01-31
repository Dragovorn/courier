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
import java.time.Instant;

public class GameStateIntegration implements HttpHandler {

    private HttpServer server;

    private Locale locale;

    private long gameStart;

    private int previousTime;

    private boolean running;

    public GameStateIntegration(int port) {
        this.locale = new Locale("/lang/en-US.lang", "en-US"); // Make sure to load our locale
        this.gameStart = -1;
        this.previousTime = Integer.MAX_VALUE;

        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0); // Make our http server
            this.server.createContext("/", this);
            this.server.setExecutor(null);
            this.server.start();

            Courier.getInstance().getLogger().info("Initiated GSI http server!");

            DiscordRPC.discordRunCallbacks(); // Manually run the first callback just to make sure everything connected properly
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
        DiscordRPC.discordRunCallbacks();

        if (!Courier.getInstance().canUpdate()) {
            return; // Make sure DiscordRPC is properly connected
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("post")) { // Make sure we are getting post requests
            String encoding = "ISO-8859-1";
            String qry;

            try (InputStream in = exchange.getRequestBody(); ByteArrayOutputStream out = new ByteArrayOutputStream()) { // Decode post data
                byte[] buf = new byte[4096];

                for (int x = in.read(buf); x > 0; x = in.read(buf)) {
                    out.write(buf, 0, x);
                }

                qry = new String(out.toByteArray(), encoding); // Encode post data to a string
            }

            JsonObject object = new Gson().fromJson(qry, JsonObject.class);

            DiscordRichPresence presence = new DiscordRichPresence();

            if (object.has("map")) {
                JsonObject hero = object.get("hero").getAsJsonObject();
                JsonObject map = object.get("map").getAsJsonObject();

                String unlocalized_hero = hero.get("name").getAsString();

                String status;

                if (!this.running) {
                    this.running = true;
                }

                int gameTime = map.get("clock_time").getAsInt();

                if (this.previousTime < gameTime && gameTime < 0) {
                    this.gameStart = -1;
                }

                this.previousTime = gameTime;

                if (this.gameStart == -1) {
                    this.gameStart = Instant.now().getEpochSecond() - gameTime;
                }

                String bonus = " ";

                if (map.get("paused").getAsBoolean()) {
                    bonus += "(Paused)";
                    this.gameStart = -1;
                } else {
                    if (map.get("game_state").getAsString().equals("DOTA_GAMERULES_STATE_PRE_GAME")) {
                        bonus += "(Preparing)";
                    } else if (!hero.get("alive").getAsBoolean()) {
                        bonus += hero.get("buyback_cooldown").getAsInt() == 0 ? "(Buyback " + hero.get("buyback_cost") + "g)" : "(No Buyback)";
                    }

                    if (this.gameStart > Instant.now().getEpochSecond()) {
                        presence.endTimestamp = this.gameStart;
                    } else {
                        presence.startTimestamp = this.gameStart;
                    }
                }

                status = (hero.getAsJsonObject().get("alive").getAsBoolean() ? "Alive" : "Dead") + bonus;

                presence.details = "Level " + hero.get("level").getAsInt() + " " + this.locale.translate(unlocalized_hero);
                presence.state = status;
                presence.largeImageKey = unlocalized_hero;

                DiscordRPC.discordUpdatePresence(presence);

                if (Courier.getInstance().isDebug()) {
                    Courier.getInstance().getLogger().info(qry);
                }
            } else if (this.running) {
                DiscordRPC.discordClearPresence();
                this.running = false;
                this.gameStart = -1;
            }
        }

        exchange.close();
    }
}