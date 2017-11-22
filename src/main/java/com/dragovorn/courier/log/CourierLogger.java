package com.dragovorn.courier.log;

import com.dragovorn.courier.Courier;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class CourierLogger extends Logger {

    private static final Formatter FORMATTER = new CourierFormatter();

    public CourierLogger() {
        super("Courier", null);
        setLevel(Level.ALL);

        try {
            FileHandler file = new FileHandler(Courier.logDir.getPath() + "/" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "-%g.log", 1 << 28, 8, true);
            file.setFormatter(FORMATTER);
            addHandler(file);

            ConsoleHandler console = new ConsoleHandler();
            console.setFormatter(FORMATTER);
            addHandler(console);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}