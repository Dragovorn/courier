package com.dragovorn.courier;

import com.dragovorn.courier.gsi.GameStateIntegration;
import com.dragovorn.courier.log.CourierLogger;
import com.dragovorn.courier.log.LoggingOutputStream;
import com.dragovorn.courier.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Courier {

    private GameStateIntegration integration;

    private Logger logger;

    public static File baseDir;
    public static File configFile;
    public static File logDir;

    private static Courier instance;

    private boolean update;
    private boolean debug;
    private boolean publicIds;

    public Courier() {
        instance = this;
        baseDir = new File(System.getProperty("user.home"), ".courier"); // Best place to store log files
        configFile = new File(baseDir, "config.json");
        logDir = new File(Courier.baseDir, "logs");
        baseDir.mkdirs();
        logDir.mkdirs();
        this.logger = new CourierLogger();

        System.setErr(new PrintStream(new LoggingOutputStream(this.logger, Level.SEVERE), true)); // Make sure everything is set to our logger
        System.setOut(new PrintStream(new LoggingOutputStream(this.logger, Level.INFO), true));

        DiscordRPC.discordInitialize("383021631951339532", new DiscordEventHandlers.Builder()
                .setReadyEventHandler((user) -> {
                    setUpdate(true);
                    this.logger.info("Connected to Discord user " + user.username + "#" + user.discriminator + "!");
                })
                .build(), true);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown)); // Register our shutdown hook to make sure all the shutdown code is being executed

        SystemTray tray = SystemTray.get();

        if (tray == null) {
            throw new RuntimeException("Unable to load System Tray!");
        }

        tray.setTooltip("Courier");

        try {
            tray.setImage(ImageIO.read(FileUtil.getResource("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        tray.setStatus("v" + Version.getVersion());

        Menu menu = tray.getMenu();

        MenuItem tmp = new MenuItem("Open Logs", this::openLogs);
        tmp.setTooltip("Open your logs folder to provide logs in an issue report");

        menu.add(tmp);

        tmp = new MenuItem("Open Issue Tracker", this::openIssueTracker);
        tmp.setTooltip("Opens the website to report an issue");

        menu.add(tmp);

        tmp = new MenuItem("Credits", this::openContributors);
        tmp.setTooltip("Opens the website that shows all the contributors");

        menu.add(tmp);
        menu.add(new Separator());

        Checkbox box = new Checkbox("Debug Mode", this::handleDebugClick);
        box.setChecked(false);

        menu.add(box);

        box = new Checkbox("Make IDs Public", this::handleIDsClick);
        box.setChecked(false); // TODO: Make this based off of the config

        menu.add(box);

        tmp = new MenuItem("Exit", this::exit);
        tmp.setTooltip("Exit Courier");

        menu.add(tmp);

        this.logger.info("Courier v" + Version.getVersion() + " starting...");

        if (!configFile.exists()) { // Do initial setup/loading
            JsonObject config = new JsonObject();

            this.logger.info("New installation detected, going through initial setup!");
            JFileChooser selector = new JFileChooser(new File(".")); // Ask user for where their dota directory is
            selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            selector.setDialogTitle("Select Dota 2 Beta Folder"); // Set the prompt for the directory picker

            if (selector.showOpenDialog(Main.parent) == JFileChooser.APPROVE_OPTION) {
                File file = selector.getSelectedFile();
                config.addProperty("directory", file.getAbsolutePath());
                this.logger.info("User selected: " + file.getAbsolutePath());
                this.logger.info("Double checking selected directory...");

                GSIState state = generateGSI(file);

                if (state == GSIState.CREATED || state == GSIState.WORKING) { // Make sure that everything is working
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    try {
                        FileWriter writer = new FileWriter(configFile);

                        gson.toJson(config, writer); // Save the config file
                        writer.close();

                        this.logger.info("Courier v" + Version.getVersion() + " started!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    this.logger.info("The user is a liar... Shutting down!");
                    System.exit(0);
                }
            } else {
                this.logger.info("Action cancelled by user! Shutting down!");
                System.exit(0);
            }
        } else {
            this.logger.info("Found config file! Verifying config integrity!");

            JsonObject config = new JsonObject();

            try {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(new FileReader(configFile));
                config = jsonElement.getAsJsonObject();
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            if (generateGSI(new File(config.get("directory").getAsString())) == GSIState.BROKEN) {
                this.logger.info("Config file pointing to incorrect directory! Shutting down!");
                System.exit(0);
            } else {
                this.logger.info("Courier v" + Version.getVersion() + " started!");
            }
        }

        this.integration = new GameStateIntegration(322);
    }

    public static Courier getInstance() {
        return instance;
    }

    public GameStateIntegration getIntegration() {
        return this.integration;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public synchronized void setUpdate(boolean update) {
        this.update = update;
    }

    public synchronized boolean canUpdate() { // Make sure to avoid data racing with this xd
        return this.update;
    }

    public synchronized void setDebug(boolean debug) {
        this.debug = debug;
    }

    public synchronized boolean isDebug() { // Make sure to avoid data racing with this xd
        return this.debug;
    }

    private GSIState generateGSI(File file) {
        GSIState state = GSIState.BROKEN;

        file = new File(file, "game"); // Make sure it's actually a dota directory

        if (file.exists()) {
            file = new File(file, "dota");

            if (file.exists()) {
                file = new File(file, "cfg");

                if (file.mkdirs()) {
                    this.logger.info("Creating CFG directory!");
                }

                file = new File(file, "gamestate_integration"); // Begin making our important files

                if (file.mkdirs()) {
                    this.logger.info("Creating gamestate_integration directory...");
                }

                file = new File(file, "gamestate_integration_courier.cfg");

                if (file.exists()) {
                    state = GSIState.WORKING;
                } else {
                    this.logger.info("Generating GSI file...");
                    try {
                        PrintWriter writer = new PrintWriter(file); // I could make this part copy a file from our resources... Might do this later down the line
                        writer.println("\"Dota 2 Integration Configuration\"");
                        writer.println("{");
                        writer.println("    \"uri\"           \"http://localhost:322/\"");
                        writer.println("    \"timeout\"       \"5.0\"");
                        writer.println("    \"buffer\"        \"0.1\"");
                        writer.println("    \"throttle\"      \"0.1\"");
                        writer.println("    \"heartbeat\"     \"10.0\"");
                        writer.println("    \"data\"");
                        writer.println("    {");
                        writer.println("        \"provider\"      \"1\"");
                        writer.println("        \"player\"        \"1\"");
                        writer.println("        \"map\"           \"1\"");
                        writer.println("        \"hero\"          \"1\"");
                        writer.println("        \"abilities\"     \"1\"");
                        writer.println("        \"items\"         \"1\"");
                        writer.println("    }");
                        writer.println("}");
                        writer.close(); // Literally most important part of this 'generation' code
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    JOptionPane.showMessageDialog(null, "If you have Dota 2 currently running please restart it.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    state = GSIState.CREATED;
                }
            }
        }

        return state;
    }

    private void shutdown() {
        this.logger.info("Courier v" + Version.getVersion() + " shutting down!");

        if (this.integration != null) {
            this.integration.stop();
        }

        for (Handler handler : this.logger.getHandlers()) { // Close our logger handler
            handler.close();
        }

        DiscordRPC.discordShutdown();
    }

    private void exit(ActionEvent event) {
        System.exit(0);
    }

    /*
     * Event is just there to allow lambda-ing, I don't actually use it (does anyone?)
     */
    private void openLogs(ActionEvent event) {
        try {
            Desktop.getDesktop().open(logDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Event is just there to allow lambda-ing, I don't actually use it (does anyone?)
     */
    private void openIssueTracker(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/Dragovorn/courier/issues"));
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Event is just there to allow lambda-ing, I don't actually use it (does anyone?)
     */
    private void openContributors(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/Dragovorn/courier/graphs/contributors"));
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDebugClick(ActionEvent event) {
        setDebug(((Checkbox) event.getSource()).getChecked());

        if (isDebug()) {
            this.logger.info("Enabling Debug Mode...");
        } else {
            this.logger.info("Disabling Debug Mode...");
        }
    }

    private void handleIDsClick(ActionEvent event) {
        this.publicIds = ((Checkbox) event.getSource()).getChecked();

        if (this.publicIds) {
            this.logger.info("Enabling Public Ids...");
        } else {
            this.logger.info("Disabling Public Ids...");
        }
    }

    private enum GSIState {
        WORKING,
        CREATED,
        BROKEN
    }
}