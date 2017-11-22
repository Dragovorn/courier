package com.dragovorn.courier;

import com.dragovorn.courier.gsi.GameStateIntegration;
import com.dragovorn.courier.log.CourierLogger;
import com.dragovorn.courier.log.LoggingOutputStream;
import com.dragovorn.courier.util.FileUtil;
import com.google.gson.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
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

    private static final int DOTA = 570;

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

        try {
            TrayIcon icon = new TrayIcon(ImageIO.read(FileUtil.getResource("icon.png")), "Courier"); // Make us a tray icon, so people can kill our program

            PopupMenu menu = new PopupMenu();
            menu.add("Courier v" + Version.getVersion());
            menu.addSeparator();

            MenuItem exit = new MenuItem("Exit"); // So our program can be stopped
            exit.addActionListener(this::shutdown);

            MenuItem logs = new MenuItem("View Logs Folder"); // So those who aren't good w/ technology can easily send log files
            logs.addActionListener(this::openLogs);

            menu.add(logs);
            menu.add(exit);

            icon.setPopupMenu(menu);

            SystemTray.getSystemTray().add(icon);
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }

        this.logger.info("Courier v" + Version.getVersion() + " starting...");

        if (!configFile.exists()) { // Do initial setup/loading
            JsonObject config = new JsonObject();

            this.logger.info("New installation detected, going through initial setup!");
            JFileChooser selector = new JFileChooser(new File(".")); // Ask user for where their dota directory is
            selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            selector.setDialogTitle("Select Dota 2 Folder");

            if (selector.showOpenDialog(Main.invisible) == JFileChooser.APPROVE_OPTION) {
                File file = selector.getSelectedFile();
                config.addProperty("directory", file.getAbsolutePath());
                this.logger.info("User selected: " + file.getAbsolutePath());
                this.logger.info("Checking to see if the user is being truthful...");

                GSIState state = generateGSI(file);

                if (state == GSIState.CRAETED || state == GSIState.WORKING) {
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
                    shutdown(null);
                }
            } else {
                this.logger.info("Action cancelled by user! Shutting down!");
                shutdown(null);
            }
        } else {
            this.logger.info("Found config file! Making sure everything is where it should be!");

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
                shutdown(null);
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

    private GSIState generateGSI(File file) {
        GSIState state = GSIState.BROKEN;

        file = new File(file, "game"); // Make sure it's actually a dota directory

        if (file.exists()) {
            file = new File(file, "dota");

            if (file.exists()) {
                file = new File(file, "cfg");

                if (file.mkdirs()) {
                    this.logger.info("There appears to be no CFG directory, creating it...");
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
                        writer.println("    \"heartbeat\"     \"2.0\"");
                        writer.println("    \"data\"");
                        writer.println("    {");
                        writer.println("        \"provider\"      \"0\"");
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
                    state = GSIState.CRAETED;
                }
            }
        }

        return state;
    }

    /*
     * Event is just there to allow lambda-ing, I don't actually use it (does anyone?)
     */
    private void shutdown(ActionEvent event) {
        this.logger.info("Courier v" + Version.getVersion() + " shutting down!");

        for (Handler handler : this.logger.getHandlers()) { // Close our logger handler
            handler.close();
        }

        System.exit(0);
    }

    /*
     * Event is just there to allow lambda-ing, I don't actually use it (does anyone?)
     */
    private void openLogs(ActionEvent event) {
        try {
            this.logger.info("User wants to see logs folder, tidy up lads, we're having people over!");
            Desktop.getDesktop().open(logDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private enum GSIState {
        WORKING,
        CRAETED,
        BROKEN
    }
}