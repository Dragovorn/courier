package com.dragovorn.courier;

import com.dragovorn.courier.gsi.GameStateIntegration;
import com.dragovorn.courier.log.CourierLogger;
import com.dragovorn.courier.log.LoggingOutputStream;
import com.dragovorn.courier.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Courier {

    private GameStateIntegration integration;

    private Logger logger;

    public static File baseDir;
    public static File logDir;

    public Courier() {
        baseDir = new File(System.getProperty("user.home"), ".courier");
        logDir = new File(Courier.baseDir, "logs");
        baseDir.mkdirs();
        logDir.mkdirs();
        this.logger = new CourierLogger();

        System.setErr(new PrintStream(new LoggingOutputStream(this.logger, Level.SEVERE), true));
        System.setOut(new PrintStream(new LoggingOutputStream(this.logger, Level.INFO), true));

        try {
            TrayIcon icon = new TrayIcon(ImageIO.read(FileUtil.getResource("icon.png")), "Courier");

            PopupMenu menu = new PopupMenu();
            menu.add("Courier v" + Version.getVersion());
            menu.addSeparator();

            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(this::shutdown);

            MenuItem logs = new MenuItem("View Logs Folder");
            logs.addActionListener(this::openLogs);

            menu.add(logs);
            menu.add(exit);

            icon.setPopupMenu(menu);

            SystemTray.getSystemTray().add(icon);
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }

        this.logger.info("Courier v" + Version.getVersion() + " started!");
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
            Desktop.getDesktop().open(logDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}